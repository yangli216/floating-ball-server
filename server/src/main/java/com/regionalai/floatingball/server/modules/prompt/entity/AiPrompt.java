package com.regionalai.floatingball.server.modules.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_prompt")
public class AiPrompt extends BaseEntity {

    @TableId(value = "id_prompt", type = IdType.ASSIGN_UUID)
    private String idPrompt;

    @TableField("cd_prompt")
    private String cdPrompt;

    @TableField("na_prompt")
    private String naPrompt;

    @TableField("sys_prompt")
    private String sysPrompt;

    @TableField("user_template")
    private String userTemplate;

    @TableField("version_num")
    private String versionNum;

    @TableField("sd_prompt_type")
    private String sdPromptType;

    @TableField("sd_status")
    private String sdStatus;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;
}
