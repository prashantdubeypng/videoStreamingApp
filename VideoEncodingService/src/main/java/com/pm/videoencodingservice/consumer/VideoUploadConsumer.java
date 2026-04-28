package com.pm.videoencodingservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.videoencodingservice.Repository.VideoRepository;
import com.pm.videoencodingservice.Services.EncodingWorker;
import com.pm.videoencodingservice.Services.RedisLockService;
import com.pm.videoencodingservice.model.Video;
import com.pm.videoencodingservice.model.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Kafka consumer for "video-uploaded" events.
 * Performs distributed locking, idempotency checks, and
 * hands off actual encoding work to an async thread pool.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadConsumer {

    private final ObjectMapper objectMapper;
    private final RedisLockService lockService;
    private final VideoRepository videoRepository;
    private final ExecutorService encodingExecutor;
    private final EncodingWorker worker;

    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    @KafkaListener(
            topics = "video-uploaded",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message, Acknowledgment ack) {

        VideoUploadedEvent event;

        // STEP 1: Deserialize
        try {
            event = objectMapper.readValue(message, VideoUploadedEvent.class);
        } catch (Exception e) {
            log.error("❌ Failed to deserialize Kafka message, skipping", e);
            ack.acknowledge();
            return;
        }

        String videoId = event.getVideoId();
        String lockKey = "video:lock:" + videoId;
        log.info("📩 Received video-uploaded event | videoId={}", videoId);

        // STEP 2: Acquire Redis Lock
        boolean locked = lockService.tryLock(lockKey, LOCK_TTL);
        if (!locked) {
            log.info("⏭ Lock already held for videoId={}, skipping", videoId);
            ack.acknowledge();
            return;
        }

        try {
            // STEP 3: Fetch from MongoDB
            Optional<Video> optionalVideo = videoRepository.findById(videoId);
            if (optionalVideo.isEmpty()) {
                log.warn("⚠ Video not found in MongoDB: videoId={}", videoId);
                ack.acknowledge();
                return;
            }

            Video video = optionalVideo.get();

            // STEP 4: Idempotency — skip if already processed
            if ("PROCESSED".equals(video.getStatus())) {
                log.info("⏭ Video already processed: videoId={}", videoId);
                ack.acknowledge();
                return;
            }

            // STEP 5: Atomic CAS update → PROCESSING
            long updated = videoRepository.updateStatusIfMatches(
                    videoId, "UPLOADED", "PROCESSING", Instant.now()
            );

            if (updated == 0) {
                log.info("⏭ Status transition race lost for videoId={}", videoId);
                ack.acknowledge();
                return;
            }

            // STEP 6: Submit async encoding work
            encodingExecutor.submit(() -> worker.process(event, lockKey));

            // Ack AFTER successful hand-off
            ack.acknowledge();
            log.info("✅ Handed off videoId={} to encoding worker", videoId);

        } catch (Exception e) {
            // Release lock so Kafka retry can re-acquire
            lockService.unlock(lockKey);
            log.error("❌ Consumer failed for videoId={}, NOT acking for retry", videoId, e);
            throw e;
        }
    }
}