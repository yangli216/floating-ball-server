package com.regionalai.floatingball.server.modules.lisresult.dto;

import lombok.Data;

@Data
public class LisReportSubmitResponse {

    private String idApply;
    private String reportGroupId;
    private int itemCount;

    public LisReportSubmitResponse() {
    }

    public LisReportSubmitResponse(String idApply, String reportGroupId, int itemCount) {
        this.idApply = idApply;
        this.reportGroupId = reportGroupId;
        this.itemCount = itemCount;
    }
}
