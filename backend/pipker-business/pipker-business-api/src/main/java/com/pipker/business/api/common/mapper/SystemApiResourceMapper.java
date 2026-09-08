/**
 * @file SystemApiResourceMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_api_resource 的 MyBatis-Plus CRUD 与 API 鉴权规则投影查询。
 * @logic 单表管理走 BaseMapper；鉴权时连接 system_permission 读取已启用 API 权限编码。
 * @dependencies MyBatis-Plus、SystemApiResource、SystemApiResourceRule
 * @index_tags mybatis-plus、system-api-resource、rbac、api
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemApiResource;
import com.pipker.business.api.common.model.SystemApiResourceRule;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 系统 API 资源 Mapper。 */
public interface SystemApiResourceMapper extends BaseMapper<SystemApiResource> {

    /** 查询当前全部启用 API 权限声明的受保护 HTTP 资源。 */
    @Select("""
            SELECT r.id, p.permission_code, r.http_method, r.path_pattern
            FROM system_api_resource r
            JOIN system_permission p ON p.id = r.permission_id
            WHERE p.status = 'ENABLED' AND p.permission_type = 'API'
            ORDER BY r.http_method, r.id
            """)
    List<SystemApiResourceRule> findAllEnabledAuthorizationRules();
}
