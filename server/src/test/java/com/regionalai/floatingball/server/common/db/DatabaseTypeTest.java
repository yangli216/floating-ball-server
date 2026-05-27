package com.regionalai.floatingball.server.common.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseTypeTest {

    @Test
    void fromShouldDefaultToOracle() {
        assertEquals(DatabaseType.ORACLE, DatabaseType.from(null));
        assertEquals(DatabaseType.ORACLE, DatabaseType.from(""));
        assertEquals(DatabaseType.ORACLE, DatabaseType.from("  "));
    }

    @Test
    void fromShouldAcceptPostgresAliases() {
        assertEquals(DatabaseType.POSTGRES, DatabaseType.from("postgres"));
        assertEquals(DatabaseType.POSTGRES, DatabaseType.from("postgresql"));
        assertEquals(DatabaseType.POSTGRES, DatabaseType.from("pg"));
        assertEquals(DatabaseType.POSTGRES, DatabaseType.from(" PostgreSQL "));
    }

    @Test
    void fromShouldRejectUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseType.from("mysql"));
    }
}
