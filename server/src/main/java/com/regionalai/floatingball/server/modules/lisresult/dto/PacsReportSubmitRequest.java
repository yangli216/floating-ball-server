package com.regionalai.floatingball.server.modules.lisresult.dto;

import lombok.Data;

@Data
public class PacsReportSubmitRequest {

    private String result;
    private String remark;
    private String clinicalImpression;
    private String negativePositive;
    private String diagnosticImaging;
    private String cdStudy;
    private String idDept;
    private String naDept;
    private String reportDoctor;
    private String auditDoctor;
}
