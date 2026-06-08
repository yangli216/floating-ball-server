-- Oracle business schema initialization script.
-- Run this file after bootstrap.sql.
-- Before execution, switch to the target schema/user and make sure
-- the default tablespace has been prepared by DBA or execution context.
--
-- This file only contains object DDL and seed data.
-- It does not declare TABLESPACE clauses explicitly.
-- Connect with the same schema as FB_DB_USERNAME before running this file.
-- Current application default schema is RBMH_AI.
-- This file intentionally keeps only plain Oracle DDL/DML so it can run in
-- SQL Developer, Navicat, DBeaver and other generic SQL clients.
--
-- Writing convention:
-- 1. CREATE TABLE
-- 2. COMMENT ON TABLE / COMMENT ON COLUMN
-- 3. CREATE INDEX
-- 4. Seed data at the end

CREATE TABLE c_ai_region (
    id_region            VARCHAR2(32) PRIMARY KEY,
    cd_region            VARCHAR2(64),
    na_region            VARCHAR2(128) NOT NULL,
    id_parent            VARCHAR2(32),
    sd_region_type       VARCHAR2(32),
    sd_status            VARCHAR2(2) DEFAULT '1' NOT NULL,
    sort_order           NUMBER(10) DEFAULT 0,
    des_region           VARCHAR2(500),
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_region IS '区域表';
COMMENT ON COLUMN c_ai_region.id_region IS '区域主键ID';
COMMENT ON COLUMN c_ai_region.cd_region IS '区域编码';
COMMENT ON COLUMN c_ai_region.na_region IS '区域名称';
COMMENT ON COLUMN c_ai_region.id_parent IS '上级区域ID';
COMMENT ON COLUMN c_ai_region.sd_region_type IS '区域类型';
COMMENT ON COLUMN c_ai_region.sd_status IS '启停状态：1启用 0停用';
COMMENT ON COLUMN c_ai_region.sort_order IS '排序号';
COMMENT ON COLUMN c_ai_region.des_region IS '区域说明';
COMMENT ON COLUMN c_ai_region.fg_active IS '逻辑删除标记，不用于启停状态';
COMMENT ON COLUMN c_ai_region.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_region.update_time IS '更新时间';

CREATE INDEX idx_c_ai_region_active ON c_ai_region (fg_active, sd_status);


CREATE TABLE c_ai_org (
    id_org               VARCHAR2(32) PRIMARY KEY,
    cd_org               VARCHAR2(64),
    na_org               VARCHAR2(128) NOT NULL,
    id_parent            VARCHAR2(32),
    id_region            VARCHAR2(32),
    sd_org_type          VARCHAR2(32),
    sd_status            VARCHAR2(2) DEFAULT '1' NOT NULL,
    sort_order           NUMBER(10) DEFAULT 0,
    des_org              VARCHAR2(500),
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_org IS '机构表';
COMMENT ON COLUMN c_ai_org.id_org IS '机构主键ID';
COMMENT ON COLUMN c_ai_org.cd_org IS '机构编码';
COMMENT ON COLUMN c_ai_org.na_org IS '机构名称';
COMMENT ON COLUMN c_ai_org.id_parent IS '上级机构ID';
COMMENT ON COLUMN c_ai_org.id_region IS '所属区域ID';
COMMENT ON COLUMN c_ai_org.sd_org_type IS '机构类型';
COMMENT ON COLUMN c_ai_org.sd_status IS '启停状态：1启用 0停用';
COMMENT ON COLUMN c_ai_org.sort_order IS '排序号';
COMMENT ON COLUMN c_ai_org.des_org IS '机构说明';
COMMENT ON COLUMN c_ai_org.fg_active IS '逻辑删除标记，不用于启停状态';
COMMENT ON COLUMN c_ai_org.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_org.update_time IS '更新时间';

CREATE INDEX idx_c_ai_org_active ON c_ai_org (fg_active, sd_status);
CREATE INDEX idx_c_ai_org_code ON c_ai_org (cd_org, fg_active);
CREATE UNIQUE INDEX uk_c_ai_org_code_active ON c_ai_org (
    CASE WHEN fg_active = '1' THEN cd_org END
);


CREATE TABLE c_ai_device (
    id_device            VARCHAR2(32) PRIMARY KEY,
    cd_device            VARCHAR2(128) NOT NULL,
    na_device            VARCHAR2(128),
    id_org               VARCHAR2(32) NOT NULL,
    id_region            VARCHAR2(32),
    id_bind_user         VARCHAR2(32),
    device_token         VARCHAR2(64) NOT NULL,
    device_public_key    VARCHAR2(1000),
    sd_status            VARCHAR2(2) DEFAULT '0' NOT NULL,
    dt_last_heartbeat    TIMESTAMP,
    dt_registered        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_version       VARCHAR2(64),
    os_info              VARCHAR2(500),
    register_ip          VARCHAR2(64),
    last_seen_ip         VARCHAR2(64),
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_device IS '设备表';
COMMENT ON COLUMN c_ai_device.id_device IS '设备主键ID';
COMMENT ON COLUMN c_ai_device.cd_device IS '设备编码';
COMMENT ON COLUMN c_ai_device.na_device IS '设备名称';
COMMENT ON COLUMN c_ai_device.id_org IS '所属机构ID';
COMMENT ON COLUMN c_ai_device.id_region IS '所属区域ID';
COMMENT ON COLUMN c_ai_device.id_bind_user IS '绑定用户ID';
COMMENT ON COLUMN c_ai_device.device_token IS '设备令牌';
COMMENT ON COLUMN c_ai_device.device_public_key IS '设备ECDSA P-256公钥（SPKI DER base64）';
COMMENT ON COLUMN c_ai_device.sd_status IS '设备状态';
COMMENT ON COLUMN c_ai_device.dt_last_heartbeat IS '最后心跳时间';
COMMENT ON COLUMN c_ai_device.dt_registered IS '注册时间';
COMMENT ON COLUMN c_ai_device.client_version IS '客户端版本';
COMMENT ON COLUMN c_ai_device.os_info IS '操作系统信息';
COMMENT ON COLUMN c_ai_device.register_ip IS '注册来源IP';
COMMENT ON COLUMN c_ai_device.last_seen_ip IS '最近访问来源IP';
COMMENT ON COLUMN c_ai_device.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_device.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_device.update_time IS '更新时间';

CREATE INDEX idx_c_ai_device_org ON c_ai_device (id_org, fg_active);
CREATE INDEX idx_c_ai_device_token ON c_ai_device (device_token, fg_active);
CREATE INDEX idx_c_ai_device_register_ip ON c_ai_device (register_ip, fg_active);
CREATE INDEX idx_c_ai_device_last_seen_ip ON c_ai_device (last_seen_ip, fg_active);
CREATE UNIQUE INDEX uk_c_ai_device_code_org_active ON c_ai_device (
    CASE WHEN fg_active = '1' THEN id_org END,
    CASE WHEN fg_active = '1' THEN cd_device END
);
CREATE UNIQUE INDEX uk_c_ai_device_token_active ON c_ai_device (
    CASE WHEN fg_active = '1' THEN device_token END
);


CREATE TABLE c_ai_config (
    id_config                VARCHAR2(32) PRIMARY KEY,
    cd_config                VARCHAR2(64),
    na_config                VARCHAR2(128) NOT NULL,
    provider                 VARCHAR2(32),
    api_base_url             VARCHAR2(500),
    api_key_encrypted        VARCHAR2(1000),
    model_name               VARCHAR2(128),
    fast_model_name          VARCHAR2(128),
    enable_thinking          CHAR(1) DEFAULT '0' NOT NULL,
    audio_api_key_encrypted  VARCHAR2(1000),
    audio_base_url           VARCHAR2(500),
    audio_model              VARCHAR2(128),
    speech_provider          VARCHAR2(64),
    speech_model             VARCHAR2(128),
    knowledge_base_enabled   CHAR(1) DEFAULT '0' NOT NULL,
    knowledge_base_base_url  VARCHAR2(500),
    pmphai_enabled           CHAR(1) DEFAULT '0' NOT NULL,
    pmphai_base_url          VARCHAR2(500),
    pmphai_app_key_encrypted VARCHAR2(1000),
    pmphai_app_secret_encrypted VARCHAR2(1000),
    reviewer_enabled         CHAR(1) DEFAULT '0' NOT NULL,
    reviewer_base_url        VARCHAR2(500),
    reviewer_api_key_encrypted VARCHAR2(1000),
    reviewer_model           VARCHAR2(128),
    reviewer_check_examination_enabled CHAR(1) DEFAULT '1' NOT NULL,
    features_json            CLOB,
    id_org                   VARCHAR2(32),
    id_region                VARCHAR2(32),
    sd_status                VARCHAR2(2) DEFAULT '1' NOT NULL,
    fg_active                CHAR(1) DEFAULT '1' NOT NULL,
    insert_time              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_config IS 'AI配置表';
COMMENT ON COLUMN c_ai_config.id_config IS '配置主键ID';
COMMENT ON COLUMN c_ai_config.cd_config IS '配置编码';
COMMENT ON COLUMN c_ai_config.na_config IS '配置名称';
COMMENT ON COLUMN c_ai_config.provider IS '服务提供商';
COMMENT ON COLUMN c_ai_config.api_base_url IS '模型接口基础地址';
COMMENT ON COLUMN c_ai_config.api_key_encrypted IS '加密后的接口密钥';
COMMENT ON COLUMN c_ai_config.model_name IS '模型名称';
COMMENT ON COLUMN c_ai_config.fast_model_name IS 'chatFast 独立模型名称';
COMMENT ON COLUMN c_ai_config.enable_thinking IS '是否启用思考模式';
COMMENT ON COLUMN c_ai_config.audio_api_key_encrypted IS '加密后的语音接口密钥，为空时复用主模型密钥';
COMMENT ON COLUMN c_ai_config.audio_base_url IS '语音接口基础地址';
COMMENT ON COLUMN c_ai_config.audio_model IS '语音模型名称';
COMMENT ON COLUMN c_ai_config.speech_provider IS '语音服务提供商';
COMMENT ON COLUMN c_ai_config.speech_model IS '语音服务模型';
COMMENT ON COLUMN c_ai_config.knowledge_base_enabled IS '知识库开关';
COMMENT ON COLUMN c_ai_config.knowledge_base_base_url IS '知识库服务地址';
COMMENT ON COLUMN c_ai_config.pmphai_enabled IS '人卫知识库开关';
COMMENT ON COLUMN c_ai_config.pmphai_base_url IS '人卫知识库服务地址';
COMMENT ON COLUMN c_ai_config.pmphai_app_key_encrypted IS '加密后的人卫知识库App Key';
COMMENT ON COLUMN c_ai_config.pmphai_app_secret_encrypted IS '加密后的人卫知识库App Secret';
COMMENT ON COLUMN c_ai_config.reviewer_enabled IS '审查模型开关';
COMMENT ON COLUMN c_ai_config.reviewer_base_url IS '审查模型服务地址';
COMMENT ON COLUMN c_ai_config.reviewer_api_key_encrypted IS '加密后的审查模型密钥';
COMMENT ON COLUMN c_ai_config.reviewer_model IS '审查模型名称';
COMMENT ON COLUMN c_ai_config.reviewer_check_examination_enabled IS '是否启用检查项目独立审查';
COMMENT ON COLUMN c_ai_config.features_json IS '功能开关配置JSON';
COMMENT ON COLUMN c_ai_config.id_org IS '机构级配置范围ID';
COMMENT ON COLUMN c_ai_config.id_region IS '区域级配置范围ID';
COMMENT ON COLUMN c_ai_config.sd_status IS '状态';
COMMENT ON COLUMN c_ai_config.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_config.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_config.update_time IS '更新时间';

CREATE INDEX idx_c_ai_config_scope ON c_ai_config (id_org, id_region, fg_active, sd_status);


CREATE TABLE c_ai_prompt (
    id_prompt            VARCHAR2(32) PRIMARY KEY,
    cd_prompt            VARCHAR2(128) NOT NULL,
    na_prompt            VARCHAR2(128) NOT NULL,
    sys_prompt           CLOB,
    user_template        CLOB,
    version_num          VARCHAR2(64),
    sd_prompt_type       VARCHAR2(64),
    sd_status            VARCHAR2(2) DEFAULT '0' NOT NULL,
    id_org               VARCHAR2(32),
    id_region            VARCHAR2(32),
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_prompt IS 'Prompt模板表';
COMMENT ON COLUMN c_ai_prompt.id_prompt IS 'Prompt主键ID';
COMMENT ON COLUMN c_ai_prompt.cd_prompt IS 'Prompt编码';
COMMENT ON COLUMN c_ai_prompt.na_prompt IS 'Prompt名称';
COMMENT ON COLUMN c_ai_prompt.sys_prompt IS '系统提示词';
COMMENT ON COLUMN c_ai_prompt.user_template IS '用户提示词模板';
COMMENT ON COLUMN c_ai_prompt.version_num IS '版本号';
COMMENT ON COLUMN c_ai_prompt.sd_prompt_type IS 'Prompt类型';
COMMENT ON COLUMN c_ai_prompt.sd_status IS '状态';
COMMENT ON COLUMN c_ai_prompt.id_org IS '机构级作用范围ID';
COMMENT ON COLUMN c_ai_prompt.id_region IS '区域级作用范围ID';
COMMENT ON COLUMN c_ai_prompt.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_prompt.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_prompt.update_time IS '更新时间';

CREATE INDEX idx_c_ai_prompt_scope ON c_ai_prompt (cd_prompt, id_org, id_region, fg_active, sd_status);


CREATE TABLE c_ai_data_package (
    id_package           VARCHAR2(32) PRIMARY KEY,
    cd_package           VARCHAR2(128),
    na_package           VARCHAR2(128) NOT NULL,
    sd_package_type      VARCHAR2(32) NOT NULL,
    version_num          VARCHAR2(64),
    content_json         CLOB,
    sd_status            VARCHAR2(2) DEFAULT '0' NOT NULL,
    id_org               VARCHAR2(32),
    id_region            VARCHAR2(32),
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_data_package IS '数据包表';
COMMENT ON COLUMN c_ai_data_package.id_package IS '数据包主键ID';
COMMENT ON COLUMN c_ai_data_package.cd_package IS '数据包编码';
COMMENT ON COLUMN c_ai_data_package.na_package IS '数据包名称';
COMMENT ON COLUMN c_ai_data_package.sd_package_type IS '数据包类型';
COMMENT ON COLUMN c_ai_data_package.version_num IS '版本号';
COMMENT ON COLUMN c_ai_data_package.content_json IS '数据包内容JSON';
COMMENT ON COLUMN c_ai_data_package.sd_status IS '状态';
COMMENT ON COLUMN c_ai_data_package.id_org IS '机构级作用范围ID';
COMMENT ON COLUMN c_ai_data_package.id_region IS '区域级作用范围ID';
COMMENT ON COLUMN c_ai_data_package.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_data_package.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_data_package.update_time IS '更新时间';

CREATE INDEX idx_c_ai_package_scope ON c_ai_data_package (sd_package_type, id_org, id_region, fg_active, sd_status);


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
);

COMMENT ON TABLE c_ai_symptom_template IS '症状模板表';
COMMENT ON COLUMN c_ai_symptom_template.id_template IS '症状模板主键ID';
COMMENT ON COLUMN c_ai_symptom_template.cd_symptom IS '症状Key/编码';
COMMENT ON COLUMN c_ai_symptom_template.na_symptom IS '症状名称';
COMMENT ON COLUMN c_ai_symptom_template.sd_medical_mode IS '医学模式（western/tcm）';
COMMENT ON COLUMN c_ai_symptom_template.des_symptom IS '症状描述';
COMMENT ON COLUMN c_ai_symptom_template.fg_common IS '是否常用症状';
COMMENT ON COLUMN c_ai_symptom_template.sort_order IS '排序号';
COMMENT ON COLUMN c_ai_symptom_template.system_category_json IS '系统分类JSON数组';
COMMENT ON COLUMN c_ai_symptom_template.system_category_tokens IS '系统分类检索token';
COMMENT ON COLUMN c_ai_symptom_template.body_parts_json IS '部位JSON数组';
COMMENT ON COLUMN c_ai_symptom_template.body_parts_tokens IS '部位检索token';
COMMENT ON COLUMN c_ai_symptom_template.custom_script IS '自定义脚本';
COMMENT ON COLUMN c_ai_symptom_template.applicable_population_json IS '适用人群JSON';
COMMENT ON COLUMN c_ai_symptom_template.config_json IS '问诊配置JSON';
COMMENT ON COLUMN c_ai_symptom_template.tcm_metadata_json IS '中医扩展元数据JSON';
COMMENT ON COLUMN c_ai_symptom_template.id_org IS '机构级作用范围ID';
COMMENT ON COLUMN c_ai_symptom_template.id_region IS '区域级作用范围ID';
COMMENT ON COLUMN c_ai_symptom_template.sd_status IS '状态（1启用 0停用）';
COMMENT ON COLUMN c_ai_symptom_template.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_symptom_template.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_symptom_template.update_time IS '更新时间';

CREATE INDEX idx_c_ai_symptom_scope ON c_ai_symptom_template (sd_medical_mode, id_org, id_region, fg_active, sd_status);
CREATE INDEX idx_c_ai_symptom_code ON c_ai_symptom_template (cd_symptom, sd_medical_mode, id_org, id_region, fg_active);
CREATE INDEX idx_c_ai_symptom_sort ON c_ai_symptom_template (sd_medical_mode, sort_order, fg_active);


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
);

COMMENT ON TABLE c_ai_inpatient_emr_tpl_cache IS '住院病历HTML模板解析缓存表';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.id_cache IS '模板缓存主键ID';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.template_id IS 'HIS病历模板主键';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.template_hash IS 'HTML模板内容HASH';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.template_name IS '模板名称';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.html_content IS '模板HTML原文';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.fields_json IS 'data-id字段解析结果JSON';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.field_count IS '字段数量';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.sd_status IS '状态（1启用 0停用）';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_inpatient_emr_tpl_cache.update_time IS '更新时间';

CREATE INDEX idx_c_ai_inemr_tpl_id ON c_ai_inpatient_emr_tpl_cache (template_id, fg_active, sd_status);
CREATE INDEX idx_c_ai_inemr_tpl_hash ON c_ai_inpatient_emr_tpl_cache (template_hash, fg_active, sd_status);
CREATE INDEX idx_c_ai_inemr_tpl_status ON c_ai_inpatient_emr_tpl_cache (fg_active, sd_status, update_time);


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
);

COMMENT ON TABLE c_ai_symptom_template_change_log IS '症状模板修改日志表';
COMMENT ON COLUMN c_ai_symptom_template_change_log.id_log IS '修改日志主键ID';
COMMENT ON COLUMN c_ai_symptom_template_change_log.id_template IS '症状模板ID';
COMMENT ON COLUMN c_ai_symptom_template_change_log.cd_symptom IS '症状Key/编码';
COMMENT ON COLUMN c_ai_symptom_template_change_log.na_symptom IS '症状名称';
COMMENT ON COLUMN c_ai_symptom_template_change_log.sd_medical_mode IS '医学模式（western/tcm）';
COMMENT ON COLUMN c_ai_symptom_template_change_log.id_org IS '机构级作用范围ID';
COMMENT ON COLUMN c_ai_symptom_template_change_log.id_region IS '区域级作用范围ID';
COMMENT ON COLUMN c_ai_symptom_template_change_log.operation_type IS '操作类型（create/update/delete/import_builtin/import_json）';
COMMENT ON COLUMN c_ai_symptom_template_change_log.id_operator IS '操作者用户ID';
COMMENT ON COLUMN c_ai_symptom_template_change_log.cd_operator IS '操作者账号';
COMMENT ON COLUMN c_ai_symptom_template_change_log.na_operator IS '操作者姓名';
COMMENT ON COLUMN c_ai_symptom_template_change_log.change_summary IS '变更摘要';
COMMENT ON COLUMN c_ai_symptom_template_change_log.before_json IS '变更前模板快照';
COMMENT ON COLUMN c_ai_symptom_template_change_log.after_json IS '变更后模板快照';
COMMENT ON COLUMN c_ai_symptom_template_change_log.diff_json IS '字段级差异JSON';
COMMENT ON COLUMN c_ai_symptom_template_change_log.operation_time IS '操作时间';
COMMENT ON COLUMN c_ai_symptom_template_change_log.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_symptom_template_change_log.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_symptom_template_change_log.update_time IS '更新时间';

CREATE INDEX idx_c_ai_symptom_log_template ON c_ai_symptom_template_change_log (id_template, operation_time, fg_active);
CREATE INDEX idx_c_ai_symptom_log_operator ON c_ai_symptom_template_change_log (id_operator, operation_time, fg_active);
CREATE INDEX idx_c_ai_symptom_log_action ON c_ai_symptom_template_change_log (operation_type, operation_time, fg_active);
CREATE INDEX idx_c_ai_symptom_log_scope ON c_ai_symptom_template_change_log (sd_medical_mode, id_org, id_region, fg_active);


CREATE TABLE c_ai_op_log (
    id_log               VARCHAR2(32) PRIMARY KEY,
    id_device            VARCHAR2(32),
    id_org               VARCHAR2(32),
    sd_log_type          VARCHAR2(64),
    na_module            VARCHAR2(128),
    op_action            VARCHAR2(256),
    op_title             VARCHAR2(500),
    source_module        VARCHAR2(128),
    scene_code           VARCHAR2(256),
    trace_id             VARCHAR2(64),
    des_op               VARCHAR2(500),
    payload_json         CLOB,
    audio_file_path      VARCHAR2(1000),
    consultation_id      VARCHAR2(64),
    op_result            VARCHAR2(8),
    operation_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_op_log IS '操作日志表';
COMMENT ON COLUMN c_ai_op_log.id_log IS '日志主键ID';
COMMENT ON COLUMN c_ai_op_log.id_device IS '设备ID';
COMMENT ON COLUMN c_ai_op_log.id_org IS '机构ID';
COMMENT ON COLUMN c_ai_op_log.sd_log_type IS '日志类型';
COMMENT ON COLUMN c_ai_op_log.na_module IS '业务模块名称';
COMMENT ON COLUMN c_ai_op_log.op_action IS '业务动作编码';
COMMENT ON COLUMN c_ai_op_log.op_title IS '业务标题';
COMMENT ON COLUMN c_ai_op_log.source_module IS '来源模块';
COMMENT ON COLUMN c_ai_op_log.scene_code IS '业务场景编码';
COMMENT ON COLUMN c_ai_op_log.trace_id IS '调用链traceId';
COMMENT ON COLUMN c_ai_op_log.des_op IS '操作描述';
COMMENT ON COLUMN c_ai_op_log.payload_json IS '日志负载JSON';
COMMENT ON COLUMN c_ai_op_log.audio_file_path IS '语音代理录音文件路径';
COMMENT ON COLUMN c_ai_op_log.consultation_id IS '关联问诊ID（语音问诊场景）';
COMMENT ON COLUMN c_ai_op_log.op_result IS '操作结果';
COMMENT ON COLUMN c_ai_op_log.operation_time IS '操作时间';
COMMENT ON COLUMN c_ai_op_log.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_op_log.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_op_log.update_time IS '更新时间';

CREATE INDEX idx_c_ai_log_time ON c_ai_op_log (operation_time, fg_active);
CREATE INDEX idx_c_ai_op_log_trace ON c_ai_op_log (trace_id, operation_time, fg_active);
CREATE INDEX idx_c_ai_op_log_consultation ON c_ai_op_log (consultation_id, operation_time, fg_active);
CREATE INDEX idx_c_ai_op_log_scene ON c_ai_op_log (source_module, scene_code, operation_time, fg_active);



CREATE TABLE c_ai_user_consultation_log (
    id_log               VARCHAR2(32) PRIMARY KEY,
    consultation_id      VARCHAR2(64) NOT NULL,
    id_device            VARCHAR2(32),
    id_org               VARCHAR2(32),
    na_org               VARCHAR2(255),
    id_doctor            VARCHAR2(64),
    na_doctor            VARCHAR2(128),
    id_dept              VARCHAR2(64),
    na_dept              VARCHAR2(128),
    consultation_type    VARCHAR2(32) NOT NULL,
    consultation_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    patient_id           VARCHAR2(64),
    patient_name         VARCHAR2(128),
    patient_gender       VARCHAR2(32),
    patient_age          VARCHAR2(32),
    speech_text          CLOB,
    audio_file_path      VARCHAR2(1000),
    audio_file_name      VARCHAR2(255),
    audio_mime_type      VARCHAR2(128),
    audio_size           NUMBER(12),
    first_snapshot_json  CLOB,
    final_snapshot_json  CLOB,
    selection_json       CLOB,
    change_summary_json  CLOB,
    total_changes        NUMBER(5),
    status               VARCHAR2(32) DEFAULT 'generated',
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_user_consultation_log IS '运维用户日志-问诊聚合表';
COMMENT ON COLUMN c_ai_user_consultation_log.id_log IS '用户日志主键ID';
COMMENT ON COLUMN c_ai_user_consultation_log.consultation_id IS '问诊ID';
COMMENT ON COLUMN c_ai_user_consultation_log.id_device IS '设备ID';
COMMENT ON COLUMN c_ai_user_consultation_log.id_org IS '机构ID';
COMMENT ON COLUMN c_ai_user_consultation_log.na_org IS '机构名称';
COMMENT ON COLUMN c_ai_user_consultation_log.id_doctor IS '医生ID';
COMMENT ON COLUMN c_ai_user_consultation_log.na_doctor IS '医生姓名';
COMMENT ON COLUMN c_ai_user_consultation_log.id_dept IS '科室ID';
COMMENT ON COLUMN c_ai_user_consultation_log.na_dept IS '科室名称';
COMMENT ON COLUMN c_ai_user_consultation_log.consultation_type IS '问诊类型：voice语音问诊 smart智能问诊';
COMMENT ON COLUMN c_ai_user_consultation_log.consultation_time IS '问诊时间';
COMMENT ON COLUMN c_ai_user_consultation_log.patient_id IS '患者ID';
COMMENT ON COLUMN c_ai_user_consultation_log.patient_name IS '患者姓名';
COMMENT ON COLUMN c_ai_user_consultation_log.patient_gender IS '患者性别';
COMMENT ON COLUMN c_ai_user_consultation_log.patient_age IS '患者年龄';
COMMENT ON COLUMN c_ai_user_consultation_log.speech_text IS '语音问诊ASR识别文字';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_file_path IS '语音问诊录音文件路径';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_file_name IS '语音问诊录音原文件名';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_mime_type IS '语音问诊录音MIME类型';
COMMENT ON COLUMN c_ai_user_consultation_log.audio_size IS '语音问诊录音字节数';
COMMENT ON COLUMN c_ai_user_consultation_log.first_snapshot_json IS '首次AI生成内容JSON';
COMMENT ON COLUMN c_ai_user_consultation_log.final_snapshot_json IS '医生最终修改内容JSON';
COMMENT ON COLUMN c_ai_user_consultation_log.selection_json IS '最终选中状态JSON';
COMMENT ON COLUMN c_ai_user_consultation_log.change_summary_json IS '变更汇总JSON（含各类别变更数）';
COMMENT ON COLUMN c_ai_user_consultation_log.total_changes IS '变更总项数';
COMMENT ON COLUMN c_ai_user_consultation_log.status IS '状态：generated已生成 completed已完成';
COMMENT ON COLUMN c_ai_user_consultation_log.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_user_consultation_log.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_user_consultation_log.update_time IS '更新时间';

CREATE INDEX idx_c_ai_user_log_time ON c_ai_user_consultation_log (consultation_time, fg_active);
CREATE INDEX idx_c_ai_user_log_patient ON c_ai_user_consultation_log (patient_id, consultation_time, fg_active);
CREATE INDEX idx_c_ai_user_log_doctor ON c_ai_user_consultation_log (id_doctor, consultation_time, fg_active);
CREATE INDEX idx_c_ai_user_log_consultation ON c_ai_user_consultation_log (consultation_id, consultation_type, id_device, fg_active);
CREATE UNIQUE INDEX uk_c_ai_user_log_consultation_active ON c_ai_user_consultation_log (
    CASE WHEN fg_active = '1' THEN consultation_id END,
    CASE WHEN fg_active = '1' THEN consultation_type END,
    CASE WHEN fg_active = '1' THEN NVL(id_device, '-') END
);


CREATE TABLE c_ai_feature_event (
    id_event             VARCHAR2(64) PRIMARY KEY,
    id_device            VARCHAR2(32),
    id_org               VARCHAR2(32),
    id_region            VARCHAR2(32),
    feature_code         VARCHAR2(64) NOT NULL,
    feature_name         VARCHAR2(128) NOT NULL,
    event_action         VARCHAR2(128),
    idempotency_key      VARCHAR2(255) NOT NULL,
    trace_id             VARCHAR2(64),
    consultation_id      VARCHAR2(64),
    session_id           VARCHAR2(64),
    source_module        VARCHAR2(128),
    scene_code           VARCHAR2(256),
    id_doctor            VARCHAR2(64),
    na_doctor            VARCHAR2(128),
    id_dept              VARCHAR2(64),
    na_dept              VARCHAR2(128),
    event_status         VARCHAR2(32) DEFAULT 'success',
    payload_json         CLOB,
    event_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_feature_event IS '辅诊功能调用事件表';
COMMENT ON COLUMN c_ai_feature_event.id_event IS '事件主键ID';
COMMENT ON COLUMN c_ai_feature_event.id_device IS '设备ID';
COMMENT ON COLUMN c_ai_feature_event.id_org IS '机构ID';
COMMENT ON COLUMN c_ai_feature_event.id_region IS '区域ID';
COMMENT ON COLUMN c_ai_feature_event.feature_code IS '功能编码';
COMMENT ON COLUMN c_ai_feature_event.feature_name IS '功能展示名称';
COMMENT ON COLUMN c_ai_feature_event.event_action IS '功能动作编码';
COMMENT ON COLUMN c_ai_feature_event.idempotency_key IS '幂等键，同一设备内唯一';
COMMENT ON COLUMN c_ai_feature_event.trace_id IS '关联AI调用traceId';
COMMENT ON COLUMN c_ai_feature_event.consultation_id IS '关联问诊ID';
COMMENT ON COLUMN c_ai_feature_event.session_id IS '关联会话ID';
COMMENT ON COLUMN c_ai_feature_event.source_module IS '来源模块';
COMMENT ON COLUMN c_ai_feature_event.scene_code IS '场景编码';
COMMENT ON COLUMN c_ai_feature_event.id_doctor IS '医生ID';
COMMENT ON COLUMN c_ai_feature_event.na_doctor IS '医生姓名';
COMMENT ON COLUMN c_ai_feature_event.id_dept IS '科室ID';
COMMENT ON COLUMN c_ai_feature_event.na_dept IS '科室名称';
COMMENT ON COLUMN c_ai_feature_event.event_status IS '事件状态：success/failure';
COMMENT ON COLUMN c_ai_feature_event.payload_json IS '事件扩展负载JSON';
COMMENT ON COLUMN c_ai_feature_event.event_time IS '事件发生时间';
COMMENT ON COLUMN c_ai_feature_event.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_feature_event.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_feature_event.update_time IS '更新时间';

CREATE UNIQUE INDEX uk_c_ai_feature_event_idem ON c_ai_feature_event (id_device, idempotency_key);
CREATE INDEX idx_c_ai_feature_event_time ON c_ai_feature_event (event_time, fg_active);
CREATE INDEX idx_c_ai_feature_event_feature ON c_ai_feature_event (feature_name, event_time, fg_active);
CREATE INDEX idx_c_ai_feature_event_doctor ON c_ai_feature_event (id_doctor, event_time, fg_active);
CREATE INDEX idx_c_ai_feature_event_org ON c_ai_feature_event (id_org, id_region, event_time, fg_active);


CREATE TABLE c_security_rejection_log (
    id_log               VARCHAR2(32) PRIMARY KEY,
    rejection_type       VARCHAR2(64) NOT NULL,
    request_method       VARCHAR2(16),
    request_path         VARCHAR2(512),
    client_ip            VARCHAR2(64),
    id_device            VARCHAR2(32),
    cd_device            VARCHAR2(128),
    id_org               VARCHAR2(32),
    request_id           VARCHAR2(64),
    reject_reason        VARCHAR2(255),
    reject_detail        VARCHAR2(500),
    has_signature        CHAR(1) DEFAULT '0',
    timestamp_header     VARCHAR2(32),
    nonce_header         VARCHAR2(64),
    client_version       VARCHAR2(32),
    update_channel       VARCHAR2(32),
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_security_rejection_log IS '安全拒绝日志表';
COMMENT ON COLUMN c_security_rejection_log.id_log IS '日志主键ID';
COMMENT ON COLUMN c_security_rejection_log.rejection_type IS '拒绝类型';
COMMENT ON COLUMN c_security_rejection_log.request_method IS '请求方法';
COMMENT ON COLUMN c_security_rejection_log.request_path IS '请求路径';
COMMENT ON COLUMN c_security_rejection_log.client_ip IS '客户端IP';
COMMENT ON COLUMN c_security_rejection_log.id_device IS '设备ID';
COMMENT ON COLUMN c_security_rejection_log.cd_device IS '设备编码';
COMMENT ON COLUMN c_security_rejection_log.id_org IS '机构ID';
COMMENT ON COLUMN c_security_rejection_log.request_id IS '请求ID';
COMMENT ON COLUMN c_security_rejection_log.reject_reason IS '拒绝原因';
COMMENT ON COLUMN c_security_rejection_log.reject_detail IS '拒绝详情';
COMMENT ON COLUMN c_security_rejection_log.has_signature IS '是否携带签名';
COMMENT ON COLUMN c_security_rejection_log.timestamp_header IS '请求时间戳头';
COMMENT ON COLUMN c_security_rejection_log.nonce_header IS '请求nonce头';
COMMENT ON COLUMN c_security_rejection_log.client_version IS '客户端版本';
COMMENT ON COLUMN c_security_rejection_log.update_channel IS '更新通道';
COMMENT ON COLUMN c_security_rejection_log.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_security_rejection_log.insert_time IS '创建时间';
COMMENT ON COLUMN c_security_rejection_log.update_time IS '更新时间';

CREATE INDEX idx_c_security_rej_time ON c_security_rejection_log (insert_time, fg_active);
CREATE INDEX idx_c_security_rej_type ON c_security_rejection_log (rejection_type, insert_time, fg_active);
CREATE INDEX idx_c_security_rej_ip ON c_security_rejection_log (client_ip, insert_time, fg_active);
CREATE INDEX idx_c_security_rej_device ON c_security_rejection_log (id_device, insert_time, fg_active);
CREATE INDEX idx_c_security_rej_path ON c_security_rejection_log (request_path, insert_time, fg_active);


CREATE TABLE c_ai_feedback (
    id_feedback           VARCHAR2(32) PRIMARY KEY,
    id_device             VARCHAR2(32),
    id_org                VARCHAR2(32),
    na_org                VARCHAR2(255),
    id_doctor             VARCHAR2(64),
    na_doctor             VARCHAR2(128),
    id_dept               VARCHAR2(64),
    na_dept               VARCHAR2(128),
    session_id            VARCHAR2(64),
    trace_id              VARCHAR2(64),
    source_module         VARCHAR2(128),
    kind                  VARCHAR2(32) DEFAULT 'general',
    severity              VARCHAR2(16) DEFAULT 'medium',
    tags_json             VARCHAR2(1000),
    has_correction        CHAR(1) DEFAULT '0',
    has_trace             CHAR(1) DEFAULT '0',
    score                 NUMBER(2) NOT NULL,
    comment_text          VARCHAR2(2000) NOT NULL,
    screenshot_file_name  VARCHAR2(255),
    screenshot_mime_type  VARCHAR2(128),
    screenshot_data_url   CLOB,
    feedback_scope_key    VARCHAR2(255),
    id_feedback_root      VARCHAR2(32),
    previous_feedback_id  VARCHAR2(32),
    revision_no           NUMBER(10) DEFAULT 1,
    fg_latest             CHAR(1) DEFAULT '1' NOT NULL,
    chain_context_json    CLOB,
    feedback_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fg_active             CHAR(1) DEFAULT '1' NOT NULL,
    insert_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_feedback IS '用户反馈表';
COMMENT ON COLUMN c_ai_feedback.id_feedback IS '反馈主键ID';
COMMENT ON COLUMN c_ai_feedback.id_device IS '设备ID';
COMMENT ON COLUMN c_ai_feedback.id_org IS '机构ID';
COMMENT ON COLUMN c_ai_feedback.na_org IS '机构名称';
COMMENT ON COLUMN c_ai_feedback.id_doctor IS '反馈医生ID';
COMMENT ON COLUMN c_ai_feedback.na_doctor IS '反馈医生姓名';
COMMENT ON COLUMN c_ai_feedback.id_dept IS '反馈科室ID';
COMMENT ON COLUMN c_ai_feedback.na_dept IS '反馈科室名称';
COMMENT ON COLUMN c_ai_feedback.session_id IS '会话ID';
COMMENT ON COLUMN c_ai_feedback.trace_id IS '关联的 AI 调用 traceId';
COMMENT ON COLUMN c_ai_feedback.source_module IS '反馈来源模块';
COMMENT ON COLUMN c_ai_feedback.kind IS '反馈类型：general/recommendation/record_field/session';
COMMENT ON COLUMN c_ai_feedback.severity IS '严重度：low/medium/high';
COMMENT ON COLUMN c_ai_feedback.tags_json IS '问题标签 JSON 数组';
COMMENT ON COLUMN c_ai_feedback.has_correction IS '是否包含医生修正';
COMMENT ON COLUMN c_ai_feedback.has_trace IS '是否包含 AI traceId';
COMMENT ON COLUMN c_ai_feedback.score IS '反馈评分';
COMMENT ON COLUMN c_ai_feedback.comment_text IS '反馈说明';
COMMENT ON COLUMN c_ai_feedback.screenshot_file_name IS '截图文件名';
COMMENT ON COLUMN c_ai_feedback.screenshot_mime_type IS '截图 MIME 类型';
COMMENT ON COLUMN c_ai_feedback.screenshot_data_url IS '截图 Data URL';
COMMENT ON COLUMN c_ai_feedback.feedback_scope_key IS '反馈槽位唯一键（同问诊+模块）';
COMMENT ON COLUMN c_ai_feedback.id_feedback_root IS '反馈修订链根记录 ID';
COMMENT ON COLUMN c_ai_feedback.previous_feedback_id IS '上一版反馈 ID';
COMMENT ON COLUMN c_ai_feedback.revision_no IS '反馈修订版本号';
COMMENT ON COLUMN c_ai_feedback.fg_latest IS '是否最新版本';
COMMENT ON COLUMN c_ai_feedback.chain_context_json IS '前端上传的链路上下文快照';
COMMENT ON COLUMN c_ai_feedback.feedback_time IS '反馈时间';
COMMENT ON COLUMN c_ai_feedback.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_feedback.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_feedback.update_time IS '更新时间';

CREATE INDEX idx_c_ai_feedback_time ON c_ai_feedback (feedback_time, fg_active);
CREATE INDEX idx_c_ai_feedback_trace ON c_ai_feedback (trace_id, fg_active);
CREATE INDEX idx_c_ai_feedback_device ON c_ai_feedback (id_device, fg_active);
CREATE INDEX idx_c_ai_feedback_kind ON c_ai_feedback (kind, fg_active);
CREATE INDEX idx_c_ai_feedback_doctor ON c_ai_feedback (id_doctor, fg_active);
CREATE INDEX idx_c_ai_feedback_dept ON c_ai_feedback (id_dept, fg_active);
CREATE INDEX idx_c_ai_feedback_scope ON c_ai_feedback (id_device, feedback_scope_key, fg_latest, fg_active);
CREATE UNIQUE INDEX uk_c_ai_feedback_latest_scope ON c_ai_feedback (
    CASE WHEN fg_active = '1' AND fg_latest = '1' AND feedback_scope_key IS NOT NULL THEN NVL(id_device, '-') END,
    CASE WHEN fg_active = '1' AND fg_latest = '1' AND feedback_scope_key IS NOT NULL THEN feedback_scope_key END
);


CREATE TABLE c_ai_user (
    id_user              VARCHAR2(32) PRIMARY KEY,
    cd_user              VARCHAR2(64) NOT NULL,
    na_user              VARCHAR2(128) NOT NULL,
    password_hash        VARCHAR2(128) NOT NULL,
    id_org               VARCHAR2(32),
    sd_status            VARCHAR2(2) DEFAULT '1' NOT NULL,
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_user IS '用户表';
COMMENT ON COLUMN c_ai_user.id_user IS '用户主键ID';
COMMENT ON COLUMN c_ai_user.cd_user IS '用户账号';
COMMENT ON COLUMN c_ai_user.na_user IS '用户姓名';
COMMENT ON COLUMN c_ai_user.password_hash IS '密码摘要';
COMMENT ON COLUMN c_ai_user.id_org IS '所属机构ID';
COMMENT ON COLUMN c_ai_user.sd_status IS '状态';
COMMENT ON COLUMN c_ai_user.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_user.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_user.update_time IS '更新时间';

CREATE INDEX idx_c_ai_user_code ON c_ai_user (cd_user, fg_active);
CREATE INDEX idx_c_ai_user_org ON c_ai_user (id_org, fg_active);
CREATE UNIQUE INDEX uk_c_ai_user_code_active ON c_ai_user (
    CASE WHEN fg_active = '1' THEN cd_user END
);


CREATE TABLE c_ai_role (
    id_role              VARCHAR2(32) PRIMARY KEY,
    cd_role              VARCHAR2(64) NOT NULL,
    na_role              VARCHAR2(128) NOT NULL,
    des_role             VARCHAR2(500),
    sd_status            VARCHAR2(2) DEFAULT '1' NOT NULL,
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_role IS '角色表';
COMMENT ON COLUMN c_ai_role.id_role IS '角色主键ID';
COMMENT ON COLUMN c_ai_role.cd_role IS '角色编码';
COMMENT ON COLUMN c_ai_role.na_role IS '角色名称';
COMMENT ON COLUMN c_ai_role.des_role IS '角色说明';
COMMENT ON COLUMN c_ai_role.sd_status IS '状态';
COMMENT ON COLUMN c_ai_role.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_role.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_role.update_time IS '更新时间';

CREATE INDEX idx_c_ai_role_code ON c_ai_role (cd_role, fg_active);
CREATE UNIQUE INDEX uk_c_ai_role_code_active ON c_ai_role (
    CASE WHEN fg_active = '1' THEN cd_role END
);


CREATE TABLE c_ai_user_role (
    id_user_role         VARCHAR2(32) PRIMARY KEY,
    id_user              VARCHAR2(32) NOT NULL,
    id_role              VARCHAR2(32) NOT NULL,
    fg_active            CHAR(1) DEFAULT '1' NOT NULL,
    insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE c_ai_user_role IS '用户角色关联表';
COMMENT ON COLUMN c_ai_user_role.id_user_role IS '关联主键ID';
COMMENT ON COLUMN c_ai_user_role.id_user IS '用户ID';
COMMENT ON COLUMN c_ai_user_role.id_role IS '角色ID';
COMMENT ON COLUMN c_ai_user_role.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_user_role.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_user_role.update_time IS '更新时间';

CREATE INDEX idx_c_ai_user_role_user ON c_ai_user_role (id_user, fg_active);
CREATE INDEX idx_c_ai_user_role_role ON c_ai_user_role (id_role, fg_active);
CREATE UNIQUE INDEX uk_c_ai_user_role_active ON c_ai_user_role (
    CASE WHEN fg_active = '1' THEN id_user END,
    CASE WHEN fg_active = '1' THEN id_role END
);


INSERT INTO c_ai_region (id_region, cd_region, na_region, sd_region_type, sd_status, fg_active)
VALUES ('REGION001', 'REG001', '默认区域', 'district', '1', '1');

INSERT INTO c_ai_org (id_org, cd_org, na_org, id_region, sd_org_type, sd_status, fg_active)
VALUES ('ORG001', 'ORG001', '默认机构', 'REGION001', 'community', '1', '1');

INSERT INTO c_ai_config (
    id_config,
    cd_config,
    na_config,
    provider,
    api_base_url,
    model_name,
    fast_model_name,
    enable_thinking,
    audio_model,
    speech_provider,
    speech_model,
    knowledge_base_enabled,
    pmphai_enabled,
    reviewer_enabled,
    reviewer_model,
    reviewer_check_examination_enabled,
    features_json,
    id_org,
    sd_status,
    fg_active
) VALUES (
    'CFG001',
    'DEFAULT',
    '默认AI配置',
    'openai-compatible',
    'http://127.0.0.1:65535/v1',
    'gpt-4o-mini',
    'gpt-4o-mini',
    '0',
    'whisper-1',
    'openai-compatible',
    'whisper-1',
    '0',
    '0',
    '0',
    'gpt-4o-mini',
    '1',
    '{"regionalMode":true,"aiProxyEnabled":true,"auditEnabled":true}',
    'ORG001',
    '1',
    '1'
);

INSERT INTO c_ai_role (id_role, cd_role, na_role, des_role, sd_status, fg_active)
VALUES ('ROLE001', 'SYSTEM_ADMIN', '系统管理员', '拥有全部后台权限', '1', '1');

INSERT INTO c_ai_user (id_user, cd_user, na_user, password_hash, id_org, sd_status, fg_active)
VALUES ('USER001', 'admin', '系统管理员', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ORG001', '1', '1');

INSERT INTO c_ai_user_role (id_user_role, id_user, id_role, fg_active)
VALUES ('USERROLE001', 'USER001', 'ROLE001', '1');

COMMIT;
