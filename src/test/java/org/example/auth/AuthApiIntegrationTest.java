package org.example.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.auth.model.RefreshToken;
import org.example.auth.repository.RefreshTokenRepository;
import org.example.auth.repository.RevokedAccessTokenRepository;
import org.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    RefreshTokenRepository refreshTokens;
    @Autowired
    RevokedAccessTokenRepository revokedTokens;
    @Autowired
    UserRepository users;

    @BeforeEach
    void clearSessions() {
        refreshTokens.deleteAll();
        revokedTokens.deleteAll();
    }

    @Test
    void demoAccountIsSeededAndCanLogin() throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(users.findById("demo").isPresent());
        mvc.perform(login("demo", "demo123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankString())))
                .andExpect(jsonPath("$.refreshToken", not(blankString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void rejectsUnknownUserAndWrongPassword() throws Exception {
        mvc.perform(login("missing", "demo123")).andExpect(status().isUnauthorized());
        mvc.perform(login("demo", "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsBlankLoginFields() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshRotatesRefreshAndAccessTokensAndRejectsReplay() throws Exception {
        JsonNode original = loginAndReadTokens();
        JsonNode rotated = refresh(original.get("refreshToken").asText());
        org.junit.jupiter.api.Assertions.assertNotEquals(original.get("accessToken").asText(), rotated.get("accessToken").asText());
        org.junit.jupiter.api.Assertions.assertNotEquals(original.get("refreshToken").asText(), rotated.get("refreshToken").asText());

        mvc.perform(refreshRequest(original.get("refreshToken").asText())).andExpect(status().isUnauthorized());
        mvc.perform(refreshRequest(rotated.get("refreshToken").asText())).andExpect(status().isOk());
    }

    @Test
    void rejectsUnknownExpiredAndBlankRefreshTokens() throws Exception {
        mvc.perform(refreshRequest("unknown-token")).andExpect(status().isUnauthorized());
        refreshTokens.save(new RefreshToken("expired-token", "demo", "expired-access-id", Instant.now().minusSeconds(1)));
        mvc.perform(refreshRequest("expired-token")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutRevokesRefreshAndItsAccessToken() throws Exception {
        JsonNode pair = loginAndReadTokens();
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", pair.get("refreshToken").asText()))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/private").header("Authorization", "Bearer " + pair.get("accessToken").asText()))
                .andExpect(status().isUnauthorized());
        mvc.perform(refreshRequest(pair.get("refreshToken").asText())).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithUnknownTokenIsIdempotentAndBlankTokenIsRejected() throws Exception {
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"unknown-token\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedRoutesRejectMissingMalformedAndRevokedAccessTokens() throws Exception {
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/private").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());

        JsonNode pair = loginAndReadTokens();
        mvc.perform(get("/api/private").header("Authorization", "Bearer " + pair.get("accessToken").asText()))
                .andExpect(status().isNotFound()); // Authenticated; the sample project has no private controller yet.
    }

    private JsonNode loginAndReadTokens() throws Exception {
        String response = mvc.perform(login("demo", "demo123")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode refresh(String token) throws Exception {
        String response = mvc.perform(refreshRequest(token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(String username, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder refreshRequest(String token) throws Exception {
        return post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", token)));
    }
}
