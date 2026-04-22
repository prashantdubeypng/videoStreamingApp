package com.animestream.upload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompletedPartDto {

    @Min(1)
    private int partNumber;

    @NotBlank
    private String etag;
}
