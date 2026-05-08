package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;

@Data
public class FunctionUsageItemVO {
    private String moduleName;
    private long callCount;
    private long doctorCount;
    private long avgPerDoctor;
    private String growthRate;
}
