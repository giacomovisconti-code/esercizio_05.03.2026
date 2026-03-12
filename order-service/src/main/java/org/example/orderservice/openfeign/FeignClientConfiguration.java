package org.example.orderservice.openfeign;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {
    @Bean
    public Client feignClient() {
        return new ApacheHttpClient();
    }
}
