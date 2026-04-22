package com.pm.apigateway;

import com.pm.apigateway.config.UserServiceLoadBalancerConfig;
import com.pm.apigateway.config.VideoUploadServiceLoadBalancerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;

@SpringBootApplication
@LoadBalancerClients({
        @LoadBalancerClient(name = "user-service", configuration = UserServiceLoadBalancerConfig.class),
        @LoadBalancerClient(name = "video-upload-service", configuration = VideoUploadServiceLoadBalancerConfig.class)
})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
