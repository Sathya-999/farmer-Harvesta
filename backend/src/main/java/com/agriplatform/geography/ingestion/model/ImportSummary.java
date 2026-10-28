package com.agriplatform.geography.ingestion.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive verification and audit report produced after an ingestion or validation run.
 */
public class ImportSummary {

    private ImportMode mode;
    private String sourceName;
    private long totalRowsRead = 0;
    private long validRowsCount = 0;
    private long invalidRowsCount = 0;
    private long duplicateRowsCount = 0;

    // Entity counters
    private long statesInserted = 0;
    private long statesUpdated = 0;
    private long statesUnchanged = 0;

    private long districtsInserted = 0;
    private long districtsUpdated = 0;
    private long districtsUnchanged = 0;

    private long mandalsInserted = 0;
    private long mandalsUpdated = 0;
    private long mandalsUnchanged = 0;

    private long villagesInserted = 0;
    private long villagesUpdated = 0;
    private long villagesUnchanged = 0;

    private final List<ValidationError> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private long durationMs = 0;
    private boolean successful = true;

    public ImportSummary() {}

    public ImportMode getMode() { return mode; }
    public void setMode(ImportMode mode) { this.mode = mode; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public long getTotalRowsRead() { return totalRowsRead; }
    public void setTotalRowsRead(long totalRowsRead) { this.totalRowsRead = totalRowsRead; }
    public void incrementTotalRowsRead() { this.totalRowsRead++; }

    public long getValidRowsCount() { return validRowsCount; }
    public void setValidRowsCount(long validRowsCount) { this.validRowsCount = validRowsCount; }
    public void incrementValidRowsCount() { this.validRowsCount++; }

    public long getInvalidRowsCount() { return invalidRowsCount; }
    public void setInvalidRowsCount(long invalidRowsCount) { this.invalidRowsCount = invalidRowsCount; }
    public void incrementInvalidRowsCount() { this.invalidRowsCount++; }

    public long getDuplicateRowsCount() { return duplicateRowsCount; }
    public void setDuplicateRowsCount(long duplicateRowsCount) { this.duplicateRowsCount = duplicateRowsCount; }
    public void incrementDuplicateRowsCount() { this.duplicateRowsCount++; }

    public long getStatesInserted() { return statesInserted; }
    public void incrementStatesInserted() { this.statesInserted++; }

    public long getStatesUpdated() { return statesUpdated; }
    public void incrementStatesUpdated() { this.statesUpdated++; }

    public long getStatesUnchanged() { return statesUnchanged; }
    public void incrementStatesUnchanged() { this.statesUnchanged++; }

    public long getDistrictsInserted() { return districtsInserted; }
    public void incrementDistrictsInserted() { this.districtsInserted++; }

    public long getDistrictsUpdated() { return districtsUpdated; }
    public void incrementDistrictsUpdated() { this.districtsUpdated++; }

    public long getDistrictsUnchanged() { return districtsUnchanged; }
    public void incrementDistrictsUnchanged() { this.districtsUnchanged++; }

    public long getMandalsInserted() { return mandalsInserted; }
    public void incrementMandalsInserted() { this.mandalsInserted++; }
    public void decrementMandalsInserted() { if (this.mandalsInserted > 0) this.mandalsInserted--; }

    public long getMandalsUpdated() { return mandalsUpdated; }
    public void incrementMandalsUpdated() { this.mandalsUpdated++; }
    public void decrementMandalsUpdated() { if (this.mandalsUpdated > 0) this.mandalsUpdated--; }

    public long getMandalsUnchanged() { return mandalsUnchanged; }
    public void incrementMandalsUnchanged() { this.mandalsUnchanged++; }

    public long getVillagesInserted() { return villagesInserted; }
    public void incrementVillagesInserted() { this.villagesInserted++; }
    public void decrementVillagesInserted() { if (this.villagesInserted > 0) this.villagesInserted--; }

    public long getVillagesUpdated() { return villagesUpdated; }
    public void incrementVillagesUpdated() { this.villagesUpdated++; }
    public void decrementVillagesUpdated() { if (this.villagesUpdated > 0) this.villagesUpdated--; }

    public long getVillagesUnchanged() { return villagesUnchanged; }
    public void incrementVillagesUnchanged() { this.villagesUnchanged++; }

    public long getTotalInserted() {
        return statesInserted + districtsInserted + mandalsInserted + villagesInserted;
    }

    public long getTotalUpdated() {
        return statesUpdated + districtsUpdated + mandalsUpdated + villagesUpdated;
    }

    public long getTotalUnchanged() {
        return statesUnchanged + districtsUnchanged + mandalsUnchanged + villagesUnchanged;
    }

    public List<ValidationError> getErrors() { return Collections.unmodifiableList(errors); }
    public void addError(ValidationError error) {
        this.errors.add(error);
        this.successful = false;
    }

    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }

    @Override
    public String toString() {
        return "ImportSummary{" +
                "mode=" + mode +
                ", source='" + sourceName + '\'' +
                ", totalRowsRead=" + totalRowsRead +
                ", validRowsCount=" + validRowsCount +
                ", invalidRowsCount=" + invalidRowsCount +
                ", duplicateRowsCount=" + duplicateRowsCount +
                ", states=[+" + statesInserted + ", ~" + statesUpdated + ", =" + statesUnchanged + "]" +
                ", districts=[+" + districtsInserted + ", ~" + districtsUpdated + ", =" + districtsUnchanged + "]" +
                ", mandals=[+" + mandalsInserted + ", ~" + mandalsUpdated + ", =" + mandalsUnchanged + "]" +
                ", villages=[+" + villagesInserted + ", ~" + villagesUpdated + ", =" + villagesUnchanged + "]" +
                ", totalErrors=" + errors.size() +
                ", durationMs=" + durationMs +
                ", successful=" + successful +
                '}';
    }
}
