package org.example.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    // Local development key only. Replace with managed key material before deploying outside local use.
    private static final String LOCAL_SIGNING_KEY = "finance-management-local-development-key-32-bytes";
    private static final long ACCESS_TOKEN_SECONDS = 15 * 60;
    private final SecretKey key = Keys.hmacShaKeyFor(LOCAL_SIGNING_KEY.getBytes(StandardCharsets.UTF_8));

    public IssuedAccessToken issueAccessToken(String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ACCESS_TOKEN_SECONDS);
        String id = UUID.randomUUID().toString();
        String token = Jwts.builder().subject(username).id(id).issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt)).signWith(key).compact();
        return new IssuedAccessToken(token, id, expiresAt);
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long accessTokenSeconds() {
        return ACCESS_TOKEN_SECONDS;
    }

    public record IssuedAccessToken(String token, String id, Instant expiresAt) {
    }
}
