package com.pipker.starter.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.context.LogMdcKeys;
import com.pipker.starter.log.context.MdcTaskDecorator;
import com.pipker.starter.log.exception.DefaultExceptionLogReporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionAndMdcExtensionTests {

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    void exceptionReporterKeepsStackAndDeduplicatesTheRequest() {
        Logger logger = (Logger) LoggerFactory.getLogger("pipker.exception");
        boolean previousAdditivity = logger.isAdditive();
        logger.setAdditive(false);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/failure");
            MDC.put(LogMdcKeys.TRACE_ID, "trace-123");
            DefaultExceptionLogReporter reporter = new DefaultExceptionLogReporter(new PipkerLogProperties());

            reporter.report(request, new IllegalStateException("failed operation"));
            reporter.report(request, new IllegalStateException("same request must not duplicate"));

            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage()).contains("trace-123").contains("POST");
            assertThat(appender.list.getFirst().getThrowableProxy()).isNotNull();
        } finally {
            logger.detachAppender(appender);
            logger.setAdditive(previousAdditivity);
        }
    }

    @Test
    void taskDecoratorCopiesAndCleansMdcWithoutOwningAnExecutor() {
        MDC.put(LogMdcKeys.TRACE_ID, "trace-async");
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicReference<String> traceInTask = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> traceInTask.set(MDC.get(LogMdcKeys.TRACE_ID)));
        MDC.clear();

        decorated.run();

        assertThat(traceInTask).hasValue("trace-async");
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }
}
