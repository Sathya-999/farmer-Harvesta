package com.agriplatform.geography.ingestion.source;

import com.agriplatform.geography.ingestion.model.RawGeographyRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LgdCsvGeographySource Unit Tests")
class LgdCsvGeographySourceTest {

    @Test
    @DisplayName("Should parse standard LGD CSV export with header aliases")
    void testStandardLgdCsv() throws Exception {
        String csv = "State Code,State Name,District Code,District Name,Sub District Code,Sub District Name,Village Code,Village Name,Latitude,Longitude,Status\n" +
                "KA,Karnataka,501,Bengaluru,5001,North,50001,Peenya,13.029,77.525,Active\n" +
                "KA,Karnataka,501,Bengaluru,5001,North,50002,Jalahalli,13.054,77.538,Inactive\n";

        try (LgdCsvGeographySource source = new LgdCsvGeographySource("test.csv", new StringReader(csv))) {
            List<RawGeographyRecord> records = new ArrayList<>();
            for (RawGeographyRecord rec : source.readRecords()) {
                records.add(rec);
            }

            assertThat(records).hasSize(2);

            RawGeographyRecord r1 = records.get(0);
            assertThat(r1.getStateCode()).isEqualTo("KA");
            assertThat(r1.getStateName()).isEqualTo("Karnataka");
            assertThat(r1.getDistrictCode()).isEqualTo("501");
            assertThat(r1.getDistrictName()).isEqualTo("Bengaluru");
            assertThat(r1.getMandalCode()).isEqualTo("5001");
            assertThat(r1.getMandalName()).isEqualTo("North");
            assertThat(r1.getVillageCode()).isEqualTo("50001");
            assertThat(r1.getVillageName()).isEqualTo("Peenya");
            assertThat(r1.getLatitude()).isEqualTo("13.029");
            assertThat(r1.getLongitude()).isEqualTo("77.525");
            assertThat(r1.getIsActive()).isTrue();

            RawGeographyRecord r2 = records.get(1);
            assertThat(r2.getVillageName()).isEqualTo("Jalahalli");
            assertThat(r2.getIsActive()).isFalse();
        }
    }

    @Test
    @DisplayName("Should parse snake_case headers")
    void testSnakeCaseHeaders() throws Exception {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n" +
                "MH,Maharashtra,MH_PUN,Pune,MH_PUN_HAV,Haveli,6001,Wagholi\n";

        try (LgdCsvGeographySource source = new LgdCsvGeographySource("snake.csv", new StringReader(csv))) {
            List<RawGeographyRecord> records = new ArrayList<>();
            for (RawGeographyRecord rec : source.readRecords()) {
                records.add(rec);
            }

            assertThat(records).hasSize(1);
            RawGeographyRecord r = records.get(0);
            assertThat(r.getStateName()).isEqualTo("Maharashtra");
            assertThat(r.getDistrictName()).isEqualTo("Pune");
            assertThat(r.getMandalName()).isEqualTo("Haveli");
            assertThat(r.getVillageName()).isEqualTo("Wagholi");
        }
    }
}
