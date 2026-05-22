package com.regionalai.floatingball.server.modules.useractivity.dto;

import lombok.Data;

@Data
public class UserActivityItemVO {

    private String idDevice;
    private String cdDevice;
    private String naDevice;
    private String idOrg;
    private String naOrg;
    private String idRegion;
    private String naRegion;
    private String idDoctor;
    private String naDoctor;
    private String activeStatus;
    private long consultationCount;
    private long effectiveConsultationCount;
    private String lastActiveTime;
}
