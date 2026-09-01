/**
 * @file SensitiveType.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 枚举日志脱敏支持的内置敏感数据类型。
 * @logic 不同类型保留必要的末尾或域名信息，以兼顾排障可读性与数据最小暴露。
 * @dependencies Java 标准库
 * @index_tags log、sensitive、enum
 * @author holic512
 */
package com.pipker.starter.log.sensitive;

/**
 * 日志脱敏支持的内置敏感数据类型。
 */
public enum SensitiveType {

    /**
     * 密码或其他不应保留原文的秘密值。
     */
    PASSWORD,

    /**
     * 访问令牌、Cookie 或授权信息。
     */
    TOKEN,

    /**
     * 手机号或其他电话号码。
     */
    PHONE,

    /**
     * 电子邮箱地址。
     */
    EMAIL,

    /**
     * 身份证件号码。
     */
    ID_CARD,

    /**
     * 银行卡或卡号。
     */
    BANK_CARD
}
