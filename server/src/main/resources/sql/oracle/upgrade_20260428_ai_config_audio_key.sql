-- 存量库升级脚本：为 c_ai_config 补齐语音独立密钥字段
-- 说明：audio_api_key_encrypted 为空时，应用会回退使用 api_key_encrypted。

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'C_AI_CONFIG'
      AND column_name = 'AUDIO_API_KEY_ENCRYPTED';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_config ADD (audio_api_key_encrypted VARCHAR2(1000))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_config.audio_api_key_encrypted IS ''加密后的语音接口密钥，为空时复用主模型密钥''';
    END IF;
END;
/
