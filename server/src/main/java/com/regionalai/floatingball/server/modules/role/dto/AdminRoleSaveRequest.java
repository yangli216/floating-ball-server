package com.regionalai.floatingball.server.modules.role.dto;

import lombok.Data;

@Data
public class AdminRoleSaveRequest {

    private String cdRole;
    private String naRole;
    private String desRole;
    private String sdStatus;
}
