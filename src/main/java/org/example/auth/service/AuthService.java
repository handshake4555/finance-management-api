package org.example.auth.service;

import io.jsonwebtoken.Claims;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.TokenResponse;
import org.example.auth.model.AppUser;
import org.example.auth.model.RefreshToken;
import org.example.auth.model.RevokedAccessToken;
import org.example.auth.repository.RefreshTokenRepository;
import org.example.auth.repository.RevokedAccessTokenRepository;
import org.example.auth.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final long REFRESH_TOKEN_SECONDS = 7L * 24 * 60 * 60;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final RevokedAccessTokenRepository revokedTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       RevokedAccessTokenRepository revokedTokens, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.revokedTokens = revokedTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        AppUser user = users.findById(request.username()).orElseThrow(() -> new BadCredentialsException("Bad Credentials"));
        if (!user.isActive()) throw new DisabledException("Account is inactive");
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        return createTokenPair(user.getUsername());
    }

    @Transactional
    public TokenResponse register(String username, String password) {
        if (users.existsById(username)) throw new IllegalArgumentException("Username is already registered");
        AppUser user = users.save(new AppUser(username, passwordEncoder.encode(password), "USER"));
        return createTokenPair(user.getUsername());
    }

    @Transactional
    public TokenResponse refresh(String suppliedToken) {
        RefreshToken oldToken = refreshTokens.findById(suppliedToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!oldToken.getExpiresAt().isAfter(Instant.now())) {
            throw new BadCredentialsException("Expired refresh token");
        }
        revokeAccess(oldToken.getAccessTokenId());
        refreshTokens.delete(oldToken);
        return createTokenPair(oldToken.getUsername());
    }

    @Transactional
    public void logout(String suppliedToken) {
        refreshTokens.findById(suppliedToken).ifPresent(token -> {
            revokeAccess(token.getAccessTokenId());
            refreshTokens.delete(token);
        });
    }

    private TokenResponse createTokenPair(String username) {
        JwtService.IssuedAccessToken access = jwtService.issueAccessToken(username);
        String refresh = UUID.randomUUID().toString();
        refreshTokens.save(new RefreshToken(refresh, username, access.id(), Instant.now().plusSeconds(REFRESH_TOKEN_SECONDS)));
        return new TokenResponse(access.token(), refresh, "Bearer", jwtService.accessTokenSeconds());
    }

    private void revokeAccess(String tokenId) {
        // The refresh session can only revoke the linked access token until its JWT expiration.
        if (tokenId != null)
            revokedTokens.save(new RevokedAccessToken(tokenId, Instant.now().plusSeconds(jwtService.accessTokenSeconds())));
    }

    public Claims parseAccessToken(String token) {
        return jwtService.parse(token);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = users.findById(username).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        refreshTokens.findAll().stream().filter(t -> t.getUsername().equals(username)).forEach(refreshTokens::delete);
    }

    @Transactional
    public void deactivate(String username) {
        AppUser user = users.findById(username).orElseThrow(() -> new BadCredentialsException("Invalid user"));
        user.setActive(false);
        users.save(user);
        refreshTokens.findAll().stream().filter(t -> t.getUsername().equals(username)).forEach(refreshTokens::delete);
    }
}
