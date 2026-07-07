package com.regionalai.floatingball.server.modules.patientmemory.entity;

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
@TableName("c_ai_patient_memory_audit")
public class AiPatientMemoryAudit extends BaseEntity {

    @TableId(value = "id_audit", type = IdType.INPUT)
    private String idAudit;

    @TableField("id_memory")
    private String idMemory;

    @TableField("id_fact")
    private String idFact;

    @TableField("action_code")
    private String actionCode;

    @TableField("before_json")
    private String beforeJson;

    @TableField("after_json")
    private String afterJson;

    @TableField("note_text")
    private String noteText;

    @TableField("id_operator")
    private String idOperator;

    @TableField("na_operator")
    private String naOperator;

    @TableField("operation_time")
    private LocalDateTime operationTime;
}
