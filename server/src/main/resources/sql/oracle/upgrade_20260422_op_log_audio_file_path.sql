-- 存量库升级脚本：为 c_ai_op_log 补齐录音文件路径字段
-- 使用当前应用 schema 账号执行

DECLARE
    v_exists NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_exists
      FROM user_tab_columns
     WHERE table_name = 'C_AI_OP_LOG'
       AND column_name = 'AUDIO_FILE_PATH';

    IF v_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_op_log ADD (audio_file_path VARCHAR2(1000))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_op_log.audio_file_path IS ''语音代理录音文件路径''';
    END IF;
END;
/
