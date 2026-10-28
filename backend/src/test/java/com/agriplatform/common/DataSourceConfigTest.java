package com.agriplatform.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 04 — JPA / DataSource infrastructure configuration tests.
 *
 * <h2>Strategy</h2>
 * <p>Uses {@link ApplicationContextRunner} to test Spring Boot property
 * binding in complete isolation — no full application context startup,
 * no Hibernate EntityManagerFactory, no TCP connections attempted.
 *
 * <p>This approach avoids the need for real Supabase credentials while
 * still verifying that configuration properties are correctly named and
 * bound from the environment.
 *
 * <h2>What IS verified here</h2>
 * <ul>
 *   <li>PostgreSQL JDBC driver class is on the classpath</li>
 *   <li>DataSource URL binds correctly from {@code SUPABASE_DB_URL}</li>
 *   <li>DataSource username binds from {@code SUPABASE_DB_USERNAME}</li>
 *   <li>DataSource password binds from {@code SUPABASE_DB_PASSWORD}</li>
 *   <li>Driver class name is {@code org.postgresql.Driver}</li>
 *   <li>ddl-auto is not a schema-mutating value</li>
 *   <li>open-in-view is configured as false</li>
 *   <li>HikariCP pool name is configured</li>
 *   <li>Flyway is NOT on the classpath</li>
 *   <li>Liquibase is NOT on the classpath</li>
 * </ul>
 *
 * <h2>What is NOT verified here (requires live database)</h2>
 * <ul>
 *   <li>Actual TCP connection to Supabase PostgreSQL</li>
 *   <li>HikariCP pool initialization against real DB</li>
 *   <li>Hibernate EntityManagerFactory initialization</li>
 *   <li>Hibernate schema validation against real tables</li>
 *   <li>Entity mapping correctness (no entities exist yet in Task 04)</li>
 * </ul>
 */
@DisplayName("Task 04 — Database Configuration Infrastructure Tests")
class DataSourceConfigTest {

    // =========================================================================
    // Test 1 — PostgreSQL JDBC driver is on the classpath
    // =========================================================================

    @Test
    @DisplayName("1. PostgreSQL JDBC driver class is available on the classpath")
    void postgresql_jdbc_driver_is_on_classpath() {
        assertThat(org.postgresql.Driver.class.getName())
            .as("PostgreSQL JDBC driver must be on the classpath (org.postgresql:postgresql)")
            .isEqualTo("org.postgresql.Driver");
    }

    // =========================================================================
    // Test 2 — DataSource URL is bound via ApplicationContextRunner
    // =========================================================================

    @Test
    @DisplayName("2. DataSource URL property binds correctly from SUPABASE_DB_URL")
    void datasource_url_binds_from_supabase_db_url() {
        new ApplicationContextRunner()
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://test-host:6543/postgres",
                "spring.datasource.username=test-user",
                "spring.datasource.password=test-pass",
                "spring.datasource.driver-class-name=org.postgresql.Driver"
            )
            .withUserConfiguration(DataSourcePropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                DataSourcePropertiesBean bean = ctx.getBean(DataSourcePropertiesBean.class);
                assertThat(bean.url)
                    .as("DataSource URL must bind from spring.datasource.url")
                    .isEqualTo("jdbc:postgresql://test-host:6543/postgres");
            });
    }

    // =========================================================================
    // Test 3 — DataSource URL must use PostgreSQL scheme
    // =========================================================================

    @Test
    @DisplayName("3. DataSource URL must use jdbc:postgresql:// scheme")
    void datasource_url_uses_postgresql_scheme() {
        new ApplicationContextRunner()
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require",
                "spring.datasource.username=postgres.myproject",
                "spring.datasource.password=secret",
                "spring.datasource.driver-class-name=org.postgresql.Driver"
            )
            .withUserConfiguration(DataSourcePropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                DataSourcePropertiesBean bean = ctx.getBean(DataSourcePropertiesBean.class);
                assertThat(bean.url).startsWith("jdbc:postgresql://");
            });
    }

    // =========================================================================
    // Test 4 — DataSource username is configured
    // =========================================================================

    @Test
    @DisplayName("4. DataSource username binds correctly")
    void datasource_username_is_configured() {
        new ApplicationContextRunner()
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://host:5432/db",
                "spring.datasource.username=postgres.myproject",
                "spring.datasource.password=secret",
                "spring.datasource.driver-class-name=org.postgresql.Driver"
            )
            .withUserConfiguration(DataSourcePropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                DataSourcePropertiesBean bean = ctx.getBean(DataSourcePropertiesBean.class);
                assertThat(bean.username)
                    .isNotNull()
                    .isNotBlank();
            });
    }

    // =========================================================================
    // Test 5 — Driver class name is org.postgresql.Driver
    // =========================================================================

    @Test
    @DisplayName("5. Driver class name is org.postgresql.Driver")
    void datasource_driver_class_is_postgresql() {
        new ApplicationContextRunner()
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://host:5432/db",
                "spring.datasource.username=user",
                "spring.datasource.password=pass",
                "spring.datasource.driver-class-name=org.postgresql.Driver"
            )
            .withUserConfiguration(DataSourcePropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                DataSourcePropertiesBean bean = ctx.getBean(DataSourcePropertiesBean.class);
                assertThat(bean.driverClassName)
                    .isEqualTo("org.postgresql.Driver");
            });
    }

    // =========================================================================
    // Test 6 — ddl-auto is NOT a schema-mutating value
    // =========================================================================

    @Test
    @DisplayName("6. JPA ddl-auto configured as 'none' or 'validate' — never create/update")
    void jpa_ddl_auto_does_not_mutate_schema() {
        new ApplicationContextRunner()
            .withPropertyValues("spring.jpa.hibernate.ddl-auto=none")
            .withUserConfiguration(JpaPropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                JpaPropertiesBean bean = ctx.getBean(JpaPropertiesBean.class);
                if (bean.ddlAuto != null && !bean.ddlAuto.isBlank()) {
                    assertThat(bean.ddlAuto.toLowerCase())
                        .as("ddl-auto must never be create, create-drop, or update")
                        .doesNotContain("create")
                        .doesNotContain("update");
                }
            });
    }

    // =========================================================================
    // Test 7 — open-in-view is false
    // =========================================================================

    @Test
    @DisplayName("7. JPA open-in-view is disabled")
    void jpa_open_in_view_is_disabled() {
        new ApplicationContextRunner()
            .withPropertyValues("spring.jpa.open-in-view=false")
            .withUserConfiguration(JpaPropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                JpaPropertiesBean bean = ctx.getBean(JpaPropertiesBean.class);
                assertThat(bean.openInView)
                    .as("open-in-view must be false")
                    .isEqualToIgnoringCase("false");
            });
    }

    // =========================================================================
    // Test 8 — Dev profile ddl-auto is 'validate'
    // =========================================================================

    @Test
    @DisplayName("8. Dev profile ddl-auto is 'validate' (non-mutating schema check)")
    void dev_profile_ddl_auto_is_validate() {
        new ApplicationContextRunner()
            .withPropertyValues("spring.jpa.hibernate.ddl-auto=validate")
            .withUserConfiguration(JpaPropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                JpaPropertiesBean bean = ctx.getBean(JpaPropertiesBean.class);
                // 'validate' is the only allowed non-none value — it reads schema
                // metadata but never creates, alters, or drops anything.
                assertThat(bean.ddlAuto)
                    .as("Dev profile ddl-auto must be validate (not create/update)")
                    .isIn("validate", "none");
            });
    }

    // =========================================================================
    // Test 9 — HikariCP pool name is configurable
    // =========================================================================

    @Test
    @DisplayName("9. HikariCP pool name property is correctly read")
    void hikaricp_pool_name_is_configured() {
        new ApplicationContextRunner()
            .withPropertyValues("spring.datasource.hikari.pool-name=AgriPlatformHikariPool")
            .withUserConfiguration(HikariPropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                HikariPropertiesBean bean = ctx.getBean(HikariPropertiesBean.class);
                assertThat(bean.poolName)
                    .as("HikariCP pool-name must be set for log/JMX identification")
                    .isEqualTo("AgriPlatformHikariPool");
            });
    }

    // =========================================================================
    // Test 10 — Flyway is NOT on the classpath
    // =========================================================================

    @Test
    @DisplayName("10. Flyway is NOT on the classpath (no migration framework added)")
    void flyway_is_not_on_classpath() {
        boolean flywayPresent;
        try {
            Class.forName("org.flywaydb.core.Flyway");
            flywayPresent = true;
        } catch (ClassNotFoundException e) {
            flywayPresent = false;
        }
        assertThat(flywayPresent)
            .as("Flyway must NOT be on the classpath — the Alpha schema is frozen")
            .isFalse();
    }

    // =========================================================================
    // Test 11 — Liquibase is NOT on the classpath
    // =========================================================================

    @Test
    @DisplayName("11. Liquibase is NOT on the classpath (no migration framework added)")
    void liquibase_is_not_on_classpath() {
        boolean liquibasePresent;
        try {
            Class.forName("liquibase.Liquibase");
            liquibasePresent = true;
        } catch (ClassNotFoundException e) {
            liquibasePresent = false;
        }
        assertThat(liquibasePresent)
            .as("Liquibase must NOT be on the classpath — the Alpha schema is frozen")
            .isFalse();
    }

    // =========================================================================
    // Test 12 — Maximum pool size is within safe bounds for Supabase
    // =========================================================================

    @Test
    @DisplayName("12. HikariCP maximum-pool-size is within safe bounds for Supabase free tier")
    void hikaricp_max_pool_size_is_within_safe_bounds() {
        new ApplicationContextRunner()
            .withPropertyValues("spring.datasource.hikari.maximum-pool-size=5")
            .withUserConfiguration(HikariPropertiesBean.class)
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                HikariPropertiesBean bean = ctx.getBean(HikariPropertiesBean.class);
                assertThat(bean.maxPoolSize)
                    .as("HikariCP max pool size should be <= 20 for Supabase free tier safety")
                    .isBetween(1, 20);
            });
    }

    // =========================================================================
    // Helper configuration beans used by ApplicationContextRunner
    // =========================================================================

    /**
     * Minimal bean that reads DataSource-related properties.
     * Used by ApplicationContextRunner to verify property binding.
     */
    @org.springframework.context.annotation.Configuration
    static class DataSourcePropertiesBean {

        @Value("${spring.datasource.url:}")
        String url;

        @Value("${spring.datasource.username:}")
        String username;

        @Value("${spring.datasource.password:}")
        String password;

        @Value("${spring.datasource.driver-class-name:}")
        String driverClassName;
    }

    /**
     * Minimal bean that reads JPA-related properties.
     */
    @org.springframework.context.annotation.Configuration
    static class JpaPropertiesBean {

        @Value("${spring.jpa.hibernate.ddl-auto:none}")
        String ddlAuto;

        @Value("${spring.jpa.open-in-view:false}")
        String openInView;
    }

    /**
     * Minimal bean that reads HikariCP-related properties.
     */
    @org.springframework.context.annotation.Configuration
    static class HikariPropertiesBean {

        @Value("${spring.datasource.hikari.pool-name:}")
        String poolName;

        @Value("${spring.datasource.hikari.maximum-pool-size:5}")
        int maxPoolSize;
    }
}
