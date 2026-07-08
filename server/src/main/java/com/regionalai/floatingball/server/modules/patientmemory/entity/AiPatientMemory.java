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
@TableName("c_ai_patient_memory")
public class AiPatientMemory extends BaseEntity {

    @TableId(value = "id_memory", type = IdType.INPUT)
    private String idMemory;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;

    @TableField("id_his_org")
    private String idHisOrg;

    @TableField("patient_id")
    private String patientId;

    @TableField("patient_name")
    private String patientName;

    @TableField("patient_gender")
    private String patientGender;

    @TableField("patient_age")
    private String patientAge;

    @TableField("patient_birth_date")
    private String patientBirthDate;

    @TableField("memory_version")
    private Long memoryVersion;

    @TableField("summary_json")
    private String summaryJson;

    @TableField("conflict_count")
    private Integer conflictCount;

    @TableField("quality_status")
    private String qualityStatus;

    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;

    @TableField("last_source_time")
    private LocalDateTime lastSourceTime;

    @TableField("sd_status")
    private String sdStatus;
}
