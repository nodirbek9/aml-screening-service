package aml.code.screeningservice.controller;

import aml.code.screeningservice.dto.request.BulkImportRequest;
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
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ImportControllerTest {

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
    void bulkImport_Success() throws Exception {
        List<BlacklistEntryRequest> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BlacklistEntryRequest entry = new BlacklistEntryRequest();
            entry.setFullName("Import Person " + i);
            entry.setBirthDate(LocalDate.of(1980 + i, 1, 1));
            entry.setPassportNumber("IMP" + System.currentTimeMillis() + i);
            entry.setListType(ListType.TERRORIST);
            entries.add(entry);
        }

        BulkImportRequest request = new BulkImportRequest();
        request.setEntries(entries);

        mockMvc.perform(post("/blacklist/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(5))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").value(0));
    }

    @Test
    void bulkImport_Forbidden_WithOperatorToken() throws Exception {
        List<BlacklistEntryRequest> entries = new ArrayList<>();
        BlacklistEntryRequest entry = new BlacklistEntryRequest();
        entry.setFullName("Test Person");
        entry.setBirthDate(LocalDate.of(1990, 1, 1));
        entry.setPassportNumber("TEST" + System.currentTimeMillis());
        entry.setListType(ListType.EXTREMIST);
        entries.add(entry);

        BulkImportRequest request = new BulkImportRequest();
        request.setEntries(entries);

        mockMvc.perform(post("/blacklist/import")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
