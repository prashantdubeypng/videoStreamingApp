package com.animestream.upload.dto;

import lombok.Builder;

@Builder
public record AbortUploadResponse(
        String uploadId,
        String status
) {
}
