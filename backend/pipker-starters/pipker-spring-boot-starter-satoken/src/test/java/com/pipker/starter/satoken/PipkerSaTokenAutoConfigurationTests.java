package com.pipker.starter.satoken;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import com.pipker.starter.satoken.config.PipkerSaTokenAutoConfiguration;
import com.pipker.starter.satoken.dao.PipkerRedisSaTokenDao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PipkerSaTokenAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PipkerSaTokenAutoConfiguration.class));

    @Test
    void memoryStoreCreatesOnlyTheDefaultMemoryDao() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SaTokenDao.class);
            assertThat(context.getBean(SaTokenDao.class)).isInstanceOf(SaTokenDaoDefaultImpl.class);
        });
    }

    @Test
    void redisStoreCreatesPipkerRedisDaoWhenRedisInfrastructureExists() {
        contextRunner
                .withBean(StringRedisTemplate.class, PipkerSaTokenAutoConfigurationTests::redisTemplate)
                .withPropertyValues("pipker.security.auth.session-store=redis")
                .run(context -> {
                    assertThat(context).hasSingleBean(SaTokenDao.class);
                    assertThat(context.getBean(SaTokenDao.class)).isInstanceOf(PipkerRedisSaTokenDao.class);
                });
    }

    @Test
    void redisStoreFailsFastWhenRedisInfrastructureIsMissing() {
        contextRunner
                .withPropertyValues("pipker.security.auth.session-store=redis")
                .run(context -> assertThat(context).hasFailed());
    }

    private static StringRedisTemplate redisTemplate() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        return new StringRedisTemplate(connectionFactory);
    }
}
