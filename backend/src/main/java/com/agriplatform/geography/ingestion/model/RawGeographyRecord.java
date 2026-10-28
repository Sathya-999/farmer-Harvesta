package com.agriplatform.geography.ingestion.model;

import java.util.Objects;

/**
 * Raw geography record extracted from an authoritative source file or stream.
 * Represents a single row/item in a hierarchical administrative dataset.
 */
public class RawGeographyRecord {

    private long lineNumber;

    // State level
    private String stateCode;
    private String stateName;

    // District level
    private String districtCode;
    private String districtName;

    // Sub-district / Mandal / Tehsil / Taluk level
    private String mandalCode;
    private String mandalName;

    // Village level
    private String villageCode;
    private String villageName;

    // Geographic coordinates
    private String latitude;
    private String longitude;

    // Administrative status
    private Boolean isActive = true;

    public RawGeographyRecord() {}

    public long getLineNumber() { return lineNumber; }
    public void setLineNumber(long lineNumber) { this.lineNumber = lineNumber; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getDistrictCode() { return districtCode; }
    public void setDistrictCode(String districtCode) { this.districtCode = districtCode; }

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public String getMandalCode() { return mandalCode; }
    public void setMandalCode(String mandalCode) { this.mandalCode = mandalCode; }

    public String getMandalName() { return mandalName; }
    public void setMandalName(String mandalName) { this.mandalName = mandalName; }

    public String getVillageCode() { return villageCode; }
    public void setVillageCode(String villageCode) { this.villageCode = villageCode; }

    public String getVillageName() { return villageName; }
    public void setVillageName(String villageName) { this.villageName = villageName; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public boolean hasState() {
        return (stateName != null && !stateName.isBlank()) || (stateCode != null && !stateCode.isBlank());
    }

    public boolean hasDistrict() {
        return (districtName != null && !districtName.isBlank()) || (districtCode != null && !districtCode.isBlank());
    }

    public boolean hasMandal() {
        return (mandalName != null && !mandalName.isBlank()) || (mandalCode != null && !mandalCode.isBlank());
    }

    public boolean hasVillage() {
        return (villageName != null && !villageName.isBlank()) || (villageCode != null && !villageCode.isBlank());
    }

    @Override
    public String toString() {
        return "RawGeographyRecord{" +
                "line=" + lineNumber +
                ", state=" + stateName + " (" + stateCode + ")" +
                ", district=" + districtName + " (" + districtCode + ")" +
                ", mandal=" + mandalName + " (" + mandalCode + ")" +
                ", village=" + villageName + " (" + villageCode + ")" +
                '}';
    }
}
