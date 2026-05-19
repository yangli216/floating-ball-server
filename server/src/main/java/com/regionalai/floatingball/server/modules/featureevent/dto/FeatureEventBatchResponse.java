package com.regionalai.floatingball.server.modules.featureevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeatureEventBatchResponse {
    private int accepted;
    private int skipped;
}
