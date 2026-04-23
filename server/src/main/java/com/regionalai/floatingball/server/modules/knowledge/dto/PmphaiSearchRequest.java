package com.regionalai.floatingball.server.modules.knowledge.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PmphaiSearchRequest {

    @NotBlank(message = "query 不能为空")
    private String query;
    private Integer type;
    private Integer limit;
    private Double score;
    private Boolean enableAbstract;
}
