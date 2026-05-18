package com.regionalai.floatingball.server.modules.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class SecurityDistributionVO {

    private List<DistributionItem> byType;
    private List<DistributionItem> byIp;
    private List<DistributionItem> byPath;
    private List<DistributionItem> byDevice;
    private Long totalRejections;

    @Data
    public static class DistributionItem {
        private String name;
        private Long value;
        private String percentage;

        public DistributionItem() {}

        public DistributionItem(String name, Long value, String percentage) {
            this.name = name;
            this.value = value;
            this.percentage = percentage;
        }
    }
}
