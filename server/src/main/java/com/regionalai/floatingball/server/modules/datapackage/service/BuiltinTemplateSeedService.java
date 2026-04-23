package com.regionalai.floatingball.server.modules.datapackage.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.datapackage.dto.TemplateDeltaVO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BuiltinTemplateSeedService {

    private static final String WESTERN_RESOURCE_PATH = "template-seeds/western-templates.json";
    private static final String TCM_RESOURCE_PATH = "template-seeds/tcm-templates.json";
    private static final TypeReference<List<Object>> LIST_TYPE = new TypeReference<List<Object>>() {
    };

    private final ObjectMapper objectMapper;
    private volatile TemplateSnapshot cachedSnapshot;

    public BuiltinTemplateSeedService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TemplateDeltaVO getDelta(String currentVersion) {
        TemplateSnapshot snapshot = getSnapshot();
        TemplateDeltaVO vo = new TemplateDeltaVO();
        vo.setVersion(snapshot.getVersion());
        if (StringUtils.hasText(currentVersion) && currentVersion.equals(snapshot.getVersion())) {
            return vo;
        }
        vo.setWestern(snapshot.getWestern());
        vo.setTcm(snapshot.getTcm());
        return vo;
    }

    public TemplateDeltaVO getSnapshotDelta() {
        return getDelta(null);
    }

    public String getVersion() {
        return getSnapshot().getVersion();
    }

    private TemplateSnapshot getSnapshot() {
        TemplateSnapshot local = cachedSnapshot;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cachedSnapshot == null) {
                cachedSnapshot = loadSnapshot();
            }
            return cachedSnapshot;
        }
    }

    private TemplateSnapshot loadSnapshot() {
        try {
            byte[] westernBytes = readResourceBytes(WESTERN_RESOURCE_PATH);
            byte[] tcmBytes = readResourceBytes(TCM_RESOURCE_PATH);

            List<Object> western = objectMapper.readValue(westernBytes, LIST_TYPE);

            JsonNode tcmRoot = objectMapper.readTree(tcmBytes);
            JsonNode tcmSymptoms = tcmRoot.get("symptoms");
            if (tcmSymptoms == null || !tcmSymptoms.isArray()) {
                throw new IllegalStateException("内置中医模板缺少 symptoms 数组");
            }
            List<Object> tcm = objectMapper.convertValue(tcmSymptoms, LIST_TYPE);

            String version = "builtin-" + buildContentHash(westernBytes, tcmBytes);
            return new TemplateSnapshot(version, western, tcm);
        } catch (IOException ex) {
            throw new IllegalStateException("加载内置症状模板失败", ex);
        }
    }

    private byte[] readResourceBytes(String resourcePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return StreamUtils.copyToByteArray(inputStream);
        }
    }

    private String buildContentHash(byte[] westernBytes, byte[] tcmBytes) {
        byte[] merged = new byte[westernBytes.length + tcmBytes.length];
        System.arraycopy(westernBytes, 0, merged, 0, westernBytes.length);
        System.arraycopy(tcmBytes, 0, merged, westernBytes.length, tcmBytes.length);
        return DigestUtils.md5DigestAsHex(merged).substring(0, 12);
    }

    private static final class TemplateSnapshot {

        private final String version;
        private final List<Object> western;
        private final List<Object> tcm;

        private TemplateSnapshot(String version, List<Object> western, List<Object> tcm) {
            this.version = version;
            this.western = Collections.unmodifiableList(new ArrayList<Object>(western));
            this.tcm = Collections.unmodifiableList(new ArrayList<Object>(tcm));
        }

        private String getVersion() {
            return version;
        }

        private List<Object> getWestern() {
            return western;
        }

        private List<Object> getTcm() {
            return tcm;
        }
    }
}
