-- 存量库升级脚本：为 c_ai_op_log 补齐结构化日志查询列

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'C_AI_OP_LOG' AND column_name = 'OP_ACTION';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_op_log ADD (op_action VARCHAR2(256))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_op_log.op_action IS ''业务动作编码''';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'C_AI_OP_LOG' AND column_name = 'OP_TITLE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_op_log ADD (op_title VARCHAR2(500))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_op_log.op_title IS ''业务标题''';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'C_AI_OP_LOG' AND column_name = 'SOURCE_MODULE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_op_log ADD (source_module VARCHAR2(128))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_op_log.source_module IS ''来源模块''';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'C_AI_OP_LOG' AND column_name = 'SCENE_CODE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_op_log ADD (scene_code VARCHAR2(256))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_op_log.scene_code IS ''业务场景编码''';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'C_AI_OP_LOG' AND column_name = 'TRACE_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_op_log ADD (trace_id VARCHAR2(64))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN c_ai_op_log.trace_id IS ''调用链traceId''';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_OP_LOG_TRACE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_op_log_trace ON c_ai_op_log (trace_id, operation_time, fg_active)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_OP_LOG_SCENE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_op_log_scene ON c_ai_op_log (source_module, scene_code, operation_time, fg_active)';
    END IF;
END;
/