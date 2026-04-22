package com.animestream.upload;

import com.animestream.upload.config.AwsProperties;
import com.animestream.upload.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AwsProperties.class, KafkaTopicsProperties.class})
public class VideoUploadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoUploadServiceApplication.class, args);
    }
}
