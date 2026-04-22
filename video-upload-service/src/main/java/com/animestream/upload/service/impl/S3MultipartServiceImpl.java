package com.animestream.upload.service.impl;

import com.animestream.upload.config.AwsProperties;
import com.animestream.upload.dto.CompletedPartDto;
import com.animestream.upload.dto.PresignedPartUrlDto;
import com.animestream.upload.exception.ExternalServiceException;
import com.animestream.upload.service.MultipartUploadInitialization;
import com.animestream.upload.service.S3MultipartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3MultipartServiceImpl implements S3MultipartService {

    private static final int MAX_MULTIPART_PARTS = 10_000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    @Override
    public MultipartUploadInitialization initializeMultipartUpload(String key, String contentType, long fileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize must be greater than 0");
        }

        int totalParts = calculatePartCount(fileSize, awsProperties.getMultipartPartSize());
        if (totalParts > MAX_MULTIPART_PARTS) {
            throw new IllegalArgumentException("File size exceeds maximum supported multipart parts");
        }

        try {
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                    .bucket(awsProperties.getS3Bucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            String uploadId = s3Client.createMultipartUpload(createRequest).uploadId();
            List<PresignedPartUrlDto> presignedUrls = generatePresignedUrls(key, uploadId, totalParts);

            return MultipartUploadInitialization.builder()
                    .uploadId(uploadId)
                    .presignedUrls(presignedUrls)
                    .build();
        } catch (S3Exception ex) {
            throw new ExternalServiceException("Unable to initialize multipart upload in S3", ex);
        }
    }

    @Override
    public void completeMultipartUpload(String key, String uploadId, List<CompletedPartDto> parts) {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("parts must contain at least one uploaded part");
        }

        List<CompletedPart> completedParts = parts.stream()
                .sorted(Comparator.comparingInt(CompletedPartDto::getPartNumber))
                .map(part -> CompletedPart.builder()
                        .partNumber(part.getPartNumber())
                        .eTag(normalizeEtag(part.getEtag()))
                        .build())
                .toList();

        try {
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(awsProperties.getS3Bucket())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build();
            s3Client.completeMultipartUpload(completeRequest);
        } catch (S3Exception ex) {
            throw new ExternalServiceException("Unable to complete multipart upload in S3", ex);
        }
    }

    @Override
    public void abortMultipartUpload(String key, String uploadId) {
        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(awsProperties.getS3Bucket())
                .key(key)
                .uploadId(uploadId)
                .build();
        try {
            s3Client.abortMultipartUpload(abortRequest);
        } catch (NoSuchUploadException ignored) {
            log.info("S3 upload already absent for uploadId={}", uploadId);
        } catch (S3Exception ex) {
            throw new ExternalServiceException("Unable to abort multipart upload in S3", ex);
        }
    }

    @Override
    public boolean verifyObjectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(awsProperties.getS3Bucket())
                    .key(key)
                    .build());
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw new ExternalServiceException("Unable to verify uploaded S3 object", ex);
        }
    }

    private List<PresignedPartUrlDto> generatePresignedUrls(String key, String uploadId, int totalParts) {
        return java.util.stream.IntStream.rangeClosed(1, totalParts)
                .mapToObj(partNumber -> {
                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                            .bucket(awsProperties.getS3Bucket())
                            .key(key)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .build();

                    UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                            .signatureDuration(awsProperties.getPresignTtl())
                            .uploadPartRequest(uploadPartRequest)
                            .build();

                    PresignedUploadPartRequest presignedUploadPartRequest = s3Presigner.presignUploadPart(presignRequest);
                    return PresignedPartUrlDto.builder()
                            .partNumber(partNumber)
                            .url(presignedUploadPartRequest.url().toString())
                            .build();
                })
                .toList();
    }

    private int calculatePartCount(long fileSize, long partSize) {
        return (int) Math.ceil((double) fileSize / partSize);
    }

    private String normalizeEtag(String etag) {
        if (etag == null) {
            return null;
        }
        return etag.replace("\"", "");
    }
}
