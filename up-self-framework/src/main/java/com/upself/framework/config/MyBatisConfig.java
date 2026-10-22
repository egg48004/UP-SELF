package com.upself.framework.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan({"com.upself.system.mapper", "com.upself.biz.learning.mapper", "com.upself.biz.assignment.mapper", "com.upself.biz.mood.mapper"})
public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mapper/**/*.xml"));
        com.baomidou.mybatisplus.core.MybatisConfiguration c =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();
        c.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(c);
        factory.setPlugins(new Interceptor[] {new PaginationInterceptor()});
        return factory.getObject();
    }
}
