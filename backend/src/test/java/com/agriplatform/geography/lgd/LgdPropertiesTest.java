package com.agriplatform.geography.lgd;

import com.agriplatform.geography.lgd.config.LgdProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgdProperties}.
 */
@DisplayName("LgdProperties — Unit Tests")
class LgdPropertiesTest {

    private LgdProperties props;

    @BeforeEach
    void setUp() {
        props = new LgdProperties();
    }

    @Test
    @DisplayName("Default base URL matches official LGD NAPIX spec")
    void defaultBaseUrl() {
        assertThat(props.getBaseUrl())
                .isEqualTo("https://delhigw.napix.gov.in/nic/lgd");
    }

    @Test
    @DisplayName("areCredentialsConfigured() returns false when both are null (default)")
    void credentialsNotConfiguredWhenNull() {
        assertThat(props.areCredentialsConfigured()).isFalse();
    }

    @Test
    @DisplayName("areCredentialsConfigured() returns false when clientId is blank")
    void credentialsNotConfiguredWhenClientIdBlank() {
        props.setClientId("  ");
        props.setClientSecret("secret123");
        assertThat(props.areCredentialsConfigured()).isFalse();
    }

    @Test
    @DisplayName("areCredentialsConfigured() returns false when clientSecret is blank")
    void credentialsNotConfiguredWhenClientSecretBlank() {
        props.setClientId("client123");
        props.setClientSecret("");
        assertThat(props.areCredentialsConfigured()).isFalse();
    }

    @Test
    @DisplayName("areCredentialsConfigured() returns true when both are non-blank")
    void credentialsConfiguredWhenBothPresent() {
        props.setClientId("test-client-id");
        props.setClientSecret("test-client-secret");
        assertThat(props.areCredentialsConfigured()).isTrue();
    }

    @Test
    @DisplayName("toString() does NOT expose clientId or clientSecret")
    void toStringDoesNotLeakCredentials() {
        props.setClientId("super-secret-client-id");
        props.setClientSecret("super-secret-client-secret");

        String str = props.toString();

        assertThat(str).doesNotContain("super-secret-client-id");
        assertThat(str).doesNotContain("super-secret-client-secret");
        assertThat(str).contains("clientIdConfigured=true");
        assertThat(str).contains("clientSecretConfigured=true");
    }

    @Test
    @DisplayName("Default scheduler is disabled")
    void defaultSchedulerDisabled() {
        assertThat(props.isSchedulerEnabled()).isFalse();
    }

    @Test
    @DisplayName("Default retry settings are sane")
    void defaultRetrySettings() {
        assertThat(props.getMaxRetries()).isEqualTo(3);
        assertThat(props.getRetryBackoffMs()).isEqualTo(1000L);
        assertThat(props.getRequestDelayMs()).isEqualTo(500L);
    }

    @Test
    @DisplayName("Default timeouts are sane")
    void defaultTimeouts() {
        assertThat(props.getConnectTimeoutMs()).isEqualTo(15000);
        assertThat(props.getReadTimeoutMs()).isEqualTo(60000);
    }
}
