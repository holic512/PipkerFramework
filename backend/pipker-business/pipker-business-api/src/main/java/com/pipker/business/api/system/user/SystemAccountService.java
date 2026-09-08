/**
 * @file SystemAccountService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 封装系统账户的认证读取和持久化更新，不依赖任何会话或密码算法实现。
 * @logic 为认证和授权功能提供账户查询、登录时间和密码哈希升级操作，不参与会话或密码算法编排。
 * @dependencies SystemUserMapper、SystemUser、MyBatis-Plus、Spring Framework
 * @index_tags system-user、service、authentication、mybatis-plus
 * @author holic512
 */
package com.pipker.business.api.system.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pipker.business.api.common.mapper.SystemUserMapper;
import com.pipker.business.api.common.model.SystemUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 系统账户服务。
 */
@Service
public class SystemAccountService {

    private final SystemUserMapper systemUserMapper;

    /**
     * 创建账户服务。
     *
     * @param systemUserMapper 系统用户 Mapper
     */
    public SystemAccountService(SystemUserMapper systemUserMapper) {
        this.systemUserMapper = systemUserMapper;
    }

    /**
     * 按用户名查询账户。
     *
     * @param username 用户名
     * @return 账户；不存在时返回 {@code null}
     */
    public SystemUser findByUsername(String username) {
        return systemUserMapper.selectOne(new LambdaQueryWrapper<SystemUser>()
                .eq(SystemUser::getUsername, username));
    }

    /**
     * 按主键查询账户。
     *
     * @param userId 用户主键
     * @return 账户；不存在时返回 {@code null}
     */
    public SystemUser findById(long userId) {
        return systemUserMapper.selectById(userId);
    }

    /**
     * 更新成功登录时间。
     *
     * @param userId 用户主键
     * @param loginTime 登录时间
     */
    public void recordSuccessfulLogin(long userId, LocalDateTime loginTime) {
        systemUserMapper.update(null, new LambdaUpdateWrapper<SystemUser>()
                .eq(SystemUser::getId, userId)
                .set(SystemUser::getLastLoginTime, loginTime)
                .set(SystemUser::getUpdatedAt, loginTime));
    }

    /**
     * 更新已升级的密码哈希。
     *
     * @param userId 用户主键
     * @param passwordHash 密码哈希
     * @param updatedAt 更新时间
     */
    public void updatePasswordHash(long userId, String passwordHash, LocalDateTime updatedAt) {
        systemUserMapper.update(null, new LambdaUpdateWrapper<SystemUser>()
                .eq(SystemUser::getId, userId)
                .set(SystemUser::getPasswordHash, passwordHash)
                .set(SystemUser::getUpdatedAt, updatedAt));
    }
}
