package com.regionalai.floatingball.server.modules.knowledge.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PmphaiPageUrlRequest {

    @NotBlank(message = "pageName 不能为空")
    private String pageName;
    private String id;
    private String kgBaseId;
    private String kgFields;
    private String contentId;
    private String muluId;
    private String catalogueId;
    private String originUrl;
}
