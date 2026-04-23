package com.regionalai.floatingball.server.modules.knowledge.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PmphaiClipRequest {

    @NotBlank(message = "id 不能为空")
    private String id;
}
