package org.example.apigateway;

import org.example.apigateway.rate_limit.RateLimiterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

@Configuration
public class GatewayConfig {

    @Autowired
    private AuthenticationFilter filter;

    @Autowired
    private KeyResolver userKeyResolver;



    //? UTILS
    // Rate Limiter per la rotta di Login
    @Bean
    public RedisRateLimiter redisRateLimiterLogin(){
        return new RedisRateLimiter(10,10,1);
    }

    // Rate Limiter per la registrazione
    @Bean
    public RedisRateLimiter redisRateLimiterRegistration(){
        return new RedisRateLimiter(1,4,1);
    }

    // Rate Limiter per la get di risorse
    @Bean
    @Primary
    public RedisRateLimiter redisRateLimiterGetResources(){
        return new RedisRateLimiter(40,40,1);
    }


    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder){
        return builder.routes()
                //* Product Service
                // Rate Limiter per lista prodotti (rotta aperta)
                .route("products-rate-limiter",p-> p.path("/api/products-service/products/all")
                .filters(f->f
                        .requestRateLimiter(c -> c
                                .setRateLimiter(redisRateLimiterGetResources())
                                .setKeyResolver(userKeyResolver)
                                .setDenyEmptyKey(true)
                                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS))
                        .stripPrefix(2)
                )
                .uri("lb://PRODUCT-SERVICE"))

                // Rate Limiter per ricerca prodotti (rotta aperta)
                .route("product-search-rate-limiter",p-> p.path("/api/products-service/products/search**")
                        .filters(f->f
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiterGetResources())
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(true)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS))
                                .stripPrefix(2)
                        )
                        .uri("lb://PRODUCT-SERVICE"))

                // Applicazione filtri per autenticazione
                .route(p-> p.path("/api/products-service/**")
                        .filters(f->f
                                .stripPrefix(2)
                                .filter(filter)
                        )
                        .uri("lb://PRODUCT-SERVICE"))

                //* USER SERVICE
                // Rate Limiter per Login utente
                .route("login-rate-limiter", p-> p
                        .path("/api/users-service/auth/login")
                        .filters(f->f
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiterLogin())
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(true)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS))
                                .stripPrefix(2)
                        )
                        .uri("lb://USER-SERVICE"))

                // Rate Limiter per Registrazione utente
                .route("register-rate-limiter",p-> p.path("/api/users-service/users/register")
                        .filters(f->f
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiterRegistration())
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(true)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS))
                                .stripPrefix(2)
                        )
                        .uri("lb://USER-SERVICE"))

                // Applicazione filtri per autenticazione
                .route(p-> p.path("/api/users-service/**")
                        .filters(f->f
                                .stripPrefix(2)
                                .filter(filter)
                        )
                        .uri("lb://USER-SERVICE"))

                //* INVENTORY SERVICE
                .route(p-> p.path("/api/inventory-service/**")
                        .filters(f->f
                                .stripPrefix(2)
                                .filter(filter)
                        )
                        .uri("lb://INVENTORY-SERVICE"))

                //* ORDER SERVICE
                .route(p-> p.path("/api/orders-service/**")
                        .filters(f->f
                                .stripPrefix(2)
                                .filter(filter)
                        )
                        .uri("lb://ORDER-SERVICE"))

                //* NOTIFICATION SERVICE
                .route(p-> p.path("/api/notification-service/**")
                        .filters(f->f
                                .stripPrefix(2)
                                .filter(filter)
                        )
                        .uri("lb://NOTIFICATION-SERVICE"))

                .build();
    }


}
