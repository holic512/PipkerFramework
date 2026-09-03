/**
 * @file DatabaseProfileValidator.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 校验运行时环境和数据库是否选择了合法且唯一的 Profile。
 * @logic 测试 Profile 使用测试资源中的 H2；其他环境必须且只能启用 dev 或 prod 之一，以及 sqlite、mysql 或 postgresql 之一。
 * @dependencies Spring Core Environment、Spring Context
 * @index_tags server、configuration、profile、database、validation
 * @author holic512
 */
package com.pipker.server.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 防止多个数据库配置同时生效或生产环境意外没有数据库配置。
 */
@Component
@Profile("!test")
public final class DatabaseProfileValidator implements InitializingBean {

    private static final List<String> ENVIRONMENT_PROFILES = List.of("dev", "prod");
    private static final List<String> DATABASE_PROFILES = List.of("sqlite", "mysql", "postgresql");

    private final Environment environment;

    public DatabaseProfileValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String[] selectedProfiles = selectedProfiles();
        List<String> activeEnvironmentProfiles = Arrays.stream(selectedProfiles)
                .filter(ENVIRONMENT_PROFILES::contains)
                .toList();
        if (activeEnvironmentProfiles.size() != 1) {
            throw new IllegalStateException(
                    "Exactly one environment profile must be active: dev or prod; "
                            + "active environment profiles: " + activeEnvironmentProfiles
            );
        }

        List<String> activeDatabaseProfiles = Arrays.stream(selectedProfiles)
                .filter(DATABASE_PROFILES::contains)
                .toList();
        if (activeDatabaseProfiles.size() != 1) {
            throw new IllegalStateException(
                    "Exactly one database profile must be active: sqlite, mysql, or postgresql; "
                            + "active database profiles: " + activeDatabaseProfiles
            );
        }
    }

    private String[] selectedProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? environment.getDefaultProfiles() : activeProfiles;
    }
}
