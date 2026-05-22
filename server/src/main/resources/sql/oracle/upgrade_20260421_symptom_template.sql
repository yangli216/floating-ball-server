-- 存量库升级脚本：补建 c_ai_symptom_template
-- 使用当前应用 schema 账号执行

DECLARE
    v_exists NUMBER;

    PROCEDURE create_index_if_missing(
        p_index_name IN VARCHAR2,
        p_index_ddl  IN VARCHAR2
    ) IS
    BEGIN
        SELECT COUNT(*)
          INTO v_exists
          FROM user_indexes
         WHERE index_name = UPPER(p_index_name);

        IF v_exists = 0 THEN
            EXECUTE IMMEDIATE p_index_ddl;
        END IF;
    END;
BEGIN
    SELECT COUNT(*)
      INTO v_exists
      FROM user_tables
     WHERE table_name = 'C_AI_SYMPTOM_TEMPLATE';

    IF v_exists = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE c_ai_symptom_template (
                id_template              VARCHAR2(32) PRIMARY KEY,
                cd_symptom               VARCHAR2(128) NOT NULL,
                na_symptom               VARCHAR2(200) NOT NULL,
                sd_medical_mode          VARCHAR2(16) NOT NULL,
                des_symptom              VARCHAR2(1000),
                fg_common                CHAR(1) DEFAULT '0' NOT NULL,
                sort_order               NUMBER(10) DEFAULT 0,
                system_category_json     CLOB,
                system_category_tokens   VARCHAR2(1000),
                body_parts_json          CLOB,
                body_parts_tokens        VARCHAR2(1000),
                custom_script            CLOB,
                applicable_population_json CLOB,
                config_json              CLOB,
                tcm_metadata_json        CLOB,
                id_org                   VARCHAR2(32),
                id_region                VARCHAR2(32),
                sd_status                VARCHAR2(2) DEFAULT '1' NOT NULL,
                fg_active                CHAR(1) DEFAULT '1' NOT NULL,
                insert_time              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ]';

        EXECUTE IMMEDIATE q'[COMMENT ON TABLE c_ai_symptom_template IS '症状模板表']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.id_template IS '症状模板主键ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.cd_symptom IS '症状Key/编码']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.na_symptom IS '症状名称']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.sd_medical_mode IS '医学模式（western/tcm）']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.des_symptom IS '症状描述']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.fg_common IS '是否常用症状']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.sort_order IS '排序号']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.system_category_json IS '系统分类JSON数组']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.system_category_tokens IS '系统分类检索token']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.body_parts_json IS '部位JSON数组']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.body_parts_tokens IS '部位检索token']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.custom_script IS '自定义脚本']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.applicable_population_json IS '适用人群JSON']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.config_json IS '问诊配置JSON']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.tcm_metadata_json IS '中医扩展元数据JSON']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.id_org IS '机构级作用范围ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.id_region IS '区域级作用范围ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.sd_status IS '状态（1启用 0停用）']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.fg_active IS '逻辑删除标记']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.insert_time IS '创建时间']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template.update_time IS '更新时间']';
    END IF;

    create_index_if_missing(
        'IDX_C_AI_SYMPTOM_SCOPE',
        'CREATE INDEX idx_c_ai_symptom_scope ON c_ai_symptom_template (sd_medical_mode, id_org, id_region, fg_active, sd_status)'
    );
    create_index_if_missing(
        'IDX_C_AI_SYMPTOM_CODE',
        'CREATE INDEX idx_c_ai_symptom_code ON c_ai_symptom_template (cd_symptom, sd_medical_mode, id_org, id_region, fg_active)'
    );
    create_index_if_missing(
        'IDX_C_AI_SYMPTOM_SORT',
        'CREATE INDEX idx_c_ai_symptom_sort ON c_ai_symptom_template (sd_medical_mode, sort_order, fg_active)'
    );

    SELECT COUNT(*)
      INTO v_exists
      FROM user_tables
     WHERE table_name = 'C_AI_SYMPTOM_TEMPLATE_CHANGE_LOG';

    IF v_exists = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE c_ai_symptom_template_change_log (
                id_log                  VARCHAR2(32) PRIMARY KEY,
                id_template             VARCHAR2(32),
                cd_symptom              VARCHAR2(128),
                na_symptom              VARCHAR2(200),
                sd_medical_mode         VARCHAR2(16),
                id_org                  VARCHAR2(32),
                id_region               VARCHAR2(32),
                operation_type          VARCHAR2(32) NOT NULL,
                id_operator             VARCHAR2(32),
                cd_operator             VARCHAR2(64),
                na_operator             VARCHAR2(128),
                change_summary          VARCHAR2(1000),
                before_json             CLOB,
                after_json              CLOB,
                diff_json               CLOB,
                operation_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                fg_active               CHAR(1) DEFAULT '1' NOT NULL,
                insert_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ]';

        EXECUTE IMMEDIATE q'[COMMENT ON TABLE c_ai_symptom_template_change_log IS '症状模板修改日志表']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.id_log IS '修改日志主键ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.id_template IS '症状模板ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.cd_symptom IS '症状Key/编码']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.na_symptom IS '症状名称']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.sd_medical_mode IS '医学模式（western/tcm）']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.id_org IS '机构级作用范围ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.id_region IS '区域级作用范围ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.operation_type IS '操作类型（create/update/delete/import_builtin/import_json）']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.id_operator IS '操作者用户ID']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.cd_operator IS '操作者账号']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.na_operator IS '操作者姓名']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.change_summary IS '变更摘要']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.before_json IS '变更前模板快照']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.after_json IS '变更后模板快照']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.diff_json IS '字段级差异JSON']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.operation_time IS '操作时间']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.fg_active IS '逻辑删除标记']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.insert_time IS '创建时间']';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN c_ai_symptom_template_change_log.update_time IS '更新时间']';
    END IF;

    create_index_if_missing(
        'IDX_C_AI_SYMPTOM_LOG_TEMPLATE',
        'CREATE INDEX idx_c_ai_symptom_log_template ON c_ai_symptom_template_change_log (id_template, operation_time, fg_active)'
    );
    create_index_if_missing(
        'IDX_C_AI_SYMPTOM_LOG_OPERATOR',
        'CREATE INDEX idx_c_ai_symptom_log_operator ON c_ai_symptom_template_change_log (id_operator, operation_time, fg_active)'
    );
    create_index_if_missing(
        'IDX_C_AI_SYMPTOM_LOG_ACTION',
        'CREATE INDEX idx_c_ai_symptom_log_action ON c_ai_symptom_template_change_log (operation_type, operation_time, fg_active)'
    );
    create_index_if_missing(
        'IDX_C_AI_SYMPTOM_LOG_SCOPE',
        'CREATE INDEX idx_c_ai_symptom_log_scope ON c_ai_symptom_template_change_log (sd_medical_mode, id_org, id_region, fg_active)'
    );
END;
/
