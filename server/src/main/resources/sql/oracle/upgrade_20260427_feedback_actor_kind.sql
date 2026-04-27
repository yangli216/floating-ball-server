-- 存量库升级脚本：为 c_ai_feedback 增补 反馈医生/机构/科室、反馈类型/严重度/标签 等字段
-- 使用当前应用 schema 账号执行；脚本可重复执行

DECLARE
    PROCEDURE add_column_if_missing(p_table IN VARCHAR2,
                                    p_column IN VARCHAR2,
                                    p_ddl IN VARCHAR2,
                                    p_comment IN VARCHAR2) IS
        v_exists NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_exists
          FROM user_tab_columns
         WHERE table_name = UPPER(p_table)
           AND column_name = UPPER(p_column);
        IF v_exists = 0 THEN
            EXECUTE IMMEDIATE p_ddl;
        END IF;
        IF p_comment IS NOT NULL THEN
            EXECUTE IMMEDIATE 'COMMENT ON COLUMN ' || p_table || '.' || p_column || ' IS ''' || p_comment || '''';
        END IF;
    END;
BEGIN
    add_column_if_missing('c_ai_feedback', 'id_doctor',
        'ALTER TABLE c_ai_feedback ADD (id_doctor VARCHAR2(64))',
        '反馈医生ID（来自握手 urt.userRoleDepts）');
    add_column_if_missing('c_ai_feedback', 'na_doctor',
        'ALTER TABLE c_ai_feedback ADD (na_doctor VARCHAR2(128))',
        '反馈医生姓名');
    add_column_if_missing('c_ai_feedback', 'id_dept',
        'ALTER TABLE c_ai_feedback ADD (id_dept VARCHAR2(64))',
        '反馈科室ID');
    add_column_if_missing('c_ai_feedback', 'na_dept',
        'ALTER TABLE c_ai_feedback ADD (na_dept VARCHAR2(128))',
        '反馈科室名称');
    add_column_if_missing('c_ai_feedback', 'na_org',
        'ALTER TABLE c_ai_feedback ADD (na_org VARCHAR2(255))',
        '反馈机构名称（id_org 已存在，仅补名称）');
    add_column_if_missing('c_ai_feedback', 'kind',
        'ALTER TABLE c_ai_feedback ADD (kind VARCHAR2(32) DEFAULT ''general'')',
        '反馈类型：general/recommendation/record_field/session');
    add_column_if_missing('c_ai_feedback', 'severity',
        'ALTER TABLE c_ai_feedback ADD (severity VARCHAR2(16) DEFAULT ''medium'')',
        '严重度：low/medium/high');
    add_column_if_missing('c_ai_feedback', 'tags_json',
        'ALTER TABLE c_ai_feedback ADD (tags_json VARCHAR2(1000))',
        '问题标签 JSON 数组');
    add_column_if_missing('c_ai_feedback', 'has_correction',
        'ALTER TABLE c_ai_feedback ADD (has_correction CHAR(1) DEFAULT ''0'')',
        '是否包含医生修正：1是 0否');
    add_column_if_missing('c_ai_feedback', 'has_trace',
        'ALTER TABLE c_ai_feedback ADD (has_trace CHAR(1) DEFAULT ''0'')',
        '是否包含 AI traceId：1是 0否');
END;
/

-- 索引（重复执行容错）
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEEDBACK_KIND';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feedback_kind ON c_ai_feedback (kind, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEEDBACK_DOCTOR';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feedback_doctor ON c_ai_feedback (id_doctor, fg_active)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEEDBACK_DEPT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feedback_dept ON c_ai_feedback (id_dept, fg_active)';
    END IF;
END;
/
