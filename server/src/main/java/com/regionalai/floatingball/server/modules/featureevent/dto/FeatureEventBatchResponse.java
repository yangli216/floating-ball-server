package com.regionalai.floatingball.server.modules.featureevent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class FeatureEventBatchResponse {
    private int accepted;
    private int skipped;
    private int rejected;
    private List<Rejection> rejections = new ArrayList<Rejection>();

    public FeatureEventBatchResponse(int accepted, int skipped) {
        this.accepted = accepted;
        this.skipped = skipped;
    }

    public void addRejection(int index, String eventId, String featureCode, String reason) {
        this.rejected++;
        this.rejections.add(new Rejection(index, eventId, featureCode, reason));
    }

    @Data
    @NoArgsConstructor
    public static class Rejection {
        private int index;
        private String eventId;
        private String featureCode;
        private String reason;

        public Rejection(int index, String eventId, String featureCode, String reason) {
            this.index = index;
            this.eventId = eventId;
            this.featureCode = featureCode;
            this.reason = reason;
        }
    }
}
