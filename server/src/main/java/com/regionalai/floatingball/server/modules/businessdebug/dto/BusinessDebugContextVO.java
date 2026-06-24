package com.regionalai.floatingball.server.modules.businessdebug.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BusinessDebugContextVO {

    private BusinessDebugConsultationItem run;
    private Map<String, Object> context;
    private List<BusinessDebugNodeVO> nodes;
}
