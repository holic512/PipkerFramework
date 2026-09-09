/**
 * @file SystemApiResourceMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_api_resource 的 MyBatis-Plus 单表数据访问能力。
 * @logic 历史 API 资源表仅保留基础持久化映射；运行时接口授权以 PermissionEnum 注解和角色权限编码关联为准，不再从路径资源规则读取权限定义。
 * @dependencies MyBatis-Plus、SystemApiResource
 * @index_tags mybatis-plus、system-api-resource、legacy、persistence
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemApiResource;

/** 系统 API 资源 Mapper。 */
public interface SystemApiResourceMapper extends BaseMapper<SystemApiResource> {
}
