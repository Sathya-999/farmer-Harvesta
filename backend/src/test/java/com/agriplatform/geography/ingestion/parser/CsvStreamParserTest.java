package com.agriplatform.geography.ingestion.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvStreamParser Unit Tests")
class CsvStreamParserTest {

    @Test
    @DisplayName("Should parse standard unquoted CSV rows")
    void testStandardCsv() throws Exception {
        String csv = "a,b,c\n1,2,3\n4,5,6";
        try (CsvStreamParser parser = new CsvStreamParser(new StringReader(csv))) {
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : parser) {
                rows.add(row);
            }
            assertThat(rows).hasSize(3);
            assertThat(rows.get(0)).containsExactly("a", "b", "c");
            assertThat(rows.get(1)).containsExactly("1", "2", "3");
            assertThat(rows.get(2)).containsExactly("4", "5", "6");
        }
    }

    @Test
    @DisplayName("Should handle quoted fields with embedded commas")
    void testQuotedFieldsWithCommas() throws Exception {
        String csv = "id,name,description\n1,\"New Delhi, NCR\",\"Capital, India\"";
        try (CsvStreamParser parser = new CsvStreamParser(new StringReader(csv))) {
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : parser) {
                rows.add(row);
            }
            assertThat(rows).hasSize(2);
            assertThat(rows.get(1)).containsExactly("1", "New Delhi, NCR", "Capital, India");
        }
    }

    @Test
    @DisplayName("Should handle escaped double quotes within quotes")
    void testEscapedDoubleQuotes() throws Exception {
        String csv = "col1,col2\n\"He said \"\"Hello\"\"\",val2";
        try (CsvStreamParser parser = new CsvStreamParser(new StringReader(csv))) {
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : parser) {
                rows.add(row);
            }
            assertThat(rows).hasSize(2);
            assertThat(rows.get(1).get(0)).isEqualTo("He said \"Hello\"");
        }
    }

    @Test
    @DisplayName("Should handle multiline fields with newlines inside quotes")
    void testMultilineQuotedFields() throws Exception {
        String csv = "id,notes\n1,\"Line 1\nLine 2\"\n2,Normal";
        try (CsvStreamParser parser = new CsvStreamParser(new StringReader(csv))) {
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : parser) {
                rows.add(row);
            }
            assertThat(rows).hasSize(3);
            assertThat(rows.get(1).get(1)).isEqualTo("Line 1\nLine 2");
            assertThat(rows.get(2).get(1)).isEqualTo("Normal");
        }
    }

    @Test
    @DisplayName("Should automatically strip UTF-8 Byte Order Mark (BOM)")
    void testBomStripping() throws Exception {
        String csv = "\uFEFFheader1,header2\nval1,val2";
        try (CsvStreamParser parser = new CsvStreamParser(new StringReader(csv))) {
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : parser) {
                rows.add(row);
            }
            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).get(0)).isEqualTo("header1");
        }
    }

    @Test
    @DisplayName("Should handle CRLF line endings")
    void testCrlfLineEndings() throws Exception {
        String csv = "a,b\r\n1,2\r\n3,4";
        try (CsvStreamParser parser = new CsvStreamParser(new StringReader(csv))) {
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : parser) {
                rows.add(row);
            }
            assertThat(rows).hasSize(3);
            assertThat(rows.get(1)).containsExactly("1", "2");
        }
    }
}
