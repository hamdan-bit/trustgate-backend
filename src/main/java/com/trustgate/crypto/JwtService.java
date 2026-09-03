package com.trustgate.crypto;

import com.trustgate.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey hmacJwtKey;
    
    // Default 30 minutes, configurable via application.yml
    @Value("${trustgate.jwt.expiration-minutes:30}")
    private long expirationMinutes;

    public JwtService(SecretKey hmacJwtKey) {
        // JJWT requires keys to be at least 256 bits for HS256. 
        // We derive a proper JJWT SecretKey from our loaded keystore key.
        this.hmacJwtKey = Keys.hmacShaKeyFor(hmacJwtKey.getEncoded());
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(hmacJwtKey, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<Claims> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(hmacJwtKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            // Token is invalid, expired, or tampered with
            return Optional.empty();
        }
    }
}
