package com.pm.videoencodingservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.encoding")
public class EncodingProperties {

    private String tempDir;
    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private int segmentDuration = 6;
    private boolean parallelEncoding = true;
    private int maxParallelResolutions = 4;
}
