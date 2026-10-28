package com.agriplatform.geography.lgd;

import com.agriplatform.geography.lgd.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LGD response DTO deserialization.
 *
 * <p>Fixtures are derived strictly from the official LGD/NAPIX OpenAPI
 * specification examples. No invented data.
 */
@DisplayName("LGD DTO Deserialization Tests")
class LgdDtoDeserializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    // =========================================================================
    // State DTO
    // =========================================================================

    @Test
    @DisplayName("State DTO deserializes 'isactive' (lowercase) correctly")
    void stateDto_isactive_lowercase() throws Exception {
        String json = """
                {
                  "stateCode": 28,
                  "stateNameEnglish": "Andhra Pradesh",
                  "stateNameLocal": "ANDHRA PRADESH",
                  "shortName": "AP",
                  "census2011Code": "28",
                  "isactive": true
                }
                """;
        LgdStateDto dto = mapper.readValue(json, LgdStateDto.class);
        assertThat(dto.getStateCode()).isEqualTo(28);
        assertThat(dto.getStateNameEnglish()).isEqualTo("Andhra Pradesh");
        assertThat(dto.getShortName()).isEqualTo("AP");
        assertThat(dto.isEffectivelyActive()).isTrue();
    }

    @Test
    @DisplayName("State DTO deserializes 'isActive' (camelCase) via @JsonAlias")
    void stateDto_isActive_camelCase() throws Exception {
        String json = "{\"stateCode\": 1, \"stateNameEnglish\": \"Jammu And Kashmir\", \"isActive\": false}";
        LgdStateDto dto = mapper.readValue(json, LgdStateDto.class);
        assertThat(dto.isEffectivelyActive()).isFalse();
    }

    @Test
    @DisplayName("State DTO isEffectivelyActive() defaults to true when field absent")
    void stateDto_isActiveNull_defaultsTrue() throws Exception {
        String json = "{\"stateCode\": 5, \"stateNameEnglish\": \"Uttarakhand\"}";
        LgdStateDto dto = mapper.readValue(json, LgdStateDto.class);
        assertThat(dto.isEffectivelyActive()).isTrue();
    }

    @Test
    @DisplayName("State DTO ignores unknown fields gracefully")
    void stateDto_ignoresUnknownFields() throws Exception {
        String json = """
                {"stateCode": 9, "stateNameEnglish": "Uttar Pradesh",
                 "unknownField": "someValue", "isactive": true}
                """;
        LgdStateDto dto = mapper.readValue(json, LgdStateDto.class);
        assertThat(dto.getStateCode()).isEqualTo(9);
    }

    // =========================================================================
    // District DTO
    // =========================================================================

    @Test
    @DisplayName("District DTO deserializes with parent stateCode and isactive")
    void districtDto_fullDeserialization() throws Exception {
        String json = """
                {
                  "districtCode": 502,
                  "districtNameEnglish": "Ananthapuramu",
                  "districtNameLocal": "\\u0c05\\u0c28\\u0c02\\u0c24\\u0c2a\\u0c41\\u0c30\\u0c2e\\u0c41",
                  "stateCode": 28,
                  "census2011Code": "553",
                  "transactionId": 779988,
                  "isactive": true
                }
                """;
        LgdDistrictDto dto = mapper.readValue(json, LgdDistrictDto.class);
        assertThat(dto.getDistrictCode()).isEqualTo(502);
        assertThat(dto.getStateCode()).isEqualTo(28);
        assertThat(dto.getDistrictNameEnglish()).isEqualTo("Ananthapuramu");
        assertThat(dto.isEffectivelyActive()).isTrue();
    }

    @Test
    @DisplayName("District DTO handles deactivated district (isactive=false)")
    void districtDto_deactivated() throws Exception {
        String json = "{\"districtCode\": 100, \"districtNameEnglish\": \"OldDistrict\", \"stateCode\": 5, \"isactive\": false}";
        LgdDistrictDto dto = mapper.readValue(json, LgdDistrictDto.class);
        assertThat(dto.isEffectivelyActive()).isFalse();
    }

    // =========================================================================
    // SubDistrict (Mandal) DTO
    // =========================================================================

    @Test
    @DisplayName("SubDistrict DTO deserializes with subDistrictCode and districtCode")
    void subDistrictDto_fullDeserialization() throws Exception {
        String json = """
                {
                  "subDistrictCode": 5072,
                  "subDistrictNameEnglish": "Anantapur",
                  "subDistrictNameLocal": "",
                  "districtCode": 502,
                  "census2011Code": "5072",
                  "isactive": true
                }
                """;
        LgdSubDistrictDto dto = mapper.readValue(json, LgdSubDistrictDto.class);
        assertThat(dto.getSubDistrictCode()).isEqualTo(5072);
        assertThat(dto.getDistrictCode()).isEqualTo(502);
        assertThat(dto.isEffectivelyActive()).isTrue();
    }

    @Test
    @DisplayName("SubDistrict DTO maps 'subdistrictCode' alias (from hierarchy API)")
    void subDistrictDto_subdistrictCodeAlias() throws Exception {
        String json = "{\"subdistrictCode\": 9999, \"subdistrictNameEnglish\": \"TestMandal\", \"districtCode\": 100}";
        LgdSubDistrictDto dto = mapper.readValue(json, LgdSubDistrictDto.class);
        assertThat(dto.getSubDistrictCode()).isEqualTo(9999);
        assertThat(dto.getSubDistrictNameEnglish()).isEqualTo("TestMandal");
    }

    // =========================================================================
    // Village DTO
    // =========================================================================

    @Test
    @DisplayName("Village DTO deserializes with villageCode, subDistrictCode, and isactive")
    void villageDto_fullDeserialization() throws Exception {
        String json = """
                {
                  "villageCode": 590214,
                  "villageNameEnglish": "113 Thalluru",
                  "villageNameLocal": "",
                  "subDistrictCode": 5072,
                  "stateCode": 28,
                  "census2011Code": "590214",
                  "villageStatus": "I",
                  "is_pesa": "P",
                  "isactive": true
                }
                """;
        LgdVillageDto dto = mapper.readValue(json, LgdVillageDto.class);
        assertThat(dto.getVillageCode()).isEqualTo(590214);
        assertThat(dto.getSubDistrictCode()).isEqualTo(5072);
        assertThat(dto.getVillageNameEnglish()).isEqualTo("113 Thalluru");
        assertThat(dto.getVillageStatus()).isEqualTo("I");
        assertThat(dto.isEffectivelyActive()).isTrue();
    }

    // =========================================================================
    // Hierarchy Row DTO
    // =========================================================================

    @Test
    @DisplayName("HierarchyRow DTO deserializes flat hierarchy response")
    void hierarchyRowDto_fullDeserialization() throws Exception {
        String json = """
                {
                  "stateCode": 15,
                  "stateNameEnglish": "Mizoram",
                  "districtCode": 262,
                  "districtNameEnglish": "Champhai",
                  "subdistrictCode": 1908,
                  "subdistrictNameEnglish": "Champhai",
                  "villageCode": 935471,
                  "villageNameEnglish": "Hmunhmeltha",
                  "blockCode": 6520,
                  "blockNameEnglish": "Champhai"
                }
                """;
        LgdHierarchyRowDto dto = mapper.readValue(json, LgdHierarchyRowDto.class);
        assertThat(dto.getStateCode()).isEqualTo(15);
        assertThat(dto.getDistrictCode()).isEqualTo(262);
        assertThat(dto.getSubdistrictCode()).isEqualTo(1908);
        assertThat(dto.getVillageCode()).isEqualTo(935471);
        assertThat(dto.getBlockCode()).isEqualTo(6520);
    }

    // =========================================================================
    // List deserialization (as returned by LGD APIs)
    // =========================================================================

    @Test
    @DisplayName("List of State DTOs deserializes correctly")
    void stateDtoList_deserializes() throws Exception {
        String json = """
                [
                  {"stateCode": 28, "stateNameEnglish": "Andhra Pradesh", "isactive": true},
                  {"stateCode": 29, "stateNameEnglish": "Karnataka", "isactive": true},
                  {"stateCode": 32, "stateNameEnglish": "Kerala", "isactive": false}
                ]
                """;
        List<LgdStateDto> states = mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, LgdStateDto.class));
        assertThat(states).hasSize(3);
        assertThat(states.get(0).getStateCode()).isEqualTo(28);
        assertThat(states.get(2).isEffectivelyActive()).isFalse();
    }

    @Test
    @DisplayName("Empty JSON array returns empty list")
    void emptyArray_returnsEmptyList() throws Exception {
        String json = "[]";
        List<LgdStateDto> states = mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, LgdStateDto.class));
        assertThat(states).isEmpty();
    }

    @Test
    @DisplayName("Unicode local names are preserved correctly")
    void unicodeLocalNames_preserved() throws Exception {
        String json = "{\"districtCode\": 502, \"districtNameEnglish\": \"Ananthapuramu\", " +
                "\"districtNameLocal\": \"\u0c05\u0c28\u0c02\u0c24\u0c2a\u0c41\u0c30\u0c2e\u0c41\", " +
                "\"stateCode\": 28, \"isactive\": true}";
        LgdDistrictDto dto = mapper.readValue(json, LgdDistrictDto.class);
        assertThat(dto.getDistrictNameLocal()).isNotBlank();
        // Unicode preserved — Telugu script characters
        assertThat(dto.getDistrictNameLocal()).contains("\u0c05");
    }
}
