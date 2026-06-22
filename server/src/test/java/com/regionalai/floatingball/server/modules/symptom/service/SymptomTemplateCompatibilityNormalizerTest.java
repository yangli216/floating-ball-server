package com.regionalai.floatingball.server.modules.symptom.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SymptomTemplateCompatibilityNormalizerTest {

    @Test
    void shouldPromoteSharedSectionApplicablePopulationToSymptomLevel() {
        Map<String, Object> section = new LinkedHashMap<String, Object>();
        section.put("id", "section-1");
        section.put("title", "症状属性问诊");
        section.put("applicablePopulation", Collections.<String, Object>singletonMap("genders", Collections.singletonList("2")));
        section.put("fields", Collections.emptyList());

        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("sections", Collections.singletonList(section));

        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("key", "nipple-pain");
        template.put("name", "乳头痛");
        template.put("applicablePopulation", Collections.<String, Object>emptyMap());
        template.put("config", config);

        Map<String, Object> normalized = SymptomTemplateCompatibilityNormalizer.normalizeTemplateMap(template);

        @SuppressWarnings("unchecked")
        Map<String, Object> applicablePopulation = (Map<String, Object>) normalized.get("applicablePopulation");
        assertEquals(Collections.singletonList("2"), applicablePopulation.get("genders"));
        @SuppressWarnings("unchecked")
        Map<String, Object> normalizedConfig = (Map<String, Object>) normalized.get("config");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) normalizedConfig.get("sections");
        assertNull(sections.get(0).get("applicablePopulation"));
    }

    @Test
    void shouldKeepSymptomLevelPopulationWhenAlreadyDefined() {
        Map<String, Object> section = new LinkedHashMap<String, Object>();
        section.put("id", "section-1");
        section.put("title", "症状属性问诊");
        section.put("applicablePopulation", Collections.<String, Object>singletonMap("genders", Collections.singletonList("2")));
        section.put("fields", Collections.emptyList());

        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("sections", Collections.singletonList(section));

        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("key", "glans-redness");
        template.put("name", "龟头红");
        template.put("applicablePopulation", Collections.<String, Object>singletonMap("genders", Collections.singletonList("1")));
        template.put("config", config);

        Map<String, Object> normalized = SymptomTemplateCompatibilityNormalizer.normalizeTemplateMap(template);

        @SuppressWarnings("unchecked")
        Map<String, Object> applicablePopulation = (Map<String, Object>) normalized.get("applicablePopulation");
        assertEquals(Collections.singletonList("1"), applicablePopulation.get("genders"));
        @SuppressWarnings("unchecked")
        Map<String, Object> normalizedConfig = (Map<String, Object>) normalized.get("config");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) normalizedConfig.get("sections");
        assertEquals(Collections.singletonList("2"), castApplicableGenders(sections.get(0)));
    }

    @Test
    void shouldKeepSymptomVisibleWhenSectionsHaveDifferentConditions() {
        Map<String, Object> section1 = new LinkedHashMap<String, Object>();
        section1.put("id", "section-1");
        section1.put("applicablePopulation", Collections.<String, Object>singletonMap("genders", Collections.singletonList("1")));
        section1.put("fields", Collections.emptyList());

        Map<String, Object> section2 = new LinkedHashMap<String, Object>();
        section2.put("id", "section-2");
        section2.put("applicablePopulation", Collections.<String, Object>singletonMap("genders", Collections.singletonList("2")));
        section2.put("fields", Collections.emptyList());

        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("sections", Arrays.asList(section1, section2));

        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("key", "mixed");
        template.put("name", "混合条件症状");
        template.put("applicablePopulation", Collections.<String, Object>emptyMap());
        template.put("config", config);

        Map<String, Object> normalized = SymptomTemplateCompatibilityNormalizer.normalizeTemplateMap(template);

        @SuppressWarnings("unchecked")
        Map<String, Object> applicablePopulation = (Map<String, Object>) normalized.get("applicablePopulation");
        assertEquals(Collections.emptyMap(), applicablePopulation);
    }

    @Test
    void shouldRemoveDuplicateSectionPopulationWhenSymptomLevelAlreadyDefined() {
        Map<String, Object> section = new LinkedHashMap<String, Object>();
        section.put("id", "section-1");
        section.put("title", "症状属性问诊");
        section.put("applicablePopulation", Collections.<String, Object>singletonMap("genders", Collections.singletonList("2")));
        section.put("fields", Collections.emptyList());

        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("sections", Collections.singletonList(section));

        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("key", "nipple-pain");
        template.put("name", "乳头痛");
        template.put("applicablePopulation", Collections.<String, Object>singletonMap("genders", Collections.singletonList("2")));
        template.put("config", config);

        Map<String, Object> normalized = SymptomTemplateCompatibilityNormalizer.normalizeTemplateMap(template);

        @SuppressWarnings("unchecked")
        Map<String, Object> normalizedConfig = (Map<String, Object>) normalized.get("config");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) normalizedConfig.get("sections");
        assertNull(sections.get(0).get("applicablePopulation"));
    }

    @SuppressWarnings("unchecked")
    private List<String> castApplicableGenders(Map<String, Object> section) {
        Map<String, Object> applicablePopulation = (Map<String, Object>) section.get("applicablePopulation");
        return (List<String>) applicablePopulation.get("genders");
    }
}
