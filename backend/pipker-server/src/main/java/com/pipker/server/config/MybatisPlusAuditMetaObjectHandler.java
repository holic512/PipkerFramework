/**
 * @file MybatisPlusAuditMetaObjectHandler.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 为声明审计填充字段的 MyBatis-Plus 系统实体维护创建和更新时间。
 * @logic 插入时仅填充空的 createdAt 与 updatedAt，更新时仅填充空的 updatedAt，不覆盖业务显式传入的时间。
 * @dependencies MyBatis-Plus、Spring Framework、Java 标准库
 * @index_tags mybatis-plus、audit、meta-object-handler、persistence
 * @author holic512
 */
package com.pipker.server.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** MyBatis-Plus 审计字段填充器。 */
@Component
public class MybatisPlusAuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
