package aml.code.screeningservice.controller;

import aml.code.screeningservice.dto.request.LoginRequest;
import aml.code.screeningservice.dto.request.RegisterRequest;
import aml.code.screeningservice.entity.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // FIX 1: register() faqat Long qaytaradi, token emas
    // FIX 2: @PreAuthorize("hasAnyRole('ADMIN')") bor — .with(user()) qo'shildi
    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("testuser_" + System.currentTimeMillis());
        request.setPassword("password123");
        request.setEmail("test@test.com");
        request.setRole(UserRole.OPERATOR);

        mockMvc.perform(post("/auth/register")
                        .with(user("system_admin").roles("ADMIN")) // FIX: @PreAuthorize bypass
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // FIX: register returns Long, just check 200
        // register() returns Long (user ID), "$.token" degan field YO'Q
    }

    // FIX 3: login "accessToken" qaytaradi, "token" emas
    @Test
    void login_Success() throws Exception {
        String uniqueName = "loginuser_" + System.currentTimeMillis();

        // Step 1: register user with mock admin
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(uniqueName);
        registerRequest.setPassword("password123");
        registerRequest.setEmail(uniqueName + "@test.com");
        registerRequest.setRole(UserRole.OPERATOR);

        mockMvc.perform(post("/auth/register")
                        .with(user("system_admin").roles("ADMIN")) // FIX
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Step 2: login and check accessToken
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(uniqueName);
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())   // FIX: "accessToken" not "token"
                .andExpect(jsonPath("$.refreshToken").exists()); // FIX: check refreshToken too
    }

    @Test
    void login_Fail_WrongPassword() throws Exception {
        String uniqueName = "wrongpass_" + System.currentTimeMillis();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(uniqueName);
        registerRequest.setPassword("password123");
        registerRequest.setEmail(uniqueName + "@test.com");
        registerRequest.setRole(UserRole.OPERATOR);

        mockMvc.perform(post("/auth/register")
                .with(user("system_admin").roles("ADMIN")) // FIX
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(uniqueName);
        loginRequest.setPassword("WRONG_PASSWORD");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Bu to'g'ri edi
    }

    @Test
    void login_Fail_UserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent_user_xyz");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Bu to'g'ri edi
    }

    // YANGI TEST: register without token should return 403
    @Test
    void register_Fail_WithoutAdminToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("user_no_auth");
        request.setPassword("password123");
        request.setEmail("noauth@test.com");
        request.setRole(UserRole.OPERATOR);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403 — @PreAuthorize blocks it
    }
}
