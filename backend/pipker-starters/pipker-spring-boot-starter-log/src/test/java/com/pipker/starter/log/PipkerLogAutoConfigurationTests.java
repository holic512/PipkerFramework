package com.pipker.starter.log;

import com.pipker.starter.log.annotation.OperationLog;
import com.pipker.starter.log.config.PipkerLogAutoConfiguration;
import com.pipker.starter.log.operation.OperationLogHandler;
import com.pipker.starter.log.operation.OperationLogRecord;
import com.pipker.starter.log.sensitive.LogSanitizer;
import com.pipker.starter.log.sensitive.LogValueRenderer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipkerLogAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, PipkerLogAutoConfiguration.class));

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, PipkerLogAutoConfiguration.class));

    @Test
    void disabledStarterDoesNotCreateCoreOrOperationBeans() {
        contextRunner
                .withPropertyValues("pipker.log.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LogSanitizer.class);
                    assertThat(context).doesNotHaveBean(LogValueRenderer.class);
                    assertThat(context).doesNotHaveBean(OperationLogHandler.class);
                });
    }

    @Test
    void nonWebContextDoesNotRegisterRequestFilter() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
    }

    @Test
    void allWebRequestFeaturesDisabledSkipsSharedFilterRegistration() {
        webContextRunner
                .withPropertyValues(
                        "pipker.log.trace.enabled=false",
                        "pipker.log.request.enabled=false",
                        "pipker.log.slow-request.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
    }

    @Test
    void customOperationHandlerReplacesDefaultAndReceivesSafeRecords() {
        contextRunner
                .withUserConfiguration(OperationTestConfiguration.class)
                .withPropertyValues(
                        "pipker.log.operation.record-parameters=true",
                        "pipker.log.operation.record-result=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OperationLogHandler.class);
                    CapturingOperationLogHandler handler = context.getBean(CapturingOperationLogHandler.class);
                    OperationTestService service = context.getBean(OperationTestService.class);

                    assertThat(service.create(Map.of("password", "plain-secret", "name", "Pipker")))
                            .isEqualTo("created");
                    assertThat(handler.records).hasSize(1);
                    OperationLogRecord record = handler.records.getFirst();
                    assertThat(record.success()).isTrue();
                    assertThat(record.parameters()).contains("******").doesNotContain("plain-secret");
                    assertThat(record.result()).isEqualTo("\"created\"");

                    assertThatThrownBy(service::fail).isInstanceOf(IllegalStateException.class);
                    assertThat(handler.records).hasSize(2);
                    assertThat(handler.records.getLast().success()).isFalse();
                    assertThat(handler.records.getLast().exceptionType()).isEqualTo(IllegalStateException.class.getName());
                });
    }

    @Test
    void failingOperationHandlerDoesNotChangeBusinessResult() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.pipker.starter.log.operation.OperationLogAspect");
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            contextRunner
                    .withUserConfiguration(FailingOperationHandlerConfiguration.class)
                    .run(context -> assertThat(context.getBean(OperationTestService.class).create(Map.of("name", "Pipker")))
                            .isEqualTo("created"));
        } finally {
            logger.setLevel(previousLevel);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OperationTestConfiguration {

        @Bean
        OperationTestService operationTestService() {
            return new OperationTestService();
        }

        @Bean
        CapturingOperationLogHandler operationLogHandler() {
            return new CapturingOperationLogHandler();
        }
    }

    static class OperationTestService {

        @OperationLog(module = "测试", operation = "创建")
        public String create(Map<String, String> payload) {
            return "created";
        }

        @OperationLog(module = "测试", operation = "失败")
        public void fail() {
            throw new IllegalStateException("operation failure");
        }
    }

    static class CapturingOperationLogHandler implements OperationLogHandler {

        private final List<OperationLogRecord> records = new ArrayList<>();

        @Override
        public void handle(OperationLogRecord record) {
            records.add(record);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class FailingOperationHandlerConfiguration {

        @Bean
        OperationTestService operationTestService() {
            return new OperationTestService();
        }

        @Bean
        OperationLogHandler operationLogHandler() {
            return record -> {
                throw new IllegalStateException("storage unavailable");
            };
        }
    }
}
