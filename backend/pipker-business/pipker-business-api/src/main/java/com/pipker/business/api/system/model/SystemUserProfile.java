/**
 * @file SystemUserProfile.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义可以安全返回给已认证客户端的系统用户基础资料。
 * @logic 从 system_user 投影时排除密码、手机号和邮箱等不必要私密字段。
 * @dependencies SystemUser
 * @index_tags system-user、api、profile
 * @author holic512
 */
package com.pipker.business.api.system.model;

/**
 * 系统用户公开资料。
 */
public record SystemUserProfile(
        Long id,
        String username,
        String nickname,
        String avatar
) {

    /**
     * 从账户持久化模型创建公开资料。
     *
     * @param user 系统账户
     * @return 安全公开资料
     */
    public static SystemUserProfile from(SystemUser user) {
        return new SystemUserProfile(user.id(), user.username(), user.nickname(), user.avatar());
    }
}
