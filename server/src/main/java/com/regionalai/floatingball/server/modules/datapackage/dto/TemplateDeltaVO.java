package com.regionalai.floatingball.server.modules.datapackage.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class TemplateDeltaVO {

    private String version;
    private List<Object> western = Collections.emptyList();
    private List<Object> tcm = Collections.emptyList();
}
