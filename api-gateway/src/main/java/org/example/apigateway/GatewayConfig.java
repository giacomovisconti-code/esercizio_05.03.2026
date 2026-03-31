package org.example.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private AuthenticationFilter filter;

    @Autowired
    private KeyResolver userKeyResolver;

    //? UTILS
    public RedisRateLimiter redisRateLimiter(){
        return new RedisRateLimiter(1,1,1);
    }

    // Product Service
    @Bean
    public RouteLocator productService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/products-service/**")
                .filters(f->f
                        .stripPrefix(2)
                        .filter(filter)
                    )
                .uri("lb://PRODUCT-SERVICE")).build();
    }

    //* Route Limiter ProductService getAllProducts
    @Bean
    public RouteLocator rateLimitGetAllProducts(RouteLocatorBuilder builder){
        return builder.routes().route("products-rate-limiter",p-> p.path("/api/products-service/products/all")
                .filters(f->f
                        .requestRateLimiter(c -> c
                                .setRateLimiter(redisRateLimiter())
                                .setKeyResolver(userKeyResolver))
                        .stripPrefix(2)
                )
                .uri("lb://PRODUCT-SERVICE")).build();
    }

    //* Route Limiter ProductService productSearch
    @Bean
    public RouteLocator rateLimitProductSearch(RouteLocatorBuilder builder){
        return builder.routes().route("product-search-rate-limiter",p-> p.path("/api/products-service/products/search**")
                .filters(f->f
                        .requestRateLimiter(c -> c
                                .setRateLimiter(redisRateLimiter())
                                .setKeyResolver(userKeyResolver))
                        .stripPrefix(2)
                )
                .uri("lb://PRODUCT-SERVICE")).build();
    }

    // User Service
    @Bean
    public RouteLocator userService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/users-service/**")
                .filters(f->f
                        .stripPrefix(2)
                        .filter(filter)
                )
                .uri("lb://USER-SERVICE")).build();
    }

    //* Route Limiter UserService Login
    @Bean
    public RouteLocator rateLimitUserLogin(RouteLocatorBuilder builder){
        return builder.routes().route("login-rate-limiter",p-> p.path("/api/users-service/auth/login")
                .filters(f->f
                        .requestRateLimiter(c -> c
                                .setRateLimiter(redisRateLimiter())
                                .setKeyResolver(userKeyResolver))
                        .stripPrefix(2)
                )
                .uri("lb://USER-SERVICE")).build();
    }

    //* Route Limiter UserService Register
    @Bean
    public RouteLocator rateLimitUserRegistration(RouteLocatorBuilder builder){
        return builder.routes().route("register-rate-limiter",p-> p.path("/api/users-service/users/register")
                .filters(f->f
                        .requestRateLimiter(c -> c
                                .setRateLimiter(redisRateLimiter())
                                .setKeyResolver(userKeyResolver))
                        .stripPrefix(2)
                )
                .uri("lb://USER-SERVICE")).build();
    }

    // Inventory Service
    @Bean
    public RouteLocator inventoryService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/inventory-service/**")
                .filters(f->f
                        .stripPrefix(2)
                        .filter(filter)
                )
                .uri("lb://INVENTORY-SERVICE")).build();
    }

    // Notification Service
    @Bean
    public RouteLocator notificationsService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/notification-service/**")
                .uri("lb://NOTIFICATION-SERVICE")).build();
    }

    // Order Service
    @Bean
    public RouteLocator ordersService(RouteLocatorBuilder builder){
        return builder.routes().route(p-> p.path("/api/orders-service/**")
                .filters(f->f
                        .stripPrefix(2)
                        .filter(filter)
                )
                .uri("lb://ORDER-SERVICE")).build();
    }

}
