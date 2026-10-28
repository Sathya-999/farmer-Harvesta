package com.agriplatform.geography.lgd.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LGD API response DTO for a Village record.
 *
 * <p>Fields sourced strictly from the official LGD/NAPIX OpenAPI specification
 * (activevillagesbystate_2.0.0.json). No invented fields.
 *
 * <p>Note: LGD does NOT provide latitude/longitude. Village geo-coordinates will be NULL.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LgdVillageDto {

    /** LGD authoritative integer village code. Stored as String in our {@code code} column. */
    private Integer villageCode;

    private String villageNameEnglish;
    private String villageNameLocal;

    /** Parent sub-district LGD integer code. Used to resolve our internal mandal UUID. */
    private Integer subDistrictCode;

    /** Parent district code — available in v2 responses. */
    private Integer districtCode;

    /** Parent state code. */
    private Integer stateCode;

    private String census2011Code;
    private String effectiveDate;
    private String lastUpdated;
    private Integer majorVersion;
    private Integer minorVersion;
    private Long transactionId;
    private Integer operationCode;
    private String transactionDescription;

    /** Village status field (e.g. "I" = inhabited). */
    private String villageStatus;

    /** PESA status (e.g. "P" = PESA). */
    private String is_pesa;

    @JsonAlias({"isactive", "isActive"})
    private Boolean isactive;

    public LgdVillageDto() {}

    public Integer getVillageCode() { return villageCode; }
    public void setVillageCode(Integer villageCode) { this.villageCode = villageCode; }

    public String getVillageNameEnglish() { return villageNameEnglish; }
    public void setVillageNameEnglish(String villageNameEnglish) { this.villageNameEnglish = villageNameEnglish; }

    public String getVillageNameLocal() { return villageNameLocal; }
    public void setVillageNameLocal(String villageNameLocal) { this.villageNameLocal = villageNameLocal; }

    public Integer getSubDistrictCode() { return subDistrictCode; }
    public void setSubDistrictCode(Integer subDistrictCode) { this.subDistrictCode = subDistrictCode; }

    public Integer getDistrictCode() { return districtCode; }
    public void setDistrictCode(Integer districtCode) { this.districtCode = districtCode; }

    public Integer getStateCode() { return stateCode; }
    public void setStateCode(Integer stateCode) { this.stateCode = stateCode; }

    public String getCensus2011Code() { return census2011Code; }
    public void setCensus2011Code(String census2011Code) { this.census2011Code = census2011Code; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public Integer getMajorVersion() { return majorVersion; }
    public void setMajorVersion(Integer majorVersion) { this.majorVersion = majorVersion; }

    public Integer getMinorVersion() { return minorVersion; }
    public void setMinorVersion(Integer minorVersion) { this.minorVersion = minorVersion; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Integer getOperationCode() { return operationCode; }
    public void setOperationCode(Integer operationCode) { this.operationCode = operationCode; }

    public String getTransactionDescription() { return transactionDescription; }
    public void setTransactionDescription(String transactionDescription) { this.transactionDescription = transactionDescription; }

    public String getVillageStatus() { return villageStatus; }
    public void setVillageStatus(String villageStatus) { this.villageStatus = villageStatus; }

    public String getIs_pesa() { return is_pesa; }
    public void setIs_pesa(String is_pesa) { this.is_pesa = is_pesa; }

    public Boolean getIsactive() { return isactive; }
    public void setIsactive(Boolean isactive) { this.isactive = isactive; }

    public boolean isEffectivelyActive() {
        return isactive == null || isactive;
    }
}
