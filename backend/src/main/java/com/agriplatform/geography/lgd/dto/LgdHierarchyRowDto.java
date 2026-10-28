package com.agriplatform.geography.lgd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LGD API response DTO for a single row from the paginated full hierarchy API.
 *
 * <p>Path: POST /stateDistrictSubdistBlockVillage/{stateCode}/page/{page}
 *
 * <p>Fields sourced strictly from the official LGD/NAPIX OpenAPI specification
 * (state-district-subdistrict-block-village-api_1.0.0.json). No invented fields.
 *
 * <p>This single call provides all four levels of the hierarchy in one flat row.
 * Used only for initial full-load synchronization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LgdHierarchyRowDto {

    private Integer stateCode;
    private String stateNameEnglish;

    private Integer districtCode;
    private String districtNameEnglish;

    private Integer subdistrictCode;
    private String subdistrictNameEnglish;

    private Integer villageCode;
    private String villageNameEnglish;

    /** Block code — present in hierarchy response. Not directly mapped to our schema. */
    private Integer blockCode;
    private String blockNameEnglish;

    public LgdHierarchyRowDto() {}

    public Integer getStateCode() { return stateCode; }
    public void setStateCode(Integer stateCode) { this.stateCode = stateCode; }

    public String getStateNameEnglish() { return stateNameEnglish; }
    public void setStateNameEnglish(String stateNameEnglish) { this.stateNameEnglish = stateNameEnglish; }

    public Integer getDistrictCode() { return districtCode; }
    public void setDistrictCode(Integer districtCode) { this.districtCode = districtCode; }

    public String getDistrictNameEnglish() { return districtNameEnglish; }
    public void setDistrictNameEnglish(String districtNameEnglish) { this.districtNameEnglish = districtNameEnglish; }

    public Integer getSubdistrictCode() { return subdistrictCode; }
    public void setSubdistrictCode(Integer subdistrictCode) { this.subdistrictCode = subdistrictCode; }

    public String getSubdistrictNameEnglish() { return subdistrictNameEnglish; }
    public void setSubdistrictNameEnglish(String subdistrictNameEnglish) { this.subdistrictNameEnglish = subdistrictNameEnglish; }

    public Integer getVillageCode() { return villageCode; }
    public void setVillageCode(Integer villageCode) { this.villageCode = villageCode; }

    public String getVillageNameEnglish() { return villageNameEnglish; }
    public void setVillageNameEnglish(String villageNameEnglish) { this.villageNameEnglish = villageNameEnglish; }

    public Integer getBlockCode() { return blockCode; }
    public void setBlockCode(Integer blockCode) { this.blockCode = blockCode; }

    public String getBlockNameEnglish() { return blockNameEnglish; }
    public void setBlockNameEnglish(String blockNameEnglish) { this.blockNameEnglish = blockNameEnglish; }
}
