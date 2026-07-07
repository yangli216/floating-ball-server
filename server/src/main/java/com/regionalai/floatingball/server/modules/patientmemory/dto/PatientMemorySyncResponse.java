package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class PatientMemorySyncResponse {

    private String memoryId;
    private Long memoryVersion;
    private String nextCursor;
    private int accepted;
    private int skipped;
    private List<RejectedObservation> rejected = new ArrayList<RejectedObservation>();
    private Set<String> changedFactTypes = new LinkedHashSet<String>();
    private PatientMemoryBriefVO brief;

    public void addRejection(int index, String sourceKey, String reason) {
        rejected.add(new RejectedObservation(index, sourceKey, reason));
    }

    @Data
    public static class RejectedObservation {
        private final int index;
        private final String sourceKey;
        private final String reason;
    }
}
