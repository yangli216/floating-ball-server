-- 存量库升级脚本：补建住院病历 HTML 模板解析缓存表

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'C_AI_INPATIENT_EMR_TPL_CACHE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE c_ai_inpatient_emr_tpl_cache (
                id_cache             VARCHAR2(32) PRIMARY KEY,
                template_id          VARCHAR2(128) NOT NULL,
                template_hash        VARCHAR2(128) NOT NULL,
                template_name        VARCHAR2(200),
                html_content         CLOB,
                fields_json          CLOB,
                field_count          NUMBER(10) DEFAULT 0,
                sd_status            VARCHAR2(2) DEFAULT '1' NOT NULL,
                fg_active            CHAR(1) DEFAULT '1' NOT NULL,
                insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ]';

        EXECUTE IMMEDIATE q'[COMMENT ON TABLE c_ai_inpatient_emr_tpl_cache IS '住院病历HTML模板解析缓存表']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.id_cache IS '模板缓存主键ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.template_id IS 'HIS病历模板主键']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.template_hash IS 'HTML模板内容HASH']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.template_name IS '模板名称']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.html_content IS '模板HTML原文']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.fields_json IS 'data-id字段解析结果JSON']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.field_count IS '字段数量']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.sd_status IS '状态（1启用 0停用）']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.fg_active IS '逻辑删除标记']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.insert_time IS '创建时间']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.update_time IS '更新时间']';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_INPATIENT_EMR_TPL_CACHE' AND column_name = 'TEMPLATE_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_inpatient_emr_tpl_cache ADD (template_id VARCHAR2(128))';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.template_id IS 'HIS病历模板主键']';
        EXECUTE IMMEDIATE q'[
            UPDATE c_ai_inpatient_emr_tpl_cache
            SET template_id = template_hash
            WHERE template_id IS NULL
        ]';
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_inpatient_emr_tpl_cache MODIFY (template_id VARCHAR2(128) NOT NULL)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_INEMR_TPL_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_inemr_tpl_id ON c_ai_inpatient_emr_tpl_cache (template_id, fg_active, sd_status)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_INEMR_TPL_HASH';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_inemr_tpl_hash ON c_ai_inpatient_emr_tpl_cache (template_hash, fg_active, sd_status)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_INEMR_TPL_STATUS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_inemr_tpl_status ON c_ai_inpatient_emr_tpl_cache (fg_active, sd_status, update_time)';
    END IF;
END;
/
