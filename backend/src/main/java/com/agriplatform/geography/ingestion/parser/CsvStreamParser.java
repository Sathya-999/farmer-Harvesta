package com.agriplatform.geography.ingestion.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * High-performance, streaming RFC-4180 compliant CSV parser.
 *
 * <p>Features:
 * <ul>
 *   <li>Streams rows one by one — constant memory overhead regardless of file size.</li>
 *   <li>Strips UTF-8 Byte Order Mark (BOM) automatically.</li>
 *   <li>Supports escaped double quotes ({@code ""}).</li>
 *   <li>Handles field-internal newlines within quoted fields.</li>
 *   <li>Handles custom separators (comma by default).</li>
 * </ul>
 */
public class CsvStreamParser implements Iterable<List<String>>, AutoCloseable {

    private final Reader reader;
    private final char delimiter;
    private boolean closed = false;

    public CsvStreamParser(Reader reader) {
        this(reader, ',');
    }

    public CsvStreamParser(Reader reader, char delimiter) {
        this.reader = (reader instanceof BufferedReader br) ? br : new BufferedReader(reader);
        this.delimiter = delimiter;
    }

    @Override
    public Iterator<List<String>> iterator() {
        return new CsvIterator();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            reader.close();
        }
    }

    private class CsvIterator implements Iterator<List<String>> {

        private List<String> nextRow = null;
        private boolean eof = false;
        private boolean firstChar = true;

        @Override
        public boolean hasNext() {
            if (nextRow != null) {
                return true;
            }
            if (eof) {
                return false;
            }
            try {
                nextRow = readRow();
                return nextRow != null;
            } catch (IOException e) {
                throw new RuntimeException("Error reading CSV stream", e);
            }
        }

        @Override
        public List<String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            List<String> row = nextRow;
            nextRow = null;
            return row;
        }

        private List<String> readRow() throws IOException {
            List<String> fields = new ArrayList<>();
            StringBuilder fieldBuilder = new StringBuilder();
            boolean inQuotes = false;
            boolean hadQuotes = false;
            boolean rowStarted = false;

            while (true) {
                int c = reader.read();

                if (firstChar) {
                    firstChar = false;
                    // Strip UTF-8 BOM if present
                    if (c == 0xFEFF) {
                        c = reader.read();
                    }
                }

                if (c == -1) {
                    eof = true;
                    if (rowStarted || inQuotes || hadQuotes || fieldBuilder.length() > 0) {
                        fields.add(fieldBuilder.toString().trim());
                        return fields;
                    }
                    return null;
                }

                char ch = (char) c;

                if (inQuotes) {
                    if (ch == '"') {
                        // Check if escaped quote
                        reader.mark(1);
                        int nextC = reader.read();
                        if (nextC == '"') {
                            fieldBuilder.append('"');
                        } else {
                            reader.reset();
                            inQuotes = false;
                        }
                    } else {
                        fieldBuilder.append(ch);
                    }
                    rowStarted = true;
                } else {
                    if (ch == '"') {
                        inQuotes = true;
                        hadQuotes = true;
                        rowStarted = true;
                    } else if (ch == delimiter) {
                        fields.add(fieldBuilder.toString().trim());
                        fieldBuilder.setLength(0);
                        hadQuotes = false;
                        rowStarted = true;
                    } else if (ch == '\r') {
                        // Lookahead for \n
                        reader.mark(1);
                        int nextC = reader.read();
                        if (nextC != '\n') {
                            reader.reset();
                        }
                        fields.add(fieldBuilder.toString().trim());
                        return fields;
                    } else if (ch == '\n') {
                        fields.add(fieldBuilder.toString().trim());
                        return fields;
                    } else {
                        fieldBuilder.append(ch);
                        rowStarted = true;
                    }
                }
            }
        }
    }
}
