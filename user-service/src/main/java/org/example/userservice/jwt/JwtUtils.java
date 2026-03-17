package org.example.userservice.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    private final SecretKey key ;
    private final long expiration;

    JwtUtils(
            @Value("${jwt.expiration.ms}") long expiration,
            @Value("${jwt.secret}") String secret
    ){
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }


    public String generateToken(String userId, String role){

        Map<String, String> claims = Map.of("userId",userId, "role", role);
        // Genero il token
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(claims.get("userId"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }
}
