package com.agriplatform.geography.ingestion.source;

import com.agriplatform.geography.ingestion.model.RawGeographyRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON source for Local Government Directory (LGD) or Open Government Data (OGD) exports.
 *
 * <p>Supports both raw JSON arrays ({@code [ {...}, {...} ]}) and envelope objects ({@code { "data": [...] }}
 * or {@code { "records": [...] }}).
 */
public class LgdJsonGeographySource implements GeographySource {

    private final String sourceName;
    private final JsonNode rootNode;

    public LgdJsonGeographySource(String sourceName, InputStream inputStream, ObjectMapper objectMapper) throws IOException {
        this.sourceName = (sourceName != null && !sourceName.isBlank()) ? sourceName : "json-stream";
        this.rootNode = objectMapper.readTree(inputStream);
    }

    public LgdJsonGeographySource(String sourceName, Reader reader, ObjectMapper objectMapper) throws IOException {
        this.sourceName = (sourceName != null && !sourceName.isBlank()) ? sourceName : "json-stream";
        this.rootNode = objectMapper.readTree(reader);
    }

    public LgdJsonGeographySource(String sourceName, String jsonString, ObjectMapper objectMapper) throws IOException {
        this.sourceName = (sourceName != null && !sourceName.isBlank()) ? sourceName : "json-payload";
        this.rootNode = objectMapper.readTree(jsonString);
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public Iterable<RawGeographyRecord> readRecords() {
        JsonNode recordsArray = rootNode;
        if (rootNode.isObject()) {
            if (rootNode.has("data") && rootNode.get("data").isArray()) {
                recordsArray = rootNode.get("data");
            } else if (rootNode.has("records") && rootNode.get("records").isArray()) {
                recordsArray = rootNode.get("records");
            } else if (rootNode.has("items") && rootNode.get("items").isArray()) {
                recordsArray = rootNode.get("items");
            }
        }

        List<RawGeographyRecord> list = new ArrayList<>();
        if (recordsArray.isArray()) {
            long line = 1;
            for (JsonNode item : recordsArray) {
                line++;
                RawGeographyRecord record = new RawGeographyRecord();
                record.setLineNumber(line);

                record.setStateCode(text(item, "stateCode", "state_code", "state_lgd_code"));
                record.setStateName(text(item, "stateName", "state_name", "state"));

                record.setDistrictCode(text(item, "districtCode", "district_code", "district_lgd_code"));
                record.setDistrictName(text(item, "districtName", "district_name", "district"));

                record.setMandalCode(text(item, "mandalCode", "mandal_code", "subdistrictCode", "subdistrict_code", "sub_district_code", "tehsil_code"));
                record.setMandalName(text(item, "mandalName", "mandal_name", "subdistrictName", "subdistrict_name", "sub_district_name", "tehsil_name"));

                record.setVillageCode(text(item, "villageCode", "village_code", "village_lgd_code"));
                record.setVillageName(text(item, "villageName", "village_name", "village"));

                record.setLatitude(text(item, "latitude", "lat"));
                record.setLongitude(text(item, "longitude", "lon", "long", "lng"));

                if (item.has("isActive")) {
                    record.setIsActive(item.get("isActive").asBoolean(true));
                } else if (item.has("is_active")) {
                    record.setIsActive(item.get("is_active").asBoolean(true));
                } else if (item.has("status")) {
                    record.setIsActive(!"inactive".equalsIgnoreCase(item.get("status").asText()));
                }

                list.add(record);
            }
        }
        return list;
    }

    private String text(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                String val = node.get(key).asText().trim();
                if (!val.isEmpty()) {
                    return val;
                }
            }
        }
        return null;
    }
}
