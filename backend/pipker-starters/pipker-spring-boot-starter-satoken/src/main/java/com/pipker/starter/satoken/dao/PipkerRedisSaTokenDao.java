/**
 * @file PipkerRedisSaTokenDao.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Sa-Token
 * @description Implements Sa-Token persistence with Spring Data Redis while preserving Sa-Token's string and object session contracts.
 * @logic Prefixes all Sa-Token keys, delegates string values and TTL operations to StringRedisTemplate, serializes objects through Sa-Token's configured serializer, and scans keys without Redis KEYS commands.
 * @dependencies Sa-Token Core, Spring Data Redis
 * @index_tags starter,sa-token,redis,dao,session
 * @author holic512
 */
package com.pipker.starter.satoken.dao;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.auto.SaTokenDaoByObjectFollowString;
import cn.dev33.satoken.util.SaFoxUtil;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PipkerRedisSaTokenDao implements SaTokenDaoByObjectFollowString {

    private static final String KEY_PREFIX = "pipker:sa-token:";

    private final StringRedisTemplate redisTemplate;

    public PipkerRedisSaTokenDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(wrapKey(key));
    }

    @Override
    public void set(String key, String value, long timeout) {
        if (timeout == 0 || timeout <= NOT_VALUE_EXPIRE) {
            return;
        }

        String wrappedKey = wrapKey(key);
        if (timeout == NEVER_EXPIRE) {
            redisTemplate.opsForValue().set(wrappedKey, value);
            return;
        }
        redisTemplate.opsForValue().set(wrappedKey, value, timeout, TimeUnit.SECONDS);
    }

    @Override
    public void update(String key, String value) {
        String wrappedKey = wrapKey(key);
        redisTemplate.execute((RedisCallback<Boolean>) connection -> connection.set(
                redisTemplate.getStringSerializer().serialize(wrappedKey),
                redisTemplate.getStringSerializer().serialize(value),
                Expiration.keepTtl(),
                RedisStringCommands.SetOption.ifPresent()
        ));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(wrapKey(key));
    }

    @Override
    public long getTimeout(String key) {
        Long timeout = redisTemplate.getExpire(wrapKey(key), TimeUnit.SECONDS);
        return timeout == null ? NOT_VALUE_EXPIRE : timeout;
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        String wrappedKey = wrapKey(key);
        if (timeout == NEVER_EXPIRE) {
            if (getTimeout(key) != NEVER_EXPIRE) {
                String value = get(key);
                if (value != null) {
                    redisTemplate.opsForValue().set(wrappedKey, value);
                }
            }
            return;
        }
        redisTemplate.expire(wrappedKey, timeout, TimeUnit.SECONDS);
    }

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        String pattern = wrapKey(prefix + "*" + keyword + "*");
        Set<String> matchingKeys = new LinkedHashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1_000).build();

        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    String key = redisTemplate.getStringSerializer().deserialize(cursor.next());
                    if (key != null) {
                        matchingKeys.add(unwrapKey(key));
                    }
                }
            }
            return null;
        });

        return SaFoxUtil.searchList(new ArrayList<>(matchingKeys), start, size, sortType);
    }

    private String wrapKey(String key) {
        return KEY_PREFIX + key;
    }

    private String unwrapKey(String key) {
        return key.startsWith(KEY_PREFIX) ? key.substring(KEY_PREFIX.length()) : key;
    }
}
