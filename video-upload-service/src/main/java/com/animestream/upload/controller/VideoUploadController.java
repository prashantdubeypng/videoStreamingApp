package com.animestream.upload.controller;

import com.animestream.upload.dto.AbortUploadResponse;
import com.animestream.upload.dto.CompleteMultipartUploadRequest;
import com.animestream.upload.dto.CompleteMultipartUploadResponse;
import com.animestream.upload.dto.InitMultipartUploadRequest;
import com.animestream.upload.dto.InitMultipartUploadResponse;
import com.animestream.upload.service.VideoUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/upload")
public class VideoUploadController {

    private final VideoUploadService videoUploadService;

    @PostMapping("/init")
    public ResponseEntity<InitMultipartUploadResponse> initializeMultipartUpload(
            @Valid @RequestBody InitMultipartUploadRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoUploadService.initializeUpload(request));
    }

    @PostMapping("/complete")
    public ResponseEntity<CompleteMultipartUploadResponse> completeMultipartUpload(
            @Valid @RequestBody CompleteMultipartUploadRequest request
    ) {
        return ResponseEntity.ok(videoUploadService.completeUpload(request));
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<AbortUploadResponse> abortMultipartUpload(@PathVariable String uploadId) {
        return ResponseEntity.ok(videoUploadService.abortUpload(uploadId));
    }
    @GetMapping("/test")
    public ResponseEntity<?> test(){
        return ResponseEntity.ok("working");
    }
}
