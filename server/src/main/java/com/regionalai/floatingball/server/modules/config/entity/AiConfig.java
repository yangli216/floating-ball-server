package com.regionalai.floatingball.server.modules.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_config")
public class AiConfig extends BaseEntity {

    @TableId(value = "id_config", type = IdType.ASSIGN_UUID)
    private String idConfig;

    @TableField("cd_config")
    private String cdConfig;

    @TableField("na_config")
    private String naConfig;

    @TableField("provider")
    private String provider;

    @TableField("api_base_url")
    private String apiBaseUrl;

    @TableField("api_key_encrypted")
    private String apiKeyEncrypted;

    @TableField("model_name")
    private String modelName;

    @TableField("fast_model_name")
    private String fastModelName;

    @TableField("enable_thinking")
    private String enableThinking;

    @TableField("audio_api_key_encrypted")
    private String audioApiKeyEncrypted;

    @TableField("audio_base_url")
    private String audioBaseUrl;

    @TableField("audio_model")
    private String audioModel;

    @TableField("speech_provider")
    private String speechProvider;

    @TableField("speech_model")
    private String speechModel;

    @TableField("knowledge_base_enabled")
    private String knowledgeBaseEnabled;

    @TableField("knowledge_base_base_url")
    private String knowledgeBaseBaseUrl;

    @TableField("pmphai_enabled")
    private String pmphaiEnabled;

    @TableField("pmphai_base_url")
    private String pmphaiBaseUrl;

    @TableField("pmphai_app_key_encrypted")
    private String pmphaiAppKeyEncrypted;

    @TableField("pmphai_app_secret_encrypted")
    private String pmphaiAppSecretEncrypted;

    @TableField("reviewer_enabled")
    private String reviewerEnabled;

    @TableField("reviewer_base_url")
    private String reviewerBaseUrl;

    @TableField("reviewer_api_key_encrypted")
    private String reviewerApiKeyEncrypted;

    @TableField("reviewer_model")
    private String reviewerModel;

    @TableField("reviewer_check_examination_enabled")
    private String reviewerCheckExaminationEnabled;

    @TableField("features_json")
    private String featuresJson;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;

    @TableField("sd_status")
    private String sdStatus;
}
