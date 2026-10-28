package com.agriplatform.geography;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.service.GeographyIngestionService;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Task 05 — Production End-to-End Geography Completion Gate against Supabase")
public class SupabaseProductionIngestionAndVerificationTest {

    @Autowired
    private GeographyIngestionService ingestionService;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private MandalRepository mandalRepository;

    @Autowired
    private VillageRepository villageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("Phase 4: Ingest real official LGD dataset into Supabase")
    void testPhase4RealLgdIngestion() throws Exception {
        ClassPathResource resource = new ClassPathResource("production_lgd_geography.csv");
        assertThat(resource.exists()).isTrue();

        try (InputStream is = resource.getInputStream()) {
            LgdCsvGeographySource source = new LgdCsvGeographySource("production_lgd_geography.csv", is);
            ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

            System.out.println("\n=== PHASE 4 INGESTION SUMMARY ===");
            System.out.println("Successful: " + summary.isSuccessful());
            System.out.println("Total Rows: " + summary.getTotalRowsRead());
            System.out.println("Valid: " + summary.getValidRowsCount());
            System.out.println("Invalid: " + summary.getInvalidRowsCount());
            System.out.println("Duplicate: " + summary.getDuplicateRowsCount());
            System.out.println("States Inserted: " + summary.getStatesInserted());
            System.out.println("Districts Inserted: " + summary.getDistrictsInserted());
            System.out.println("Mandals Inserted: " + summary.getMandalsInserted());
            System.out.println("Villages Inserted: " + summary.getVillagesInserted());

            assertThat(summary.isSuccessful()).isTrue();
            assertThat(summary.getInvalidRowsCount()).isEqualTo(0);
            assertThat(summary.getDuplicateRowsCount()).isEqualTo(0);
            assertThat(summary.getStatesInserted() + summary.getStatesUnchanged()).isEqualTo(36);
            assertThat(summary.getDistrictsInserted() + summary.getDistrictsUnchanged()).isEqualTo(784);
            assertThat(summary.getMandalsInserted() + summary.getMandalsUnchanged()).isGreaterThan(0);
            assertThat(summary.getVillagesInserted() + summary.getVillagesUnchanged()).isGreaterThan(0);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Phase 5: Real Supabase Database Verification (Row counts & Hierarchy)")
    void testPhase5RealDatabaseVerification() {
        long stateCount = stateRepository.count();
        long districtCount = districtRepository.count();
        long mandalCount = mandalRepository.count();
        long villageCount = villageRepository.count();

        System.out.println("\n=== PHASE 5 REAL SUPABASE ROW COUNTS ===");
        System.out.println("States count: " + stateCount);
        System.out.println("Districts count: " + districtCount);
        System.out.println("Mandals count: " + mandalCount);
        System.out.println("Villages count: " + villageCount);

        assertThat(stateCount).isEqualTo(36);
        assertThat(districtCount).isEqualTo(784);
        assertThat(mandalCount).isGreaterThan(0);
        assertThat(villageCount).isGreaterThan(0);

        // 1. Orphan Districts
        Integer orphanDistricts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.districts d LEFT JOIN public.states s ON d.state_id = s.id WHERE s.id IS NULL",
                Integer.class);
        System.out.println("Orphan districts: " + orphanDistricts);
        assertThat(orphanDistricts).isEqualTo(0);

        // 2. Orphan Mandals
        Integer orphanMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m LEFT JOIN public.districts d ON m.district_id = d.id WHERE d.id IS NULL",
                Integer.class);
        System.out.println("Orphan mandals: " + orphanMandals);
        assertThat(orphanMandals).isEqualTo(0);

        // 3. Orphan Villages
        Integer orphanVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v LEFT JOIN public.mandals m ON v.mandal_id = m.id WHERE m.id IS NULL",
                Integer.class);
        System.out.println("Orphan villages: " + orphanVillages);
        assertThat(orphanVillages).isEqualTo(0);

        // 4. Duplicate state codes
        Integer dupStateCodes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (SELECT code FROM public.states GROUP BY code HAVING count(*) > 1) t",
                Integer.class);
        assertThat(dupStateCodes).isEqualTo(0);

        // 5. Active state flags
        Integer inactiveStates = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.states WHERE is_active = false",
                Integer.class);
        System.out.println("Inactive states: " + inactiveStates);
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Phase 8: Idempotency Test (Repeat import of same data produces 0 duplicates)")
    void testPhase8IdempotentRepeatImport() throws Exception {
        long stateCountBefore = stateRepository.count();
        long districtCountBefore = districtRepository.count();
        long mandalCountBefore = mandalRepository.count();
        long villageCountBefore = villageRepository.count();

        ClassPathResource resource = new ClassPathResource("production_lgd_geography.csv");
        try (InputStream is = resource.getInputStream()) {
            LgdCsvGeographySource source = new LgdCsvGeographySource("production_lgd_geography.csv", is);
            ImportSummary summary2 = ingestionService.ingest(source, ImportMode.IMPORT);

            System.out.println("\n=== PHASE 8 IDEMPOTENCY SUMMARY ===");
            System.out.println("Successful: " + summary2.isSuccessful());
            System.out.println("States Inserted: " + summary2.getStatesInserted() + ", Unchanged: " + summary2.getStatesUnchanged());
            System.out.println("Districts Inserted: " + summary2.getDistrictsInserted() + ", Unchanged: " + summary2.getDistrictsUnchanged());
            System.out.println("Mandals Inserted: " + summary2.getMandalsInserted() + ", Unchanged: " + summary2.getMandalsUnchanged());
            System.out.println("Villages Inserted: " + summary2.getVillagesInserted() + ", Unchanged: " + summary2.getVillagesUnchanged());

            assertThat(summary2.isSuccessful()).isTrue();
            assertThat(summary2.getStatesInserted()).isEqualTo(0);
            assertThat(summary2.getDistrictsInserted()).isEqualTo(0);
            assertThat(summary2.getMandalsInserted()).isEqualTo(0);
            assertThat(summary2.getVillagesInserted()).isEqualTo(0);

            assertThat(summary2.getStatesUnchanged()).isEqualTo(stateCountBefore);
            assertThat(summary2.getDistrictsUnchanged()).isEqualTo(districtCountBefore);

            long stateCountAfter = stateRepository.count();
            long districtCountAfter = districtRepository.count();
            long mandalCountAfter = mandalRepository.count();
            long villageCountAfter = villageRepository.count();

            assertThat(stateCountAfter).isEqualTo(stateCountBefore);
            assertThat(districtCountAfter).isEqualTo(districtCountBefore);
            assertThat(mandalCountAfter).isEqualTo(mandalCountBefore);
            assertThat(villageCountAfter).isEqualTo(villageCountBefore);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Phase 6: Real Internal API End-to-End Hierarchy Traversal")
    void testPhase6RealApiTraversal() throws Exception {
        // Step 1: GET /api/v1/geography/states
        MvcResult stateResult = mockMvc.perform(get("/api/v1/geography/states")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode stateJson = objectMapper.readTree(stateResult.getResponse().getContentAsString());
        assertThat(stateJson.get("success").asBoolean()).isTrue();
        JsonNode statesArray = stateJson.get("data");
        assertThat(statesArray.size()).isEqualTo(36);

        // Find a state that has mandals/villages in our dataset (e.g. Andhra Pradesh)
        JsonNode targetState = null;
        for (JsonNode s : statesArray) {
            if ("Andhra Pradesh".equalsIgnoreCase(s.get("name").asText())) {
                targetState = s;
                break;
            }
        }
        if (targetState == null) {
            targetState = statesArray.get(0);
        }

        String stateIdStr = targetState.get("id").asText();
        String stateName = targetState.get("name").asText();
        String stateCode = targetState.get("code").asText();
        UUID realStateId = UUID.fromString(stateIdStr);
        System.out.println("\n=== PHASE 6 REAL API TRAVERSAL ===");
        System.out.println("1. REAL STATE: " + stateName + " [ID: " + realStateId + ", Code: " + stateCode + "]");

        // Step 2: GET /api/v1/geography/states/{realStateId}/districts
        MvcResult districtResult = mockMvc.perform(get("/api/v1/geography/states/{stateId}/districts", realStateId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode districtJson = objectMapper.readTree(districtResult.getResponse().getContentAsString());
        assertThat(districtJson.get("success").asBoolean()).isTrue();
        JsonNode districtsArray = districtJson.get("data");
        assertThat(districtsArray.size()).isGreaterThan(0);

        // Find a district with mandals (e.g. Ananthapuramu)
        JsonNode targetDistrict = null;
        for (JsonNode d : districtsArray) {
            if ("Ananthapuramu".equalsIgnoreCase(d.get("name").asText())) {
                targetDistrict = d;
                break;
            }
        }
        if (targetDistrict == null) {
            targetDistrict = districtsArray.get(0);
        }

        String districtIdStr = targetDistrict.get("id").asText();
        String districtName = targetDistrict.get("name").asText();
        String districtCode = targetDistrict.get("code").asText();
        UUID realDistrictId = UUID.fromString(districtIdStr);
        System.out.println("2. REAL DISTRICT: " + districtName + " [ID: " + realDistrictId + ", Code: " + districtCode + "]");

        // Step 3: GET /api/v1/geography/districts/{realDistrictId}/mandals
        MvcResult mandalResult = mockMvc.perform(get("/api/v1/geography/districts/{districtId}/mandals", realDistrictId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode mandalJson = objectMapper.readTree(mandalResult.getResponse().getContentAsString());
        assertThat(mandalJson.get("success").asBoolean()).isTrue();
        JsonNode mandalsArray = mandalJson.get("data");
        assertThat(mandalsArray.size()).isGreaterThan(0);

        JsonNode targetMandal = mandalsArray.get(0);
        String mandalIdStr = targetMandal.get("id").asText();
        String mandalName = targetMandal.get("name").asText();
        String mandalCode = targetMandal.get("code").asText();
        UUID realMandalId = UUID.fromString(mandalIdStr);
        System.out.println("3. REAL MANDAL: " + mandalName + " [ID: " + realMandalId + ", Code: " + mandalCode + "]");

        // Step 4: GET /api/v1/geography/mandals/{realMandalId}/villages
        MvcResult villageResult = mockMvc.perform(get("/api/v1/geography/mandals/{mandalId}/villages", realMandalId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode villageJson = objectMapper.readTree(villageResult.getResponse().getContentAsString());
        assertThat(villageJson.get("success").asBoolean()).isTrue();
        JsonNode villagesArray = villageJson.get("data");
        assertThat(villagesArray.size()).isGreaterThan(0);

        JsonNode targetVillage = villagesArray.get(0);
        String villageIdStr = targetVillage.get("id").asText();
        String villageName = targetVillage.get("name").asText();
        String villageCode = targetVillage.get("code").asText();
        UUID realVillageId = UUID.fromString(villageIdStr);
        System.out.println("4. REAL VILLAGE: " + villageName + " [ID: " + realVillageId + ", Code: " + villageCode + "]");

        System.out.println("Chain verified 100% End-to-End: State -> District -> Mandal -> Village");
    }

    @Test
    @Order(5)
    @DisplayName("Phase 7: Security Gate Verification")
    void testPhase7SecurityGate() throws Exception {
        System.out.println("\n=== PHASE 7 SECURITY GATE VERIFICATION ===");

        // 1. Public GET is allowed
        mockMvc.perform(get("/api/v1/geography/states"))
                .andExpect(status().isOk());

        // 2. Non-GET on public geography endpoints is blocked
        mockMvc.perform(post("/api/v1/geography/states")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/geography/states/00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/geography/states/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());

        // 3. Admin endpoints blocked without auth
        mockMvc.perform(multipart("/api/v1/admin/geography/import-csv")
                        .file(new MockMultipartFile("file", "test.csv", "text/csv", "header".getBytes())))
                .andExpect(status().isUnauthorized());

        // 4. Admin endpoints blocked for normal farmer JWT (ROLE_authenticated)
        mockMvc.perform(multipart("/api/v1/admin/geography/import-csv")
                        .file(new MockMultipartFile("file", "test.csv", "text/csv", "header".getBytes()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                .andExpect(status().isForbidden());

        System.out.println("Security Gate: Public GET allowed, Writes blocked, Admin endpoints protected.");
    }

    @Test
    @Order(6)
    @DisplayName("Phase 9: Edge Cases Verification")
    void testPhase9EdgeCases() throws Exception {
        System.out.println("\n=== PHASE 9 EDGE CASES VERIFICATION ===");

        // 1. Invalid UUID format returns 400 Bad Request
        mockMvc.perform(get("/api/v1/geography/states/invalid-uuid-string/districts"))
                .andExpect(status().isBadRequest());

        // 2. Nonexistent parent UUID returns 404 Not Found
        mockMvc.perform(get("/api/v1/geography/states/{id}/districts", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/geography/mandals/{id}/villages", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        // 3. Valid parent with no children returns 200 with empty array
        // Find a district with no mandals (e.g. Nicobars or similar in 784 districts)
        List<District> allDistricts = districtRepository.findAll();
        for (District d : allDistricts) {
            if (mandalRepository.findActiveByDistrictId(d.getId()).isEmpty()) {
                MvcResult res = mockMvc.perform(get("/api/v1/geography/districts/{id}/mandals", d.getId()))
                        .andExpect(status().isOk())
                        .andReturn();
                JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
                assertThat(data.isArray()).isTrue();
                assertThat(data.size()).isEqualTo(0);
                break;
            }
        }

        System.out.println("Edge Cases: 400 on malformed UUID, 404 on nonexistent parent, 200 [] on parent with no children.");
    }
}
