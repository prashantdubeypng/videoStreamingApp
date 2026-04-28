package com.animestream.upload.kafka;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record VideoUploadedEvent(
        UUID eventId,
        UUID videoId,
        String s3Key,
        String userId,
        Instant uploadedAt,
        String contentType,
        String traceId
) {
}
