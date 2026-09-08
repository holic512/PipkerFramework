/**
 * @file MybatisPlusPaginationConfiguration.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 注册系统管理查询使用的 MyBatis-Plus 分页拦截器。
 * @logic 让 Page 查询在运行时按当前数据源方言生成计数与分页 SQL，并统一限制单页最大记录数。
 * @dependencies MyBatis-Plus、MyBatis-Plus JSQLParser、Spring Framework
 * @index_tags mybatis-plus、pagination、persistence、configuration
 * @author holic512
 */
package com.pipker.server.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 分页配置。 */
@Configuration(proxyBeanMethods = false)
public class MybatisPlusPaginationConfiguration {

    /**
     * 创建可根据实际数据源识别方言的分页拦截器。
     *
     * @return MyBatis-Plus 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor();
        pagination.setMaxLimit(100L);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
