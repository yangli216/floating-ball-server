package com.regionalai.floatingball.server.modules.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserSaveRequest {

    private String cdUser;
    private String naUser;
    private String password;
    private String idOrg;
    private List<String> roleIds;
    private String sdStatus;
}
