package com.regionalai.floatingball.server.modules.security.dto;

import lombok.Data;

@Data
public class SecurityQueryDTO {

    private String dateFrom;
    private String dateTo;
    private String timeRange;
}
