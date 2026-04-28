-- 存量库升级脚本：新增运维用户日志问诊聚合表
-- 使用当前应用 schema 账号执行；脚本可重复执行

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'C_AI_USER_CONSULTATION_LOG';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
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
                first_snapshot_json  CLOB,
                final_snapshot_json  CLOB,
                selection_json       CLOB,
                status               VARCHAR2(32) DEFAULT ''generated'',
                fg_active            CHAR(1) DEFAULT ''1'' NOT NULL,
                insert_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )';
    END IF;
END;
/

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
COMMENT ON COLUMN c_ai_user_consultation_log.first_snapshot_json IS '首次AI生成内容JSON';
COMMENT ON COLUMN c_ai_user_consultation_log.final_snapshot_json IS '医生最终修改内容JSON';
COMMENT ON COLUMN c_ai_user_consultation_log.selection_json IS '最终选中状态JSON';
COMMENT ON COLUMN c_ai_user_consultation_log.status IS '状态：generated已生成 completed已完成';
COMMENT ON COLUMN c_ai_user_consultation_log.fg_active IS '逻辑删除标记';
COMMENT ON COLUMN c_ai_user_consultation_log.insert_time IS '创建时间';
COMMENT ON COLUMN c_ai_user_consultation_log.update_time IS '更新时间';

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_USER_LOG_TIME';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_user_log_time ON c_ai_user_consultation_log (consultation_time, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_USER_LOG_PATIENT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_user_log_patient ON c_ai_user_consultation_log (patient_id, consultation_time, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_USER_LOG_DOCTOR';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_user_log_doctor ON c_ai_user_consultation_log (id_doctor, consultation_time, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_USER_LOG_CONSULTATION';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_user_log_consultation ON c_ai_user_consultation_log (consultation_id, consultation_type, id_device, fg_active)';
    END IF;
END;
/
