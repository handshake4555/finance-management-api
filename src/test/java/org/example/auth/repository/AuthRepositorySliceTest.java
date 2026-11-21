package org.example.auth.repository;

import org.example.auth.model.AppUser;
import org.example.auth.model.RefreshToken;
import org.example.auth.model.RevokedAccessToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AuthRepositorySliceTest {
    @Autowired
    UserRepository users;
    @Autowired
    RefreshTokenRepository refreshTokens;
    @Autowired
    RevokedAccessTokenRepository revokedTokens;

    @Test
    void persistsAndLooksUpIdentityAndTokenEntities() {
        var user = users.save(new AppUser("alice", "hash", "USER"));
        assertTrue(users.findById("alice").orElseThrow().isActive());
        user.setActive(false);
        assertFalse(users.findById("alice").orElseThrow().isActive());
        var expires = Instant.now().plusSeconds(60);
        var refresh = refreshTokens.save(new RefreshToken("refresh", "alice", "access-id", expires));
        assertEquals("alice", refreshTokens.findById("refresh").orElseThrow().getUsername());
        assertEquals("access-id", refresh.getAccessTokenId());
        revokedTokens.save(new RevokedAccessToken("access-id", expires));
        assertTrue(revokedTokens.existsById("access-id"));
    }
}
