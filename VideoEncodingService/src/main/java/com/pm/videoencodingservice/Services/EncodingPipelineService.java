package com.pm.videoencodingservice.Services;

import com.pm.videoencodingservice.config.EncodingProperties;
import com.pm.videoencodingservice.model.EncodingProfile;
import com.pm.videoencodingservice.model.StreamVariant;
import com.pm.videoencodingservice.model.Video;
import com.pm.videoencodingservice.model.VideoMetadata;
import com.pm.videoencodingservice.model.VideoUploadedEvent;
import com.pm.videoencodingservice.Repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Orchestrates the full video encoding pipeline:
 *
 *  1. Download raw video from S3
 *  2. Probe metadata via FFprobe
 *  3. Decide target resolutions
 *  4. Transcode + HLS segment (parallel per resolution)
 *  5. Generate master.m3u8
 *  6. Upload HLS output to S3
 *  7. Update MongoDB with PROCESSED status + stream metadata
 *  8. Cleanup temp files
 *
 * Each step is logged. On failure, status is set to FAILED and temp files
 * are cleaned up. The pipeline is idempotent — re-processing an already
 * PROCESSED video is a no-op.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncodingPipelineService {

    private final S3StorageService s3StorageService;
    private final FFmpegService ffmpegService;
    private final PlaylistService playlistService;
    private final VideoRepository videoRepository;
    private final EncodingProperties encodingProps;
    private final ExecutorService encodingExecutor;

    /**
     * Executes the full encoding pipeline for a video-uploaded event.
     * This method is called from EncodingWorker on an async thread.
     *
     * @throws RuntimeException if any step in the pipeline fails
     */
    public void execute(VideoUploadedEvent event) {
        String videoId = event.getVideoId();
        Path workDir = Path.of(encodingProps.getTempDir(), videoId);

        try {
            Files.createDirectories(workDir);
            log.info("═══════════════════════════════════════════════════════════");
            log.info("▶ PIPELINE START | videoId={} s3Key={}", videoId, event.getS3Key());
            log.info("═══════════════════════════════════════════════════════════");

            // ─── STEP 1: Download raw video from S3 ─────────────────────
            Path rawFile = workDir.resolve("raw_video");
            s3StorageService.downloadToFile(event.getS3Key(), rawFile);

            // ─── STEP 2: Probe metadata ─────────────────────────────────
            VideoMetadata metadata = ffmpegService.probe(rawFile);

            // ─── STEP 3: Decide target resolutions ──────────────────────
            List<EncodingProfile> profiles = ResolutionDecisionEngine.decide(metadata);
            log.info("📐 Resolution ladder: {} profiles → {}",
                    profiles.size(),
                    profiles.stream().map(EncodingProfile::getLabel).toList());

            // ─── STEP 4: Transcode + HLS segment (parallel) ────────────
            Path hlsOutputDir = workDir.resolve("hls");
            Files.createDirectories(hlsOutputDir);

            List<StreamVariant> variants = transcodeAllResolutions(
                    rawFile, profiles, hlsOutputDir, videoId
            );

            // ─── STEP 5: Generate master playlist ───────────────────────
            playlistService.generateMasterPlaylist(variants, hlsOutputDir);

            // ─── STEP 6: Upload HLS output to S3 ───────────────────────
            String s3Prefix = "videos/" + videoId + "/hls/";
            int uploadedFiles = s3StorageService.uploadDirectory(hlsOutputDir, s3Prefix);
            log.info("⬆ Uploaded {} HLS files to S3", uploadedFiles);

            // ─── STEP 7: Update MongoDB ─────────────────────────────────
            String masterPlaylistKey = s3Prefix + "master.m3u8";
            updateVideoAsProcessed(videoId, metadata, variants, masterPlaylistKey);

            log.info("═══════════════════════════════════════════════════════════");
            log.info("✅ PIPELINE COMPLETE | videoId={}", videoId);
            log.info("═══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("❌ PIPELINE FAILED | videoId={}", videoId, e);
            markVideoFailed(videoId, e.getMessage());
            throw new RuntimeException("Encoding pipeline failed for videoId=" + videoId, e);

        } finally {
            // ─── STEP 8: Cleanup temp files ─────────────────────────────
            cleanupWorkDir(workDir);
        }
    }

    /**
     * Transcodes all resolutions. If parallel encoding is enabled, runs
     * each resolution as a CompletableFuture on the encoding executor.
     * Otherwise, processes sequentially.
     */
    private List<StreamVariant> transcodeAllResolutions(
            Path rawFile,
            List<EncodingProfile> profiles,
            Path hlsOutputDir,
            String videoId
    ) {
        if (encodingProps.isParallelEncoding()) {
            return transcodeParallel(rawFile, profiles, hlsOutputDir, videoId);
        } else {
            return transcodeSequential(rawFile, profiles, hlsOutputDir, videoId);
        }
    }

    private List<StreamVariant> transcodeParallel(
            Path rawFile, List<EncodingProfile> profiles, Path hlsOutputDir, String videoId
    ) {
        log.info("⚡ Parallel encoding: {} resolutions", profiles.size());

        List<CompletableFuture<StreamVariant>> futures = profiles.stream()
                .map(profile -> CompletableFuture.supplyAsync(() -> {
                    int segmentCount = ffmpegService.transcodeAndSegment(rawFile, profile, hlsOutputDir);
                    return playlistService.buildVariant(profile, segmentCount, videoId);
                }, encodingExecutor))
                .toList();

        // Wait for all to complete; propagate first failure
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<StreamVariant> transcodeSequential(
            Path rawFile, List<EncodingProfile> profiles, Path hlsOutputDir, String videoId
    ) {
        log.info("🔄 Sequential encoding: {} resolutions", profiles.size());

        List<StreamVariant> variants = new ArrayList<>();
        for (EncodingProfile profile : profiles) {
            int segmentCount = ffmpegService.transcodeAndSegment(rawFile, profile, hlsOutputDir);
            variants.add(playlistService.buildVariant(profile, segmentCount, videoId));
        }
        return variants;
    }

    private void updateVideoAsProcessed(
            String videoId,
            VideoMetadata metadata,
            List<StreamVariant> variants,
            String masterPlaylistKey
    ) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus("PROCESSED");
            video.setDuration(metadata.durationSeconds());
            video.setMasterPlaylistUrl(masterPlaylistKey);
            video.setStreams(variants);
            video.setProcessedAt(Instant.now());
            video.setFailureReason(null);
            videoRepository.save(video);
            log.info("💾 MongoDB updated: videoId={} status=PROCESSED streams={}",
                    videoId, variants.size());
        });
    }

    private void markVideoFailed(String videoId, String reason) {
        try {
            videoRepository.findById(videoId).ifPresent(video -> {
                video.setStatus("FAILED");
                video.setFailureReason(reason != null ? reason.substring(0, Math.min(reason.length(), 500)) : "Unknown");
                videoRepository.save(video);
            });
        } catch (Exception e) {
            log.error("Failed to update video status to FAILED for videoId={}", videoId, e);
        }
    }

    private void cleanupWorkDir(Path workDir) {
        try {
            if (Files.exists(workDir)) {
                FileUtils.deleteDirectory(workDir.toFile());
                log.info("🧹 Cleaned up temp directory: {}", workDir);
            }
        } catch (IOException e) {
            log.warn("⚠ Failed to cleanup temp directory: {}", workDir, e);
        }
    }
}
