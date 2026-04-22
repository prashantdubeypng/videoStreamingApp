package com.animestream.upload.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.aws")
public class AwsProperties {

    @NotBlank
    private String region;

    @NotBlank
    private String s3Bucket;

    private Duration presignTtl = Duration.ofMinutes(15);

    @Min(5_242_880)
    private long multipartPartSize = 5_242_880;
}
