-- 存量库升级脚本：为操作日志补充问诊ID关联字段
-- 使用当前应用 schema 账号执行；脚本可重复执行

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_OP_LOG' AND column_name = 'CONSULTATION_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_op_log ADD (consultation_id VARCHAR2(64))';
    END IF;
END;
/

COMMENT ON COLUMN c_ai_op_log.consultation_id IS '关联问诊ID（语音问诊场景）';

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_OP_LOG_CONSULTATION';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_op_log_consultation ON c_ai_op_log (consultation_id, operation_time, fg_active)';
    END IF;
END;
/
