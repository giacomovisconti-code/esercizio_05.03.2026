package org.example.apigateway;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GatewayFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RouteValidator validator;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        // Verifico se la rotta è tra le rotte protette
        if (validator.isSecured.test(request)){

            // Verifico se il token è presente nell'Header
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                return onError(exchange, "Token Mancante");
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtils.getClaims(token);
                String role = claims.get("role", String.class);

                String path = request.getURI().getPath();
                HttpMethod method = request.getMethod();

                // USERS
                Mono<Void> userMono = usersFilters(role, path, method, exchange);
                if (userMono != null && !userMono.equals(Mono.empty())) return userMono;

                // PRODUCTS
                Mono<Void> productMono =  productsFilters(role, path, method, exchange);
                if (productMono != null && !productMono.equals(Mono.empty())) return productMono;

                // ORDERS
                Mono<Void> orderMono = ordersFilters(role, path, method, exchange);
                if (orderMono != null && !orderMono.equals(Mono.empty())) return orderMono;

                // INVENTORY
                Mono<Void> inventoryMono = inventoryFilters(role, path, method, exchange);
                if (inventoryMono != null && !inventoryMono.equals(Mono.empty())) return inventoryMono;

            } catch (Exception e) {

                return onError(exchange, "Token non valido");
            }
        }
        return chain.filter(exchange);
    }



    private Mono<Void> onError(ServerWebExchange exchange, String err) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> usersFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {
        if (path.contains("/users/all") && method == HttpMethod.GET) {
            if (!role.equals("ROLE_ADMIN")) {
               return onError(exchange, "Forbidden");
            }
        }
        return Mono.empty();

    }

    private Mono<Void> productsFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {
        if (path.contains("/products") &&
                (method == HttpMethod.POST || method == HttpMethod.PATCH || method == HttpMethod.DELETE)) {
            if (!role.equals("ROLE_ADMIN")) {
                return onError(exchange, "Forbidden");
            }
        }
        return Mono.empty();

    }

    private Mono<Void> ordersFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {
        if (path.contains("/orders") && method == HttpMethod.GET) {
            if (!role.equals("ROLE_ADMIN")) {
                return onError(exchange, "Forbidden");

            }
        }

        if ((path.contains("/orders/changestatuts") ||
                path.contains("/orders/deactivate") ||
                path.contains("/orders/delete")) &&
                method == HttpMethod.PATCH) {

            if (!role.equals("ROLE_ADMIN")) {
                return onError(exchange, "Forbidden");

            }
        }

        if (path.contains("/orders/create") && method == HttpMethod.POST){
            if (!role.equals("ROLE_USER")) return onError(exchange, "Forbidden");
        }

        return Mono.empty();

    }
    private Mono<Void> inventoryFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {
        if (path.contains("/inventory") && !role.equals("ROLE_ADMIN")) {
            return onError(exchange, "Forbidden");
        }

        return Mono.empty();
    }
}
