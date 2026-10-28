package com.agriplatform.geography.lgd.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LGD API response DTO for a District record.
 *
 * <p>Fields sourced strictly from the official LGD/NAPIX OpenAPI specification
 * (activedistrictsbystate_2.0.0.json). No invented fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LgdDistrictDto {

    /** LGD authoritative integer district code. Stored as String in our {@code code} column. */
    private Integer districtCode;

    private String districtNameEnglish;
    private String districtNameLocal;

    /** Short district name (available in v2 responses, e.g. "ALM"). */
    private String districtNameshort;

    /** Parent state LGD integer code. Used to resolve our internal state UUID. */
    private Integer stateCode;

    private String census2011Code;
    private String effectiveDate;
    private String lastUpdated;
    private Integer majorVersion;
    private Integer minorVersion;
    private Long transactionId;
    private Integer operationCode;
    private String transactionDescription;

    @JsonAlias({"isactive", "isActive"})
    private Boolean isactive;

    public LgdDistrictDto() {}

    public Integer getDistrictCode() { return districtCode; }
    public void setDistrictCode(Integer districtCode) { this.districtCode = districtCode; }

    public String getDistrictNameEnglish() { return districtNameEnglish; }
    public void setDistrictNameEnglish(String districtNameEnglish) { this.districtNameEnglish = districtNameEnglish; }

    public String getDistrictNameLocal() { return districtNameLocal; }
    public void setDistrictNameLocal(String districtNameLocal) { this.districtNameLocal = districtNameLocal; }

    public String getDistrictNameshort() { return districtNameshort; }
    public void setDistrictNameshort(String districtNameshort) { this.districtNameshort = districtNameshort; }

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

    public Boolean getIsactive() { return isactive; }
    public void setIsactive(Boolean isactive) { this.isactive = isactive; }

    public boolean isEffectivelyActive() {
        return isactive == null || isactive;
    }
}
