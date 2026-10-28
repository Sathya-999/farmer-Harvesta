package com.agriplatform.geography.lgd.sync;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result model for a complete LGD synchronization run.
 * Provides per-level observability without exposing credentials or internal secrets.
 */
public class LgdSyncResult {

    public enum SyncType { INITIAL, INCREMENTAL }

    private final SyncType syncType;
    private final OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    private long statesProcessed;
    private long statesInserted;
    private long statesUpdated;
    private long statesDeactivated;
    private long statesSkipped;

    private long districtsProcessed;
    private long districtsInserted;
    private long districtsUpdated;
    private long districtsDeactivated;
    private long districtsSkipped;

    private long mandalsProcessed;
    private long mandalsInserted;
    private long mandalsUpdated;
    private long mandalsDeactivated;
    private long mandalsSkipped;

    private long villagesProcessed;
    private long villagesInserted;
    private long villagesUpdated;
    private long villagesDeactivated;
    private long villagesSkipped;

    private long pagesProcessed;
    private long statesFailed;
    private boolean overallSuccess = true;

    private final List<String> failedStateCodes = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public LgdSyncResult(SyncType syncType) {
        this.syncType = syncType;
        this.startedAt = OffsetDateTime.now();
    }

    public void complete() {
        this.completedAt = OffsetDateTime.now();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.overallSuccess = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void addFailedStateCode(String stateCode) {
        this.failedStateCodes.add(stateCode);
        this.statesFailed++;
        this.overallSuccess = false;
    }

    public long getDurationMs() {
        if (completedAt == null) return -1;
        return completedAt.toInstant().toEpochMilli() - startedAt.toInstant().toEpochMilli();
    }

    // -------------------------------------------------------------------------
    // Counters
    // -------------------------------------------------------------------------
    public void incrementStatesProcessed()    { statesProcessed++; }
    public void incrementStatesInserted()     { statesInserted++; }
    public void incrementStatesUpdated()      { statesUpdated++; }
    public void incrementStatesDeactivated()  { statesDeactivated++; }
    public void incrementStatesSkipped()      { statesSkipped++; }

    public void incrementDistrictsProcessed()   { districtsProcessed++; }
    public void incrementDistrictsInserted()    { districtsInserted++; }
    public void incrementDistrictsUpdated()     { districtsUpdated++; }
    public void incrementDistrictsDeactivated() { districtsDeactivated++; }
    public void incrementDistrictsSkipped()     { districtsSkipped++; }

    public void incrementMandalsProcessed()    { mandalsProcessed++; }
    public void incrementMandalsInserted()     { mandalsInserted++; }
    public void incrementMandalsUpdated()      { mandalsUpdated++; }
    public void incrementMandalsDeactivated()  { mandalsDeactivated++; }
    public void incrementMandalsSkipped()      { mandalsSkipped++; }

    public void incrementVillagesProcessed()   { villagesProcessed++; }
    public void incrementVillagesInserted()    { villagesInserted++; }
    public void incrementVillagesUpdated()     { villagesUpdated++; }
    public void incrementVillagesDeactivated() { villagesDeactivated++; }
    public void incrementVillagesSkipped()     { villagesSkipped++; }

    public void incrementPagesProcessed()      { pagesProcessed++; }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------
    public SyncType getSyncType()              { return syncType; }
    public OffsetDateTime getStartedAt()       { return startedAt; }
    public OffsetDateTime getCompletedAt()     { return completedAt; }

    public long getStatesProcessed()           { return statesProcessed; }
    public long getStatesInserted()            { return statesInserted; }
    public long getStatesUpdated()             { return statesUpdated; }
    public long getStatesDeactivated()         { return statesDeactivated; }
    public long getStatesSkipped()             { return statesSkipped; }

    public long getDistrictsProcessed()        { return districtsProcessed; }
    public long getDistrictsInserted()         { return districtsInserted; }
    public long getDistrictsUpdated()          { return districtsUpdated; }
    public long getDistrictsDeactivated()      { return districtsDeactivated; }
    public long getDistrictsSkipped()          { return districtsSkipped; }

    public long getMandalsProcessed()          { return mandalsProcessed; }
    public long getMandalsInserted()           { return mandalsInserted; }
    public long getMandalsUpdated()            { return mandalsUpdated; }
    public long getMandalsDeactivated()        { return mandalsDeactivated; }
    public long getMandalsSkipped()            { return mandalsSkipped; }

    public long getVillagesProcessed()         { return villagesProcessed; }
    public long getVillagesInserted()          { return villagesInserted; }
    public long getVillagesUpdated()           { return villagesUpdated; }
    public long getVillagesDeactivated()       { return villagesDeactivated; }
    public long getVillagesSkipped()           { return villagesSkipped; }

    public long getPagesProcessed()            { return pagesProcessed; }
    public long getStatesFailed()              { return statesFailed; }
    public boolean isOverallSuccess()          { return overallSuccess; }

    public List<String> getFailedStateCodes()  { return Collections.unmodifiableList(failedStateCodes); }
    public List<String> getWarnings()          { return Collections.unmodifiableList(warnings); }
    public List<String> getErrors()            { return Collections.unmodifiableList(errors); }

    @Override
    public String toString() {
        return "LgdSyncResult{" +
                "type=" + syncType +
                ", durationMs=" + getDurationMs() +
                ", states=[processed=" + statesProcessed + ",+=" + statesInserted + ",~=" + statesUpdated +
                    ",-=" + statesDeactivated + ",skip=" + statesSkipped + "]" +
                ", districts=[processed=" + districtsProcessed + ",+=" + districtsInserted + ",~=" + districtsUpdated +
                    ",-=" + districtsDeactivated + ",skip=" + districtsSkipped + "]" +
                ", mandals=[processed=" + mandalsProcessed + ",+=" + mandalsInserted + ",~=" + mandalsUpdated +
                    ",-=" + mandalsDeactivated + ",skip=" + mandalsSkipped + "]" +
                ", villages=[processed=" + villagesProcessed + ",+=" + villagesInserted + ",~=" + villagesUpdated +
                    ",-=" + villagesDeactivated + ",skip=" + villagesSkipped + "]" +
                ", pages=" + pagesProcessed +
                ", statesFailed=" + statesFailed +
                ", success=" + overallSuccess +
                ", errors=" + errors.size() +
                '}';
    }
}
