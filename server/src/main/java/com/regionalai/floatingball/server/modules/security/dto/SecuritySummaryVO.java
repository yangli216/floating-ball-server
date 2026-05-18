package com.regionalai.floatingball.server.modules.security.dto;

import lombok.Data;

@Data
public class SecuritySummaryVO {

    private long totalRejections;
    private long recent24h;
    private long recent1h;
    private long authRejections;
    private long sigRejections;
    private long wsRejections;
    private long versionRejections;
    private String totalGrowth;
    private String authGrowth;
    private String sigGrowth;
}
