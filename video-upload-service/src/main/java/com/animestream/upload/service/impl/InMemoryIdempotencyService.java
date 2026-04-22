package com.animestream.upload.service.impl;

import com.animestream.upload.service.IdempotencyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(prefix = "idempotency.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryIdempotencyService implements IdempotencyService {

    private final Set<UUID> inProgressCompletions = ConcurrentHashMap.newKeySet();

    @Override
    public boolean acquireCompletionLock(UUID videoId) {
        return inProgressCompletions.add(videoId);
    }

    @Override
    public void releaseCompletionLock(UUID videoId) {
        inProgressCompletions.remove(videoId);
    }
}
