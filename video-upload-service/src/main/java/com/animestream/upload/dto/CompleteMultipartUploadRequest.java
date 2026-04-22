package com.animestream.upload.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CompleteMultipartUploadRequest {

    @NotNull
    private UUID videoId;

    @NotBlank
    private String uploadId;

    @NotEmpty
    private List<@Valid CompletedPartDto> parts;
}
