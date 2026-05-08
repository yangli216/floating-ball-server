package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;

import java.util.List;

@Data
public class TrendDataVO {

    private List<String> days;
    private List<Long> aiServiceValues;
    private List<Long> consultationValues;
}
