package com.regionalai.floatingball.server.modules.config.dto;

import lombok.Data;

@Data
public class AiConfigSaveRequest {

    private String idConfig;
    private String cdConfig;
    private String naConfig;
    private String provider;
    private String apiBaseUrl;
    private String apiKey;
    private String modelName;
    private String fastModelName;
    private Boolean enableThinking;
    private String audioApiKey;
    private String audioBaseUrl;
    private String audioModel;
    private String speechProvider;
    private String speechRealtimeUrl;
    private String speechModel;
    private Boolean knowledgeBaseEnabled;
    private String knowledgeBaseBaseUrl;
    private Boolean pmphaiEnabled;
    private String pmphaiBaseUrl;
    private String pmphaiAppKey;
    private String pmphaiAppSecret;
    private Boolean reviewerEnabled;
    private String reviewerBaseUrl;
    private String reviewerApiKey;
    private String reviewerModel;
    private Boolean reviewerCheckExaminationEnabled;
    private String featuresJson;
    private String idOrg;
    private String idRegion;
    private String sdStatus;
}
