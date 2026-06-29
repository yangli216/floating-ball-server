package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RecommendationPreferenceRankResponse {
    private boolean enabled;
    private String recommendationType;
    private List<Item> items = new ArrayList<Item>();

    public RecommendationPreferenceRankResponse(boolean enabled, String recommendationType) {
        this.enabled = enabled;
        this.recommendationType = recommendationType;
    }

    @Data
    @NoArgsConstructor
    public static class Item {
        private String itemKey;
        private double preferenceScore;
        private double boost;
        private int sampleCount;
        private String scope;
        private String reason;

        public Item(String itemKey, double preferenceScore, double boost, int sampleCount, String scope, String reason) {
            this.itemKey = itemKey;
            this.preferenceScore = preferenceScore;
            this.boost = boost;
            this.sampleCount = sampleCount;
            this.scope = scope;
            this.reason = reason;
        }
    }
}
