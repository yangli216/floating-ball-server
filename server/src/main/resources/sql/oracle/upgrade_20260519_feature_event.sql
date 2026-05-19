-- 存量库升级脚本：新增辅诊功能调用事件表
-- 使用当前应用 schema 账号执行；脚本可重复执行

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'C_AI_FEATURE_EVENT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
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
                event_status         VARCHAR2(32) DEFAULT ''success'',
                payload_json         CLOB,
                event_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                fg_active            CHAR(1) DEFAULT ''1'' NOT NULL,
                insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )';
    END IF;
END;
/

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

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'UK_C_AI_FEATURE_EVENT_IDEM';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_c_ai_feature_event_idem ON c_ai_feature_event (id_device, idempotency_key)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEATURE_EVENT_TIME';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feature_event_time ON c_ai_feature_event (event_time, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEATURE_EVENT_FEATURE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feature_event_feature ON c_ai_feature_event (feature_name, event_time, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEATURE_EVENT_DOCTOR';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feature_event_doctor ON c_ai_feature_event (id_doctor, event_time, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEATURE_EVENT_ORG';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feature_event_org ON c_ai_feature_event (id_org, id_region, event_time, fg_active)';
    END IF;
END;
/
