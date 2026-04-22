package com.animestream.upload.service;

import com.animestream.upload.dto.PresignedPartUrlDto;
import lombok.Builder;

import java.util.List;

@Builder
public record MultipartUploadInitialization(
        String uploadId,
        List<PresignedPartUrlDto> presignedUrls
) {
}
