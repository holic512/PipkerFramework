/**
 * @file DatabaseProfileValidatorTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 验证环境和数据库 Profile 选择规则。
 * @logic 直接测试校验器对合法组合、缺失环境或数据库以及多选冲突的处理，不启动外部数据库。
 * @dependencies Spring Test、JUnit Jupiter、AssertJ
 * @index_tags server、test、configuration、profile、database
 * @author holic512
 */
package com.pipker.server;

import com.pipker.server.config.DatabaseProfileValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseProfileValidatorTests {

    @Test
    void acceptsExactlyOneDatabaseProfile() {
        assertThatCode(() -> validator("dev", "sqlite").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingDatabaseProfile() {
        assertThatThrownBy(() -> validator("prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one database profile");
    }

    @Test
    void rejectsMissingEnvironmentProfile() {
        assertThatThrownBy(() -> validator("sqlite").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one environment profile");
    }

    @Test
    void rejectsMultipleDatabaseProfiles() {
        assertThatThrownBy(() -> validator("dev", "sqlite", "mysql").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active database profiles: [sqlite, mysql]");
    }

    @Test
    void rejectsMultipleEnvironmentProfiles() {
        assertThatThrownBy(() -> validator("dev", "prod", "sqlite").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active environment profiles: [dev, prod]");
    }

    private DatabaseProfileValidator validator(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return new DatabaseProfileValidator(environment);
    }
}
