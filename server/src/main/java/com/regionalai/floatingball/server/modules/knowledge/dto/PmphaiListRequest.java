package com.regionalai.floatingball.server.modules.knowledge.dto;

import lombok.Data;

@Data
public class PmphaiListRequest {

    private String key;
    private String kgBaseId;
    private String kgBaseName;
    private String tagId;
    private String tagName;
    private Integer pageSize;
    private Integer page;
    private String sortField;
    private String sortRule;
    private String method;
}
