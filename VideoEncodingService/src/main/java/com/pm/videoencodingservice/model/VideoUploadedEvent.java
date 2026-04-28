package com.pm.videoencodingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {
    private String videoId;
    private String userId;
    private String s3Key;
    private String contentType;
    private String traceId;
}