/**
 * 文件：PipkerRedisSaTokenDao.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Sa-Token
 * 说明：使用 Spring Data Redis 实现 Sa-Token 持久化，同时保留 Sa-Token 的字符串和对象会话契约。
 * 处理逻辑：为所有 Sa-Token 键添加前缀，将字符串值和 TTL 操作委托给 StringRedisTemplate，通过 Sa-Token 配置的序列化器处理对象，并使用扫描方式读取键而不执行 Redis KEYS 命令。
 * 依赖：Sa-Token Core、Spring Data Redis
 * 检索关键词：starter、sa-token、redis、DAO、会话
 * 作者：holic512
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

/**
 * 使用字符串 Redis 模板保存 Sa-Token 数据的 DAO 实现。
 *
 * <p>所有键都带有 Pipker 专用前缀，搜索使用 Redis SCAN，避免执行阻塞式 {@code KEYS}。</p>
 */
public class PipkerRedisSaTokenDao implements SaTokenDaoByObjectFollowString {

    private static final String KEY_PREFIX = "pipker:sa-token:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 创建 Redis 会话 DAO。
     *
     * @param redisTemplate 字符串 Redis 模板
     */
    public PipkerRedisSaTokenDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读取指定键的字符串值。
     *
     * @param key Sa-Token 原始键
     * @return Redis 中保存的值，不存在时为空
     */
    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(wrapKey(key));
    }

    /**
     * 按秒写入值，并处理立即过期和永久有效两种特殊 TTL。
     *
     * @param key Sa-Token 原始键
     * @param value 待保存的字符串值
     * @param timeout 有效期，单位为秒
     */
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

    /**
     * 在保留原 TTL 的前提下更新已存在的值。
     *
     * @param key Sa-Token 原始键
     * @param value 新字符串值
     */
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

    /**
     * 删除指定会话键。
     *
     * @param key Sa-Token 原始键
     */
    @Override
    public void delete(String key) {
        redisTemplate.delete(wrapKey(key));
    }

    /**
     * 查询指定会话键的剩余有效期。
     *
     * @param key Sa-Token 原始键
     * @return 剩余秒数，按 Sa-Token 契约返回特殊过期值
     */
    @Override
    public long getTimeout(String key) {
        Long timeout = redisTemplate.getExpire(wrapKey(key), TimeUnit.SECONDS);
        return timeout == null ? NOT_VALUE_EXPIRE : timeout;
    }

    /**
     * 更新指定会话键的有效期；永久有效时移除过期时间。
     *
     * @param key Sa-Token 原始键
     * @param timeout 新有效期，单位为秒
     */
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

    /**
     * 扫描匹配键并按 Sa-Token 规则分页、排序后返回原始键名。
     *
     * @param prefix 键前缀
     * @param keyword 搜索关键词
     * @param start 分页起始位置
     * @param size 分页大小
     * @param sortType 是否排序
     * @return 匹配的原始键名列表
     */
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

    /**
     * 添加 DAO 私有键前缀。
     */
    private String wrapKey(String key) {
        return KEY_PREFIX + key;
    }

    /**
     * 移除 DAO 私有键前缀。
     */
    private String unwrapKey(String key) {
        return key.startsWith(KEY_PREFIX) ? key.substring(KEY_PREFIX.length()) : key;
    }
}
