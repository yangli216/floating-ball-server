package com.regionalai.floatingball.server.modules.userlog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationTimelineItem {

    private String eventType;
    private String module;
    private String action;
    private String result;
    private LocalDateTime operationTime;
    private Object details;
}
