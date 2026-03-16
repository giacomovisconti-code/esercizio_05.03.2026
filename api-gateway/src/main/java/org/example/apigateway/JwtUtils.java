package org.example.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtUtils {

    @Value("${JWT_SECRET}")
    private String secret;

    private Key key;

    public JwtUtils(){
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJwt(token).getBody();

    }

    private boolean isExpired(String token){
        return getClaims(token).getExpiration().before(new Date());
    }
}
