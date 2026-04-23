package com.regionalai.floatingball.server.modules.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserView {

    private String idUser;
    private String cdUser;
    private String naUser;
    private String idOrg;
    private String naOrg;
    private List<String> roleIds;
    private List<String> roleNames;
    private String sdStatus;
    private LocalDateTime updateTime;
}
