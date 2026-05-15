package com.regionalai.floatingball.server.modules.useractivity.dto;

import lombok.Data;

import java.util.List;

@Data
public class RegionTreeNodeVO {

    private String id;
    private String name;
    private String type;
    private long userCount;
    private List<RegionTreeNodeVO> children;
}
