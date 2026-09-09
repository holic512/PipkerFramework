/**
 * @file PermissionRegistry.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 在应用启动时将 Java 权限枚举装载为不可变的内存权限目录。
 * @logic 启动阶段检测权限编码重复并快速失败；运行阶段仅从内存目录判断编码有效性和返回全量权限点，不读取或清理历史数据库关联记录。
 * @dependencies PermissionEnum、Jakarta Annotations、Spring Framework
 * @index_tags permission、registry、memory、rbac、startup、api-authorization
 * @author holic512
 */
package com.pipker.business.api.system.permission;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统有效权限点的内存目录。
 */
@Component
public class PermissionRegistry {

    private Map<String, PermissionEnum> permissionsByCode = Map.of();
    private List<PermissionPoint> permissionPoints = List.of();
    private Set<String> permissionCodes = Set.of();

    /**
     * 在 Spring 创建本组件后加载全部枚举项，并阻止重复编码造成不确定授权结果。
     */
    @PostConstruct
    public void load() {
        Map<String, PermissionEnum> loadedPermissions = new LinkedHashMap<>();
        List<PermissionPoint> loadedPoints = new ArrayList<>();
        for (PermissionEnum permission : PermissionEnum.values()) {
            PermissionEnum previous = loadedPermissions.putIfAbsent(permission.getCode(), permission);
            if (previous != null) {
                throw new IllegalStateException("Duplicate permission code: " + permission.getCode());
            }
            loadedPoints.add(new PermissionPoint(
                    permission.getCode(),
                    permission.getName(),
                    permission.getDescription()
            ));
        }
        permissionsByCode = Map.copyOf(loadedPermissions);
        permissionPoints = List.copyOf(loadedPoints);
        permissionCodes = Collections.unmodifiableSet(new LinkedHashSet<>(loadedPermissions.keySet()));
    }

    /**
     * @param permissionCode 权限编码
     * @return 编码是否仍由当前 Java 枚举定义为有效权限
     */
    public boolean contains(String permissionCode) {
        return permissionCode != null && permissionsByCode.containsKey(permissionCode);
    }

    /** @return 全部当前有效权限点，按枚举声明顺序返回。 */
    public List<PermissionPoint> list() {
        return permissionPoints;
    }

    /** @return 全部当前有效权限编码，可用于过滤或替换当前有效角色关联。 */
    public Set<String> allCodes() {
        return permissionCodes;
    }

    /**
     * 面向 HTTP 权限列表和角色权限弹窗的只读权限点投影。
     */
    public record PermissionPoint(String code, String name, String description) {
    }
}
