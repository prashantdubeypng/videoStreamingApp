package com.pm.videoencodingservice.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single HLS stream variant stored in MongoDB.
 * One per resolution that was successfully encoded.
 */
@Data
@Builder
public class StreamVariant {
    private String label;             // e.g. "1080p"
    private String resolution;        // e.g. "1920x1080"
    private int bandwidthBps;
    private int videoBitrateKbps;
    private int audioBitrateKbps;
    private String playlistS3Key;     // e.g. videos/{id}/hls/1080p/playlist.m3u8
    private int segmentCount;
}
