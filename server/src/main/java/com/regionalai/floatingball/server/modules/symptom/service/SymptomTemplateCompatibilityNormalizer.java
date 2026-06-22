package com.regionalai.floatingball.server.modules.symptom.service;

import com.regionalai.floatingball.server.modules.symptom.dto.SymptomTemplateVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SymptomTemplateCompatibilityNormalizer {

    private static final String KEY_APPLICABLE_POPULATION = "applicablePopulation";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_SECTIONS = "sections";

    private SymptomTemplateCompatibilityNormalizer() {
    }

    public static List<Object> normalizeTemplates(List<Object> templates) {
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> normalized = new ArrayList<Object>(templates.size());
        for (Object template : templates) {
            if (template instanceof Map) {
                normalized.add(normalizeTemplateMap(castMap(template)));
            } else {
                normalized.add(template);
            }
        }
        return normalized;
    }

    public static Map<String, Object> normalizeTemplateMap(Map<String, Object> template) {
        if (template == null || template.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> normalized = new LinkedHashMap<String, Object>(template);
        Map<String, Object> applicablePopulation = castMap(normalized.get(KEY_APPLICABLE_POPULATION));
        Map<String, Object> config = castMap(normalized.get(KEY_CONFIG));
        NormalizedTemplatePayload payload = normalizePayload(applicablePopulation, config);
        normalized.put(KEY_APPLICABLE_POPULATION, payload.getApplicablePopulation());
        normalized.put(KEY_CONFIG, payload.getConfig());
        return normalized;
    }

    public static void normalizeTemplateVO(SymptomTemplateVO template) {
        if (template == null) {
            return;
        }
        NormalizedTemplatePayload payload = normalizePayload(template.getApplicablePopulation(), template.getConfig());
        template.setApplicablePopulation(payload.getApplicablePopulation());
        template.setConfig(payload.getConfig());
    }

    public static NormalizedTemplatePayload normalizePayload(Map<String, Object> applicablePopulation,
                                                             Map<String, Object> config) {
        Map<String, Object> normalizedApplicablePopulation = copyMap(applicablePopulation);
        Map<String, Object> normalizedConfig = copyMap(config);
        if (isMeaningfulApplicablePopulation(normalizedApplicablePopulation)) {
            normalizedConfig = removeSharedSectionApplicablePopulation(normalizedConfig, normalizedApplicablePopulation);
            return new NormalizedTemplatePayload(normalizedApplicablePopulation, normalizedConfig);
        }
        List<Map<String, Object>> sections = extractSectionMaps(normalizedConfig);
        if (sections.isEmpty()) {
            return new NormalizedTemplatePayload(normalizedApplicablePopulation, normalizedConfig);
        }

        Map<String, Object> sharedApplicablePopulation = null;
        for (Map<String, Object> section : sections) {
            Map<String, Object> sectionApplicablePopulation = sanitizeApplicablePopulation(
                castMap(section.get(KEY_APPLICABLE_POPULATION))
            );
            if (!isMeaningfulApplicablePopulation(sectionApplicablePopulation)) {
                return new NormalizedTemplatePayload(normalizedApplicablePopulation, normalizedConfig);
            }
            if (sharedApplicablePopulation == null) {
                sharedApplicablePopulation = sectionApplicablePopulation;
                continue;
            }
            if (!Objects.equals(sharedApplicablePopulation, sectionApplicablePopulation)) {
                return new NormalizedTemplatePayload(normalizedApplicablePopulation, normalizedConfig);
            }
        }
        if (sharedApplicablePopulation != null) {
            normalizedApplicablePopulation = sharedApplicablePopulation;
            normalizedConfig = removeSharedSectionApplicablePopulation(normalizedConfig, sharedApplicablePopulation);
        }
        return new NormalizedTemplatePayload(normalizedApplicablePopulation, normalizedConfig);
    }

    private static Map<String, Object> removeSharedSectionApplicablePopulation(Map<String, Object> config,
                                                                               Map<String, Object> sharedApplicablePopulation) {
        if (config == null || config.isEmpty()) {
            return Collections.emptyMap();
        }
        Object rawSections = config.get(KEY_SECTIONS);
        if (!(rawSections instanceof List)) {
            return config;
        }
        List<?> sectionList = (List<?>) rawSections;
        List<Object> normalizedSections = new ArrayList<Object>(sectionList.size());
        for (Object rawSection : sectionList) {
            if (!(rawSection instanceof Map)) {
                normalizedSections.add(rawSection);
                continue;
            }
            Map<String, Object> section = castMap(rawSection);
            Map<String, Object> sectionApplicablePopulation = sanitizeApplicablePopulation(
                castMap(section.get(KEY_APPLICABLE_POPULATION))
            );
            if (Objects.equals(sharedApplicablePopulation, sectionApplicablePopulation)) {
                section.remove(KEY_APPLICABLE_POPULATION);
            }
            normalizedSections.add(section);
        }
        Map<String, Object> normalizedConfig = new LinkedHashMap<String, Object>(config);
        normalizedConfig.put(KEY_SECTIONS, normalizedSections);
        return normalizedConfig;
    }

    private static List<Map<String, Object>> extractSectionMaps(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return Collections.emptyList();
        }
        Object rawSections = config.get(KEY_SECTIONS);
        if (!(rawSections instanceof List)) {
            return Collections.emptyList();
        }
        List<?> sectionList = (List<?>) rawSections;
        if (sectionList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> sections = new ArrayList<Map<String, Object>>(sectionList.size());
        for (Object rawSection : sectionList) {
            if (!(rawSection instanceof Map)) {
                return Collections.emptyList();
            }
            sections.add(castMap(rawSection));
        }
        return sections;
    }

    private static boolean isMeaningfulApplicablePopulation(Map<String, Object> applicablePopulation) {
        if (applicablePopulation == null || applicablePopulation.isEmpty()) {
            return false;
        }
        Object genders = applicablePopulation.get("genders");
        if (genders instanceof List && !((List<?>) genders).isEmpty()) {
            return true;
        }
        Object ageRange = applicablePopulation.get("ageRange");
        if (ageRange instanceof Map && !((Map<?, ?>) ageRange).isEmpty()) {
            return true;
        }
        return false;
    }

    private static Map<String, Object> sanitizeApplicablePopulation(Map<String, Object> applicablePopulation) {
        if (applicablePopulation == null || applicablePopulation.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> sanitized = new LinkedHashMap<String, Object>();
        Object genders = applicablePopulation.get("genders");
        if (genders instanceof List && !((List<?>) genders).isEmpty()) {
            sanitized.put("genders", new ArrayList<Object>((List<?>) genders));
        }
        Object ageRange = applicablePopulation.get("ageRange");
        if (ageRange instanceof Map && !((Map<?, ?>) ageRange).isEmpty()) {
            sanitized.put("ageRange", new LinkedHashMap<Object, Object>((Map<?, ?>) ageRange));
        }
        return sanitized;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object raw) {
        if (!(raw instanceof Map)) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<String, Object>((Map<String, Object>) raw);
    }

    private static Map<String, Object> copyMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<String, Object>(value);
    }

    public static final class NormalizedTemplatePayload {

        private final Map<String, Object> applicablePopulation;
        private final Map<String, Object> config;

        private NormalizedTemplatePayload(Map<String, Object> applicablePopulation, Map<String, Object> config) {
            this.applicablePopulation = applicablePopulation;
            this.config = config;
        }

        public Map<String, Object> getApplicablePopulation() {
            return applicablePopulation;
        }

        public Map<String, Object> getConfig() {
            return config;
        }
    }
}
