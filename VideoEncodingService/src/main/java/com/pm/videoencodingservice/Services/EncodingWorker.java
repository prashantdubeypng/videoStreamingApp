package com.pm.videoencodingservice.Services;

import com.pm.videoencodingservice.model.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Async worker invoked by the Kafka consumer on the encoding thread pool.
 * Delegates the actual work to EncodingPipelineService and handles
 * lifecycle concerns: lock release, error logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncodingWorker {

    private final EncodingPipelineService encodingPipelineService;
    private final RedisLockService lockService;

    /**
     * Entry point for async video processing.
     * Called by VideoUploadConsumer via the encoding executor.
     *
     * @param event   the deserialized Kafka event
     * @param lockKey the Redis lock key to release when done
     */
    public void process(VideoUploadedEvent event, String lockKey) {
        String videoId = event.getVideoId();
        log.info("▶ EncodingWorker started | videoId={} lockKey={}", videoId, lockKey);

        try {
            encodingPipelineService.execute(event);
            log.info("✅ EncodingWorker finished | videoId={}", videoId);

        } catch (Exception e) {
            log.error("❌ EncodingWorker failed | videoId={}", videoId, e);
            // Pipeline already marks status=FAILED in MongoDB

        } finally {
            lockService.unlock(lockKey);
            log.debug("🔓 Released lock | key={}", lockKey);
        }
    }
}