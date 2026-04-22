package com.animestream.upload.service;

import com.animestream.upload.dto.CompletedPartDto;

import java.util.List;

public interface S3MultipartService {

    MultipartUploadInitialization initializeMultipartUpload(String key, String contentType, long fileSize);

    void completeMultipartUpload(String key, String uploadId, List<CompletedPartDto> parts);

    void abortMultipartUpload(String key, String uploadId);

    boolean verifyObjectExists(String key);
}
