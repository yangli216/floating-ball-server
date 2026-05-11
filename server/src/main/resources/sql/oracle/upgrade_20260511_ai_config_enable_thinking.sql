-- 存量库升级脚本：为 c_ai_config 补齐思考模式字段

DECLARE
    v_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'C_AI_CONFIG'
      AND column_name = 'ENABLE_THINKING';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_config ADD (enable_thinking CHAR(1) DEFAULT ''0'' NOT NULL)';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_config.enable_thinking IS ''是否启用思考模式''';
        EXECUTE IMMEDIATE 'UPDATE c_ai_config SET enable_thinking = ''0'' WHERE enable_thinking IS NULL';
        COMMIT;
    END IF;
END;
/