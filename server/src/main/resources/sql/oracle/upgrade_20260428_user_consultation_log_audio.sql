-- 存量库升级脚本：为运维用户日志补充语音问诊录音与 ASR 文本字段
-- 使用当前应用 schema 账号执行；脚本可重复执行

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_USER_CONSULTATION_LOG' AND column_name = 'SPEECH_TEXT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_user_consultation_log ADD (speech_text CLOB)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_USER_CONSULTATION_LOG' AND column_name = 'AUDIO_FILE_PATH';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_user_consultation_log ADD (audio_file_path VARCHAR2(1000))';
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_USER_CONSULTATION_LOG' AND column_name = 'AUDIO_FILE_NAME';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_user_consultation_log ADD (audio_file_name VARCHAR2(255))';
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_USER_CONSULTATION_LOG' AND column_name = 'AUDIO_MIME_TYPE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_user_consultation_log ADD (audio_mime_type VARCHAR2(128))';
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_USER_CONSULTATION_LOG' AND column_name = 'AUDIO_SIZE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_user_consultation_log ADD (audio_size NUMBER(12))';
    END IF;
END;
/

COMMENT ON COLUMN c_ai_user_consultation_log.speech_text IS '语音问诊ASR识别文字';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_file_path IS '语音问诊录音文件路径';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_file_name IS '语音问诊录音原文件名';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_mime_type IS '语音问诊录音MIME类型';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_size IS '语音问诊录音字节数';
