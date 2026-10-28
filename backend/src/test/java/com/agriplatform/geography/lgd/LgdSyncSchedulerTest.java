package com.agriplatform.geography.lgd;

import com.agriplatform.geography.lgd.config.LgdProperties;
import com.agriplatform.geography.lgd.sync.LgdSyncOperations;
import com.agriplatform.geography.lgd.sync.LgdSyncResult;
import com.agriplatform.geography.lgd.sync.LgdSyncScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LgdSyncScheduler}.
 *
 * <p>Tests verify: scheduler-disabled guard, credentials guard, sync delegation,
 * sync-in-progress guard, and safe failure handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LgdSyncScheduler — Unit Tests")
class LgdSyncSchedulerTest {

    @Mock private LgdSyncOperations syncService;

    private LgdProperties props;
    private LgdSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        props = new LgdProperties();
        scheduler = new LgdSyncScheduler(syncService, props);
    }

    // =========================================================================
    // SCHEDULER DISABLED (default)
    // =========================================================================

    @Test
    @DisplayName("Scheduled sync does nothing when scheduler-enabled=false (default)")
    void scheduledSync_disabled_doesNothing() {
        // Default: schedulerEnabled=false
        scheduler.runScheduledIncrementalSync();
        verifyNoInteractions(syncService);
    }

    // =========================================================================
    // CREDENTIALS GUARD
    // =========================================================================

    @Test
    @DisplayName("Scheduled sync does nothing when scheduler enabled but credentials absent")
    void scheduledSync_enabled_noCredentials_doesNothing() {
        props.setSchedulerEnabled(true);
        // No credentials set
        scheduler.runScheduledIncrementalSync();
        verifyNoInteractions(syncService);
    }

    // =========================================================================
    // SYNC IN PROGRESS GUARD
    // =========================================================================

    @Test
    @DisplayName("Scheduled sync skips when a sync is already in progress")
    void scheduledSync_alreadyInProgress_skips() {
        props.setSchedulerEnabled(true);
        props.setClientId("test-client-id");
        props.setClientSecret("test-client-secret");

        when(syncService.isSyncInProgress()).thenReturn(true);

        scheduler.runScheduledIncrementalSync();

        verify(syncService, times(1)).isSyncInProgress();
        verify(syncService, never()).runIncrementalSync(any(), any());
    }

    // =========================================================================
    // SUCCESSFUL SCHEDULED SYNC
    // =========================================================================

    @Test
    @DisplayName("Scheduled sync delegates to runIncrementalSync with DD-MM-YYYY dates")
    void scheduledSync_enabled_withCredentials_delegates() {
        props.setSchedulerEnabled(true);
        props.setClientId("test-client-id");
        props.setClientSecret("test-client-secret");

        LgdSyncResult result = new LgdSyncResult(LgdSyncResult.SyncType.INCREMENTAL);
        result.complete();

        when(syncService.isSyncInProgress()).thenReturn(false);
        when(syncService.runIncrementalSync(anyString(), anyString())).thenReturn(result);

        scheduler.runScheduledIncrementalSync();

        verify(syncService, times(1)).runIncrementalSync(anyString(), anyString());
    }

    @Test
    @DisplayName("Scheduled sync passes DD-MM-YYYY formatted dates")
    void scheduledSync_dateFormat_isDdMmYyyy() {
        props.setSchedulerEnabled(true);
        props.setClientId("test-client-id");
        props.setClientSecret("test-client-secret");

        LgdSyncResult result = new LgdSyncResult(LgdSyncResult.SyncType.INCREMENTAL);
        result.complete();

        when(syncService.isSyncInProgress()).thenReturn(false);

        org.mockito.ArgumentCaptor<String> fromCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> toCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        when(syncService.runIncrementalSync(fromCaptor.capture(), toCaptor.capture())).thenReturn(result);

        scheduler.runScheduledIncrementalSync();

        String from = fromCaptor.getValue();
        String to = toCaptor.getValue();

        // DD-MM-YYYY format: exactly 10 chars, parts separated by '-'
        assertThat(from).matches("\\d{2}-\\d{2}-\\d{4}");
        assertThat(to).matches("\\d{2}-\\d{2}-\\d{4}");
        // 'from' should be before 'to' (7 days ago vs today)
        assertThat(from).isNotEqualTo(to);
    }

    // =========================================================================
    // FAILURE SAFETY
    // =========================================================================

    @Test
    @DisplayName("Scheduled sync does not propagate exceptions — application must not crash")
    void scheduledSync_exceptionInSync_doesNotPropagate() {
        props.setSchedulerEnabled(true);
        props.setClientId("test-client-id");
        props.setClientSecret("test-client-secret");

        when(syncService.isSyncInProgress()).thenReturn(false);
        when(syncService.runIncrementalSync(anyString(), anyString()))
                .thenThrow(new RuntimeException("Network failure"));

        // Must NOT throw — scheduler must never crash the Spring application
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> scheduler.runScheduledIncrementalSync());
    }
}
