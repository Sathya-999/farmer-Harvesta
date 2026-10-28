package com.agriplatform.geography.ingestion.validation;

import com.agriplatform.geography.ingestion.model.RawGeographyRecord;
import com.agriplatform.geography.ingestion.model.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeographyValidator Unit Tests")
class GeographyValidatorTest {

    private GeographyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GeographyValidator();
        validator.reset();
    }

    @Test
    @DisplayName("Should accept valid full hierarchy record")
    void testValidHierarchy() {
        RawGeographyRecord record = new RawGeographyRecord();
        record.setLineNumber(2);
        record.setStateCode("KA");
        record.setStateName("Karnataka");
        record.setDistrictCode("BLR");
        record.setDistrictName("Bengaluru Urban");
        record.setMandalCode("BLRN");
        record.setMandalName("Bengaluru North");
        record.setVillageCode("YEL");
        record.setVillageName("Yelahanka");
        record.setLatitude("13.1007");
        record.setLongitude("77.5963");

        List<ValidationError> errors = validator.validateRecord(record);
        assertThat(errors).isEmpty();

        boolean isDup = validator.checkAndTrackDuplicates(record, errors);
        assertThat(isDup).isFalse();
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should reject blank state name and missing state code")
    void testMissingStateFields() {
        RawGeographyRecord record = new RawGeographyRecord();
        record.setLineNumber(2);
        record.setStateName("   ");
        record.setStateCode("");

        List<ValidationError> errors = validator.validateRecord(record);
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.field().equals("name") && e.level().equals("STATE"));
        assertThat(errors).anyMatch(e -> e.field().equals("code") && e.level().equals("STATE"));
    }

    @Test
    @DisplayName("Should reject district without parent state")
    void testDistrictWithoutState() {
        RawGeographyRecord record = new RawGeographyRecord();
        record.setLineNumber(2);
        record.setDistrictName("Mysuru");

        List<ValidationError> errors = validator.validateRecord(record);
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.level().equals("STATE"));
    }

    @Test
    @DisplayName("Should reject mandal without parent district")
    void testMandalWithoutDistrict() {
        RawGeographyRecord record = new RawGeographyRecord();
        record.setLineNumber(2);
        record.setStateCode("KA");
        record.setStateName("Karnataka");
        record.setMandalName("Hunsur");

        List<ValidationError> errors = validator.validateRecord(record);
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.field().equals("district") && e.level().equals("MANDAL"));
    }

    @Test
    @DisplayName("Should reject village without parent mandal")
    void testVillageWithoutMandal() {
        RawGeographyRecord record = new RawGeographyRecord();
        record.setLineNumber(2);
        record.setStateCode("KA");
        record.setStateName("Karnataka");
        record.setDistrictName("Mysuru");
        record.setVillageName("Bilikere");

        List<ValidationError> errors = validator.validateRecord(record);
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.field().equals("mandal") && e.level().equals("VILLAGE"));
    }

    @Test
    @DisplayName("Should reject out-of-range coordinates")
    void testInvalidCoordinates() {
        RawGeographyRecord record = new RawGeographyRecord();
        record.setLineNumber(2);
        record.setStateCode("KA");
        record.setStateName("Karnataka");
        record.setDistrictName("Mysuru");
        record.setMandalName("Hunsur");
        record.setVillageName("Bilikere");
        record.setLatitude("95.0"); // Invalid > 90
        record.setLongitude("185.0"); // Invalid > 180

        List<ValidationError> errors = validator.validateRecord(record);
        assertThat(errors).hasSize(2);
        assertThat(errors).anyMatch(e -> e.field().equals("latitude"));
        assertThat(errors).anyMatch(e -> e.field().equals("longitude"));
    }

    @Test
    @DisplayName("Should detect duplicate state in same dataset")
    void testDuplicateState() {
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("MH");
        rec1.setStateName("Maharashtra");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("MH");
        rec2.setStateName("Maharashtra");

        List<ValidationError> errs1 = new ArrayList<>();
        validator.checkAndTrackDuplicates(rec1, errs1);
        assertThat(errs1).isEmpty();

        List<ValidationError> errs2 = new ArrayList<>();
        boolean isDup = validator.checkAndTrackDuplicates(rec2, errs2);
        assertThat(isDup).isTrue();
        assertThat(errs2).isNotEmpty();
    }

    @Test
    @DisplayName("Should allow same district name in different states")
    void testSameDistrictDifferentStates() {
        // Example: Bilaspur exists in Himachal Pradesh and Chhattisgarh
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("HP");
        rec1.setStateName("Himachal Pradesh");
        rec1.setDistrictCode("HP_BIL");
        rec1.setDistrictName("Bilaspur");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("CG");
        rec2.setStateName("Chhattisgarh");
        rec2.setDistrictCode("CG_BIL");
        rec2.setDistrictName("Bilaspur");

        List<ValidationError> errs1 = new ArrayList<>();
        boolean dup1 = validator.checkAndTrackDuplicates(rec1, errs1);
        assertThat(dup1).isFalse();

        List<ValidationError> errs2 = new ArrayList<>();
        boolean dup2 = validator.checkAndTrackDuplicates(rec2, errs2);
        assertThat(dup2).isFalse();
        assertThat(errs2).isEmpty();
    }

    @Test
    @DisplayName("Should detect duplicate district under the same state")
    void testDuplicateDistrictUnderSameState() {
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("KA");
        rec1.setStateName("Karnataka");
        rec1.setDistrictCode("BLR");
        rec1.setDistrictName("Bengaluru Urban");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("KA");
        rec2.setStateName("Karnataka");
        rec2.setDistrictCode("BLR_2");
        rec2.setDistrictName("Bengaluru Urban"); // Same name under same state

        List<ValidationError> errs1 = new ArrayList<>();
        validator.checkAndTrackDuplicates(rec1, errs1);

        List<ValidationError> errs2 = new ArrayList<>();
        boolean isDup = validator.checkAndTrackDuplicates(rec2, errs2);
        assertThat(isDup).isTrue();
        assertThat(errs2).anyMatch(e -> e.level().equals("DISTRICT") && e.field().equals("name"));
    }

    @Test
    @DisplayName("Should allow same village name under different mandals")
    void testSameVillageDifferentMandals() {
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("ST1");
        rec1.setStateName("StateOne");
        rec1.setDistrictName("DistOne");
        rec1.setMandalName("MandalA");
        rec1.setVillageName("Rampur");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("ST1");
        rec2.setStateName("StateOne");
        rec2.setDistrictName("DistOne");
        rec2.setMandalName("MandalB");
        rec2.setVillageName("Rampur");

        List<ValidationError> errs1 = new ArrayList<>();
        boolean dup1 = validator.checkAndTrackDuplicates(rec1, errs1);
        assertThat(dup1).isFalse();

        List<ValidationError> errs2 = new ArrayList<>();
        boolean dup2 = validator.checkAndTrackDuplicates(rec2, errs2);
        assertThat(dup2).isFalse();
        assertThat(errs2).isEmpty();
    }

    @Test
    @DisplayName("Case A: Same mandal + same village name + different LGD codes => BOTH accepted")
    void testCaseA_sameMandalSameNameDifferentCodes() {
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("28");
        rec1.setStateName("Andhra Pradesh");
        rec1.setDistrictCode("503");
        rec1.setDistrictName("Chittoor");
        rec1.setMandalCode("7427");
        rec1.setMandalName("Chittoor Urban");
        rec1.setVillageCode("596707");
        rec1.setVillageName("Mapakshi");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("28");
        rec2.setStateName("Andhra Pradesh");
        rec2.setDistrictCode("503");
        rec2.setDistrictName("Chittoor");
        rec2.setMandalCode("7427");
        rec2.setMandalName("Chittoor Urban");
        rec2.setVillageCode("596708"); // Different official LGD code
        rec2.setVillageName("Mapakshi"); // Identical name in same mandal

        List<ValidationError> errs1 = new ArrayList<>();
        boolean dup1 = validator.checkAndTrackDuplicates(rec1, errs1);
        assertThat(dup1).isFalse();
        assertThat(errs1).isEmpty();

        List<ValidationError> errs2 = new ArrayList<>();
        boolean dup2 = validator.checkAndTrackDuplicates(rec2, errs2);
        assertThat(dup2).isFalse();
        assertThat(errs2).isEmpty();
    }

    @Test
    @DisplayName("Case B: Same mandal + same village name + same LGD code => duplicate rejected")
    void testCaseB_sameMandalSameNameSameCode() {
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("28");
        rec1.setStateName("Andhra Pradesh");
        rec1.setDistrictCode("503");
        rec1.setDistrictName("Chittoor");
        rec1.setMandalCode("7427");
        rec1.setMandalName("Chittoor Urban");
        rec1.setVillageCode("596707");
        rec1.setVillageName("Mapakshi");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("28");
        rec2.setStateName("Andhra Pradesh");
        rec2.setDistrictCode("503");
        rec2.setDistrictName("Chittoor");
        rec2.setMandalCode("7427");
        rec2.setMandalName("Chittoor Urban");
        rec2.setVillageCode("596707"); // Same LGD code
        rec2.setVillageName("Mapakshi");

        List<ValidationError> errs1 = new ArrayList<>();
        boolean dup1 = validator.checkAndTrackDuplicates(rec1, errs1);
        assertThat(dup1).isFalse();

        List<ValidationError> errs2 = new ArrayList<>();
        boolean dup2 = validator.checkAndTrackDuplicates(rec2, errs2);
        assertThat(dup2).isTrue();
        assertThat(errs2).isNotEmpty();
        assertThat(errs2).anyMatch(e -> e.level().equals("VILLAGE") && e.field().equals("code"));
    }

    @Test
    @DisplayName("Case C: Different mandals + same village name + different LGD codes => accepted")
    void testCaseC_differentMandalsSameNameDifferentCodes() {
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("28");
        rec1.setStateName("Andhra Pradesh");
        rec1.setDistrictCode("503");
        rec1.setDistrictName("Chittoor");
        rec1.setMandalCode("5431");
        rec1.setMandalName("Gudipala");
        rec1.setVillageCode("597063");
        rec1.setVillageName("Ramapuram");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("28");
        rec2.setStateName("Andhra Pradesh");
        rec2.setDistrictCode("503");
        rec2.setDistrictName("Chittoor");
        rec2.setMandalCode("5406");
        rec2.setMandalName("Nagari");
        rec2.setVillageCode("596401");
        rec2.setVillageName("Ramapuram");

        List<ValidationError> errs1 = new ArrayList<>();
        boolean dup1 = validator.checkAndTrackDuplicates(rec1, errs1);
        assertThat(dup1).isFalse();
        assertThat(errs1).isEmpty();

        List<ValidationError> errs2 = new ArrayList<>();
        boolean dup2 = validator.checkAndTrackDuplicates(rec2, errs2);
        assertThat(dup2).isFalse();
        assertThat(errs2).isEmpty();
    }

    @Test
    @DisplayName("Case D: Different mandals + same village name + same LGD code => handled according to LGD-code identity rules")
    void testCaseD_differentMandalsSameNameSameCode() {
        RawGeographyRecord rec1 = new RawGeographyRecord();
        rec1.setLineNumber(2);
        rec1.setStateCode("28");
        rec1.setStateName("Andhra Pradesh");
        rec1.setDistrictCode("503");
        rec1.setDistrictName("Chittoor");
        rec1.setMandalCode("5431");
        rec1.setMandalName("Gudipala");
        rec1.setVillageCode("597063");
        rec1.setVillageName("Ramapuram");

        RawGeographyRecord rec2 = new RawGeographyRecord();
        rec2.setLineNumber(3);
        rec2.setStateCode("28");
        rec2.setStateName("Andhra Pradesh");
        rec2.setDistrictCode("503");
        rec2.setDistrictName("Chittoor");
        rec2.setMandalCode("5406");
        rec2.setMandalName("Nagari");
        rec2.setVillageCode("597063"); // Same code under different mandal
        rec2.setVillageName("Ramapuram");

        List<ValidationError> errs1 = new ArrayList<>();
        boolean dup1 = validator.checkAndTrackDuplicates(rec1, errs1);
        assertThat(dup1).isFalse();
        assertThat(errs1).isEmpty();

        List<ValidationError> errs2 = new ArrayList<>();
        boolean dup2 = validator.checkAndTrackDuplicates(rec2, errs2);
        // Under mandal-scoped code tracking (mandalScopeKey::code), different mandals produce distinct keys
        assertThat(dup2).isFalse();
        assertThat(errs2).isEmpty();
    }
}
