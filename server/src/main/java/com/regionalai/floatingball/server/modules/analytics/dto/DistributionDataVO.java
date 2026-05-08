package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;

import java.util.List;

@Data
public class DistributionDataVO {
    private List<DistributionItemVO> orgDistribution;
    private List<RegionDistributionItemVO> regionDistribution;
    private Long totalService;
}
