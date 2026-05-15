package aml.code.screeningservice.controller;

import aml.code.screeningservice.dto.request.BlacklistEntryRequest;
import aml.code.screeningservice.dto.request.RegisterRequest;
import aml.code.screeningservice.entity.enums.ListType;
import aml.code.screeningservice.entity.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BlacklistControllerTest {

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

    private String adminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setName("admin" + System.currentTimeMillis());
        adminRequest.setPassword("admin123");
        adminRequest.setEmail("admin@test.com");
        adminRequest.setRole(UserRole.ADMIN);

        String adminResponse = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andReturn().getResponse().getContentAsString();

        adminToken = objectMapper.readTree(adminResponse).get("token").asText();

        RegisterRequest operatorRequest = new RegisterRequest();
        operatorRequest.setName("operator" + System.currentTimeMillis());
        operatorRequest.setPassword("operator123");
        operatorRequest.setEmail("operator@test.com");
        operatorRequest.setRole(UserRole.OPERATOR);

        String operatorResponse = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorRequest)))
                .andReturn().getResponse().getContentAsString();

        operatorToken = objectMapper.readTree(operatorResponse).get("token").asText();
    }

    @Test
    void create_Success_WithAdminToken() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Иванов Иван");
        request.setBirthDate(LocalDate.of(1980, 1, 1));
        request.setPassportNumber("AB" + System.currentTimeMillis());
        request.setListType(ListType.TERRORIST);

        mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void create_Forbidden_WithOperatorToken() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Петров Петр");
        request.setBirthDate(LocalDate.of(1985, 5, 5));
        request.setPassportNumber("CD" + System.currentTimeMillis());
        request.setListType(ListType.EXTREMIST);

        mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_Unauthorized_WithoutToken() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Сидоров Сидор");
        request.setBirthDate(LocalDate.of(1990, 3, 3));
        request.setPassportNumber("EF" + System.currentTimeMillis());
        request.setListType(ListType.TERRORIST);

        mockMvc.perform(post("/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_Success() throws Exception {
        mockMvc.perform(get("/blacklist")
                        .header("Authorization", "Bearer " + operatorToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void delete_Success_EntryStillExists() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Delete Test");
        request.setBirthDate(LocalDate.of(1975, 7, 7));
        request.setPassportNumber("GH" + System.currentTimeMillis());
        request.setListType(ListType.TERRORIST);

        String createResponse = mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.parseLong(createResponse);

        mockMvc.perform(delete("/blacklist/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/blacklist/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
