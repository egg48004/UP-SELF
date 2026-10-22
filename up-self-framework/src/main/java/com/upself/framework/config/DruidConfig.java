package com.upself.framework.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.upself.common.enums.DataSourceType;
import com.upself.framework.datasource.DynamicDataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;

import java.util.*;

import javax.sql.DataSource;

@Configuration
public class DruidConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.druid.master")
    public DruidDataSource masterDataSource() {
        return new DruidDataSource();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "spring.datasource.druid.slave",
            name = "enabled",
            havingValue = "true")
    @ConfigurationProperties("spring.datasource.druid.slave")
    public DruidDataSource slaveDataSource() {
        return new DruidDataSource();
    }

    @Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dataSource(
            @Qualifier("masterDataSource") DataSource master,
            @Qualifier("slaveDataSource") ObjectProvider<DataSource> slave) {
        Map<Object, Object> sources = new HashMap<>();
        sources.put(DataSourceType.MASTER.name(), master);
        DataSource optional = slave.getIfAvailable();
        if (optional != null) sources.put(DataSourceType.SLAVE.name(), optional);
        return new DynamicDataSource(master, sources);
    }
}
