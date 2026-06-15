package com.regionalai.floatingball.server.common.db;

public final class DatabaseDialectHolder {

    private static volatile DatabaseDialect dialect = new DatabaseDialect(DatabaseDialect.Kind.ORACLE);

    private DatabaseDialectHolder() {
    }

    public static DatabaseDialect get() {
        return dialect;
    }

    public static void set(DatabaseDialect currentDialect) {
        if (currentDialect != null) {
            dialect = currentDialect;
        }
    }
}
