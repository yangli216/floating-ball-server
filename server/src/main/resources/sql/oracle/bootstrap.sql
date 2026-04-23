-- Oracle schema bootstrap script.
-- Run this file with SYSTEM or another DBA-capable account
-- on an existing Oracle instance/service.
--
-- This project does not create the Oracle database instance itself.
-- It only prepares the application schema/user when a dedicated schema is used.
--
-- Current default runtime user: RBMH_AI
-- Current default object tablespace: FLOATING_BALL_TS
-- Change the datafile path before first execution.
-- Default dedicated schema password template: RBMH_AI
-- Please adjust before production use.

DECLARE
    v_target_user     VARCHAR2(64) := 'RBMH_AI';
    v_target_password VARCHAR2(128) := 'RBMH_AI';
    v_tablespace_name VARCHAR2(64) := 'FLOATING_BALL_TS';
    v_tablespace_file VARCHAR2(4000) := '/u01/app/oracle/oradata/PHIS/floating_ball_ts01.dbf';
    v_count           NUMBER;
BEGIN
    SELECT COUNT(1)
      INTO v_count
      FROM dba_tablespaces
     WHERE tablespace_name = v_tablespace_name;

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLESPACE ' || v_tablespace_name
            || ' DATAFILE '''
            || REPLACE(v_tablespace_file, '''', '''''')
            || ''' SIZE 512M AUTOEXTEND ON NEXT 128M MAXSIZE UNLIMITED EXTENT MANAGEMENT LOCAL SEGMENT SPACE MANAGEMENT AUTO';
        DBMS_OUTPUT.PUT_LINE('Tablespace created: ' || v_tablespace_name);
    ELSE
        DBMS_OUTPUT.PUT_LINE('Tablespace already exists: ' || v_tablespace_name);
    END IF;

    IF UPPER(v_target_user) = 'SYSTEM' THEN
        DBMS_OUTPUT.PUT_LINE('Target user is SYSTEM, bootstrap create-user step skipped.');
        DBMS_OUTPUT.PUT_LINE('Please switch to the target schema/default tablespace before running init.sql.');
        RETURN;
    END IF;

    SELECT COUNT(1)
      INTO v_count
      FROM dba_users
     WHERE username = UPPER(v_target_user);

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER ' || v_target_user
            || ' IDENTIFIED BY "' || v_target_password || '" DEFAULT TABLESPACE '
            || v_tablespace_name || ' TEMPORARY TABLESPACE TEMP';
        DBMS_OUTPUT.PUT_LINE('User created: ' || v_target_user);
    ELSE
        DBMS_OUTPUT.PUT_LINE('User already exists: ' || v_target_user);
    END IF;

    EXECUTE IMMEDIATE 'ALTER USER ' || v_target_user || ' DEFAULT TABLESPACE ' || v_tablespace_name;
    EXECUTE IMMEDIATE 'ALTER USER ' || v_target_user || ' QUOTA UNLIMITED ON ' || v_tablespace_name;
    EXECUTE IMMEDIATE 'GRANT CREATE SESSION TO ' || v_target_user;
    EXECUTE IMMEDIATE 'GRANT CREATE TABLE TO ' || v_target_user;
    EXECUTE IMMEDIATE 'GRANT CREATE VIEW TO ' || v_target_user;
    EXECUTE IMMEDIATE 'GRANT CREATE SEQUENCE TO ' || v_target_user;
    EXECUTE IMMEDIATE 'GRANT CREATE TRIGGER TO ' || v_target_user;
    EXECUTE IMMEDIATE 'GRANT CREATE PROCEDURE TO ' || v_target_user;
    EXECUTE IMMEDIATE 'GRANT CREATE SYNONYM TO ' || v_target_user;

    DBMS_OUTPUT.PUT_LINE('Privileges granted to: ' || v_target_user);
END;
/
