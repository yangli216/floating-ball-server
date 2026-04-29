-- 存量库升级脚本：为运维用户日志补充变更计数字段
-- 使用当前应用 schema 账号执行；脚本可重复执行

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_USER_CONSULTATION_LOG' AND column_name = 'CHANGE_SUMMARY_JSON';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_user_consultation_log ADD (change_summary_json CLOB)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'C_AI_USER_CONSULTATION_LOG' AND column_name = 'TOTAL_CHANGES';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE c_ai_user_consultation_log ADD (total_changes NUMBER(5))';
    END IF;
END;
/

COMMENT ON COLUMN c_ai_user_consultation_log.change_summary_json IS '变更汇总JSON（含各类别变更数）';
COMMENT ON COLUMN c_ai_user_consultation_log.total_changes IS '变更总项数';
