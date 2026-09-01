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

public enum SensitiveType {
    PASSWORD,
    TOKEN,
    PHONE,
    EMAIL,
    ID_CARD,
    BANK_CARD
}
