package com.pm.videoencodingservice.Services;

import com.pm.videoencodingservice.model.EncodingProfile;
import com.pm.videoencodingservice.model.StreamVariant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates the HLS master playlist (master.m3u8) that references
 * all resolution-specific variant playlists for adaptive bitrate streaming.
 */
@Slf4j
@Service
public class PlaylistService {

    /**
     * Generates master.m3u8 inside {@code outputDir}.
     *
     * Format:
     *   #EXTM3U
     *   #EXT-X-STREAM-INF:BANDWIDTH=5192000,RESOLUTION=1920x1080
     *   1080p/playlist.m3u8
     *   ...
     */
    public Path generateMasterPlaylist(List<StreamVariant> variants, Path outputDir) {
        log.info("📋 Generating master playlist with {} variants", variants.size());

        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:3\n");
        sb.append("\n");

        for (StreamVariant variant : variants) {
            sb.append(String.format(
                    "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%s,NAME=\"%s\"\n",
                    variant.getBandwidthBps(),
                    variant.getResolution(),
                    variant.getLabel()
            ));
            sb.append(variant.getLabel()).append("/playlist.m3u8\n");
            sb.append("\n");
        }

        Path masterFile = outputDir.resolve("master.m3u8");
        try {
            Files.writeString(masterFile, sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write master playlist", e);
        }

        log.info("📋 Master playlist written: {}", masterFile);
        return masterFile;
    }

    /**
     * Builds a StreamVariant from an encoding profile and segment count.
     */
    public StreamVariant buildVariant(EncodingProfile profile, int segmentCount, String videoId) {
        String playlistKey = String.format("videos/%s/hls/%s/playlist.m3u8", videoId, profile.getLabel());

        return StreamVariant.builder()
                .label(profile.getLabel())
                .resolution(profile.resolution())
                .bandwidthBps(profile.bandwidthBps())
                .videoBitrateKbps(profile.getVideoBitrateKbps())
                .audioBitrateKbps(profile.getAudioBitrateKbps())
                .playlistS3Key(playlistKey)
                .segmentCount(segmentCount)
                .build();
    }
}
