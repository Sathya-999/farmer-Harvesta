package com.agriplatform.geography.ingestion.service;

import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.source.GeographySource;

/**
 * Service orchestrating parsing, validation, normalization, and safe synchronization
 * of Indian geography reference data from authoritative sources.
 */
public interface GeographyIngestionService {

    /**
     * Ingests geography master data from the given source using the specified execution mode.
     *
     * @param source the data source (CSV, JSON, etc.)
     * @param mode DRY_RUN (simulation/validation only) or IMPORT (safe database upsert)
     * @return comprehensive import summary with statistics, errors, and status
     */
    ImportSummary ingest(GeographySource source, ImportMode mode);
}
