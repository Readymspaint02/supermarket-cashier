package com.zmj.gbs_commerce_system.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.zmj.gbs_commerce_system.mapper")
public class MyBatisPlusConfig {
    /**
     * 注册MyBatisPlus拦截器链到Spring容器
     * @return  MyBatis插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        // 创建MyBatisPlus拦截器链
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加自动分页拦截器并指定数据库类型
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
