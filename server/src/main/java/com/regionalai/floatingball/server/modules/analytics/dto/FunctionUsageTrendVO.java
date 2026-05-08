package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;

import java.util.List;

@Data
public class FunctionUsageTrendVO {
    private List<String> modules;
    private List<String> days;
    private List<List<Long>> values;
}
