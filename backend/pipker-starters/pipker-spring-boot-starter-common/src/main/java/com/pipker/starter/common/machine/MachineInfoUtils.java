/**
 * @file MachineInfoUtils.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Common
 * @description 读取当前 JVM 可获得的本机、运行环境和网络接口信息。
 * @logic 每次调用按当前状态构造快照；收集已启用非回环接口，单项读取受限时保留其他可用字段而不抛出异常。
 * @dependencies MachineInfo、NetworkInterfaceInfo、Java 标准库
 * @index_tags common、util、machine、system-info、network
 * @author holic512
 */
package com.pipker.starter.common.machine;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/**
 * 本机信息读取工具。
 */
public final class MachineInfoUtils {

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private MachineInfoUtils() {
    }

    /**
     * 获取当前时刻的本机运行环境快照。
     *
     * @return 不可变的本机信息快照；受限字段使用空值或空集合表示
     */
    public static MachineInfo getLocalMachineInfo() {
        return new MachineInfo(
                resolveHostName(),
                systemProperty("os.name"),
                systemProperty("os.version"),
                systemProperty("os.arch"),
                resolveAvailableProcessors(),
                systemProperty("java.vm.name"),
                systemProperty("java.vm.version"),
                resolveNetworkInterfaces()
        );
    }

    private static String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException | SecurityException ignored) {
            return "";
        }
    }

    private static String systemProperty(String key) {
        try {
            String value = System.getProperty(key);
            return value == null ? "" : value;
        } catch (SecurityException ignored) {
            return "";
        }
    }

    private static int resolveAvailableProcessors() {
        try {
            return Math.max(1, Runtime.getRuntime().availableProcessors());
        } catch (SecurityException ignored) {
            return 1;
        }
    }

    private static List<NetworkInterfaceInfo> resolveNetworkInterfaces() {
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException | SecurityException ignored) {
            return List.of();
        }
        if (interfaces == null) {
            return List.of();
        }

        List<NetworkInterfaceInfo> snapshots = new ArrayList<>();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            NetworkInterfaceInfo snapshot = inspectNetworkInterface(networkInterface);
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return snapshots.stream()
                .sorted(Comparator.comparing(NetworkInterfaceInfo::name))
                .toList();
    }

    private static NetworkInterfaceInfo inspectNetworkInterface(NetworkInterface networkInterface) {
        try {
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                return null;
            }
            String interfaceName = networkInterface.getName();
            if (interfaceName == null || interfaceName.isBlank()) {
                return null;
            }
            return new NetworkInterfaceInfo(
                    interfaceName,
                    networkInterface.getDisplayName(),
                    resolveMacAddress(networkInterface),
                    resolveIpAddresses(networkInterface)
            );
        } catch (SocketException | SecurityException ignored) {
            return null;
        }
    }

    private static String resolveMacAddress(NetworkInterface networkInterface) {
        try {
            byte[] hardwareAddress = networkInterface.getHardwareAddress();
            if (hardwareAddress == null || hardwareAddress.length == 0) {
                return "";
            }

            StringBuilder result = new StringBuilder(hardwareAddress.length * 3 - 1);
            for (int index = 0; index < hardwareAddress.length; index++) {
                if (index > 0) {
                    result.append(':');
                }
                int value = hardwareAddress[index] & 0xFF;
                result.append(HEX_DIGITS[value >>> 4]);
                result.append(HEX_DIGITS[value & 0x0F]);
            }
            return result.toString();
        } catch (SocketException | SecurityException ignored) {
            return "";
        }
    }

    private static List<String> resolveIpAddresses(NetworkInterface networkInterface) {
        try {
            List<String> addresses = new ArrayList<>();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                String address = removeScope(inetAddresses.nextElement().getHostAddress());
                if (address != null && !address.isBlank()) {
                    addresses.add(address);
                }
            }
            return addresses.stream().distinct().sorted().toList();
        } catch (SecurityException ignored) {
            return List.of();
        }
    }

    private static String removeScope(String address) {
        if (address == null) {
            return "";
        }
        int scopeSeparator = address.indexOf('%');
        return scopeSeparator < 0 ? address : address.substring(0, scopeSeparator);
    }
}
