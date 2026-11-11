package com.prospectai.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospectai.auth.dto.LoginRequest;
import com.prospectai.auth.dto.RegisterRequest;
import com.prospectai.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("Aditi Sharma", "aditi@fintech.com", "SecurePassword123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("aditi@fintech.com"))
                .andExpect(jsonPath("$.data.name").value("Aditi Sharma"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("Aditi Sharma", "aditi@fintech.com", "SecurePassword123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration attempt
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void shouldRejectRegistrationWithWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("Aditi Sharma", "aditi@fintech.com", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldAuthenticateValidCredentials() throws Exception {
        // Register first
        RegisterRequest registerReq = new RegisterRequest("Aditi Sharma", "aditi@fintech.com", "SecurePassword123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login with correct password
        LoginRequest loginReq = new LoginRequest("aditi@fintech.com", "SecurePassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("aditi@fintech.com"));
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Aditi Sharma", "aditi@fintech.com", "SecurePassword123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login with wrong password
        LoginRequest loginReq = new LoginRequest("aditi@fintech.com", "WrongPassword999!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldAllowAccessToUsersMeWithValidJwt() throws Exception {
        // Register and obtain token
        RegisterRequest registerReq = new RegisterRequest("Aditi Sharma", "aditi@fintech.com", "SecurePassword123!");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).path("data").path("token").asText();

        // Call protected /api/users/me endpoint
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("aditi@fintech.com"))
                .andExpect(jsonPath("$.data.name").value("Aditi Sharma"));
    }

    @Test
    void shouldRejectAccessToUsersMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
