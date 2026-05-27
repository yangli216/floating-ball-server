package com.regionalai.floatingball.server.common.db;

public enum DatabaseType {
    ORACLE("oracle"),
    POSTGRES("postgres");

    private final String value;

    DatabaseType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DatabaseType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ORACLE;
        }
        String normalized = value.trim().toLowerCase();
        for (DatabaseType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        if ("postgresql".equals(normalized) || "pg".equals(normalized)) {
            return POSTGRES;
        }
        throw new IllegalArgumentException("Unsupported database type: " + value);
    }
}
