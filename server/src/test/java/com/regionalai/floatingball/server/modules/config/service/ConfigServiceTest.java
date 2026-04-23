package com.regionalai.floatingball.server.modules.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.util.AesUtils;
import com.regionalai.floatingball.server.common.util.MaskingUtils;
import com.regionalai.floatingball.server.modules.config.dto.AiConfigSaveRequest;
import com.regionalai.floatingball.server.modules.config.dto.BootstrapVO;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.entity.AiConfig;
import com.regionalai.floatingball.server.modules.config.mapper.AiConfigMapper;
import com.regionalai.floatingball.server.modules.datapackage.service.DataPackageService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.prompt.service.PromptService;
import com.regionalai.floatingball.server.modules.symptom.service.SymptomTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    private static final String AES_KEY = "1234567890abcdef";

    @Mock
    private AiConfigMapper aiConfigMapper;

    @Mock
    private PromptService promptService;

    @Mock
    private DataPackageService dataPackageService;

    @Mock
    private SymptomTemplateService symptomTemplateService;

    private AesUtils aesUtils;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        aesUtils = new AesUtils(AES_KEY);
        configService = new ConfigService(
            aiConfigMapper,
            aesUtils,
            new ObjectMapper(),
            promptService,
            dataPackageService,
            symptomTemplateService
        );
    }

    @Test
    void resolveByDeviceShouldPreferOrgScopedConfig() {
        AiConfig global = buildConfig(null, null, "https://global.example.com/", "global-key", "global-model");
        AiConfig region = buildConfig(null, "REG001", "https://region.example.com/", "region-key", "region-model");
        AiConfig org = buildConfig("ORG001", null, "https://org.example.com/", "org-key", "org-model");
        org.setFeaturesJson("{\"voice\":true}");

        when(aiConfigMapper.selectList(any())).thenReturn(Arrays.asList(global, region, org));

        AiDevice device = new AiDevice();
        device.setIdOrg("ORG001");
        device.setIdRegion("REG001");

        ResolvedAiConfig resolved = configService.resolveByDevice(device);

        assertEquals("https://org.example.com", resolved.getBaseUrl());
        assertEquals("org-key", resolved.getApiKey());
        assertEquals("org-model", resolved.getModel());
        assertTrue(resolved.getFeatures().get("voice"));
    }

    @Test
    void buildBootstrapShouldUseResolvedConfigAndVisibleVersions() {
        AiConfig config = buildConfig("ORG001", "REG001", "https://llm.example.com/", "secret-key", "deepseek-chat");
        config.setAudioBaseUrl(null);
        config.setAudioModel("whisper-1");
        config.setSpeechProvider("aliyun");
        config.setSpeechModel("paraformer");
        config.setKnowledgeBaseEnabled("1");
        config.setKnowledgeBaseBaseUrl("https://kb.example.com");
        config.setPmphaiEnabled("0");
        config.setReviewerEnabled("1");
        config.setReviewerModel("reviewer-v1");
        config.setFeaturesJson("{\"voice\":true,\"knowledge\":false}");

        when(aiConfigMapper.selectList(any())).thenReturn(Arrays.asList(config));
        when(promptService.latestVisibleVersion("ORG001", "REG001")).thenReturn("2026.04.20.1");
        when(symptomTemplateService.latestVisibleVersion("ORG001", "REG001")).thenReturn("tpl-2");
        when(dataPackageService.latestVisibleVersion("mapping", "ORG001", "REG001")).thenReturn("map-3");

        AiDevice device = new AiDevice();
        device.setIdOrg("ORG001");
        device.setIdRegion("REG001");

        BootstrapVO bootstrap = configService.buildBootstrap(device);

        assertEquals("https://llm.example.com", bootstrap.getLlm().getBaseUrl());
        assertEquals("https://llm.example.com", bootstrap.getLlm().getAudioBaseUrl());
        assertEquals("whisper-1", bootstrap.getLlm().getAudioModel());
        assertEquals("aliyun", bootstrap.getSpeech().getProvider());
        assertTrue(bootstrap.getKnowledgeBase().getEnabled());
        assertFalse(bootstrap.getPmphai().getEnabled());
        assertTrue(bootstrap.getReviewer().getEnabled());
        assertEquals("reviewer-v1", bootstrap.getReviewer().getModel());
        assertTrue(bootstrap.getFeatures().get("voice"));
        assertEquals("2026.04.20.1", bootstrap.getPromptVersion());
        assertEquals("tpl-2", bootstrap.getTemplateVersion());
        assertEquals("map-3", bootstrap.getDataPackageVersion());
    }

    @Test
    void updateShouldRetainExistingEncryptedApiKeyWhenRequestOmitsIt() {
        AiConfig existing = buildConfig("ORG001", "REG001", "https://llm.example.com", "persisted-key", "deepseek-chat");
        existing.setIdConfig("CFG001");

        when(aiConfigMapper.selectById("CFG001")).thenReturn(existing);

        AiConfigSaveRequest request = new AiConfigSaveRequest();
        request.setCdConfig("default");
        request.setNaConfig("默认配置");
        request.setProvider("openai-compatible");
        request.setApiBaseUrl("https://new.example.com/");
        request.setModelName("deepseek-v2");
        request.setFeaturesJson("{\"voice\":true}");
        request.setIdOrg("ORG001");
        request.setIdRegion("REG001");
        request.setSdStatus("1");

        String encryptedBefore = existing.getApiKeyEncrypted();
        String maskedExpected = MaskingUtils.maskSecret("persisted-key");

        String apiKeyMasked = configService.update("CFG001", request).getApiKeyMasked();

        assertEquals(encryptedBefore, existing.getApiKeyEncrypted());
        assertEquals("https://new.example.com/", existing.getApiBaseUrl());
        assertEquals("deepseek-v2", existing.getModelName());
        verify(aiConfigMapper).updateById(existing);
        assertEquals(maskedExpected, apiKeyMasked);
    }

    @Test
    void saveShouldRejectInvalidFeaturesJson() {
        AiConfigSaveRequest request = new AiConfigSaveRequest();
        request.setNaConfig("配置");
        request.setApiBaseUrl("https://llm.example.com");
        request.setModelName("deepseek-chat");
        request.setFeaturesJson("{bad-json}");

        BusinessException ex = assertThrows(BusinessException.class, () -> configService.save(request));

        assertEquals("featuresJson 必须是合法 JSON", ex.getMessage());
        verify(aiConfigMapper, never()).insert(any(AiConfig.class));
    }

    private AiConfig buildConfig(String idOrg, String idRegion, String apiBaseUrl, String apiKey, String modelName) {
        AiConfig config = new AiConfig();
        config.setIdOrg(idOrg);
        config.setIdRegion(idRegion);
        config.setFgActive("1");
        config.setSdStatus("1");
        config.setCdConfig("default");
        config.setNaConfig("默认配置");
        config.setProvider("openai-compatible");
        config.setApiBaseUrl(apiBaseUrl);
        config.setApiKeyEncrypted(aesUtils.encrypt(apiKey));
        config.setModelName(modelName);
        config.setKnowledgeBaseEnabled("0");
        config.setPmphaiEnabled("0");
        config.setReviewerEnabled("0");
        return config;
    }
}
