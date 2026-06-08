package com.regionalai.floatingball.server.modules.emrtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_inpatient_emr_tpl_cache")
public class AiInpatientEmrTemplateCache extends BaseEntity {

    @TableId(value = "id_cache", type = IdType.ASSIGN_UUID)
    private String idCache;

    @TableField("template_id")
    private String templateId;

    @TableField("template_hash")
    private String templateHash;

    @TableField("template_name")
    private String templateName;

    @TableField("html_content")
    private String htmlContent;

    @TableField("fields_json")
    private String fieldsJson;

    @TableField("field_count")
    private Integer fieldCount;

    @TableField("sd_status")
    private String sdStatus;
}
