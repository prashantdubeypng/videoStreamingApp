package com.pm.videoencodingservice;

import com.pm.videoencodingservice.config.AwsProperties;
import com.pm.videoencodingservice.config.EncodingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AwsProperties.class, EncodingProperties.class})
public class VideoEncodingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoEncodingServiceApplication.class, args);
    }
}
