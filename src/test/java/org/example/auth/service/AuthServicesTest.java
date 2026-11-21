package org.example.auth.service;

import org.example.auth.repository.RefreshTokenRepository;
import org.example.auth.repository.RevokedAccessTokenRepository;
import org.example.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AuthServicesTest {
    @Test
    void jwtServiceIssuesParsableTokensWithExpectedIdentityAndLifetime() {
        var jwt = new JwtService();
        var issued = jwt.issueAccessToken("alice");
        var claims = jwt.parse(issued.token());
        assertEquals("alice", claims.getSubject());
        assertEquals(issued.id(), claims.getId());
        assertEquals(900, jwt.accessTokenSeconds());
        assertTrue(issued.expiresAt().isAfter(java.time.Instant.now()));
    }

    @Test
    void authServiceRegistersUserAndIssuesRefreshableTokenPair() {
        UserRepository users = mock(UserRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedTokens = mock(RevokedAccessTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwt = new JwtService();
        when(users.existsById("alice")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded-password");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        var response = new AuthService(users, refreshTokens, revokedTokens, passwordEncoder, jwt).register("alice", "strong-password");

        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals("alice", jwt.parse(response.accessToken()).getSubject());
        verify(users).save(argThat(user -> user.getUsername().equals("alice") && user.getPasswordHash().equals("encoded-password")));
        verify(refreshTokens).save(argThat(token -> token.getUsername().equals("alice") && token.getToken().equals(response.refreshToken())));
    }
}
