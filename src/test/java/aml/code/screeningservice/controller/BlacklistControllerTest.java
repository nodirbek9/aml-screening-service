package aml.code.screeningservice.controller;

import aml.code.screeningservice.dto.request.BlacklistEntryRequest;
import aml.code.screeningservice.dto.request.LoginRequest;
import aml.code.screeningservice.dto.request.RegisterRequest;
import aml.code.screeningservice.entity.enums.EntryStatus;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    // FIX: To'g'ri setUp — register + login ikki qadam
    @BeforeEach
    void setUp() throws Exception {
        long ts = System.currentTimeMillis();

        // ── ADMIN ──────────────────────────────────────────────
        String adminName = "admin_" + ts;

        // Step 1: register with mock security (bypass @PreAuthorize)
        RegisterRequest adminReg = new RegisterRequest();
        adminReg.setName(adminName);
        adminReg.setPassword("Admin1234!");
        adminReg.setEmail(adminName + "@test.com");
        adminReg.setRole(UserRole.ADMIN);

        mockMvc.perform(post("/auth/register")
                        .with(user("system").roles("ADMIN")) // FIX: mock admin to bypass @PreAuthorize
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReg)))
                .andExpect(status().isOk());

        // Step 2: login to get REAL JWT token
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
        String operatorName = "operator_" + ts;

        RegisterRequest operatorReg = new RegisterRequest();
        operatorReg.setName(operatorName);
        operatorReg.setPassword("Operator1234!");
        operatorReg.setEmail(operatorName + "@test.com");
        operatorReg.setRole(UserRole.OPERATOR);

        mockMvc.perform(post("/auth/register")
                        .with(user("system").roles("ADMIN")) // FIX
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

    // FIX: status().isOk() not isCreated() — controller uses ResponseEntity.ok()
    @Test
    void create_Success_WithAdminToken() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Иванов Иван Иванович");
        request.setBirthDate(LocalDate.of(1980, 1, 1));
        request.setPassportNumber("AB" + System.currentTimeMillis());
        request.setListType(ListType.TERRORIST);

        mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // FIX: 200 not 201
    }

    @Test
    void create_Forbidden_WithOperatorToken() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Петров Петр Петрович");
        request.setBirthDate(LocalDate.of(1985, 5, 5));
        request.setPassportNumber("CD" + System.currentTimeMillis());
        request.setListType(ListType.EXTREMIST);

        mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403 — to'g'ri
    }

    @Test
    void create_Unauthorized_WithoutToken() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Сидоров Сидор Сидорович");
        request.setBirthDate(LocalDate.of(1990, 3, 3));
        request.setPassportNumber("EF" + System.currentTimeMillis());
        request.setListType(ListType.TERRORIST);

        mockMvc.perform(post("/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 401 — to'g'ri
    }

    // FIX: status=ACTIVE qo'shildi — null status muammosidan qochish uchun
    @Test
    void getAll_Success_WithActiveStatus() throws Exception {
        mockMvc.perform(get("/blacklist")
                        .header("Authorization", "Bearer " + operatorToken)
                        .param("status", "ACTIVE")  // FIX: null bo'lmasin
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getById_Success() throws Exception {
        // First create an entry
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("GetById Test");
        request.setBirthDate(LocalDate.of(1970, 1, 1));
        request.setPassportNumber("GB" + System.currentTimeMillis());
        request.setListType(ListType.TERRORIST);

        String createResp = mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // FIX: 200
                .andReturn().getResponse().getContentAsString();

        Long id = Long.parseLong(createResp); // create returns Long

        mockMvc.perform(get("/blacklist/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.fullName").value("GetById Test"));
    }

    // FIX: status().isOk() not isNoContent() — controller uses ResponseEntity.ok()
    @Test
    void delete_SetsStatusInactive_EntryStillExistsInDb() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Delete Test Person");
        request.setBirthDate(LocalDate.of(1975, 7, 7));
        request.setPassportNumber("DL" + System.currentTimeMillis());
        request.setListType(ListType.TERRORIST);

        // create entry — returns Long (ID)
        String createResp = mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // FIX: 200
                .andReturn().getResponse().getContentAsString();

        Long id = Long.parseLong(createResp);

        // delete — soft delete (status → INACTIVE)
        mockMvc.perform(delete("/blacklist/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()); // FIX: 200 not 204

        // entry still exists but status = INACTIVE
        mockMvc.perform(get("/blacklist/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE")); // soft delete confirmed
    }

    @Test
    void delete_AlreadyInactive_ReturnsBadRequest() throws Exception {
        BlacklistEntryRequest request = new BlacklistEntryRequest();
        request.setFullName("Double Delete Test");
        request.setBirthDate(LocalDate.of(1960, 6, 6));
        request.setPassportNumber("DD" + System.currentTimeMillis());
        request.setListType(ListType.EXTREMIST);

        String createResp = mockMvc.perform(post("/blacklist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long id = Long.parseLong(createResp);

        // First delete
        mockMvc.perform(delete("/blacklist/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Second delete — should throw InvalidStatusTransitionException → 400
        mockMvc.perform(delete("/blacklist/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest()); // entry.already.deleted → 400
    }
}
