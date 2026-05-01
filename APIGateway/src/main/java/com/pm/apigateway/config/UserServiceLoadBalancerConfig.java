package com.pm.apigateway.config;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.List;

public class UserServiceLoadBalancerConfig {

    @Bean
    public ServiceInstanceListSupplier userServiceInstanceSupplier() {
        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "user-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(List.of(
                        new DefaultServiceInstance("user-1", "user-service", "user-service", 8081, false),
                        new DefaultServiceInstance("user-2", "user-service", "user-service", 8082, false)
                ));
            }
        };
    }
}
