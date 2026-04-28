package com.pm.videoencodingservice.Repository;

import com.pm.videoencodingservice.model.Video;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface VideoRepository extends MongoRepository<Video, String> {

    /**
     * Atomic conditional update: only transitions status if current status matches.
     * Returns the number of documents modified (0 or 1).
     * This prevents race conditions between multiple encoding workers.
     */
    @Query("{ '_id': ?0, 'status': ?1 }")
    @Update("{ '$set': { 'status': ?2, 'processingStartedAt': ?3 } }")
    long updateStatusIfMatches(
            String videoId,
            String expectedStatus,
            String newStatus,
            Instant startedAt
    );
}