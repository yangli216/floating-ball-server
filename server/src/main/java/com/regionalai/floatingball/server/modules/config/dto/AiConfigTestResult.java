package com.regionalai.floatingball.server.modules.config.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiConfigTestResult {

    private boolean success;
    private String message;
    private String baseUrl;
    private String modelName;
}
