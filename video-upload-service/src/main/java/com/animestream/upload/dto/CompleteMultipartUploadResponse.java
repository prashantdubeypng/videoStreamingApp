package com.animestream.upload.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CompleteMultipartUploadResponse(
        UUID videoId,
        String s3Key,
        String status
) {
}
