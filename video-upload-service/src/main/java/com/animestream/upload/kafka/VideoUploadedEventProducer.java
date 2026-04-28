package com.animestream.upload.kafka;

import com.animestream.upload.config.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadedEventProducer {

    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    public void publish(VideoUploadedEvent event) {

        VideoUploadedEvent enrichedEvent = VideoUploadedEvent.builder()
                .eventId(event.eventId() != null ? event.eventId() : UUID.randomUUID())
                .videoId(event.videoId())
                .s3Key(event.s3Key())
                .userId(event.userId())
                .uploadedAt(event.uploadedAt() != null ? event.uploadedAt() : Instant.now())
                .contentType(event.contentType())
                .traceId(event.traceId() != null ? event.traceId() : UUID.randomUUID().toString())
                .build();

        kafkaTemplate
                .send(topics.getVideoUploaded(), enrichedEvent.videoId().toString(), enrichedEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "❌ Failed to publish video-uploaded event | videoId={} eventId={}",
                                enrichedEvent.videoId(),
                                enrichedEvent.eventId(),
                                ex
                        );
                        return;
                    }

                    log.info(
                            "✅ Published video-uploaded event | videoId={} eventId={} topic={} partition={} offset={}",
                            enrichedEvent.videoId(),
                            enrichedEvent.eventId(),
                            topics.getVideoUploaded(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}