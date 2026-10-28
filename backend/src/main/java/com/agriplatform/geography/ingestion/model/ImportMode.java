package com.agriplatform.geography.ingestion.model;

/**
 * Operating mode for the geography master data ingestion engine.
 */
public enum ImportMode {
    /**
     * Parse, validate, normalize, and simulate hierarchy resolution.
     * Generates a complete validation and simulation report without modifying the database.
     */
    DRY_RUN,

    /**
     * Full execution: parse, validate, normalize, and safely upsert into the database.
     */
    IMPORT
}
