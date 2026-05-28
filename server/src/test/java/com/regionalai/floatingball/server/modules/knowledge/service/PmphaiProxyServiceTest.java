package com.regionalai.floatingball.server.modules.knowledge.service;

import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.outbound.OutboundSecurityService;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.service.ConfigService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiPageUrlRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PmphaiProxyServiceTest {

    @Test
    void generatePageUrlUsesServerManagedPmphaiConfig() {
        ConfigService configService = mock(ConfigService.class);
        PmphaiProxyService service = new PmphaiProxyService(configService, WebClient.builder().build(), mock(OutboundSecurityService.class));

        ResolvedAiConfig resolved = new ResolvedAiConfig();
        resolved.setPmphaiEnabled(Boolean.TRUE);
        resolved.setPmphaiBaseUrl("https://pmphai.example.com");
        resolved.setPmphaiAppKey("pm-key");
        resolved.setPmphaiAppSecret("pm-secret");
        when(configService.resolveByDevice(any(AiDevice.class))).thenReturn(resolved);

        PmphaiPageUrlRequest request = new PmphaiPageUrlRequest();
        request.setPageName("detail");
        request.setId("doc-1");
        request.setKgFields("适用性别,用法用量");
        request.setContentId("chapter-1");
        request.setMuluId("mulu-1");
        request.setCatalogueId("catalogue-1");
        request.setOriginUrl("https://client.example/workbench");

        String url = service.generatePageUrl(new AiDevice(), request);

        assertThat(url).startsWith("https://pmphai.example.com/aip/oauth/authorize?app_key=pm-key");
        assertThat(url).contains("grant_type=page_token");
        assertThat(url).contains("redirect_url=https%3A%2F%2Fpmphai.example.com%2Fgateway%2Fcloud%2Fpageapi%2Frest%3FpageName%3Ddetail%26id%3Ddoc-1%26kgFields%3D%E9%80%82%E7%94%A8%E6%80%A7%E5%88%AB%2C%E7%94%A8%E6%B3%95%E7%94%A8%E9%87%8F%26contentId%3Dchapter-1%26muluId%3Dmulu-1%26catalogueId%3Dcatalogue-1");
        assertThat(url).contains("origin_url=https%3A%2F%2Fclient.example%2Fworkbench");
        assertThat(url).contains("&sign=");
    }

    @Test
    void generatePageUrlRejectsDisabledPmphaiConfig() {
        ConfigService configService = mock(ConfigService.class);
        PmphaiProxyService service = new PmphaiProxyService(configService, WebClient.builder().build(), mock(OutboundSecurityService.class));

        ResolvedAiConfig resolved = new ResolvedAiConfig();
        resolved.setPmphaiEnabled(Boolean.FALSE);
        when(configService.resolveByDevice(any(AiDevice.class))).thenReturn(resolved);

        PmphaiPageUrlRequest request = new PmphaiPageUrlRequest();
        request.setPageName("home");

        assertThatThrownBy(() -> service.generatePageUrl(new AiDevice(), request))
            .isInstanceOf(BusinessException.class)
            .hasMessage("当前设备未启用 PMPHAI 知识库");
    }
}
