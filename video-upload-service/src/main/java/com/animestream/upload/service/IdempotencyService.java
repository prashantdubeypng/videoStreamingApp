package com.animestream.upload.service;

import java.util.UUID;

public interface IdempotencyService {

    boolean acquireCompletionLock(UUID videoId);

    void releaseCompletionLock(UUID videoId);
}
