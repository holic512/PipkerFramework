package com.pipker.starter.file;

import com.pipker.starter.file.config.PipkerFileAutoConfiguration;
import com.pipker.starter.file.config.PipkerFileProperties;
import com.pipker.starter.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PipkerFileAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PipkerFileAutoConfiguration.class));

    @Test
    void defaultsToEnabledLocalStorageWithoutCreatingItsDirectory() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FileStorageService.class);
            PipkerFileProperties properties = context.getBean(PipkerFileProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getLocal().getRoot()).isEqualTo("./data/file");
            assertThat(properties.getAccessPath()).isEqualTo("/files");
        });
    }

    @Test
    void disablingTheFeatureRegistersNeitherTheServiceNorPropertiesBean() {
        contextRunner
                .withPropertyValues("pipker.file.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FileStorageService.class);
                    assertThat(context).doesNotHaveBean(PipkerFileProperties.class);
                });
    }

    @Test
    void unsafeAccessPathFailsConfigurationValidation() {
        contextRunner
                .withPropertyValues("pipker.file.access-path=/files/**")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("Could not bind properties to 'PipkerFileProperties'");
                });
    }
}
