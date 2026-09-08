/**
 * @file SystemMenu.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_menu 中的动态菜单和前端路由定义。
 * @logic 保留逻辑 componentKey 而非源码绝对路径，供授权服务构造前端菜单树。
 * @dependencies MyBatis-Plus、Lombok、Java 标准库
 * @index_tags system-menu、route、rbac、persistence
 * @author holic512
 */
package com.pipker.business.api.common.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 系统菜单实体。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_menu")
public class SystemMenu {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField("menu_name")
    private String menuName;

    @TableField("menu_type")
    private String menuType;

    @TableField("route_path")
    private String routePath;

    @TableField("route_name")
    private String routeName;

    @TableField("component_key")
    private String componentKey;

    @TableField("icon")
    private String icon;

    @TableField("sort")
    private Integer sort;

    @TableField("visible")
    private boolean visible;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** @return 是页面菜单且路由、组件信息齐全时返回 {@code true}。 */
    public boolean isRouteMenu() {
        return "MENU".equals(menuType)
                && routePath != null && !routePath.isBlank()
                && routeName != null && !routeName.isBlank()
                && componentKey != null && !componentKey.isBlank();
    }
}
