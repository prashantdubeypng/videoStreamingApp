package com.pm.videoencodingservice.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.videoencodingservice.config.EncodingProperties;
import com.pm.videoencodingservice.model.EncodingProfile;
import com.pm.videoencodingservice.model.VideoMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates all FFmpeg/FFprobe interactions.
 * Shells out via ProcessBuilder — never loads video bytes into JVM memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegService {

    private static final long PROBE_TIMEOUT_SECONDS = 60;
    private static final long TRANSCODE_TIMEOUT_HOURS = 4;

    private final EncodingProperties props;
    private final ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────────
    // PROBE — Extract metadata via FFprobe
    // ──────────────────────────────────────────────────────────────────

    /**
     * Runs FFprobe on the raw video file and returns structured metadata.
     *
     * Command: ffprobe -v quiet -print_format json -show_format -show_streams {input}
     */
    public VideoMetadata probe(Path videoFile) {
        log.info("🔍 Probing video metadata: {}", videoFile);

        List<String> command = List.of(
                props.getFfprobePath(),
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                videoFile.toAbsolutePath().toString()
        );

        String jsonOutput = executeProcess(command, PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            JsonNode root = objectMapper.readTree(jsonOutput);
            JsonNode videoStream = findVideoStream(root.get("streams"));
            JsonNode format = root.get("format");

            int width = videoStream.get("width").asInt();
            int height = videoStream.get("height").asInt();
            double duration = format.has("duration")
                    ? format.get("duration").asDouble()
                    : videoStream.path("duration").asDouble(0.0);
            String videoCodec = videoStream.get("codec_name").asText();

            // Find audio codec (may not exist)
            String audioCodec = "none";
            JsonNode audioStream = findAudioStream(root.get("streams"));
            if (audioStream != null) {
                audioCodec = audioStream.get("codec_name").asText();
            }

            long fileSize = format.has("size") ? format.get("size").asLong() : Files.size(videoFile);

            VideoMetadata metadata = VideoMetadata.builder()
                    .width(width)
                    .height(height)
                    .durationSeconds(duration)
                    .videoCodec(videoCodec)
                    .audioCodec(audioCodec)
                    .fileSizeBytes(fileSize)
                    .build();

            log.info("🔍 Probe result: {}x{}, {:.1f}s, codec={}", width, height, duration, videoCodec);
            return metadata;

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse FFprobe output", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // TRANSCODE + HLS SEGMENT — Single-pass per resolution
    // ──────────────────────────────────────────────────────────────────

    /**
     * Transcodes the source video to a specific resolution and directly
     * outputs HLS segments + playlist in one FFmpeg pass.
     *
     * Output structure:
     *   {outputDir}/{label}/playlist.m3u8
     *   {outputDir}/{label}/segment_000.ts, segment_001.ts, ...
     *
     * @return number of .ts segments generated
     */
    public int transcodeAndSegment(Path sourceFile, EncodingProfile profile, Path outputDir) {
        String label = profile.getLabel();
        Path resolutionDir = outputDir.resolve(label);

        try {
            Files.createDirectories(resolutionDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output directory: " + resolutionDir, e);
        }

        Path playlistPath = resolutionDir.resolve("playlist.m3u8");
        Path segmentPattern = resolutionDir.resolve("segment_%03d.ts");

        log.info("🎬 Transcoding to {} ({}x{}, {}kbps)", label,
                profile.getWidth(), profile.getHeight(), profile.getVideoBitrateKbps());

        List<String> command = List.of(
                props.getFfmpegPath(),
                "-i", sourceFile.toAbsolutePath().toString(),
                "-vf", "scale=-2:" + profile.getHeight(),
                "-c:v", "libx264",
                "-preset", "medium",
                "-b:v", profile.getVideoBitrateKbps() + "k",
                "-maxrate", profile.getMaxBitrateKbps() + "k",
                "-bufsize", profile.getBufSizeKbps() + "k",
                "-g", String.valueOf(profile.getGopSize()),
                "-keyint_min", String.valueOf(profile.getGopSize()),
                "-sc_threshold", "0",
                "-c:a", "aac",
                "-b:a", profile.getAudioBitrateKbps() + "k",
                "-ar", "48000",
                "-f", "hls",
                "-hls_time", String.valueOf(props.getSegmentDuration()),
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern.toAbsolutePath().toString(),
                playlistPath.toAbsolutePath().toString()
        );

        executeProcess(command, TRANSCODE_TIMEOUT_HOURS, TimeUnit.HOURS);

        // Count generated segments
        int segmentCount = countFiles(resolutionDir, ".ts");
        log.info("🎬 Transcode complete: {} → {} segments", label, segmentCount);

        return segmentCount;
    }

    // ──────────────────────────────────────────────────────────────────
    // Internals
    // ──────────────────────────────────────────────────────────────────

    private String executeProcess(List<String> command, long timeout, TimeUnit unit) {
        log.debug("Executing: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());

            boolean finished = process.waitFor(timeout, unit);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Process timed out after " + timeout + " " + unit);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Process failed (exit={}): {}", exitCode, output);
                throw new RuntimeException("FFmpeg/FFprobe exited with code " + exitCode + ": " + output);
            }

            return output;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process execution failed", e);
        }
    }

    private JsonNode findVideoStream(JsonNode streams) {
        if (streams == null) throw new RuntimeException("No streams found in FFprobe output");
        for (JsonNode s : streams) {
            if ("video".equals(s.path("codec_type").asText())) return s;
        }
        throw new RuntimeException("No video stream found in file");
    }

    private JsonNode findAudioStream(JsonNode streams) {
        if (streams == null) return null;
        for (JsonNode s : streams) {
            if ("audio".equals(s.path("codec_type").asText())) return s;
        }
        return null;
    }

    private int countFiles(Path dir, String extension) {
        try (var stream = Files.list(dir)) {
            return (int) stream.filter(f -> f.toString().endsWith(extension)).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
