/**
 * @file SystemUserMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_user 的认证查询和受控更新操作。
 * @logic 仅由账户和认证服务调用，所有密码值均已是 SecurityCryptoService 产生的哈希。
 * @dependencies MyBatis、SystemUser
 * @index_tags mybatis、system-user、authentication
 * @author holic512
 */
package com.pipker.business.api.system.mapper;

import com.pipker.business.api.system.model.SystemUser;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 系统用户 Mapper。
 */
@Mapper
public interface SystemUserMapper {

    /**
     * 按唯一用户名查询账户。
     *
     * @param username 用户名
     * @return 账户；不存在时返回 {@code null}
     */
    @Select("""
            SELECT id, username, password_hash, nickname, avatar, phone, email, status,
                   last_login_time, created_at, updated_at
            FROM system_user
            WHERE username = #{username}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "username", javaType = String.class),
            @Arg(column = "password_hash", javaType = String.class),
            @Arg(column = "nickname", javaType = String.class),
            @Arg(column = "avatar", javaType = String.class),
            @Arg(column = "phone", javaType = String.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "status", javaType = String.class),
            @Arg(column = "last_login_time", javaType = LocalDateTime.class),
            @Arg(column = "created_at", javaType = LocalDateTime.class),
            @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    SystemUser findByUsername(@Param("username") String username);

    /**
     * 按主键查询账户。
     *
     * @param id 用户主键
     * @return 账户；不存在时返回 {@code null}
     */
    @Select("""
            SELECT id, username, password_hash, nickname, avatar, phone, email, status,
                   last_login_time, created_at, updated_at
            FROM system_user
            WHERE id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "username", javaType = String.class),
            @Arg(column = "password_hash", javaType = String.class),
            @Arg(column = "nickname", javaType = String.class),
            @Arg(column = "avatar", javaType = String.class),
            @Arg(column = "phone", javaType = String.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "status", javaType = String.class),
            @Arg(column = "last_login_time", javaType = LocalDateTime.class),
            @Arg(column = "created_at", javaType = LocalDateTime.class),
            @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    SystemUser findById(@Param("id") long id);

    /**
     * 记录成功登录时间。
     *
     * @param id 用户主键
     * @param loginTime 本次登录时间
     */
    @Update("""
            UPDATE system_user
            SET last_login_time = #{loginTime}, updated_at = #{loginTime}
            WHERE id = #{id}
            """)
    void updateLastLoginTime(@Param("id") long id, @Param("loginTime") LocalDateTime loginTime);

    /**
     * 使用最新安全哈希替换旧密码。
     *
     * @param id 用户主键
     * @param passwordHash 新密码哈希
     * @param updatedAt 更新时间
     */
    @Update("""
            UPDATE system_user
            SET password_hash = #{passwordHash}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    void updatePasswordHash(
            @Param("id") long id,
            @Param("passwordHash") String passwordHash,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
