package com.agriplatform.geography.lgd.sync;

import com.agriplatform.geography.lgd.config.LgdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Scheduled periodic LGD synchronization.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>Cron expression is configurable via {@code app.lgd.sync-cron}.</li>
 *   <li>Scheduler is opt-in via {@code app.lgd.scheduler-enabled=true} (default: false).</li>
 *   <li>Does NOT prevent application startup if LGD is unavailable.</li>
 *   <li>Overlapping execution is prevented by {@link LgdSyncService#isSyncInProgress()}.</li>
 *   <li>Incremental window: last 8 days → today (weekly with 1 day overlap for safety).</li>
 * </ul>
 *
 * <h2>Enabling the scheduler</h2>
 * Set in application.yml:
 * <pre>
 *   app:
 *     lgd:
 *       scheduler-enabled: true
 *       sync-cron: "0 0 2 * * SUN"   # Every Sunday at 02:00
 * </pre>
 *
 * <h2>Rate limit note</h2>
 * The official LGD NAPIX specification does NOT establish a definitive rate limit.
 * The configurable {@code app.lgd.request-delay-ms} is used as a conservative guard.
 */
@Component
public class LgdSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LgdSyncScheduler.class);

    /** DD-MM-YYYY format required by all LGD duration APIs. */
    private static final DateTimeFormatter LGD_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final LgdSyncOperations syncService;
    private final LgdProperties props;

    public LgdSyncScheduler(LgdSyncOperations syncService, LgdProperties props) {
        this.syncService = syncService;
        this.props = props;
    }

    /**
     * Weekly incremental synchronization.
     * Cron expression from config; default = every Sunday at 02:00.
     *
     * <p>Runs only when {@code app.lgd.scheduler-enabled=true} AND credentials are configured.
     * Safe to have scheduler-enabled=false in dev/test environments.
     */
    @Scheduled(cron = "${app.lgd.sync-cron:0 0 2 * * SUN}")
    public void runScheduledIncrementalSync() {
        if (!props.isSchedulerEnabled()) {
            log.debug("[LGD-SCHEDULER] Scheduled sync is disabled (app.lgd.scheduler-enabled=false). Skipping.");
            return;
        }

        if (!props.areCredentialsConfigured()) {
            log.warn("[LGD-SCHEDULER] Skipping scheduled sync — LGD credentials not configured. " +
                    "Set LGD_CLIENT_ID and LGD_CLIENT_SECRET environment variables.");
            return;
        }

        if (syncService.isSyncInProgress()) {
            log.warn("[LGD-SCHEDULER] Sync already in progress — skipping scheduled trigger.");
            return;
        }

        // Incremental window: past 8 days → today (weekly + 1 day overlap)
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(8);

        String fromStr = from.format(LGD_DATE_FORMAT);
        String toStr   = today.format(LGD_DATE_FORMAT);

        log.info("[LGD-SCHEDULER] Starting scheduled incremental sync: {} → {}", fromStr, toStr);
        try {
            LgdSyncResult result = syncService.runIncrementalSync(fromStr, toStr);
            log.info("[LGD-SCHEDULER] Scheduled sync completed: success={}, states={}, districts={}, mandals={}, villages={}",
                    result.isOverallSuccess(),
                    result.getStatesProcessed(),
                    result.getDistrictsProcessed(),
                    result.getMandalsProcessed(),
                    result.getVillagesProcessed());
        } catch (Exception e) {
            // Fail safely — scheduler must not crash the application
            log.error("[LGD-SCHEDULER] Scheduled sync encountered an error: {}", e.getMessage(), e);
        }
    }
}
