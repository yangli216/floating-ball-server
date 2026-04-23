-- 存量库升级脚本：为 c_ai_config 补齐服务端托管的 PMPHAI / Reviewer 字段
-- 使用当前应用 schema 账号执行

DECLARE
    v_exists NUMBER;

    PROCEDURE add_column_if_missing(
        p_column_name IN VARCHAR2,
        p_definition  IN VARCHAR2,
        p_comment     IN VARCHAR2
    ) IS
    BEGIN
        SELECT COUNT(*)
          INTO v_exists
          FROM user_tab_columns
         WHERE table_name = 'C_AI_CONFIG'
           AND column_name = UPPER(p_column_name);

        IF v_exists = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE c_ai_config ADD (' || p_definition || ')';
            EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_config.' || p_column_name || ' IS '''
                || REPLACE(p_comment, '''', '''''') || '''';
        END IF;
    END;
BEGIN
    add_column_if_missing('pmphai_enabled', 'pmphai_enabled CHAR(1) DEFAULT ''0'' NOT NULL', '人卫知识库开关');
    add_column_if_missing('pmphai_base_url', 'pmphai_base_url VARCHAR2(500)', '人卫知识库服务地址');
    add_column_if_missing('pmphai_app_key_encrypted', 'pmphai_app_key_encrypted VARCHAR2(1000)', '加密后的人卫知识库App Key');
    add_column_if_missing('pmphai_app_secret_encrypted', 'pmphai_app_secret_encrypted VARCHAR2(1000)', '加密后的人卫知识库App Secret');
    add_column_if_missing('reviewer_enabled', 'reviewer_enabled CHAR(1) DEFAULT ''0'' NOT NULL', '审查模型开关');
    add_column_if_missing('reviewer_base_url', 'reviewer_base_url VARCHAR2(500)', '审查模型服务地址');
    add_column_if_missing('reviewer_api_key_encrypted', 'reviewer_api_key_encrypted VARCHAR2(1000)', '加密后的审查模型密钥');
    add_column_if_missing('reviewer_model', 'reviewer_model VARCHAR2(128)', '审查模型名称');
END;
/
