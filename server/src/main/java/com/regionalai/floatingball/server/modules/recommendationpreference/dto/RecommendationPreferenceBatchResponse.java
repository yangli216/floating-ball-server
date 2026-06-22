package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RecommendationPreferenceBatchResponse {
    private int accepted;
    private int skipped;
    private int rejected;
    private List<Rejection> rejections = new ArrayList<Rejection>();

    public void addRejection(int index, String eventId, String recommendationType, String reason) {
        this.rejected++;
        this.rejections.add(new Rejection(index, eventId, recommendationType, reason));
    }

    @Data
    @NoArgsConstructor
    public static class Rejection {
        private int index;
        private String eventId;
        private String recommendationType;
        private String reason;

        public Rejection(int index, String eventId, String recommendationType, String reason) {
            this.index = index;
            this.eventId = eventId;
            this.recommendationType = recommendationType;
            this.reason = reason;
        }
    }
}
