package com.animestream.upload.dto;

import lombok.Builder;

@Builder
public record PresignedPartUrlDto(
        int partNumber,
        String url
) {
}
