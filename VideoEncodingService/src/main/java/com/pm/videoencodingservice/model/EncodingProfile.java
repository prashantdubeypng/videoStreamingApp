package com.pm.videoencodingservice.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Defines a single encoding target with resolution, bitrate ladder,
 * and keyframe interval. Pre-built constants follow industry-standard
 * ABR bitrate ladders (Apple HLS authoring spec).
 */
@Getter
@Builder
public class EncodingProfile {

    private final String label;            // e.g. "1080p"
    private final int width;
    private final int height;
    private final int videoBitrateKbps;
    private final int maxBitrateKbps;
    private final int bufSizeKbps;
    private final int audioBitrateKbps;
    private final int gopSize;             // keyframe interval in frames

    /** Total bandwidth in bits/sec for HLS master playlist. */
    public int bandwidthBps() {
        return (videoBitrateKbps + audioBitrateKbps) * 1000;
    }

    /** Resolution string for master playlist (e.g. "1920x1080"). */
    public String resolution() {
        return width + "x" + height;
    }

    // ─── Industry-standard bitrate ladder ────────────────────────────

    public static final EncodingProfile P_2160 = EncodingProfile.builder()
            .label("2160p").width(3840).height(2160)
            .videoBitrateKbps(14000).maxBitrateKbps(16800).bufSizeKbps(28000)
            .audioBitrateKbps(192).gopSize(120)
            .build();

    public static final EncodingProfile P_1080 = EncodingProfile.builder()
            .label("1080p").width(1920).height(1080)
            .videoBitrateKbps(5000).maxBitrateKbps(6000).bufSizeKbps(10000)
            .audioBitrateKbps(192).gopSize(90)
            .build();

    public static final EncodingProfile P_720 = EncodingProfile.builder()
            .label("720p").width(1280).height(720)
            .videoBitrateKbps(2800).maxBitrateKbps(3360).bufSizeKbps(5600)
            .audioBitrateKbps(128).gopSize(90)
            .build();

    public static final EncodingProfile P_480 = EncodingProfile.builder()
            .label("480p").width(854).height(480)
            .videoBitrateKbps(1400).maxBitrateKbps(1680).bufSizeKbps(2800)
            .audioBitrateKbps(96).gopSize(60)
            .build();
}
