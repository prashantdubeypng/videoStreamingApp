package com.animestream.upload.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record InitMultipartUploadResponse(
        UUID videoId,
        String uploadId,
        String s3Key,
        List<PresignedPartUrlDto> presignedUrls
) {
}
