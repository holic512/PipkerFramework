/**
 * @file MachineInfo.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Common
 * @description 表示一次本机操作系统、JVM 与网络接口采集形成的不可变快照。
 * @logic 规范化缺失文本和网络接口集合，使受限运行环境仍能向调用方返回可安全读取的部分信息。
 * @dependencies NetworkInterfaceInfo、Java 标准库
 * @index_tags common、machine、system-info、snapshot
 * @author holic512
 */
package com.pipker.starter.common.machine;

import java.util.List;
import java.util.Objects;

/**
 * 本机运行环境快照。
 *
 * @param hostName 主机名；不可读取时为空字符串
 * @param operatingSystemName 操作系统名称；不可读取时为空字符串
 * @param operatingSystemVersion 操作系统版本；不可读取时为空字符串
 * @param architecture 系统架构；不可读取时为空字符串
 * @param availableProcessors 可用 CPU 核数，至少为 1
 * @param jvmName JVM 名称；不可读取时为空字符串
 * @param jvmVersion JVM 版本；不可读取时为空字符串
 * @param networkInterfaces 已启用非回环网络接口的快照列表
 */
public record MachineInfo(
        String hostName,
        String operatingSystemName,
        String operatingSystemVersion,
        String architecture,
        int availableProcessors,
        String jvmName,
        String jvmVersion,
        List<NetworkInterfaceInfo> networkInterfaces
) {

    /**
     * 规范化文本、CPU 数量和网络接口集合。
     */
    public MachineInfo {
        hostName = normalize(hostName);
        operatingSystemName = normalize(operatingSystemName);
        operatingSystemVersion = normalize(operatingSystemVersion);
        architecture = normalize(architecture);
        availableProcessors = Math.max(1, availableProcessors);
        jvmName = normalize(jvmName);
        jvmVersion = normalize(jvmVersion);
        networkInterfaces = networkInterfaces == null
                ? List.of()
                : networkInterfaces.stream().filter(Objects::nonNull).toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
