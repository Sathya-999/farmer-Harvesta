package com.agriplatform.geography.ingestion.source;

import com.agriplatform.geography.ingestion.model.RawGeographyRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LgdJsonGeographySource Unit Tests")
class LgdJsonGeographySourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should parse JSON array with camelCase and snake_case fields")
    void testJsonArrayParsing() throws Exception {
        String json = """
                [
                  {
                    "stateCode": "TN",
                    "stateName": "Tamil Nadu",
                    "districtCode": "601",
                    "districtName": "Coimbatore",
                    "mandalCode": "6001",
                    "mandalName": "Pollachi",
                    "villageCode": "60001",
                    "villageName": "Anamalai",
                    "latitude": "10.5843",
                    "longitude": "76.9322",
                    "isActive": true
                  }
                ]
                """;

        try (LgdJsonGeographySource source = new LgdJsonGeographySource("test.json", json, objectMapper)) {
            List<RawGeographyRecord> records = new ArrayList<>();
            for (RawGeographyRecord rec : source.readRecords()) {
                records.add(rec);
            }

            assertThat(records).hasSize(1);
            RawGeographyRecord r = records.get(0);
            assertThat(r.getStateCode()).isEqualTo("TN");
            assertThat(r.getStateName()).isEqualTo("Tamil Nadu");
            assertThat(r.getDistrictName()).isEqualTo("Coimbatore");
            assertThat(r.getMandalName()).isEqualTo("Pollachi");
            assertThat(r.getVillageName()).isEqualTo("Anamalai");
            assertThat(r.getLatitude()).isEqualTo("10.5843");
            assertThat(r.getLongitude()).isEqualTo("76.9322");
            assertThat(r.getIsActive()).isTrue();
        }
    }

    @Test
    @DisplayName("Should parse JSON envelope with 'data' array")
    void testJsonEnvelopeParsing() throws Exception {
        String json = """
                {
                  "status": "success",
                  "data": [
                    {
                      "state_code": "KL",
                      "state_name": "Kerala",
                      "district_code": "701",
                      "district_name": "Palakkad",
                      "subdistrict_code": "7001",
                      "subdistrict_name": "Chittur",
                      "village_code": "70001",
                      "village_name": "Kozhinjampara"
                    }
                  ]
                }
                """;

        try (LgdJsonGeographySource source = new LgdJsonGeographySource("envelope.json", json, objectMapper)) {
            List<RawGeographyRecord> records = new ArrayList<>();
            for (RawGeographyRecord rec : source.readRecords()) {
                records.add(rec);
            }

            assertThat(records).hasSize(1);
            RawGeographyRecord r = records.get(0);
            assertThat(r.getStateCode()).isEqualTo("KL");
            assertThat(r.getStateName()).isEqualTo("Kerala");
            assertThat(r.getDistrictName()).isEqualTo("Palakkad");
            assertThat(r.getMandalName()).isEqualTo("Chittur");
            assertThat(r.getVillageName()).isEqualTo("Kozhinjampara");
        }
    }
}
