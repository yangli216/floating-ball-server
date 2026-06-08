package com.regionalai.floatingball.server.modules.emrtemplate.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class InpatientEmrTemplateResolveRequest {

    private String templateHash;

    private String templateId;

    private String templateName;

    private String htmlContent;

    private List<Map<String, Object>> fields = Collections.emptyList();
}
