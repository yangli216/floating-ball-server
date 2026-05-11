-- 存量库升级脚本：为 c_ai_config 补齐检查项目独立审查开关

DECLARE
    v_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'C_AI_CONFIG'
      AND column_name = 'REVIEWER_CHECK_EXAMINATION_ENABLED';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_config ADD (reviewer_check_examination_enabled CHAR(1) DEFAULT ''1'' NOT NULL)';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_config.reviewer_check_examination_enabled IS ''是否启用检查项目独立审查''';
        EXECUTE IMMEDIATE 'UPDATE c_ai_config SET reviewer_check_examination_enabled = ''1'' WHERE reviewer_check_examination_enabled IS NULL';
        COMMIT;
    END IF;
END;
/