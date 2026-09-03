/**
 * @file RandomIdentifierUtils.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Common
 * @description 使用 SecureRandom 生成适合日志、URL 和一般业务标识的 Base62 乱序字符串。
 * @logic 复用线程安全的安全随机数生成器，从大小写字母和数字字符集中逐位抽取；默认长度固定为 32。
 * @dependencies Java 标准库
 * @index_tags common、util、random、identifier、base62
 * @author holic512
 */
package com.pipker.starter.common.util;

import java.security.SecureRandom;

/**
 * Base62 乱序标识符生成工具。
 */
public final class RandomIdentifierUtils {

    /**
     * 默认标识符长度。
     */
    public static final int DEFAULT_LENGTH = 32;

    private static final char[] ALPHANUMERIC_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RandomIdentifierUtils() {
    }

    /**
     * 生成默认长度的 Base62 乱序标识符。
     *
     * @return 32 位大小写字母和数字组成的随机字符串
     */
    public static String randomAlphanumeric() {
        return randomAlphanumeric(DEFAULT_LENGTH);
    }

    /**
     * 生成指定长度的 Base62 乱序标识符。
     *
     * @param length 标识符长度，必须大于零
     * @return 指定长度的大小写字母和数字随机字符串
     * @throws IllegalArgumentException 当 {@code length} 小于 1 时抛出
     */
    public static String randomAlphanumeric(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be greater than zero");
        }

        char[] identifier = new char[length];
        for (int index = 0; index < length; index++) {
            identifier[index] = ALPHANUMERIC_CHARACTERS[SECURE_RANDOM.nextInt(ALPHANUMERIC_CHARACTERS.length)];
        }
        return new String(identifier);
    }
}
