/**
 * @file SystemMenuMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_menu 的 MyBatis-Plus 单表数据访问能力。
 * @logic 授权和角色配置服务通过 Lambda 条件查询启用、可见菜单及其页面子集。
 * @dependencies MyBatis-Plus、SystemMenu
 * @index_tags mybatis-plus、system-menu、rbac
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemMenu;

/** 系统菜单 Mapper。 */
public interface SystemMenuMapper extends BaseMapper<SystemMenu> {
}
