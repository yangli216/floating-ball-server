package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.Data;

@Data
public class FeedbackListQuery {
    private long current = 1;
    private long size = 10;
    private Boolean includeHistory;
    private String keyword;
    private Integer score;
    private String sourceModule;
    private String kind;
    private String severity;
    private String doctor;
    private String dept;
    private String org;
    private Boolean hasCorrection;
    private Boolean hasTrace;
    private String dateFrom;
    private String dateTo;
}
