package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class FeedbackTimelineItem {
    private String type;
    private LocalDateTime time;
    private String title;
    private String result;
    private Map<String, Object> payload;
}
