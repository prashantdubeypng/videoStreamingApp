package com.animestream.upload.kafka;

import com.animestream.upload.config.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadedEventProducer {

    private final KafkaTemplate<String, VideoUploadedEvent> videoUploadedEventKafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    public void publish(VideoUploadedEvent event) {
        videoUploadedEventKafkaTemplate.send(kafkaTopicsProperties.getVideoUploaded(), event.videoId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish video-uploaded event for videoId={}", event.videoId(), ex);
                        return;
                    }
                    log.info("Published video-uploaded event for videoId={} to topic={}",
                            event.videoId(),
                            kafkaTopicsProperties.getVideoUploaded());
                });
    }
}
