package com.regionalai.floatingball.server.modules.audit.service;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogDisplayCatalogTest {

    private final AuditLogDisplayCatalog catalog = new AuditLogDisplayCatalog();

    @Test
    void shouldResolveHighFrequencyActionLabels() {
        assertEquals("实时语音识别链路", catalog.resolveActionLabel("speech_realtime"));
        assertEquals("语音转写链路", catalog.resolveActionLabel("speech_transcribe"));
        assertEquals("区域化运行时初始化完成", catalog.resolveActionLabel("regional_runtime_initialized"));
        assertEquals("终端心跳", catalog.resolveActionLabel("heartbeat"));
        assertEquals("接收用药引用回执", catalog.resolveActionLabel("reference_feedback:medicine"));
        assertEquals("发起诊断引用", catalog.resolveActionLabel("request_reference:diagnosis"));
    }

    @Test
    void shouldResolveAnalyticsSourceModulesAndStructuredAliases() {
        assertEquals("鉴别排查", catalog.resolveSourceModuleLabel("consultation_checklist"));
        assertEquals("中医诊断审查", catalog.resolveSourceModuleLabel("tcm_diagnosis_reviewer"));

        Collection<String> actionCodes = catalog.lookupActionCodes("诊断引用");
        assertTrue(actionCodes.contains("request_reference:diagnosis"));
        assertTrue(actionCodes.contains("reference_feedback:diagnosis"));
    }
}