package org.example.apigateway;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // Elenco api che non necessitano di Jwt
    public  static final List<String> openApiEndopoints = List.of(
            "/api/users/auth/login",
            "/api/users/users/register"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndopoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
