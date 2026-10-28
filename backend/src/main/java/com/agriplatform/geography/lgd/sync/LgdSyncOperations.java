package com.agriplatform.geography.lgd.sync;

/**
 * Contract for LGD synchronization operations.
 *
 * <p>Decoupling via interface allows:
 * <ul>
 *   <li>Standard JDK proxy-based mocking in tests (no Byte Buddy inline instrumentation required).</li>
 *   <li>Future implementation alternatives (async, event-driven).</li>
 *   <li>Clean dependency injection in controllers.</li>
 * </ul>
 */
public interface LgdSyncOperations {

    /**
     * Performs a complete initial synchronization from LGD (all India, paginated).
     * Idempotent: safe to run multiple times.
     */
    LgdSyncResult runInitialSync();

    /**
     * Performs an incremental synchronization for the specified date range.
     *
     * @param from start date in DD-MM-YYYY format (required by LGD duration APIs)
     * @param to   end date in DD-MM-YYYY format
     */
    LgdSyncResult runIncrementalSync(String from, String to);

    /**
     * Returns true if a sync is currently running.
     */
    boolean isSyncInProgress();
}
