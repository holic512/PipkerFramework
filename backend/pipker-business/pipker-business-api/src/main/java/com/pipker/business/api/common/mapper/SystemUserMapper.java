/**
 * @file SystemUserMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_user 的 MyBatis-Plus 单表数据访问能力。
 * @logic 由账户服务通过 Lambda Wrapper 完成认证查询和受控更新，密码字段始终保存安全哈希。
 * @dependencies MyBatis-Plus、SystemUser
 * @index_tags mybatis-plus、system-user、authentication
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.pipker.business.api.common.model.SystemUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 系统用户 Mapper。
 */
public interface SystemUserMapper extends BaseMapper<SystemUser> {
}
