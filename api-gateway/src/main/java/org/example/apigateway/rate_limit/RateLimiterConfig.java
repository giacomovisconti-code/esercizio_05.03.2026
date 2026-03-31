package org.example.apigateway.rate_limit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Controlla se il proxy ha passato l'IP originale dell'utente
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // Prende il primo IP della lista (nel caso di proxy multipli)
                return Mono.just(xForwardedFor.split(",")[0]);
            }

            // Fallback sull'indirizzo remoto diretto
            return Mono.just(Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown"));
        };

    }
}
