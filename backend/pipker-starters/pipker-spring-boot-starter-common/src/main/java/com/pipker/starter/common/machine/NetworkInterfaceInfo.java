/**
 * @file NetworkInterfaceInfo.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Common
 * @description 表示一个可用本机网络接口及其硬件地址、IP 地址快照。
 * @logic 将不可读取的文本和地址集合归一为空值，并以不可变集合交付给调用方。
 * @dependencies Java 标准库
 * @index_tags common、machine、network、mac、ip
 * @author holic512
 */
package com.pipker.starter.common.machine;

import java.util.List;
import java.util.Objects;

/**
 * 网络接口信息快照。
 *
 * @param name 系统接口名称
 * @param displayName 接口显示名称；不可读取时为空字符串
 * @param macAddress 大写冒号分隔的 MAC 地址；不可读取或不存在时为空字符串
 * @param ipAddresses 接口上的 IPv4 或 IPv6 地址列表
 */
public record NetworkInterfaceInfo(
        String name,
        String displayName,
        String macAddress,
        List<String> ipAddresses
) {

    /**
     * 规范化可选文本与地址列表。
     */
    public NetworkInterfaceInfo {
        name = normalize(name);
        displayName = normalize(displayName);
        macAddress = normalize(macAddress);
        ipAddresses = ipAddresses == null
                ? List.of()
                : ipAddresses.stream()
                .filter(Objects::nonNull)
                .map(NetworkInterfaceInfo::normalize)
                .filter(address -> !address.isEmpty())
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
