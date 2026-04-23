package com.regionalai.floatingball.server.modules.symptom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_symptom_template")
public class AiSymptomTemplate extends BaseEntity {

    @TableId(value = "id_template", type = IdType.ASSIGN_UUID)
    private String idTemplate;

    @TableField("cd_symptom")
    private String cdSymptom;

    @TableField("na_symptom")
    private String naSymptom;

    @TableField("sd_medical_mode")
    private String sdMedicalMode;

    @TableField("des_symptom")
    private String desSymptom;

    @TableField("fg_common")
    private String fgCommon;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("system_category_json")
    private String systemCategoryJson;

    @TableField("system_category_tokens")
    private String systemCategoryTokens;

    @TableField("body_parts_json")
    private String bodyPartsJson;

    @TableField("body_parts_tokens")
    private String bodyPartsTokens;

    @TableField("custom_script")
    private String customScript;

    @TableField("applicable_population_json")
    private String applicablePopulationJson;

    @TableField("config_json")
    private String configJson;

    @TableField("tcm_metadata_json")
    private String tcmMetadataJson;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;

    @TableField("sd_status")
    private String sdStatus;
}
