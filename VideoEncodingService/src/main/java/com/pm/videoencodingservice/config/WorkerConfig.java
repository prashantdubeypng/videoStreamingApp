package com.pm.videoencodingservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class WorkerConfig {

    /**
     * Thread pool used for async encoding worker hand-off from Kafka consumer.
     * CallerRunsPolicy provides backpressure when queue is full.
     */
    @Bean
    public ExecutorService encodingExecutor() {
        return new ThreadPoolExecutor(
                4, // core threads
                8, // max threads
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}