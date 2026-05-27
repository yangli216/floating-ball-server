package com.regionalai.floatingball.server.common.db;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "floating-ball.database")
public class DatabaseProperties {

    private String type = DatabaseType.ORACLE.value();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DatabaseType resolveType() {
        return DatabaseType.from(type);
    }
}
