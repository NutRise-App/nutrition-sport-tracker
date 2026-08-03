package com.example.nutritionsporttracker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthorizedForProtectedEndpointWithoutToken()
            throws Exception {

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication required"));
    }

    @Test
    void shouldRejectInvalidRegistrationRequest()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "123",
                                  "fullName": "",
                                  "age": 8,
                                  "weight": -1,
                                  "height": 0,
                                  "gender": "",
                                  "activityLevel": null,
                                  "goal": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectSecondRegistrationWithSameEmail()
            throws Exception {

        String requestBody = """
                {
                  "email": "duplicate@example.com",
                  "password": "Password123",
                  "fullName": "Meral Ateş",
                  "age": 24,
                  "weight": 60.0,
                  "height": 165.0,
                  "gender": "FEMALE",
                  "activityLevel": "MODERATELY_ACTIVE",
                  "goal": "MAINTAIN_WEIGHT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("EMAIL_ALREADY_EXISTS"));
    }
}
