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
@TableName("c_ai_patient_memory_fact")
public class AiPatientMemoryFact extends BaseEntity {

    @TableId(value = "id_fact", type = IdType.INPUT)
    private String idFact;

    @TableField("id_memory")
    private String idMemory;

    @TableField("fact_key")
    private String factKey;

    @TableField("fact_key_hash")
    private String factKeyHash;

    @TableField("fact_type")
    private String factType;

    @TableField("fact_code")
    private String factCode;

    @TableField("fact_name")
    private String factName;

    @TableField("value_text")
    private String valueText;

    @TableField("fact_status")
    private String factStatus;

    @TableField("confidence_level")
    private String confidenceLevel;

    @TableField("evidence_text")
    private String evidenceText;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_key")
    private String sourceKey;

    @TableField("latest_observation_id")
    private String latestObservationId;

    @TableField("origin_code")
    private String originCode;

    @TableField("fg_suppressed")
    private String fgSuppressed;

    @TableField("revision_no")
    private Integer revisionNo;

    @TableField("first_observed_time")
    private LocalDateTime firstObservedTime;

    @TableField("last_observed_time")
    private LocalDateTime lastObservedTime;
}
