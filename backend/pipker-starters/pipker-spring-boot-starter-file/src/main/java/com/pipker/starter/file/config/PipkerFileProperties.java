/**
 * @file PipkerFileProperties.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 绑定本地文件存储开关、根目录和公开访问路径配置。
 * @logic 校验目录字符串与访问路径前缀；根目录按应用当前工作目录解析，相对访问路径不包含协议、域名或端口。
 * @dependencies Spring Boot Configuration Properties、Jakarta Validation、FileStorageUtils
 * @index_tags starter、file-storage、configuration、local、access-path
 * @author holic512
 */
package com.pipker.starter.file.config;

import com.pipker.starter.file.util.FileStorageUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * 绑定 {@code pipker.file} 下的本地文件存储设置。
 */
@Validated
@ConfigurationProperties("pipker.file")
public class PipkerFileProperties {

    /**
     * 是否注册文件服务与公开资源映射。
     */
    private boolean enabled = true;

    /**
     * 本地文件系统配置。
     */
    @Valid
    @NotNull
    private Local local = new Local();

    /**
     * 不含域名的公开文件访问路径前缀。
     */
    @NotBlank
    private String accessPath = "/files";

    /**
     * 返回文件模块是否启用。
     *
     * @return 启用时为 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置文件模块是否启用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回本地文件系统配置。
     *
     * @return 本地文件系统配置
     */
    public Local getLocal() {
        return local;
    }

    /**
     * 设置本地文件系统配置。
     *
     * @param local 本地文件系统配置
     */
    public void setLocal(Local local) {
        this.local = local;
    }

    /**
     * 返回规范化后的公开访问路径前缀。
     *
     * @return 以单个 {@code /} 开头且不以 {@code /} 结尾的路径前缀
     */
    public String getAccessPath() {
        return FileStorageUtils.normalizeAccessPath(accessPath);
    }

    /**
     * 设置公开访问路径前缀。
     *
     * @param accessPath 不含域名的路径前缀
     */
    public void setAccessPath(String accessPath) {
        this.accessPath = accessPath;
    }

    /**
     * 将已配置的本地根目录解析为标准绝对路径。
     *
     * @return 标准绝对根目录
     */
    public Path resolveLocalRoot() {
        return Path.of(local.getRoot()).toAbsolutePath().normalize();
    }

    /**
     * 校验公开访问前缀不包含路径穿越、通配符或 URL 查询成分。
     *
     * @return 路径安全时为 {@code true}
     */
    @AssertTrue(message = "pipker.file.access-path must be a safe absolute URL path prefix without wildcards")
    public boolean isAccessPathValid() {
        return FileStorageUtils.isValidAccessPath(accessPath);
    }

    /**
     * 校验本地根目录可被当前 JVM 解析。
     *
     * @return 根目录合法时为 {@code true}
     */
    @AssertTrue(message = "pipker.file.local.root must be a valid filesystem path")
    public boolean isLocalRootValid() {
        if (local == null || local.getRoot() == null || local.getRoot().isBlank()) {
            return false;
        }
        try {
            Path.of(local.getRoot());
            return true;
        } catch (InvalidPathException exception) {
            return false;
        }
    }

    /**
     * 本地文件系统根目录配置。
     */
    public static class Local {

        /**
         * 相对当前工作目录的默认文件根目录。
         */
        @NotBlank
        private String root = "./data/files";

        /**
         * 返回配置的本地文件根目录。
         *
         * @return 原始文件根目录配置
         */
        public String getRoot() {
            return root;
        }

        /**
         * 设置本地文件根目录。
         *
         * @param root 文件根目录
         */
        public void setRoot(String root) {
            this.root = root;
        }
    }
}
