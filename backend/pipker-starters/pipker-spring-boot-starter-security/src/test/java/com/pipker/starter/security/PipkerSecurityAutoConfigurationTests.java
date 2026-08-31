package com.pipker.starter.security;

import com.pipker.starter.security.config.PipkerSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PipkerSecurityAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PipkerSecurityAutoConfiguration.class));

    @Test
    void missingKeyForTheSelectedAlgorithmFailsContextStartup() {
        contextRunner
                .withPropertyValues("pipker.security.crypto.encryption.algorithm=aes-gcm")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("AES-GCM key must be configured");
                });
    }
}
