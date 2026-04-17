package com.pm.apigateway.config;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
public class LoadBalancerConfig {

    @Bean
    public ServiceInstanceListSupplier userServiceSupplier() {
        return new ServiceInstanceListSupplier() {

            @Override
            public String getServiceId() {
                return "UserServices";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(List.of(
                        new DefaultServiceInstance("user-1", "UserServices", "localhost", 8081, false),
                        new DefaultServiceInstance("user-2", "UserServices", "localhost", 8082, false)
                ));
            }
        };
    }
}