-- 存量库升级脚本：为 c_ai_config 补齐 chatFast 独立模型字段

DECLARE
    v_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'C_AI_CONFIG'
      AND column_name = 'FAST_MODEL_NAME';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_config ADD (fast_model_name VARCHAR2(128))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_config.fast_model_name IS ''chatFast 独立模型名称''';
        EXECUTE IMMEDIATE 'UPDATE c_ai_config SET fast_model_name = model_name WHERE fast_model_name IS NULL';
        COMMIT;
    END IF;
END;
/