package com.regionalai.floatingball.server.common.db;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseScriptTest {

    @Test
    void postgresInitShouldContainCurrentSchemaAndAvoidOracleTypes() throws Exception {
        String sql = resourceText("sql/postgres/init.sql");

        assertTrue(sql.contains("CREATE TABLE c_ai_device"));
        assertTrue(sql.contains("device_public_key    text"));
        assertTrue(sql.contains("change_summary_json  text"));
        assertTrue(sql.contains("total_changes        integer"));
        assertTrue(sql.contains("CREATE TABLE c_security_rejection_log"));
        assertTrue(sql.contains("INSERT INTO c_ai_region"));
        assertTrue(sql.contains("INSERT INTO c_ai_config"));
        assertFalse(sql.contains("VARCHAR2"));
        assertFalse(sql.contains("CLOB"));
        assertFalse(sql.contains("NUMBER("));
    }

    @Test
    void oracleInitShouldContainFieldsAddedAfterOriginalBaseline() throws Exception {
        String sql = resourceText("sql/oracle/init.sql");

        assertTrue(sql.contains("device_public_key    CLOB"));
        assertTrue(sql.contains("change_summary_json  CLOB"));
        assertTrue(sql.contains("total_changes        NUMBER(5)"));
        assertTrue(sql.contains("CREATE TABLE c_security_rejection_log"));
    }

    private String resourceText(String path) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        assertTrue(stream != null);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }
}
