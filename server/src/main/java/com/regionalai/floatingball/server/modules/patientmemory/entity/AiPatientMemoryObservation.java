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
@TableName("c_ai_patient_memory_obs")
public class AiPatientMemoryObservation extends BaseEntity {

    @TableId(value = "id_observation", type = IdType.INPUT)
    private String idObservation;

    @TableField("id_memory")
    private String idMemory;

    @TableField("id_device")
    private String idDevice;

    @TableField("source_key")
    private String sourceKey;

    @TableField("source_key_hash")
    private String sourceKeyHash;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_version")
    private String sourceVersion;

    @TableField("operation_code")
    private String operationCode;

    @TableField("payload_hash")
    private String payloadHash;

    @TableField("visit_id")
    private String visitId;

    @TableField("occurred_time")
    private LocalDateTime occurredTime;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("facts_json")
    private String factsJson;

    @TableField("fg_latest")
    private String fgLatest;
}
