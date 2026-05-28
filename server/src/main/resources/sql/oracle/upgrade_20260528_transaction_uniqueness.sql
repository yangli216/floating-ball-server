-- 存量库升级脚本：补关键业务唯一索引，配合服务端事务边界防止并发重复写入
-- 使用当前应用 schema 账号执行；脚本可重复执行
-- 若存在重复激活数据，脚本会中止，请先按错误提示清理重复记录后重试

DECLARE
    v_count NUMBER;

    FUNCTION table_exists(p_table_name VARCHAR2) RETURN BOOLEAN IS
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM user_tables
         WHERE table_name = UPPER(p_table_name);
        RETURN v_count > 0;
    END;

    PROCEDURE assert_no_duplicate(
        p_check_sql VARCHAR2,
        p_message VARCHAR2
    ) IS
    BEGIN
        EXECUTE IMMEDIATE p_check_sql INTO v_count;
        IF v_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20080, p_message || '，重复组数=' || v_count);
        END IF;
    END;

    PROCEDURE create_index_if_missing(
        p_index_name VARCHAR2,
        p_ddl VARCHAR2
    ) IS
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM user_indexes
         WHERE index_name = UPPER(p_index_name);
        IF v_count = 0 THEN
            EXECUTE IMMEDIATE p_ddl;
        END IF;
    END;
BEGIN
    IF table_exists('c_ai_org') THEN
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT cd_org
                   FROM c_ai_org
                  WHERE fg_active = ''1'' AND cd_org IS NOT NULL
                  GROUP BY cd_org
                 HAVING COUNT(*) > 1
             )',
            'c_ai_org 存在重复激活机构编码 cd_org'
        );
        create_index_if_missing(
            'uk_c_ai_org_code_active',
            'CREATE UNIQUE INDEX uk_c_ai_org_code_active ON c_ai_org (
                 CASE WHEN fg_active = ''1'' THEN cd_org END
             )'
        );
    END IF;

    IF table_exists('c_ai_device') THEN
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT id_org, cd_device
                   FROM c_ai_device
                  WHERE fg_active = ''1''
                  GROUP BY id_org, cd_device
                 HAVING COUNT(*) > 1
             )',
            'c_ai_device 存在重复激活设备编码 id_org + cd_device'
        );
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT device_token
                   FROM c_ai_device
                  WHERE fg_active = ''1''
                  GROUP BY device_token
                 HAVING COUNT(*) > 1
             )',
            'c_ai_device 存在重复激活设备令牌 device_token'
        );
        create_index_if_missing(
            'uk_c_ai_device_code_org_active',
            'CREATE UNIQUE INDEX uk_c_ai_device_code_org_active ON c_ai_device (
                 CASE WHEN fg_active = ''1'' THEN id_org END,
                 CASE WHEN fg_active = ''1'' THEN cd_device END
             )'
        );
        create_index_if_missing(
            'uk_c_ai_device_token_active',
            'CREATE UNIQUE INDEX uk_c_ai_device_token_active ON c_ai_device (
                 CASE WHEN fg_active = ''1'' THEN device_token END
             )'
        );
    END IF;

    IF table_exists('c_ai_user_consultation_log') THEN
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT consultation_id, consultation_type, NVL(id_device, ''-'') AS id_device_key
                   FROM c_ai_user_consultation_log
                  WHERE fg_active = ''1''
                  GROUP BY consultation_id, consultation_type, NVL(id_device, ''-'')
                 HAVING COUNT(*) > 1
             )',
            'c_ai_user_consultation_log 存在重复激活问诊日志 consultation_id + consultation_type + id_device'
        );
        create_index_if_missing(
            'uk_c_ai_user_log_consultation_active',
            'CREATE UNIQUE INDEX uk_c_ai_user_log_consultation_active ON c_ai_user_consultation_log (
                 CASE WHEN fg_active = ''1'' THEN consultation_id END,
                 CASE WHEN fg_active = ''1'' THEN consultation_type END,
                 CASE WHEN fg_active = ''1'' THEN NVL(id_device, ''-'') END
             )'
        );
    END IF;

    IF table_exists('c_ai_feedback') THEN
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT NVL(id_device, ''-'') AS id_device_key, feedback_scope_key
                   FROM c_ai_feedback
                  WHERE fg_active = ''1''
                    AND fg_latest = ''1''
                    AND feedback_scope_key IS NOT NULL
                  GROUP BY NVL(id_device, ''-''), feedback_scope_key
                 HAVING COUNT(*) > 1
             )',
            'c_ai_feedback 存在重复激活最新版反馈 id_device + feedback_scope_key'
        );
        create_index_if_missing(
            'uk_c_ai_feedback_latest_scope',
            'CREATE UNIQUE INDEX uk_c_ai_feedback_latest_scope ON c_ai_feedback (
                 CASE WHEN fg_active = ''1'' AND fg_latest = ''1'' AND feedback_scope_key IS NOT NULL THEN NVL(id_device, ''-'') END,
                 CASE WHEN fg_active = ''1'' AND fg_latest = ''1'' AND feedback_scope_key IS NOT NULL THEN feedback_scope_key END
             )'
        );
    END IF;

    IF table_exists('c_ai_user') THEN
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT cd_user
                   FROM c_ai_user
                  WHERE fg_active = ''1''
                  GROUP BY cd_user
                 HAVING COUNT(*) > 1
             )',
            'c_ai_user 存在重复激活登录账号 cd_user'
        );
        create_index_if_missing(
            'uk_c_ai_user_code_active',
            'CREATE UNIQUE INDEX uk_c_ai_user_code_active ON c_ai_user (
                 CASE WHEN fg_active = ''1'' THEN cd_user END
             )'
        );
    END IF;

    IF table_exists('c_ai_role') THEN
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT cd_role
                   FROM c_ai_role
                  WHERE fg_active = ''1''
                  GROUP BY cd_role
                 HAVING COUNT(*) > 1
             )',
            'c_ai_role 存在重复激活角色编码 cd_role'
        );
        create_index_if_missing(
            'uk_c_ai_role_code_active',
            'CREATE UNIQUE INDEX uk_c_ai_role_code_active ON c_ai_role (
                 CASE WHEN fg_active = ''1'' THEN cd_role END
             )'
        );
    END IF;

    IF table_exists('c_ai_user_role') THEN
        assert_no_duplicate(
            'SELECT COUNT(*) FROM (
                 SELECT id_user, id_role
                   FROM c_ai_user_role
                  WHERE fg_active = ''1''
                  GROUP BY id_user, id_role
                 HAVING COUNT(*) > 1
             )',
            'c_ai_user_role 存在重复激活用户角色映射 id_user + id_role'
        );
        create_index_if_missing(
            'uk_c_ai_user_role_active',
            'CREATE UNIQUE INDEX uk_c_ai_user_role_active ON c_ai_user_role (
                 CASE WHEN fg_active = ''1'' THEN id_user END,
                 CASE WHEN fg_active = ''1'' THEN id_role END
             )'
        );
    END IF;
END;
/
