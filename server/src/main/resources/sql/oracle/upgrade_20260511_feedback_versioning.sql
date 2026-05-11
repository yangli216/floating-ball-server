-- 存量库升级脚本：为 c_ai_feedback 增补反馈修订链与 latest 标记字段
DECLARE
    v_count NUMBER;

    PROCEDURE add_column_if_missing(
        p_table_name VARCHAR2,
        p_column_name VARCHAR2,
        p_ddl VARCHAR2
    ) IS
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM user_tab_columns
         WHERE table_name = UPPER(p_table_name)
           AND column_name = UPPER(p_column_name);
        IF v_count = 0 THEN
            EXECUTE IMMEDIATE p_ddl;
        END IF;
    END;
BEGIN
    add_column_if_missing('c_ai_feedback', 'feedback_scope_key',
        'ALTER TABLE c_ai_feedback ADD (feedback_scope_key VARCHAR2(255))');
    add_column_if_missing('c_ai_feedback', 'id_feedback_root',
        'ALTER TABLE c_ai_feedback ADD (id_feedback_root VARCHAR2(32))');
    add_column_if_missing('c_ai_feedback', 'previous_feedback_id',
        'ALTER TABLE c_ai_feedback ADD (previous_feedback_id VARCHAR2(32))');
    add_column_if_missing('c_ai_feedback', 'revision_no',
        'ALTER TABLE c_ai_feedback ADD (revision_no NUMBER(10) DEFAULT 1)');
    add_column_if_missing('c_ai_feedback', 'fg_latest',
        'ALTER TABLE c_ai_feedback ADD (fg_latest CHAR(1) DEFAULT ''1'' NOT NULL)');

    EXECUTE IMMEDIATE 'UPDATE c_ai_feedback SET revision_no = NVL(revision_no, 1) WHERE revision_no IS NULL';
    EXECUTE IMMEDIATE 'UPDATE c_ai_feedback SET fg_latest = NVL(fg_latest, ''1'') WHERE fg_latest IS NULL';
    EXECUTE IMMEDIATE 'UPDATE c_ai_feedback SET id_feedback_root = NVL(id_feedback_root, id_feedback) WHERE id_feedback_root IS NULL';

    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_C_AI_FEEDBACK_SCOPE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_c_ai_feedback_scope ON c_ai_feedback (id_device, feedback_scope_key, fg_latest, fg_active)';
    END IF;
END;
/