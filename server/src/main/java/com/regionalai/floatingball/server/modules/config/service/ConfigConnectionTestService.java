package com.regionalai.floatingball.server.modules.config.service;

import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.util.AesUtils;
import com.regionalai.floatingball.server.modules.ai.service.AiProxyService;
import com.regionalai.floatingball.server.modules.config.dto.AiConfigSaveRequest;
import com.regionalai.floatingball.server.modules.config.dto.AiConfigTestResult;
import com.regionalai.floatingball.server.modules.config.entity.AiConfig;
import com.regionalai.floatingball.server.modules.config.mapper.AiConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConfigConnectionTestService {

    private final AiConfigMapper aiConfigMapper;
    private final AesUtils aesUtils;
    private final AiProxyService aiProxyService;

    public ConfigConnectionTestService(AiConfigMapper aiConfigMapper,
                                       AesUtils aesUtils,
                                       AiProxyService aiProxyService) {
        this.aiConfigMapper = aiConfigMapper;
        this.aesUtils = aesUtils;
        this.aiProxyService = aiProxyService;
    }

    public AiConfigTestResult testMainModel(AiConfigSaveRequest request) {
        if (request == null) {
            throw new BusinessException("请求体不能为空");
        }
        String baseUrl = request.getApiBaseUrl();
        String modelName = request.getModelName();
        String apiKey = resolveApiKey(request);
        String message = aiProxyService.testChatConnection(baseUrl, apiKey, modelName, Boolean.TRUE.equals(request.getEnableThinking()));
        return new AiConfigTestResult(true, message, baseUrl, modelName);
    }

    private String resolveApiKey(AiConfigSaveRequest request) {
        if (StringUtils.hasText(request.getApiKey())) {
            return request.getApiKey().trim();
        }
        if (!StringUtils.hasText(request.getIdConfig())) {
            throw new BusinessException("请先填写 API Key");
        }
        AiConfig existing = aiConfigMapper.selectById(request.getIdConfig());
        if (existing == null) {
            throw new BusinessException("配置不存在，无法复用原有 API Key");
        }
        String apiKey = aesUtils.decrypt(existing.getApiKeyEncrypted());
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException("当前配置未保存有效 API Key，请重新填写后再测试");
        }
        return apiKey;
    }
}
