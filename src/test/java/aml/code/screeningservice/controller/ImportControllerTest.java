package aml.code.screeningservice.controller;

import aml.code.screeningservice.dto.request.BlacklistEntryRequest;
import aml.code.screeningservice.dto.request.BulkImportRequest;
import aml.code.screeningservice.dto.request.LoginRequest;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    // FIX: Same setup pattern as BlacklistControllerTest
    @BeforeEach
    void setUp() throws Exception {
        long ts = System.currentTimeMillis();

        // ── ADMIN ──────────────────────────────────────────────
        String adminName = "admin_import_" + ts;

        RegisterRequest adminReg = new RegisterRequest();
        adminReg.setName(adminName);
        adminReg.setPassword("Admin1234!");
        adminReg.setEmail(adminName + "@test.com");
        adminReg.setRole(UserRole.ADMIN);

        // FIX: .with(user()) to bypass @PreAuthorize
        mockMvc.perform(post("/auth/register")
                        .with(user("system").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReg)))
                .andExpect(status().isOk());

        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setUsername(adminName);
        adminLogin.setPassword("Admin1234!");

        String adminLoginResp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // FIX: "accessToken" not "token"
        adminToken = objectMapper.readTree(adminLoginResp).get("accessToken").asText();

        // ── OPERATOR ───────────────────────────────────────────
        String operatorName = "operator_import_" + ts;

        RegisterRequest operatorReg = new RegisterRequest();
        operatorReg.setName(operatorName);
        operatorReg.setPassword("Operator1234!");
        operatorReg.setEmail(operatorName + "@test.com");
        operatorReg.setRole(UserRole.OPERATOR);

        mockMvc.perform(post("/auth/register")
                        .with(user("system").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorReg)))
                .andExpect(status().isOk());

        LoginRequest operatorLogin = new LoginRequest();
        operatorLogin.setUsername(operatorName);
        operatorLogin.setPassword("Operator1234!");

        String operatorLoginResp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // FIX: "accessToken" not "token"
        operatorToken = objectMapper.readTree(operatorLoginResp).get("accessToken").asText();
    }

    @Test
    void bulkImport_Success_AllImported() throws Exception {
        List<BlacklistEntryRequest> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BlacklistEntryRequest entry = new BlacklistEntryRequest();
            entry.setFullName("Import Person " + i + "_" + System.currentTimeMillis());
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
                .andExpect(status().isCreated()) // ImportController uses HttpStatus.CREATED → 201 ✓
                .andExpect(jsonPath("$.imported").value(5))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").value(0));
    }

    @Test
    void bulkImport_Forbidden_WithOperatorToken() throws Exception {
        List<BlacklistEntryRequest> entries = new ArrayList<>();
        BlacklistEntryRequest entry = new BlacklistEntryRequest();
        entry.setFullName("Operator Test Person");
        entry.setBirthDate(LocalDate.of(1990, 1, 1));
        entry.setPassportNumber("OPTEST" + System.currentTimeMillis());
        entry.setListType(ListType.EXTREMIST);
        entries.add(entry);

        BulkImportRequest request = new BulkImportRequest();
        request.setEntries(entries);

        mockMvc.perform(post("/blacklist/import")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403 — to'g'ri
    }

    @Test
    void bulkImport_SkipsDuplicates() throws Exception {
        String passportNumber = "DUP" + System.currentTimeMillis();

        // First import — creates the entry
        List<BlacklistEntryRequest> firstBatch = List.of(createEntry("Duplicate Person", passportNumber));
        BulkImportRequest firstRequest = new BulkImportRequest();
        firstRequest.setEntries(firstBatch);

        mockMvc.perform(post("/blacklist/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(1));

        // Second import — same passport → should be skipped
        List<BlacklistEntryRequest> secondBatch = List.of(
                createEntry("Duplicate Person 2", passportNumber),  // same passport → skip
                createEntry("New Person", "NEW" + System.currentTimeMillis()) // new → import
        );
        BulkImportRequest secondRequest = new BulkImportRequest();
        secondRequest.setEntries(secondBatch);

        mockMvc.perform(post("/blacklist/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    void bulkImport_Unauthorized_WithoutToken() throws Exception {
        BulkImportRequest request = new BulkImportRequest();
        request.setEntries(List.of(createEntry("Test", "TP" + System.currentTimeMillis())));

        mockMvc.perform(post("/blacklist/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 401
    }

    // Helper method
    private BlacklistEntryRequest createEntry(String fullName, String passport) {
        BlacklistEntryRequest entry = new BlacklistEntryRequest();
        entry.setFullName(fullName);
        entry.setBirthDate(LocalDate.of(1980, 1, 1));
        entry.setPassportNumber(passport);
        entry.setListType(ListType.TERRORIST);
        return entry;
    }
}
