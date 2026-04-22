package com.animestream.upload.service;

import com.animestream.upload.dto.AbortUploadResponse;
import com.animestream.upload.dto.CompleteMultipartUploadRequest;
import com.animestream.upload.dto.CompleteMultipartUploadResponse;
import com.animestream.upload.dto.InitMultipartUploadRequest;
import com.animestream.upload.dto.InitMultipartUploadResponse;

public interface VideoUploadService {

    InitMultipartUploadResponse initializeUpload(InitMultipartUploadRequest request);

    CompleteMultipartUploadResponse completeUpload(CompleteMultipartUploadRequest request);

    AbortUploadResponse abortUpload(String uploadId);
}
