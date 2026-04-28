package com.pm.videoencodingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document tracking a video through the encoding lifecycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "videos")
public class Video {

    @Id
    private String id;

    private String userId;
    private String s3Key;               // raw video S3 key
    private String status;              // UPLOADED → PROCESSING → PROCESSED | FAILED

    private double duration;
    private String masterPlaylistUrl;   // videos/{id}/hls/master.m3u8
    private List<StreamVariant> streams;

    private Instant processingStartedAt;
    private Instant processedAt;
    private String failureReason;
}