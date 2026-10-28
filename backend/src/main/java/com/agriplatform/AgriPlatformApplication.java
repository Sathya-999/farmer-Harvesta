package com.agriplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Agricultural Intelligence Platform — Application Entry Point.
 *
 * <p>This is a Modular Monolith. All feature modules (auth, farmer, plot,
 * cultivation, history, analytics, notification, geography, common) reside
 * under the {@code com.agriplatform} root package and are managed by
 * Spring's component-scan, which is anchored here.
 *
 * <p>Do NOT move this class out of the root package.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class AgriPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriPlatformApplication.class, args);
    }
}
