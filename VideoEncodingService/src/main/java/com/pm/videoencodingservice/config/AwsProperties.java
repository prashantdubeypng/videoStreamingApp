package com.pm.videoencodingservice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.aws")
public class AwsProperties {

    @NotBlank
    private String region;

    @NotBlank
    private String s3Bucket;
}
