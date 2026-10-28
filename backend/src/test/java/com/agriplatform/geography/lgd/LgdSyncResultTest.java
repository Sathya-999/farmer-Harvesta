package com.agriplatform.geography.lgd;

import com.agriplatform.geography.lgd.sync.LgdSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgdSyncResult}.
 */
@DisplayName("LgdSyncResult — Unit Tests")
class LgdSyncResultTest {

    private LgdSyncResult result;

    @BeforeEach
    void setUp() {
        result = new LgdSyncResult(LgdSyncResult.SyncType.INITIAL);
    }

    @Test
    @DisplayName("Initial state: overallSuccess=true, all counters zero")
    void initialState() {
        assertThat(result.isOverallSuccess()).isTrue();
        assertThat(result.getStatesInserted()).isZero();
        assertThat(result.getDistrictsInserted()).isZero();
        assertThat(result.getMandalsInserted()).isZero();
        assertThat(result.getVillagesInserted()).isZero();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("addError() marks overallSuccess=false")
    void addError_marksFailure() {
        result.addError("Something went wrong");
        assertThat(result.isOverallSuccess()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    @DisplayName("addWarning() does NOT affect overallSuccess")
    void addWarning_doesNotAffectSuccess() {
        result.addWarning("Minor issue");
        assertThat(result.isOverallSuccess()).isTrue();
        assertThat(result.getWarnings()).hasSize(1);
    }

    @Test
    @DisplayName("addFailedStateCode() marks failure and increments statesFailed")
    void addFailedStateCode_marksFailure() {
        result.addFailedStateCode("28");
        assertThat(result.isOverallSuccess()).isFalse();
        assertThat(result.getStatesFailed()).isEqualTo(1);
        assertThat(result.getFailedStateCodes()).contains("28");
    }

    @Test
    @DisplayName("complete() sets completedAt and getDurationMs() returns positive")
    void complete_setsDuration() throws InterruptedException {
        Thread.sleep(5);
        result.complete();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getDurationMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Counter increments work correctly")
    void counterIncrements() {
        result.incrementStatesInserted();
        result.incrementStatesInserted();
        result.incrementDistrictsUpdated();
        result.incrementMandalsDeactivated();
        result.incrementVillagesSkipped();
        result.incrementPagesProcessed();

        assertThat(result.getStatesInserted()).isEqualTo(2);
        assertThat(result.getDistrictsUpdated()).isEqualTo(1);
        assertThat(result.getMandalsDeactivated()).isEqualTo(1);
        assertThat(result.getVillagesSkipped()).isEqualTo(1);
        assertThat(result.getPagesProcessed()).isEqualTo(1);
    }

    @Test
    @DisplayName("toString() contains key observable fields")
    void toString_containsKeyFields() {
        result.incrementStatesInserted();
        String str = result.toString();
        assertThat(str).contains("INITIAL");
        assertThat(str).contains("states=[");
    }

    @Test
    @DisplayName("getFailedStateCodes() returns unmodifiable list")
    void failedStateCodes_isUnmodifiable() {
        result.addFailedStateCode("5");
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> result.getFailedStateCodes().add("6"));
    }
}
