package com.pipker.starter.log;

import com.pipker.starter.log.annotation.Sensitive;
import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.sensitive.DefaultLogSanitizer;
import com.pipker.starter.log.sensitive.SensitiveType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultLogSanitizerTests {

    private final DefaultLogSanitizer sanitizer = new DefaultLogSanitizer(new PipkerLogProperties());

    @Test
    void masksDefaultFieldNamesAndAnnotatedRecordComponents() {
        Object sanitized = sanitizer.sanitize(Map.of(
                "password", "plain-secret",
                "accessToken", "token-value",
                "phone", "13812345678",
                "profile", new Profile("alice@example.com", "6222020202020202")
        ));

        assertThat(sanitized.toString())
                .contains("password=******")
                .contains("accessToken=******")
                .contains("138******5678")
                .contains("al******@example.com")
                .contains("6222******0202")
                .doesNotContain("plain-secret")
                .doesNotContain("token-value");
    }

    @Test
    void handlesCircularObjectsWithoutThrowing() {
        CircularValue value = new CircularValue();
        value.next = value;

        assertThat(sanitizer.sanitize(value).toString()).contains("<circular-reference>");
    }

    record Profile(@Sensitive(SensitiveType.EMAIL) String email, @Sensitive(SensitiveType.BANK_CARD) String bankCard) {
    }

    static class CircularValue {
        private CircularValue next;

        public CircularValue getNext() {
            return next;
        }
    }
}
