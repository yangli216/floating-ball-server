package com.regionalai.floatingball.server.modules.lisresult.dto;

import lombok.Data;

@Data
public class LisReportItemRequest {

    private String cdResult;
    private String naResult;
    private String testResult;
    private String resultQualitative;
    private String referenceRange;
    private String referenceLow;
    private String referenceHigh;
    private String resultUnit;
    private String resultHint;
}
