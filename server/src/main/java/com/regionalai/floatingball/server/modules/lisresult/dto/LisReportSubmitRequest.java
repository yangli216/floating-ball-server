package com.regionalai.floatingball.server.modules.lisresult.dto;

import lombok.Data;

import java.util.List;

@Data
public class LisReportSubmitRequest {

    private String reportDoctor;
    private String auditDoctor;
    private String instrumentCode;
    private String instrumentName;
    private List<LisReportItemRequest> items;
}
