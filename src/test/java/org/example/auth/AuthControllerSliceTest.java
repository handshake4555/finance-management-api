package org.example.auth;

import org.example.auth.dto.TokenResponse;
import org.example.auth.service.AuthService;
import org.example.auth.service.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(org.example.auth.controller.AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerSliceTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService auth;
    @MockBean JwtAuthenticationFilter jwtFilter;

    @Test void registerValidatesAndDelegatesRequest() throws Exception {
        when(auth.register("alice", "strong-password")).thenReturn(new TokenResponse("access", "refresh", "Bearer", 900));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"strong-password\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.accessToken").value("access"));
        verify(auth).register("alice", "strong-password");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void authenticatedProfileAndLogoutAreMapped() throws Exception {
        mvc.perform(get("/api/auth/me").principal(() -> "alice"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("alice"));
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isNoContent());
        verify(auth).logout("refresh");
    }
}
