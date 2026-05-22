package com.regionalai.floatingball.server.modules.symptom.entity;

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
@TableName("c_ai_symptom_template_change_log")
public class AiSymptomTemplateChangeLog extends BaseEntity {

    @TableId(value = "id_log", type = IdType.ASSIGN_UUID)
    private String idLog;

    @TableField("id_template")
    private String idTemplate;

    @TableField("cd_symptom")
    private String cdSymptom;

    @TableField("na_symptom")
    private String naSymptom;

    @TableField("sd_medical_mode")
    private String sdMedicalMode;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;

    @TableField("operation_type")
    private String operationType;

    @TableField("id_operator")
    private String idOperator;

    @TableField("cd_operator")
    private String cdOperator;

    @TableField("na_operator")
    private String naOperator;

    @TableField("change_summary")
    private String changeSummary;

    @TableField("before_json")
    private String beforeJson;

    @TableField("after_json")
    private String afterJson;

    @TableField("diff_json")
    private String diffJson;

    @TableField("operation_time")
    private LocalDateTime operationTime;
}
