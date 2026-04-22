package com.animestream.upload.kafka;

import lombok.Builder;

import java.util.UUID;

@Builder
public record VideoUploadedEvent(
        UUID videoId,
        String s3Key,
        String userId
) {
}
