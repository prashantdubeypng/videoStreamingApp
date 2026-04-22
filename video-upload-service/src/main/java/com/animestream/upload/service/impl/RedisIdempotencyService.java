package com.animestream.upload.service.impl;

import com.animestream.upload.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "idempotency.redis", name = "enabled", havingValue = "true")
public class RedisIdempotencyService implements IdempotencyService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final String LOCK_PREFIX = "video:complete:lock:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean acquireCompletionLock(UUID videoId) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + videoId, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseCompletionLock(UUID videoId) {
        stringRedisTemplate.delete(LOCK_PREFIX + videoId);
    }
}
