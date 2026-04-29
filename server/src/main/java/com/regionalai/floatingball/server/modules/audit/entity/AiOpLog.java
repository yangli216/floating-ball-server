package com.regionalai.floatingball.server.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_op_log")
public class AiOpLog extends BaseEntity {

    @TableId(value = "id_log", type = IdType.ASSIGN_UUID)
    private String idLog;

    @TableField("id_device")
    private String idDevice;

    @TableField("id_org")
    private String idOrg;

    @TableField("sd_log_type")
    private String sdLogType;

    @TableField("na_module")
    private String naModule;

    @TableField("des_op")
    private String desOp;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("audio_file_path")
    private String audioFilePath;

    @TableField("consultation_id")
    private String consultationId;

    @TableField("op_result")
    private String opResult;

    @TableField("operation_time")
    private LocalDateTime operationTime;
}
