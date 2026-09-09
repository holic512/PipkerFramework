/**
 * @file Permission.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 声明一个 MVC 接口或控制器需要的系统权限点。
 * @logic 仅接受 PermissionEnum 常量，阻止接口用角色名称或任意字符串表达授权要求；拦截器在请求分派前读取该注解。
 * @dependencies PermissionEnum、Java Annotation API
 * @index_tags permission、annotation、rbac、api-authorization、spring-mvc
 * @author holic512
 */
package com.pipker.business.api.system.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为接口声明所需权限点。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Permission {

    /** @return 当前接口必须拥有的权限点。 */
    PermissionEnum value();
}
