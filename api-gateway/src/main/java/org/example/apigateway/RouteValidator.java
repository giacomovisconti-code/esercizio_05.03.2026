package org.example.apigateway;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    AntPathMatcher pathMatcher = new AntPathMatcher();
    // Elenco api che non necessitano di Jwt
    public final List<String> openApiEndopoints = List.of(
            "/**/auth/login",
            "/**/users/register",
            "/**/products/all",
            "/**/products/search**"
    );


//        ,
//        "/**/products/search**"



    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndopoints
                    .stream()
                    .noneMatch(pattern -> pathMatcher.match(pattern, request.getURI().getPath()));
}
