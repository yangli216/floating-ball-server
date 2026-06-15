package com.regionalai.floatingball.server.common.db;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DatabaseDialectInitializer {

    private final DatabaseDialect dialect;

    public DatabaseDialectInitializer(DatabaseDialect dialect) {
        this.dialect = dialect;
    }

    @PostConstruct
    public void initialize() {
        DatabaseDialectHolder.set(dialect);
    }
}
