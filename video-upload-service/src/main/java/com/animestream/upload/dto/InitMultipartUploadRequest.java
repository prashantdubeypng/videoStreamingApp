package com.animestream.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitMultipartUploadRequest {

    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;

    @Positive
    private long fileSize;
}
