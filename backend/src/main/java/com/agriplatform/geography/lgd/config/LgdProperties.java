package com.agriplatform.geography.lgd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed configuration properties for the official LGD/NAPIX API integration.
 *
 * <h2>Credential Policy</h2>
 * <ul>
 *   <li>{@code clientId} and {@code clientSecret} MUST be supplied via environment variables.</li>
 *   <li>They are NEVER logged, never returned via any API endpoint, never committed to source.</li>
 *   <li>Environment variables: {@code LGD_CLIENT_ID}, {@code LGD_CLIENT_SECRET}.</li>
 * </ul>
 *
 * <h2>LGD Code Policy</h2>
 * LGD uses authoritative integer codes (stateCode, districtCode, subdistrictCode, villageCode).
 * These are stored as strings in the existing {@code code} column of each geography table.
 * This policy avoids any schema change while preserving exact LGD identity for idempotent sync.
 */
@Component
@ConfigurationProperties(prefix = "app.lgd")
public class LgdProperties {

    /**
     * Base URL of the NAPIX LGD gateway.
     * Default: https://delhigw.napix.gov.in/nic/lgd (confirmed from official spec).
     */
    private String baseUrl = "https://delhigw.napix.gov.in/nic/lgd";

    /**
     * NAPIX API Client ID. Injected from environment variable {@code LGD_CLIENT_ID}.
     * NEVER set a real value in application.yml or any source-controlled file.
     */
    private String clientId;

    /**
     * NAPIX API Client Secret. Injected from environment variable {@code LGD_CLIENT_SECRET}.
     * NEVER set a real value in application.yml or any source-controlled file.
     */
    private String clientSecret;

    /**
     * Cron expression for periodic incremental synchronization.
     * Default: every Sunday at 02:00 IST (weekly — appropriate for LGD update frequency).
     */
    private String syncCron = "0 0 2 * * SUN";

    /**
     * Whether the scheduler is enabled at all.
     * Set to false in test/CI environments to prevent scheduled sync.
     */
    private boolean schedulerEnabled = false;

    /**
     * Connection timeout in milliseconds for LGD API calls.
     */
    private int connectTimeoutMs = 15000;

    /**
     * Read timeout in milliseconds for LGD API calls.
     */
    private int readTimeoutMs = 60000;

    /**
     * Maximum number of retry attempts for transient failures.
     * Applies only to 5xx and connection errors.
     */
    private int maxRetries = 3;

    /**
     * Initial backoff delay in milliseconds between retries.
     * Each retry doubles this (exponential backoff).
     */
    private long retryBackoffMs = 1000;

    /**
     * Minimum delay in milliseconds between successive LGD API calls.
     * Guards against rate limiting. The official spec does NOT establish a
     * specific rate limit — this is a conservative default.
     * Configurable without code change via application config.
     */
    private long requestDelayMs = 500;

    // -------------------------------------------------------------------------
    // Getters and Setters — explicit (no Lombok: Java 25 compat)
    // -------------------------------------------------------------------------

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getSyncCron() { return syncCron; }
    public void setSyncCron(String syncCron) { this.syncCron = syncCron; }

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }

    public long getRequestDelayMs() { return requestDelayMs; }
    public void setRequestDelayMs(long requestDelayMs) { this.requestDelayMs = requestDelayMs; }

    /**
     * Validates that credentials are configured. Does NOT log credentials.
     *
     * @return true if both clientId and clientSecret are non-blank.
     */
    public boolean areCredentialsConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    @Override
    public String toString() {
        // NEVER include clientId or clientSecret in toString
        return "LgdProperties{" +
                "baseUrl='" + baseUrl + '\'' +
                ", clientIdConfigured=" + (clientId != null && !clientId.isBlank()) +
                ", clientSecretConfigured=" + (clientSecret != null && !clientSecret.isBlank()) +
                ", syncCron='" + syncCron + '\'' +
                ", schedulerEnabled=" + schedulerEnabled +
                ", connectTimeoutMs=" + connectTimeoutMs +
                ", readTimeoutMs=" + readTimeoutMs +
                ", maxRetries=" + maxRetries +
                ", retryBackoffMs=" + retryBackoffMs +
                ", requestDelayMs=" + requestDelayMs +
                '}';
    }
}
