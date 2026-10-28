package com.agriplatform.geography.ingestion.source;

import com.agriplatform.geography.ingestion.model.RawGeographyRecord;

import java.io.Closeable;
import java.io.IOException;

/**
 * Abstraction representing an external or uploaded geography master data source.
 * Decouples the ingestion engine from the underlying format or transport mechanism (CSV, JSON, LGD SOAP/REST, etc.).
 */
public interface GeographySource extends Closeable {

    /**
     * Human-readable identifier or filename for the source (e.g. "lgd_villages_2026.csv").
     */
    String getSourceName();

    /**
     * Streams or iterates raw geography records parsed from the source.
     */
    Iterable<RawGeographyRecord> readRecords() throws IOException;

    @Override
    default void close() throws IOException {}
}
