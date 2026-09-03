/**
 * @file UuidUtils.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Common
 * @description 提供标准 UUID 及兼容日志 TraceId 的无连字符 UUID 生成能力。
 * @logic 委托 Java UUID 随机生成器产生版本四 UUID，并按调用方需要保留或移除连字符。
 * @dependencies Java 标准库
 * @index_tags common、util、uuid、identifier、trace-id
 * @author holic512
 */
package com.pipker.starter.common.util;

import java.util.UUID;

/**
 * UUID 生成工具。
 */
public final class UuidUtils {

    private UuidUtils() {
    }

    /**
     * 生成带连字符的标准随机 UUID。
     *
     * @return 标准 UUID 字符串，例如 {@code 3b241101-e2bb-4255-8caf-4136c566a962}
     */
    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成不带连字符的随机 UUID。
     *
     * @return 32 位小写十六进制 UUID 字符串
     */
    public static String randomUuidWithoutHyphens() {
        return randomUuid().replace("-", "");
    }
}
