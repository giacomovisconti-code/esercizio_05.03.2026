package org.example.apigateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    // Product Service
    @Bean
    public RouteLocator productService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/products/**")
                .filters(f->f.stripPrefix(2))
                .uri("lb://PRODUCT-SERVICE")).build();
    }

    // User Service
    @Bean
    public RouteLocator userService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/users/**")
                .filters(f->f.stripPrefix(2))
                .uri("lb://USER-SERVICE")).build();
    }

    // Inventory Service
    @Bean
    public RouteLocator inventoryService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/inventory/**")
                .filters(f->f.stripPrefix(2))
                .uri("lb://INVENTORY-SERVICE")).build();
    }

    // Notification Service
    @Bean
    public RouteLocator notificationsService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/notification/**")
                .uri("lb://NOTIFICATION-SERVICE")).build();
    }

    // Order Service
    @Bean
    public RouteLocator ordersService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/orders/**")
                .filters(f->f.stripPrefix(2))
                .uri("lb://ORDER-SERVICE")).build();
    }

}
