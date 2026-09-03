/**
 * @file SystemLoginTypes.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 声明框架统一系统账户使用的登录会话域。
 * @logic 仅提供 SYSTEM 账户域，不将任何角色编码混入 Sa-Token 登录类型。
 * @dependencies LoginType
 * @index_tags auth、login-type、system
 * @author holic512
 */
package com.pipker.business.common.auth;

/**
 * 框架内置登录会话域。
 */
public final class SystemLoginTypes {

    /**
     * 统一系统用户会话域。
     */
    public static final LoginType SYSTEM = new LoginType("SYSTEM");

    private SystemLoginTypes() {
    }
}
