package org.example.apigateway;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GatewayFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RouteValidator validator;

    AntPathMatcher pathMatcher = new AntPathMatcher();



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
            if (!authHeader.startsWith("Bearer ") || authHeader.isBlank()) return onError(exchange, "Token non valido");
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtils.getClaims(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                ServerHttpRequest mutatedReq = exchange.getRequest()
                        .mutate()
                        .headers( h -> {
                                    h.remove("X-User-Id");
                                    h.add("X-User-Id", userId);
                                    h.remove("role");
                                    h.add("role", role);
                        })
                        .build();

                String path = request.getPath().pathWithinApplication().value();
                HttpMethod method = request.getMethod();

                exchange = exchange.mutate().request(mutatedReq).build();

                // USERS
                Mono<Void> userMono = usersFilters(role, path, method, exchange);
                if (!userMono.equals(Mono.empty()) ) return userMono;

                // PRODUCTS
                Mono<Void> productMono =  productsFilters(role, path, method, exchange);
                if (!productMono.equals(Mono.empty()) ) return productMono;

                // ORDERS
                Mono<Void> orderMono = ordersFilters(role, path, method, exchange);
                if (!orderMono.equals(Mono.empty()) ) return orderMono;

                // INVENTORY
                Mono<Void> inventoryMono = inventoryFilters(role, path, method, exchange);
                if (!inventoryMono.equals(Mono.empty()) ) return inventoryMono;

            } catch (Exception e) {

                return onError(exchange, "Token non valido");
            }
        }
        return chain.filter(exchange);
    }



    private Mono<Void> onError(ServerWebExchange exchange, String err) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
            {
              "status": %d,
              "error": "%s"
            }
            """.formatted(HttpStatus.FORBIDDEN.value(), err);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> usersFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {

        if (pathMatcher.match("/users/all", path) && method == HttpMethod.GET) {
            if (!role.equals("ROLE_ADMIN")) {
               return onError(exchange, "Forbidden");
            }
        }

        if(pathMatcher.match("/users/giveAdmin/*", path) && method == HttpMethod.PATCH) {
            if (!role.equals("ROLE_ADMIN")){
                return onError(exchange, "Forbidden");
            }
        }

       return Mono.empty() ;
    }

    private Mono<Void> productsFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {
        if (pathMatcher.match("/products/create", path) &&
                method == HttpMethod.POST) {
            if (!role.equals("ROLE_ADMIN")) {
                return onError(exchange, "Forbidden");
            }
        }

        if (pathMatcher.match("/products/update", path) &&
                method == HttpMethod.PATCH) {
            if (!role.equals("ROLE_ADMIN")) {
                return onError(exchange, "Forbidden");
            }
        }

        if (pathMatcher.match("/products/delete/*", path) &&
                method == HttpMethod.DELETE) {
            if (!role.equals("ROLE_ADMIN")) {
                return onError(exchange, "Forbidden");
            }
        }


        return Mono.empty();

    }

    private Mono<Void> ordersFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {

        if ((pathMatcher.match("/orders/changestatus/*", path) ||
                pathMatcher.match("/orders/deactivate/*", path) ||
                pathMatcher.match("/orders/delete/*", path) ||
                pathMatcher.match("/orders/reactivate/*", path)) &&
                method == HttpMethod.PATCH) {

            if (!role.equals("ROLE_ADMIN")) {
                return onError(exchange, "Forbidden");

            }
        }

        if (pathMatcher.match("/orders/create", path) && method == HttpMethod.POST){
            if (!role.equals("ROLE_USER")) return onError(exchange, "Forbidden");
        }

        if (method == HttpMethod.GET && pathMatcher.match("/orders", path)) {
            if (!role.equals("ROLE_ADMIN")) return onError(exchange, "Forbidden");
        }

        if (method == HttpMethod.GET && pathMatcher.match("/orders/*", path)) {
            if (!role.equals("ROLE_ADMIN") && !role.equals("ROLE_USER")) return onError(exchange, "Forbidden");
        }

        return Mono.empty();

    }
    private Mono<Void> inventoryFilters(String role, String path, HttpMethod method, ServerWebExchange exchange) {
        if (pathMatcher.match("/inventory/**", path) && !role.equals("ROLE_ADMIN")) {
            return onError(exchange, "Forbidden");
        }

        return Mono.empty();
    }
}
