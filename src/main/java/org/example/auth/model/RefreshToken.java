package org.example.auth.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    private String token;
    private String username;
    private String accessTokenId;
    private Instant expiresAt;

    protected RefreshToken() {
    }

    public RefreshToken(String token, String username, String accessTokenId, Instant expiresAt) {
        this.token = token;
        this.username = username;
        this.accessTokenId = accessTokenId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getAccessTokenId() {
        return accessTokenId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
