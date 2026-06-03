package com.regionalai.floatingball.server.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleSchemaScriptTest {

    private static final Path ORACLE_SQL_DIR = Paths.get("src/main/resources/sql/oracle");

    @Test
    void oracleDeliveryShouldOnlyKeepBootstrapAndInitScripts() throws IOException {
        Set<String> actualSqlFiles = new HashSet<String>();
        try (DirectoryStream<Path> sqlScripts = Files.newDirectoryStream(ORACLE_SQL_DIR, "*.sql")) {
            for (Path sqlScript : sqlScripts) {
                actualSqlFiles.add(sqlScript.getFileName().toString());
            }
        }

        Set<String> expectedSqlFiles = new HashSet<String>(Arrays.asList("bootstrap.sql", "init.sql"));
        assertTrue(actualSqlFiles.equals(expectedSqlFiles), "oracle delivery sql files should be bootstrap.sql and init.sql");

        try (DirectoryStream<Path> upgradeScripts = Files.newDirectoryStream(ORACLE_SQL_DIR, "upgrade_*.sql")) {
            assertFalse(upgradeScripts.iterator().hasNext(), "upgrade scripts should be folded into init.sql");
        }
    }

    @Test
    void initSqlShouldContainFoldedUpgradeSchema() throws IOException {
        String initSql = readSql(ORACLE_SQL_DIR.resolve("init.sql"));

        assertContains(initSql, "device_public_key    VARCHAR2(1000)");
        assertContains(initSql, "register_ip          VARCHAR2(64)");
        assertContains(initSql, "last_seen_ip         VARCHAR2(64)");
        assertContains(initSql, "COMMENT ON COLUMN c_ai_device.device_public_key");
        assertContains(initSql, "COMMENT ON COLUMN c_ai_device.register_ip");
        assertContains(initSql, "COMMENT ON COLUMN c_ai_device.last_seen_ip");

        assertContains(initSql, "fast_model_name          VARCHAR2(128)");
        assertContains(initSql, "enable_thinking          CHAR(1) DEFAULT '0' NOT NULL");
        assertContains(initSql, "audio_api_key_encrypted  VARCHAR2(1000)");
        assertContains(initSql, "pmphai_enabled           CHAR(1) DEFAULT '0' NOT NULL");
        assertContains(initSql, "reviewer_check_examination_enabled CHAR(1) DEFAULT '1' NOT NULL");

        assertContains(initSql, "CREATE TABLE c_ai_symptom_template");
        assertContains(initSql, "CREATE TABLE c_ai_symptom_template_change_log");
        assertContains(initSql, "CREATE TABLE c_ai_feature_event");
        assertContains(initSql, "CREATE TABLE c_security_rejection_log");

        assertContains(initSql, "op_action            VARCHAR2(256)");
        assertContains(initSql, "op_title             VARCHAR2(500)");
        assertContains(initSql, "source_module        VARCHAR2(128)");
        assertContains(initSql, "scene_code           VARCHAR2(256)");
        assertContains(initSql, "trace_id             VARCHAR2(64)");
        assertContains(initSql, "audio_file_path      VARCHAR2(1000)");
        assertContains(initSql, "consultation_id      VARCHAR2(64)");

        assertContains(initSql, "speech_text          CLOB");
        assertContains(initSql, "audio_file_name      VARCHAR2(255)");
        assertContains(initSql, "change_summary_json  CLOB");
        assertContains(initSql, "total_changes        NUMBER(5)");

        assertContains(initSql, "kind                  VARCHAR2(32) DEFAULT 'general'");
        assertContains(initSql, "severity              VARCHAR2(16) DEFAULT 'medium'");
        assertContains(initSql, "revision_no           NUMBER(10) DEFAULT 1");
        assertContains(initSql, "fg_latest             CHAR(1) DEFAULT '1' NOT NULL");

        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_device_code_org_active");
        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_device_token_active");
        assertContains(initSql, "CREATE INDEX idx_c_ai_device_register_ip");
        assertContains(initSql, "CREATE INDEX idx_c_ai_device_last_seen_ip");
        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_feature_event_idem");
        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_feedback_latest_scope");
        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_user_log_consultation_active");
        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_user_code_active");
        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_role_code_active");
        assertContains(initSql, "CREATE UNIQUE INDEX uk_c_ai_user_role_active");

        assertContains(initSql, "CREATE INDEX idx_c_security_rej_time");
        assertContains(initSql, "CREATE INDEX idx_c_security_rej_type");
        assertContains(initSql, "CREATE INDEX idx_c_security_rej_ip");
        assertContains(initSql, "CREATE INDEX idx_c_security_rej_device");
        assertContains(initSql, "CREATE INDEX idx_c_security_rej_path");
    }

    private String readSql(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void assertContains(String sql, String expected) {
        assertTrue(sql.contains(expected), "init.sql should contain: " + expected);
    }
}
