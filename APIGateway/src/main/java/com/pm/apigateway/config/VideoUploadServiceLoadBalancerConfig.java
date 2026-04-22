package com.pm.apigateway.config;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.List;

public class VideoUploadServiceLoadBalancerConfig {

    @Bean
    public ServiceInstanceListSupplier videoUploadServiceInstanceSupplier() {
        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "video-upload-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(List.of(
                        new DefaultServiceInstance("upload-1", "video-upload-service", "localhost", 8083, false)
                ));
            }
        };
    }
}
