package com.animestream.upload.service.impl;

import com.animestream.upload.dto.AbortUploadResponse;
import com.animestream.upload.dto.CompleteMultipartUploadRequest;
import com.animestream.upload.dto.CompleteMultipartUploadResponse;
import com.animestream.upload.dto.InitMultipartUploadRequest;
import com.animestream.upload.dto.InitMultipartUploadResponse;
import com.animestream.upload.entity.UploadStatus;
import com.animestream.upload.entity.VideoUpload;
import com.animestream.upload.exception.ConflictException;
import com.animestream.upload.exception.ExternalServiceException;
import com.animestream.upload.exception.ForbiddenException;
import com.animestream.upload.exception.ResourceNotFoundException;
import com.animestream.upload.kafka.VideoUploadedEvent;
import com.animestream.upload.kafka.VideoUploadedEventProducer;
import com.animestream.upload.repository.VideoUploadRepository;
import com.animestream.upload.service.CurrentUserProvider;
import com.animestream.upload.service.IdempotencyService;
import com.animestream.upload.service.MultipartUploadInitialization;
import com.animestream.upload.service.S3MultipartService;
import com.animestream.upload.service.VideoUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadServiceImpl implements VideoUploadService {

    private final VideoUploadRepository videoUploadRepository;
    private final S3MultipartService s3MultipartService;
    private final CurrentUserProvider currentUserProvider;
    private final IdempotencyService idempotencyService;
    private final VideoUploadedEventProducer videoUploadedEventProducer;

    @Override
    @Transactional
    public InitMultipartUploadResponse initializeUpload(InitMultipartUploadRequest request) {
        String userId = currentUserProvider.getCurrentUserId();
        UUID videoId = UUID.randomUUID();
        String sanitizedFileName = sanitizeFileName(request.getFileName());
        String s3Key = "videos/" + videoId + "/raw/" + sanitizedFileName;

        MultipartUploadInitialization initialization = s3MultipartService.initializeMultipartUpload(
                s3Key,
                request.getContentType(),
                request.getFileSize()
        );

        VideoUpload upload = VideoUpload.builder()
                .id(videoId)
                .userId(userId)
                .uploadId(initialization.uploadId())
                .s3Key(s3Key)
                .status(UploadStatus.UPLOADING)
                .build();
        videoUploadRepository.save(upload);

        log.info("Initialized multipart upload videoId={}, uploadId={}, userId={}",
                videoId, initialization.uploadId(), userId);

        return InitMultipartUploadResponse.builder()
                .videoId(videoId)
                .uploadId(initialization.uploadId())
                .s3Key(s3Key)
                .presignedUrls(initialization.presignedUrls())
                .build();
    }

    @Override
    @Transactional
    public CompleteMultipartUploadResponse completeUpload(CompleteMultipartUploadRequest request) {
        String userId = currentUserProvider.getCurrentUserId();
        VideoUpload existingUpload = videoUploadRepository.findById(request.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("Upload not found for videoId=" + request.getVideoId()));

        validateOwnership(existingUpload, userId);
        if (!existingUpload.getUploadId().equals(request.getUploadId())) {
            throw new ResourceNotFoundException("Upload not found for provided uploadId");
        }

        if (existingUpload.getStatus() == UploadStatus.COMPLETED) {
            return buildCompleteResponse(existingUpload);
        }

        boolean lockAcquired = idempotencyService.acquireCompletionLock(request.getVideoId());
        if (!lockAcquired) {
            throw new ConflictException("Completion already in progress for videoId=" + request.getVideoId());
        }

        try {
            VideoUpload upload = videoUploadRepository.findForUpdate(request.getVideoId(), request.getUploadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Upload not found for completion"));
            validateOwnership(upload, userId);

            if (upload.getStatus() == UploadStatus.COMPLETED) {
                return buildCompleteResponse(upload);
            }

            s3MultipartService.completeMultipartUpload(upload.getS3Key(), upload.getUploadId(), request.getParts());
            if (!s3MultipartService.verifyObjectExists(upload.getS3Key())) {
                throw new ExternalServiceException("S3 object verification failed after completion");
            }

            upload.setStatus(UploadStatus.COMPLETED);
            videoUploadRepository.save(upload);

            videoUploadedEventProducer.publish(VideoUploadedEvent.builder()
                    .videoId(upload.getId())
                    .s3Key(upload.getS3Key())
                    .userId(upload.getUserId())
                    .build());

            log.info("Completed multipart upload videoId={}, uploadId={}, userId={}",
                    upload.getId(), upload.getUploadId(), upload.getUserId());

            return buildCompleteResponse(upload);
        } finally {
            idempotencyService.releaseCompletionLock(request.getVideoId());
        }
    }

    @Override
    @Transactional
    public AbortUploadResponse abortUpload(String uploadId) {
        String userId = currentUserProvider.getCurrentUserId();
        VideoUpload upload = videoUploadRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload not found for uploadId=" + uploadId));

        validateOwnership(upload, userId);
        if (upload.getStatus() == UploadStatus.COMPLETED) {
            throw new ConflictException("Cannot abort an already completed upload");
        }

        s3MultipartService.abortMultipartUpload(upload.getS3Key(), upload.getUploadId());
        upload.setStatus(UploadStatus.FAILED);
        videoUploadRepository.save(upload);

        log.info("Aborted multipart upload videoId={}, uploadId={}, userId={}",
                upload.getId(), upload.getUploadId(), upload.getUserId());

        return AbortUploadResponse.builder()
                .uploadId(uploadId)
                .status(upload.getStatus().name())
                .build();
    }

    private CompleteMultipartUploadResponse buildCompleteResponse(VideoUpload upload) {
        return CompleteMultipartUploadResponse.builder()
                .videoId(upload.getId())
                .s3Key(upload.getS3Key())
                .status(upload.getStatus().name())
                .build();
    }

    private void validateOwnership(VideoUpload upload, String currentUserId) {
        if (!upload.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("Upload does not belong to current user");
        }
    }

    private String sanitizeFileName(String originalFileName) {
        String trimmed = originalFileName == null ? "" : originalFileName.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        return trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
