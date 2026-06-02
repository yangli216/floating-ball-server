package com.regionalai.floatingball.server.modules.featureevent.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureEventCatalog {

    public static final String VOICE_CONSULTATION = "voice_consultation";
    public static final String SMART_CONSULTATION = "smart_consultation";
    public static final String REPORT_INTERPRETATION = "report_interpretation";
    public static final String CHAT = "chat";
    public static final String DIAGNOSIS_CHECKLIST = "diagnosis_checklist";
    public static final String DIAGNOSIS_RECOMMENDATION = "diagnosis_recommendation";
    public static final String MEDICATION_RECOMMENDATION = "medication_recommendation";
    public static final String EXAMINATION_RECOMMENDATION = "examination_recommendation";
    public static final String LAB_TEST_RECOMMENDATION = "lab_test_recommendation";
    public static final String PROCEDURE_RECOMMENDATION = "procedure_recommendation";
    public static final String TREATMENT_PLAN_RECOMMENDATION = "treatment_plan_recommendation";
    public static final String KNOWLEDGE_USAGE = "knowledge_usage";

    private static final Map<String, String> FEATURE_NAMES = buildFeatureNames();

    private FeatureEventCatalog() {
    }

    public static String resolveName(String featureCode) {
        if (featureCode == null) {
            return null;
        }
        return FEATURE_NAMES.get(featureCode.trim());
    }

    public static List<String> featureNames() {
        return Arrays.asList(
            "语音问诊",
            "智能问诊",
            "报告单解读",
            "聊天",
            "AI诊断鉴别",
            "AI推荐诊断",
            "AI推荐用药",
            "AI推荐检查",
            "AI推荐检验",
            "AI推荐处置",
            "AI推荐治疗方案",
            "知识库使用"
        );
    }

    private static Map<String, String> buildFeatureNames() {
        Map<String, String> names = new LinkedHashMap<String, String>();
        names.put(VOICE_CONSULTATION, "语音问诊");
        names.put(SMART_CONSULTATION, "智能问诊");
        names.put(REPORT_INTERPRETATION, "报告单解读");
        names.put(CHAT, "聊天");
        names.put(DIAGNOSIS_CHECKLIST, "AI诊断鉴别");
        names.put(DIAGNOSIS_RECOMMENDATION, "AI推荐诊断");
        names.put(MEDICATION_RECOMMENDATION, "AI推荐用药");
        names.put(EXAMINATION_RECOMMENDATION, "AI推荐检查");
        names.put(LAB_TEST_RECOMMENDATION, "AI推荐检验");
        names.put(PROCEDURE_RECOMMENDATION, "AI推荐处置");
        names.put(TREATMENT_PLAN_RECOMMENDATION, "AI推荐治疗方案");
        names.put(KNOWLEDGE_USAGE, "知识库使用");
        return names;
    }
}
