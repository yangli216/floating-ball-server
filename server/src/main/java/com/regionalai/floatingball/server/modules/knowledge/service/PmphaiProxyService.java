package com.regionalai.floatingball.server.modules.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.service.ConfigService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiClipRequest;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiListRequest;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiPageUrlRequest;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiSearchRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PmphaiProxyService {

    private static final String DEFAULT_PMPHAI_BASE_URL = "https://pmphai.example.com";

    private final ConfigService configService;
    private final WebClient webClient;
    private final ConcurrentMap<String, TokenHolder> tokenCache = new ConcurrentHashMap<String, TokenHolder>();

    public PmphaiProxyService(ConfigService configService, WebClient webClient) {
        this.configService = configService;
        this.webClient = webClient;
    }

    public JsonNode search(AiDevice device, PmphaiSearchRequest request) {
        PmphaiConfig config = resolvePmphaiConfig(device);
        JsonNode response = webClient.post()
            .uri(buildJsonApiUrl(config, getAccessToken(config), "aiKnowledge"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(buildSearchBody(request))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        return extractData(response, "PMPHAI 搜索响应为空");
    }

    public JsonNode clip(AiDevice device, PmphaiClipRequest request) {
        PmphaiConfig config = resolvePmphaiConfig(device);
        JsonNode response = webClient.post()
            .uri(buildJsonApiUrl(config, getAccessToken(config), "aiKnowledgeClip"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(singleValueBody("id", request.getId()))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        return extractData(response, "PMPHAI 详情响应为空");
    }

    public JsonNode list(AiDevice device, PmphaiListRequest request) {
        PmphaiConfig config = resolvePmphaiConfig(device);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        form.add("method", StringUtils.hasText(request.getMethod()) ? request.getMethod() : "list");
        form.add("pageSize", String.valueOf(request.getPageSize() == null ? 10 : request.getPageSize()));
        form.add("page", String.valueOf(request.getPage() == null ? 1 : request.getPage()));
        addIfPresent(form, "key", request.getKey());
        addIfPresent(form, "kgBaseId", request.getKgBaseId());
        addIfPresent(form, "kgBaseName", request.getKgBaseName());
        addIfPresent(form, "tagId", request.getTagId());
        addIfPresent(form, "tagName", request.getTagName());
        addIfPresent(form, "sortField", request.getSortField());
        addIfPresent(form, "sortRule", request.getSortRule());

        JsonNode response = webClient.post()
            .uri(buildStandardApiUrl(config, getAccessToken(config)))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        return extractData(response, "PMPHAI 列表响应为空");
    }

    public String generatePageUrl(AiDevice device, PmphaiPageUrlRequest request) {
        PmphaiConfig config = resolvePmphaiConfig(device);
        long timestamp = System.currentTimeMillis();

        Map<String, String> redirectParams = new LinkedHashMap<String, String>();
        redirectParams.put("pageName", request.getPageName());
        putIfPresent(redirectParams, "kgBaseId", request.getKgBaseId());
        putIfPresent(redirectParams, "id", request.getId());
        putIfPresent(redirectParams, "kgFields", request.getKgFields());
        putIfPresent(redirectParams, "contentId", request.getContentId());
        putIfPresent(redirectParams, "muluId", request.getMuluId());
        putIfPresent(redirectParams, "catalogueId", request.getCatalogueId());

        String redirectUrl = config.getBaseUrl() + "/gateway/cloud/pageapi/rest?" + toQueryString(redirectParams);
        String originUrl = StringUtils.hasText(request.getOriginUrl()) ? request.getOriginUrl() : "https://www.pmphai.com";
        String encodedRedirectUrl = encodeUrlValue(redirectUrl);
        String encodedOriginUrl = encodeUrlValue(originUrl);

        Map<String, String> signParams = new TreeMap<String, String>();
        signParams.put("app_key", config.getAppKey());
        signParams.put("grant_type", "page_token");
        signParams.put("origin_url", encodedOriginUrl);
        signParams.put("redirect_url", encodedRedirectUrl);
        signParams.put("timestamp", String.valueOf(timestamp));

        return config.getBaseUrl()
            + "/aip/oauth/authorize?app_key=" + config.getAppKey()
            + "&grant_type=page_token"
            + "&timestamp=" + timestamp
            + "&sign=" + generateSign(signParams, config.getAppSecret(), config.getAppKey())
            + "&redirect_url=" + encodedRedirectUrl
            + "&origin_url=" + encodedOriginUrl;
    }

    public JsonNode knowledgeBases(AiDevice device, String kgBaseId) {
        PmphaiConfig config = resolvePmphaiConfig(device);
        JsonNode response = webClient.post()
            .uri(buildStandardApiUrl(config, getAccessToken(config)) + "&method=kgbases")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(StringUtils.hasText(kgBaseId) ? singleValueBody("kgBaseId", kgBaseId) : new LinkedHashMap<String, String>())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        return extractData(response, "PMPHAI 知识库列表响应为空");
    }

    public JsonNode categories(AiDevice device, String kgBaseId) {
        if (!StringUtils.hasText(kgBaseId)) {
            throw new BusinessException("kgBaseId 不能为空");
        }
        PmphaiListRequest request = new PmphaiListRequest();
        request.setMethod("tag");
        request.setKgBaseId(kgBaseId);
        return list(device, request);
    }

    private PmphaiConfig resolvePmphaiConfig(AiDevice device) {
        ResolvedAiConfig resolved = configService.resolveByDevice(device);
        if (!Boolean.TRUE.equals(resolved.getPmphaiEnabled())) {
            throw new BusinessException("当前设备未启用 PMPHAI 知识库");
        }
        if (!StringUtils.hasText(resolved.getPmphaiAppKey()) || !StringUtils.hasText(resolved.getPmphaiAppSecret())) {
            throw new BusinessException("当前设备未配置 PMPHAI 凭据");
        }
        String baseUrl = StringUtils.hasText(resolved.getPmphaiBaseUrl()) ? resolved.getPmphaiBaseUrl() : DEFAULT_PMPHAI_BASE_URL;
        return new PmphaiConfig(baseUrl.replaceAll("/+$", ""), resolved.getPmphaiAppKey(), resolved.getPmphaiAppSecret());
    }

    private String getAccessToken(PmphaiConfig config) {
        String cacheKey = config.getBaseUrl() + "|" + config.getAppKey();
        TokenHolder cached = tokenCache.get(cacheKey);
        if (cached != null && cached.getExpiresAt() > System.currentTimeMillis()) {
            return cached.getAccessToken();
        }

        long timestamp = System.currentTimeMillis();
        Map<String, String> params = new TreeMap<String, String>();
        params.put("app_key", config.getAppKey());
        params.put("grant_type", "access_token");
        params.put("timestamp", String.valueOf(timestamp));
        params.put("sign", generateSign(params, config.getAppSecret(), config.getAppKey()));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            form.add(entry.getKey(), entry.getValue());
        }

        JsonNode response = webClient.post()
            .uri(config.getBaseUrl() + "/oauth2/access_token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        JsonNode data = response == null ? null : response.path("data");
        String accessToken = data == null ? null : data.path("accessToken").asText(null);
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException("获取 PMPHAI token 失败");
        }
        long expiresIn = data.path("expiresIn").asLong(3600L);
        long expiresAt = System.currentTimeMillis() + Math.max(60L, expiresIn - 300L) * 1000L;
        tokenCache.put(cacheKey, new TokenHolder(accessToken, expiresAt));
        return accessToken;
    }

    private Map<String, Object> buildSearchBody(PmphaiSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", request.getQuery());
        body.put("type", request.getType() == null ? Integer.valueOf(1) : request.getType());
        body.put("limit", request.getLimit() == null ? Integer.valueOf(5) : request.getLimit());
        if (request.getScore() != null && request.getScore().doubleValue() > 0D) {
            body.put("score", request.getScore());
        }
        if (request.getEnableAbstract() != null) {
            body.put("enableAbstract", request.getEnableAbstract());
        }
        return body;
    }

    private Map<String, String> singleValueBody(String key, String value) {
        Map<String, String> body = new LinkedHashMap<String, String>();
        body.put(key, value);
        return body;
    }

    private JsonNode extractData(JsonNode response, String emptyMessage) {
        if (response == null) {
            throw new BusinessException(emptyMessage);
        }
        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new BusinessException(emptyMessage);
        }
        return data;
    }

    private String buildJsonApiUrl(PmphaiConfig config, String token, String method) {
        return config.getBaseUrl() + "/gateway/cloud/cloudapi/rest/json?token=" + token + "&method=" + method;
    }

    private String buildStandardApiUrl(PmphaiConfig config, String token) {
        return config.getBaseUrl() + "/gateway/cloud/cloudapi/rest?token=" + token;
    }

    private void addIfPresent(MultiValueMap<String, String> form, String key, String value) {
        if (StringUtils.hasText(value)) {
            form.add(key, value);
        }
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    private String toQueryString(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }

    private String encodeUrlValue(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
                .replace("%7E", "~");
        } catch (Exception ex) {
            throw new BusinessException("PMPHAI URL 编码失败");
        }
    }

    private String generateSign(Map<String, String> params, String appSecret, String appKey) {
        try {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    builder.append("&");
                }
                builder.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            builder.append(appSecret).append(appKey);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : bytes) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new BusinessException("生成 PMPHAI 签名失败");
        }
    }

    private static class PmphaiConfig {

        private final String baseUrl;
        private final String appKey;
        private final String appSecret;

        private PmphaiConfig(String baseUrl, String appKey, String appSecret) {
            this.baseUrl = baseUrl;
            this.appKey = appKey;
            this.appSecret = appSecret;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getAppKey() {
            return appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }
    }

    private static class TokenHolder {

        private final String accessToken;
        private final long expiresAt;

        private TokenHolder(String accessToken, long expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}
