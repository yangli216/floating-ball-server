package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationPreferenceRankRequest {
    private String recommendationType;
    private String scene;
    private String doctorId;
    private String deptId;
    private List<Candidate> candidates;

    @Data
    public static class Candidate {
        private String itemKey;
        private String itemId;
        private String itemCode;
        private String itemName;
        private Integer originalRank;
    }
}
