package com.regionalai.floatingball.server.modules.emrtemplate.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class InpatientEmrTemplateCacheVO {

    private String id;

    private String templateHash;

    private String templateName;

    private String htmlContent;

    private List<Map<String, Object>> fields = Collections.emptyList();

    private Integer fieldCount;

    private String sdStatus;

    private Boolean cacheHit;

    private Long createdAt;

    private Long updatedAt;
}
