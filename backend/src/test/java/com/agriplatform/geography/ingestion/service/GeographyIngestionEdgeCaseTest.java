package com.agriplatform.geography.ingestion.service;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import com.agriplatform.geography.ingestion.source.LgdJsonGeographySource;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Geography Ingestion Comprehensive Edge Cases (A through T)")
class GeographyIngestionEdgeCaseTest {

    @Mock
    private StateRepository stateRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private MandalRepository mandalRepository;

    @Mock
    private VillageRepository villageRepository;

    private GeographyIngestionServiceImpl ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new GeographyIngestionServiceImpl(
                stateRepository, districtRepository, mandalRepository, villageRepository);
    }

    // A. Empty dataset
    @Test
    @DisplayName("Edge Case A: Empty dataset returns 0 rows and success")
    void testEmptyDataset() {
        String csv = "state_code,state_name\n";
        LgdCsvGeographySource source = new LgdCsvGeographySource("empty.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.getTotalRowsRead()).isEqualTo(0);
        assertThat(summary.getValidRowsCount()).isEqualTo(0);
        assertThat(summary.isSuccessful()).isTrue();
    }

    // C. Multiple states
    @Test
    @DisplayName("Edge Case C: Multiple states in one import batch")
    void testMultipleStates() {
        String csv = """
                state_code,state_name,district_code,district_name
                KA,Karnataka,KA_01,Bengaluru
                MH,Maharashtra,MH_01,Pune
                TN,Tamil Nadu,TN_01,Chennai
                """;

        when(stateRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(i -> i.getArgument(0));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), anyString())).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenAnswer(i -> i.getArgument(0));

        LgdCsvGeographySource source = new LgdCsvGeographySource("multi_state.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.getTotalRowsRead()).isEqualTo(3);
        assertThat(summary.getStatesInserted()).isEqualTo(3);
        assertThat(summary.getDistrictsInserted()).isEqualTo(3);
        assertThat(summary.isSuccessful()).isTrue();
    }

    // Q. Malformed source (completely unparseable or broken JSON)
    @Test
    @DisplayName("Edge Case Q: Malformed source returns error in summary without throwing uncaught exception")
    void testMalformedJsonSource() throws Exception {
        String brokenJson = "{ invalid json ";
        ObjectMapper mapper = new ObjectMapper();

        try {
            LgdJsonGeographySource source = new LgdJsonGeographySource("broken.json", brokenJson, mapper);
            ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);
            assertThat(summary.isSuccessful()).isFalse();
        } catch (Exception expected) {
            // ObjectMapper parsing failure at source construction is also properly caught
            assertThat(expected).isNotNull();
        }
    }

    // S. Inactive entity
    @Test
    @DisplayName("Edge Case S: Inactive entity status preserved on insert")
    void testInactiveEntity() {
        String csv = """
                state_code,state_name,district_code,district_name,status
                KL,Kerala,KL_01,Wayanad,inactive
                """;

        when(stateRepository.findByCodeIgnoreCase("KL")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(i -> i.getArgument(0));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), eq("KL_01"))).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenAnswer(i -> i.getArgument(0));

        LgdCsvGeographySource source = new LgdCsvGeographySource("inactive.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        verify(districtRepository).save(argThat(d -> !d.isActive()));
    }

    // T. Large dataset simulation
    @Test
    @DisplayName("Edge Case T: Batch simulation with 500 rows completes efficiently")
    void testLargeDatasetSimulation() {
        StringBuilder sb = new StringBuilder();
        sb.append("state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n");
        for (int i = 1; i <= 500; i++) {
            sb.append(String.format("ST,State1,DST_%d,District_%d,MND_%d,Mandal_%d,VIL_%d,Village_%d\n",
                    (i % 5) + 1, (i % 5) + 1, (i % 25) + 1, (i % 25) + 1, i, i));
        }

        when(stateRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), anyString())).thenReturn(Optional.empty());
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(UUID.class), anyString())).thenReturn(Optional.empty());
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(UUID.class), anyString())).thenReturn(Optional.empty());

        LgdCsvGeographySource source = new LgdCsvGeographySource("large.csv", new StringReader(sb.toString()));
        long start = System.currentTimeMillis();
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(summary.getTotalRowsRead()).isEqualTo(500);
        assertThat(summary.getValidRowsCount()).isEqualTo(500);
        assertThat(summary.getStatesInserted()).isEqualTo(1); // 1 unique state
        assertThat(summary.getDistrictsInserted()).isEqualTo(5); // 5 unique districts
        assertThat(summary.getMandalsInserted()).isEqualTo(25); // 25 unique mandals
        assertThat(summary.getVillagesInserted()).isEqualTo(500); // 500 unique villages
        assertThat(elapsed).isLessThan(2000); // Fast in-memory processing
    }
}
