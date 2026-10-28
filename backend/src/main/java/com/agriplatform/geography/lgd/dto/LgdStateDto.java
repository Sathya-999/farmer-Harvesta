package com.agriplatform.geography.lgd.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LGD API response DTO for a State record.
 *
 * <p>Fields sourced strictly from the official LGD/NAPIX OpenAPI specification
 * (active-states_1.0.0.json). No invented fields.
 *
 * <p>Jackson @JsonAlias handles the isactive/isActive casing variation
 * observed across different LGD API versions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LgdStateDto {

    /** LGD authoritative integer state code. Stored as String in our {@code code} column. */
    private Integer stateCode;

    private String stateNameEnglish;
    private String stateNameLocal;

    /** Short two-letter abbreviation (e.g. "AP"). */
    private String shortName;

    private String census2011Code;
    private String effectiveDate;
    private String lastUpdated;
    private Integer majorVersion;
    private Integer minorVersion;

    /** "S" = State, "U" = Union Territory */
    private String stateOrUt;

    private String transactionId;
    private Integer operationCode;
    private String transactionDescription;

    @JsonAlias({"isactive", "isActive"})
    private Boolean isactive;

    public LgdStateDto() {}

    public Integer getStateCode() { return stateCode; }
    public void setStateCode(Integer stateCode) { this.stateCode = stateCode; }

    public String getStateNameEnglish() { return stateNameEnglish; }
    public void setStateNameEnglish(String stateNameEnglish) { this.stateNameEnglish = stateNameEnglish; }

    public String getStateNameLocal() { return stateNameLocal; }
    public void setStateNameLocal(String stateNameLocal) { this.stateNameLocal = stateNameLocal; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

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

    public String getStateOrUt() { return stateOrUt; }
    public void setStateOrUt(String stateOrUt) { this.stateOrUt = stateOrUt; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Integer getOperationCode() { return operationCode; }
    public void setOperationCode(Integer operationCode) { this.operationCode = operationCode; }

    public String getTransactionDescription() { return transactionDescription; }
    public void setTransactionDescription(String transactionDescription) { this.transactionDescription = transactionDescription; }

    public Boolean getIsactive() { return isactive; }
    public void setIsactive(Boolean isactive) { this.isactive = isactive; }

    /** Null-safe active check; defaults to true per LGD convention. */
    public boolean isEffectivelyActive() {
        return isactive == null || isactive;
    }
}
