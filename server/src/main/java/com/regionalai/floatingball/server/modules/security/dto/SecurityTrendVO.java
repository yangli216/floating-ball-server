package com.regionalai.floatingball.server.modules.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class SecurityTrendVO {

    private List<String> days;
    private List<Long> totalValues;
    private List<Long> authValues;
    private List<Long> sigValues;
}
