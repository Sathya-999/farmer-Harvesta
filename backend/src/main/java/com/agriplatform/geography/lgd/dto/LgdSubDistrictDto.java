package com.agriplatform.geography.lgd.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LGD API response DTO for a Sub-District (Mandal/Tehsil/Taluk) record.
 *
 * <p>Fields sourced strictly from the official LGD/NAPIX OpenAPI specification
 * (activesubdistrictsbystate_1.0.0.json). No invented fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LgdSubDistrictDto {

    /** LGD authoritative integer sub-district code. Stored as String in our {@code code} column. */
    private Integer subDistrictCode;

    private String subDistrictNameEnglish;
    private String subDistrictNameLocal;

    /** Parent district LGD integer code. Used to resolve our internal district UUID. */
    private Integer districtCode;

    /** Parent state LGD integer code. Available in v2 duration responses. */
    private Integer stateCode;

    /** District name in English — available in v2 duration API responses. */
    private String districtNameEnglish;

    private String census2011Code;
    private String effectiveDate;
    private String lastUpdated;
    private Integer majorVersion;
    private Integer minorVersion;
    private Long transactionId;
    private Integer operationCode;
    private String transactionDescription;

    /** District Level Code — present in some records. */
    private Integer dlc;

    @JsonAlias({"isactive", "isActive"})
    private Boolean isactive;

    public LgdSubDistrictDto() {}

    public Integer getSubDistrictCode() { return subDistrictCode; }
    public void setSubDistrictCode(Integer subDistrictCode) { this.subDistrictCode = subDistrictCode; }

    // Jackson also maps "subdistrictCode" from hierarchy API
    @JsonAlias("subdistrictCode")
    public void setSubdistrictCode(Integer code) { this.subDistrictCode = code; }

    public String getSubDistrictNameEnglish() { return subDistrictNameEnglish; }
    public void setSubDistrictNameEnglish(String subDistrictNameEnglish) { this.subDistrictNameEnglish = subDistrictNameEnglish; }

    // Hierarchy API uses "subdistrictNameEnglish"
    @JsonAlias("subdistrictNameEnglish")
    public void setSubdistrictNameEnglish(String name) { this.subDistrictNameEnglish = name; }

    public String getSubDistrictNameLocal() { return subDistrictNameLocal; }
    public void setSubDistrictNameLocal(String subDistrictNameLocal) { this.subDistrictNameLocal = subDistrictNameLocal; }

    public Integer getDistrictCode() { return districtCode; }
    public void setDistrictCode(Integer districtCode) { this.districtCode = districtCode; }

    public Integer getStateCode() { return stateCode; }
    public void setStateCode(Integer stateCode) { this.stateCode = stateCode; }

    public String getDistrictNameEnglish() { return districtNameEnglish; }
    public void setDistrictNameEnglish(String districtNameEnglish) { this.districtNameEnglish = districtNameEnglish; }

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

    public Integer getDlc() { return dlc; }
    public void setDlc(Integer dlc) { this.dlc = dlc; }

    public Boolean getIsactive() { return isactive; }
    public void setIsactive(Boolean isactive) { this.isactive = isactive; }

    public boolean isEffectivelyActive() {
        return isactive == null || isactive;
    }
}
