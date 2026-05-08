package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegionDistributionItemVO extends DistributionItemVO {
    private String percentage;
}
