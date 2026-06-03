alter table C_AI_DEVICE add register_ip VARCHAR2(64);
alter table C_AI_DEVICE add last_seen_ip VARCHAR2(64);
COMMENT ON COLUMN c_ai_device.register_ip IS '注册来源IP';
COMMENT ON COLUMN c_ai_device.last_seen_ip IS '最近访问来源IP';