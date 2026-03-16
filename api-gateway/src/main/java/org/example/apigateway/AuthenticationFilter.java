package org.example.apigateway;

import com.netflix.spectator.impl.Config;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ObjectInputFilter;
import java.util.List;
import java.util.function.Predicate;

public class AuthenticationFilter implements GatewayFilter {

    @Autowired
    private JwtUtils jwtUtils;

    private RouteValidator validator;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return null;
    }


    public GatewayFilter apply(Config config){
        return (((exchange, chain) -> {

            // Verifico se la rotta è tra le rotte protette
            if (validator.isSecured.test((ServerHttpRequest) exchange.getRequest())) {

                // Verifico se il token è presente nell'Header
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Mancante");
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")){

                    // Mi salvo il token
                    authHeader = authHeader.substring(7);

                    Claims claims = jwtUtils.getClaims(authHeader);
                    List roles = claims.get("roles", List.class);

                    String path = exchange.getRequest().getURI().getPath();

                    // Effettuo il controllo su ruolo e ruoli utenti


                }


            }

            return chain.filter(exchange);
        }));
    }
}
