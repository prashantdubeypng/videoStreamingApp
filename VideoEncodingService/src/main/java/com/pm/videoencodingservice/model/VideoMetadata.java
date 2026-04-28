package com.pm.videoencodingservice.model;

import lombok.Builder;

/**
 * Immutable metadata extracted from the raw video via FFprobe.
 */
@Builder
public record VideoMetadata(
        int width,
        int height,
        double durationSeconds,
        String videoCodec,
        String audioCodec,
        long fileSizeBytes
) {

    /**
     * Returns a human-readable resolution label (e.g., "1080p").
     */
    public String resolutionLabel() {
        if (height >= 2160) return "2160p";
        if (height >= 1080) return "1080p";
        if (height >= 720)  return "720p";
        if (height >= 480)  return "480p";
        return height + "p";
    }
}
