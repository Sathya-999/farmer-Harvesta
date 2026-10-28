package com.agriplatform.geography.ingestion.source;

import com.agriplatform.geography.ingestion.model.RawGeographyRecord;
import com.agriplatform.geography.ingestion.parser.CsvStreamParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Standard CSV source for Local Government Directory (LGD) or Open Government Data (OGD) exports.
 *
 * <p>Supports automatic header variation mapping, BOM removal, and streaming iteration.
 */
public class LgdCsvGeographySource implements GeographySource {

    private final String sourceName;
    private final Reader reader;
    private CsvStreamParser parser;

    public LgdCsvGeographySource(String sourceName, InputStream inputStream) {
        this(sourceName, new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public LgdCsvGeographySource(String sourceName, Reader reader) {
        this.sourceName = (sourceName != null && !sourceName.isBlank()) ? sourceName : "csv-stream";
        this.reader = reader;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public Iterable<RawGeographyRecord> readRecords() throws IOException {
        this.parser = new CsvStreamParser(reader);
        Iterator<List<String>> rowIterator = parser.iterator();

        if (!rowIterator.hasNext()) {
            return Collections.emptyList();
        }

        List<String> headerRow = rowIterator.next();
        HeaderMap headerMap = HeaderMap.fromHeaders(headerRow);

        return () -> new Iterator<>() {
            private long currentLine = 1; // header was line 1

            @Override
            public boolean hasNext() {
                return rowIterator.hasNext();
            }

            @Override
            public RawGeographyRecord next() {
                List<String> row = rowIterator.next();
                currentLine++;
                return headerMap.toRecord(row, currentLine);
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (parser != null) {
            parser.close();
        } else if (reader != null) {
            reader.close();
        }
    }

    private static class HeaderMap {
        private int stateCodeCol = -1;
        private int stateNameCol = -1;
        private int districtCodeCol = -1;
        private int districtNameCol = -1;
        private int mandalCodeCol = -1;
        private int mandalNameCol = -1;
        private int villageCodeCol = -1;
        private int villageNameCol = -1;
        private int latitudeCol = -1;
        private int longitudeCol = -1;
        private int activeCol = -1;

        public static HeaderMap fromHeaders(List<String> headers) {
            HeaderMap map = new HeaderMap();
            for (int i = 0; i < headers.size(); i++) {
                String cleanHeader = headers.get(i).trim().toLowerCase().replaceAll("[_\\s-]", "");
                switch (cleanHeader) {
                    case "statecode", "statelgdcode", "stateid" -> map.stateCodeCol = i;
                    case "statename", "state" -> map.stateNameCol = i;
                    case "districtcode", "districtlgdcode", "districtid" -> map.districtCodeCol = i;
                    case "districtname", "district" -> map.districtNameCol = i;
                    case "mandalcode", "subdistrictcode", "tehsilcode", "talukcode", "subdistrictlgdcode", "blockcode" -> map.mandalCodeCol = i;
                    case "mandalname", "subdistrictname", "tehsilname", "talukname", "subdistrict", "mandal", "tehsil", "taluk", "blockname" -> map.mandalNameCol = i;
                    case "villagecode", "villagelgdcode", "villageid" -> map.villageCodeCol = i;
                    case "villagename", "village" -> map.villageNameCol = i;
                    case "latitude", "lat" -> map.latitudeCol = i;
                    case "longitude", "lon", "long", "lng" -> map.longitudeCol = i;
                    case "isactive", "status", "active" -> map.activeCol = i;
                }
            }
            return map;
        }

        public RawGeographyRecord toRecord(List<String> row, long lineNumber) {
            RawGeographyRecord record = new RawGeographyRecord();
            record.setLineNumber(lineNumber);

            record.setStateCode(get(row, stateCodeCol));
            record.setStateName(get(row, stateNameCol));

            record.setDistrictCode(get(row, districtCodeCol));
            record.setDistrictName(get(row, districtNameCol));

            record.setMandalCode(get(row, mandalCodeCol));
            record.setMandalName(get(row, mandalNameCol));

            record.setVillageCode(get(row, villageCodeCol));
            record.setVillageName(get(row, villageNameCol));

            record.setLatitude(get(row, latitudeCol));
            record.setLongitude(get(row, longitudeCol));

            String activeStr = get(row, activeCol);
            if (activeStr != null) {
                String s = activeStr.toLowerCase();
                record.setIsActive(!"false".equals(s) && !"0".equals(s) && !"inactive".equals(s) && !"no".equals(s));
            }

            return record;
        }

        private String get(List<String> row, int index) {
            if (index < 0 || index >= row.size()) {
                return null;
            }
            String val = row.get(index);
            return (val == null || val.isBlank()) ? null : val.trim();
        }
    }
}
