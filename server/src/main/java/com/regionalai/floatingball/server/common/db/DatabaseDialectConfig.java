package com.regionalai.floatingball.server.common.db;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DatabaseDialectConfig {

    @Bean
    public DatabaseDialect databaseDialect(DatabaseProperties properties) {
        DatabaseType type = properties.resolveType();
        if (type == DatabaseType.POSTGRES) {
            return new PostgresDatabaseDialect();
        }
        return new OracleDatabaseDialect();
    }

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("Oracle", DatabaseType.ORACLE.value());
        properties.setProperty("PostgreSQL", DatabaseType.POSTGRES.value());
        provider.setProperties(properties);
        return provider;
    }
}
