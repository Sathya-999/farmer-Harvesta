package com.agriplatform.geography.ingestion.validation;

import com.agriplatform.geography.ingestion.model.RawGeographyRecord;
import com.agriplatform.geography.ingestion.model.ValidationError;
import com.agriplatform.geography.ingestion.normalizer.GeographyNormalizer;

import java.math.BigDecimal;
import java.util.*;

/**
 * Pre-import validator enforcing hierarchical integrity, schema constraints, coordinate limits,
 * and duplicate detection for geography master data.
 */
public class GeographyValidator {

    private static final BigDecimal LAT_MIN = new BigDecimal("-90.000000");
    private static final BigDecimal LAT_MAX = new BigDecimal("90.000000");
    private static final BigDecimal LON_MIN = new BigDecimal("-180.000000");
    private static final BigDecimal LON_MAX = new BigDecimal("180.000000");

    // Duplicate tracking sets within the source stream
    private final Set<String> seenStateNames = new HashSet<>();
    private final Set<String> seenStateCodes = new HashSet<>();
    private final Set<String> seenDistricts = new HashSet<>();
    private final Set<String> seenDistrictCodes = new HashSet<>();
    private final Set<String> seenMandals = new HashSet<>();
    private final Set<String> seenMandalCodes = new HashSet<>();
    private final Set<String> seenVillages = new HashSet<>();
    private final Set<String> seenVillageCodes = new HashSet<>();

    public void reset() {
        seenStateNames.clear();
        seenStateCodes.clear();
        seenDistricts.clear();
        seenDistrictCodes.clear();
        seenMandals.clear();
        seenMandalCodes.clear();
        seenVillages.clear();
        seenVillageCodes.clear();
    }

    /**
     * Validates a single raw geography record against business rules and schema constraints.
     *
     * @param record the record to validate
     * @return list of validation errors (empty if valid)
     */
    public List<ValidationError> validateRecord(RawGeographyRecord record) {
        List<ValidationError> errors = new ArrayList<>();
        long line = record.getLineNumber();

        if (record == null) {
            errors.add(new ValidationError(line, "RECORD", "row", "null", "Row is null"));
            return errors;
        }

        // Must have at least a state
        if (!record.hasState()) {
            errors.add(new ValidationError(line, "STATE", "name", record.getStateName(), "State name cannot be blank or whitespace-only"));
            errors.add(new ValidationError(line, "STATE", "code", record.getStateCode(), "State code is required (cannot be blank)"));
            return errors;
        }

        // Normalization checks
        String normStateName = GeographyNormalizer.normalizeText(record.getStateName());
        String normStateCode = GeographyNormalizer.normalizeCode(record.getStateCode());

        // 1. STATE VALIDATION
        if (normStateName == null || normStateName.isBlank()) {
            errors.add(new ValidationError(line, "STATE", "name", record.getStateName(), "State name cannot be blank or whitespace-only"));
        }
        if (normStateCode == null || normStateCode.isBlank()) {
            errors.add(new ValidationError(line, "STATE", "code", record.getStateCode(), "State code is required (cannot be blank)"));
        }

        String stateKey = normStateCode != null ? normStateCode : (normStateName != null ? normStateName.toLowerCase() : "");

        // 2. DISTRICT VALIDATION
        boolean hasDistrict = record.hasDistrict();
        String normDistrictName = GeographyNormalizer.normalizeText(record.getDistrictName());
        String normDistrictCode = GeographyNormalizer.normalizeCode(record.getDistrictCode());

        if (hasDistrict) {
            if (normDistrictName == null || normDistrictName.isBlank()) {
                errors.add(new ValidationError(line, "DISTRICT", "name", record.getDistrictName(), "District name cannot be blank"));
            }
            if (normStateName == null && normStateCode == null) {
                errors.add(new ValidationError(line, "DISTRICT", "state", null, "District has missing or unknown parent state"));
            }
        }

        String districtKey = stateKey + "::" + (normDistrictCode != null ? normDistrictCode : (normDistrictName != null ? normDistrictName.toLowerCase() : ""));

        // 3. MANDAL VALIDATION
        boolean hasMandal = record.hasMandal();
        String normMandalName = GeographyNormalizer.normalizeText(record.getMandalName());
        String normMandalCode = GeographyNormalizer.normalizeCode(record.getMandalCode());

        if (hasMandal) {
            if (!hasDistrict) {
                errors.add(new ValidationError(line, "MANDAL", "district", null, "Mandal has missing parent district"));
            }
            if (normMandalName == null || normMandalName.isBlank()) {
                errors.add(new ValidationError(line, "MANDAL", "name", record.getMandalName(), "Mandal name cannot be blank"));
            }
        }

        String mandalKey = districtKey + "::" + (normMandalCode != null ? normMandalCode : (normMandalName != null ? normMandalName.toLowerCase() : ""));

        // 4. VILLAGE VALIDATION
        boolean hasVillage = record.hasVillage();
        String normVillageName = GeographyNormalizer.normalizeText(record.getVillageName());
        String normVillageCode = GeographyNormalizer.normalizeCode(record.getVillageCode());

        if (hasVillage) {
            if (!hasMandal) {
                errors.add(new ValidationError(line, "VILLAGE", "mandal", null, "Village has missing parent mandal"));
            }
            if (normVillageName == null || normVillageName.isBlank()) {
                errors.add(new ValidationError(line, "VILLAGE", "name", record.getVillageName(), "Village name cannot be blank"));
            }

            // Coordinate validation
            if (record.getLatitude() != null && !record.getLatitude().isBlank()) {
                try {
                    GeographyNormalizer.parseCoordinate(record.getLatitude(), LAT_MIN, LAT_MAX, "latitude");
                } catch (IllegalArgumentException e) {
                    errors.add(new ValidationError(line, "VILLAGE", "latitude", record.getLatitude(), e.getMessage()));
                }
            }

            if (record.getLongitude() != null && !record.getLongitude().isBlank()) {
                try {
                    GeographyNormalizer.parseCoordinate(record.getLongitude(), LON_MIN, LON_MAX, "longitude");
                } catch (IllegalArgumentException e) {
                    errors.add(new ValidationError(line, "VILLAGE", "longitude", record.getLongitude(), e.getMessage()));
                }
            }
        }

        // If village is not specified but mandal is not specified and district is not specified: this is a pure State record.
        // Hierarchy rules:
        // Village requires Mandal -> District -> State.
        // Mandal requires District -> State.
        // District requires State.
        if (hasVillage && !hasMandal) {
            errors.add(new ValidationError(line, "HIERARCHY", "mandal", null, "Village cannot be orphaned without a parent mandal"));
        }
        if (hasMandal && !hasDistrict) {
            errors.add(new ValidationError(line, "HIERARCHY", "district", null, "Mandal cannot be orphaned without a parent district"));
        }

        return errors;
    }

    /**
     * Checks if this record introduces a duplicate within the current import dataset.
     * Duplicate detection is hierarchical:
     * - Same district name under different states is ALLOWED.
     * - Same mandal name under different districts is ALLOWED.
     * - Same village name under different mandals is ALLOWED.
     * - Duplicate under the same parent is flagged.
     *
     * @return true if duplicate was detected, false if novel
     */
    public boolean checkAndTrackDuplicates(RawGeographyRecord record, List<ValidationError> errors) {
        long line = record.getLineNumber();
        boolean isDuplicate = false;

        String normStateName = GeographyNormalizer.normalizeText(record.getStateName());
        String normStateCode = GeographyNormalizer.normalizeCode(record.getStateCode());
        String stateKey = normStateName != null ? normStateName.toLowerCase() : "";

        // If this is a pure state record
        if (!record.hasDistrict() && !record.hasMandal() && !record.hasVillage()) {
            if (normStateName != null && !seenStateNames.add(stateKey)) {
                errors.add(new ValidationError(line, "STATE", "name", record.getStateName(), "Duplicate state name in dataset: " + normStateName));
                isDuplicate = true;
            }
            if (normStateCode != null && !seenStateCodes.add(normStateCode)) {
                errors.add(new ValidationError(line, "STATE", "code", record.getStateCode(), "Duplicate state code in dataset: " + normStateCode));
                isDuplicate = true;
            }
            return isDuplicate;
        }

        // If district level
        String normDistrictName = GeographyNormalizer.normalizeText(record.getDistrictName());
        String normDistrictCode = GeographyNormalizer.normalizeCode(record.getDistrictCode());
        String districtScopeKey = stateKey + "::" + (normDistrictName != null ? normDistrictName.toLowerCase() : "");

        if (record.hasDistrict() && !record.hasMandal() && !record.hasVillage()) {
            if (normDistrictName != null && !seenDistricts.add(districtScopeKey)) {
                errors.add(new ValidationError(line, "DISTRICT", "name", record.getDistrictName(),
                        "Duplicate district under state: " + normDistrictName));
                isDuplicate = true;
            }
            if (normDistrictCode != null && !seenDistrictCodes.add(stateKey + "::" + normDistrictCode)) {
                errors.add(new ValidationError(line, "DISTRICT", "code", record.getDistrictCode(),
                        "Duplicate district code under state: " + normDistrictCode));
                isDuplicate = true;
            }
            return isDuplicate;
        }

        // If mandal level
        String normMandalName = GeographyNormalizer.normalizeText(record.getMandalName());
        String normMandalCode = GeographyNormalizer.normalizeCode(record.getMandalCode());
        String mandalScopeKey = districtScopeKey + "::" + (normMandalName != null ? normMandalName.toLowerCase() : "");

        if (record.hasMandal() && !record.hasVillage()) {
            if (normMandalName != null && !seenMandals.add(mandalScopeKey)) {
                errors.add(new ValidationError(line, "MANDAL", "name", record.getMandalName(),
                        "Duplicate mandal under district: " + normMandalName));
                isDuplicate = true;
            }
            if (normMandalCode != null && !seenMandalCodes.add(districtScopeKey + "::" + normMandalCode)) {
                errors.add(new ValidationError(line, "MANDAL", "code", record.getMandalCode(),
                        "Duplicate mandal code under district: " + normMandalCode));
                isDuplicate = true;
            }
            return isDuplicate;
        }

        // If village level
        String normVillageName = GeographyNormalizer.normalizeText(record.getVillageName());
        String normVillageCode = GeographyNormalizer.normalizeCode(record.getVillageCode());
        String villageScopeKey = mandalScopeKey + "::" + (normVillageName != null ? normVillageName.toLowerCase() : "");

        if (record.hasVillage()) {
            if (normVillageCode != null && !normVillageCode.isBlank()) {
                // Authoritative identity by LGD village code under mandal
                if (!seenVillageCodes.add(mandalScopeKey + "::" + normVillageCode)) {
                    errors.add(new ValidationError(line, "VILLAGE", "code", record.getVillageCode(),
                            "Duplicate village code under mandal: " + normVillageCode));
                    isDuplicate = true;
                }
            } else {
                // Fallback to name-based identity ONLY when authoritative LGD code is absent
                if (normVillageName != null && !seenVillages.add(villageScopeKey)) {
                    errors.add(new ValidationError(line, "VILLAGE", "name", record.getVillageName(),
                            "Duplicate village under mandal: " + normVillageName));
                    isDuplicate = true;
                }
            }
        }

        return isDuplicate;
    }
}
