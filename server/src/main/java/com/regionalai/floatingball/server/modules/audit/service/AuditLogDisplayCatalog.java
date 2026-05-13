package com.regionalai.floatingball.server.modules.audit.service;

import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public final class AuditLogDisplayCatalog {

    private final Map<String, String> moduleLabels = new LinkedHashMap<String, String>();
    private final Map<String, String> actionLabels = new LinkedHashMap<String, String>();
    private final Map<String, String> sourceModuleLabels = new LinkedHashMap<String, String>();
    private final Map<String, String> sceneLabels = new LinkedHashMap<String, String>();
    private final Map<String, String> titleLabels = new LinkedHashMap<String, String>();

    private final Map<String, Set<String>> moduleAliases = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Set<String>> actionAliases = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Set<String>> sourceModuleAliases = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Set<String>> sceneAliases = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Set<String>> titleAliases = new LinkedHashMap<String, Set<String>>();

    public AuditLogDisplayCatalog() {
        registerModule("feedback", "反馈弹层");
        registerModule("settings_feedback", "设置页反馈");
        registerModule("llm", "AI 对话代理", "ai", "大模型", "对话代理");
        registerModule("ai", "AI 代理", "大模型代理", "模型代理");
        registerModule("speech", "语音代理", "语音识别");
        registerModule("aliyunSpeech", "语音识别代理", "阿里云语音");
        registerModule("operation", "操作日志");
        registerModule("metric", "指标日志");
        registerModule("session", "会话日志");
        registerModule("ai_proxy", "AI 代理日志", "AI代理");
        registerModule("speech_proxy", "语音代理日志", "语音代理");
        registerModule("api_call", "接口调用");
        registerModule("button_click", "按钮点击");
        registerModule("form_submit", "表单提交");
        registerModule("error", "异常日志", "错误日志");
        registerModule("view_change", "视图切换");
        registerModule("chat", "聊天助手", "AI 对话", "聊天");
        registerModule("consultation", "智能问诊", "问诊");
        registerModule("voice_consultation", "语音问诊");
        registerModule("voice_capture", "语音采集");
        registerModule("reception", "接诊风险评估", "接诊");
        registerModule("reviewer", "独立审查 AI", "审查AI");
        registerModule("his_bridge", "HIS 桥接");
        registerModule("regional_runtime", "区域化运行时", "区域化");
        registerModule("diagnosis_path", "诊断路径");
        registerModule("navigation", "页面导航");
        registerModule("shell", "应用壳层");
        registerModule("settings", "设置");
        registerModule("system_integration", "系统集成");
        registerModule("consultation_ai", "问诊 AI");
        registerModule("consultation_reference", "问诊引用");
        registerModule("consultation_record", "问诊病历");
        registerModule("knowledge_base", "知识库");
        registerModule("lab_test", "检验项目");
        registerModule("procedure", "处置建议");
        registerModule("examination", "检查项目");

        registerSourceModule("navigation", "页面导航");
        registerSourceModule("settings_feedback", "设置页反馈");
        registerSourceModule("voice_session", "语音问诊整页");
        registerSourceModule("voice_recommendation", "语音推荐项");
        registerSourceModule("voice_record_field", "语音病例字段");
        registerSourceModule("view:chat", "聊天页");
        registerSourceModule("view:settings", "设置页");
        registerSourceModule("view:consultation", "智能问诊页");
        registerSourceModule("view:risk-alert", "风险提示页");
        registerSourceModule("view:voice-interaction", "语音胶囊");
        registerSourceModule("view:voice-result", "语音结果页");
        registerSourceModule("view:voice-consultation", "语音问诊页");
        registerSourceModule("view:reception-capsule", "接待胶囊");
        registerSourceModule("view:analytics", "数据分析页");
        registerSourceModule("view:symptom-manage", "症状库维护页");
        registerSourceModule("view:knowledge-base", "知识库页");
        registerSourceModule("feedback_panel", "反馈面板", "问题反馈");
        registerSourceModule("settings_panel", "设置页");
        registerSourceModule("chat_panel", "聊天面板");
        registerSourceModule("voice_capsule", "语音胶囊");
        registerSourceModule("voice_consultation_result", "语音问诊结果页");
        registerSourceModule("risk_alert_panel", "风险提醒面板");
        registerSourceModule("reception_capsule", "接待胶囊");
        registerSourceModule("consultation_page", "智能问诊页");
        registerSourceModule("consultation_ai", "问诊 AI");
        registerSourceModule("consultation_checklist", "鉴别排查");
        registerSourceModule("consultation_dynamic_symptom", "动态症状");
        registerSourceModule("consultation_reference", "问诊引用");
        registerSourceModule("consultation_record", "问诊病历");
        registerSourceModule("diagnosis_reviewer", "诊断审查");
        registerSourceModule("deep_link_listener", "Deep Link 监听");
        registerSourceModule("app_shell", "应用壳层");
        registerSourceModule("error_tracker", "异常跟踪");
        registerSourceModule("examination_reviewer", "检查审查");
        registerSourceModule("fact_checker", "事实核查");
        registerSourceModule("regional_runtime", "区域化运行时");
        registerSourceModule("his_bridge", "HIS 桥接");
        registerSourceModule("llm", "AI 对话代理");
        registerSourceModule("medicine_reviewer", "用药审查");
        registerSourceModule("aliyunSpeech", "语音识别代理");
        registerSourceModule("reception_risk_analysis", "接待风险分析");
        registerSourceModule("tcm_diagnosis_reviewer", "中医诊断审查");
        registerSourceModule("tcm_medicine_reviewer", "中药审查");
        registerSourceModule("diagnosis_path", "诊断路径");
        registerSourceModule("voice_consultation_ai", "语音问诊 AI");
        registerSourceModule("voice_intent", "语音意图识别");
        registerSourceModule("voice_safety_reviewer", "语音安全审查");

        registerScene("consultation", "智能问诊");
        registerScene("voice-interaction", "语音采集");
        registerScene("voice-consultation", "语音问诊");
        registerScene("feedback", "问题反馈");
        registerScene("settings", "设置");
        registerScene("risk-alert", "风险提示");
        registerScene("reception-capsule", "接待胶囊");
        registerScene("knowledge-base", "知识库");
        registerScene("floating-ball", "悬浮球");
        registerScene("reception", "接诊");
        registerScene("consultation-assist", "灵活问诊");
        registerScene("consultation-reference", "问诊引用回写");
        registerScene("consultation-record", "问诊病历");
        registerScene("consultation-diagnosis", "问诊诊断推荐");
        registerScene("reception-risk-analysis", "接待风险分析");
        registerScene("deep-link", "外部唤起");
        registerScene("regional-runtime", "区域化运行时");
        registerScene("diagnosis-path-reasoning", "诊断路径推理");
        registerScene("chat", "聊天");
        registerScene("chat-input", "语音转写");

        registerAction("chat", "AI 对话请求", "对话请求", "聊天请求");
        registerAction("transcribe", "语音转写请求", "转写请求");
        registerAction("realtime", "实时语音识别", "实时识别");
        registerAction("open_consultation", "进入智能问诊");
        registerAction("start_voice_capture", "进入语音采集");
        registerAction("open_voice_consultation", "进入语音问诊结果");
        registerAction("open_knowledge_base", "进入知识库");
        registerAction("exit_app", "退出应用");
        registerAction("open_feedback_panel", "打开问题反馈");
        registerAction("send_message", "发送对话消息");
        registerAction("start_recording", "开始语音采集");
        registerAction("stop_recording", "结束语音采集");
        registerAction("close_voice_capture", "关闭语音采集");
        registerAction("confirm_transcription", "确认语音转写");
        registerAction("retry_transcription", "放弃当前转写并重录");
        registerAction("discard_voice_result", "放弃语音问诊结果");
        registerAction("acknowledge_risk_alert", "确认风险提醒");
        registerAction("toggle_risk_detail", "展开或收起接待风险详情");
        registerAction("save_settings", "保存设置");
        registerAction("print_report", "打印问诊报告");
        registerAction("complete_consultation", "完成智能问诊");
        registerAction("submit_feedback", "提交问题反馈");
        registerAction("save_regional_connection", "保存区域化连接");
        registerAction("submit_to_his", "提交问诊结果到 HIS");
        registerAction("generate_medical_record", "生成病历草稿");
        registerAction("generate_final_report", "生成最终报告");
        registerAction("receive_deep_link", "接收外部 Deep Link");
        registerAction("receive_patient_risks", "接收 HIS 风险提示请求");
        registerAction("start_consultation", "接收 HIS 问诊启动请求");
        registerAction("receive_patient", "接收 HIS 接诊请求");
        registerAction("start_consultation_assist", "接收 HIS 灵活问诊请求");
        registerAction("start_voice_consultation", "接收 HIS 语音问诊请求");
        registerAction("analyze_patient_risk", "接待风险评估");
        registerAction("generate_diagnosis_recommendation", "生成诊断推荐");
        registerAction("request_phis_reference", "发起 PHIS 引用");
        registerAction("build_reasoning_path", "生成诊断路径说明");
        registerAction("initialize_runtime", "初始化区域化运行时");
        registerAction("regional_runtime_initialized", "区域化运行时初始化完成", "区域化启动完成");
        registerAction("register", "终端注册", "设备注册");
        registerAction("register_device", "终端注册", "设备注册");
        registerAction("bootstrap", "拉取启动配置", "拉取 bootstrap 配置", "获取启动配置");
        registerAction("get_bootstrap", "拉取启动配置", "获取 bootstrap 配置");
        registerAction("heartbeat", "终端心跳", "设备心跳");
        registerAction("speech_transcribe", "语音转写链路", "语音转写");
        registerAction("speech_realtime", "实时语音识别链路", "实时语音识别");
        registerAction("chat_stream", "AI 流式对话请求", "流式对话请求");
        registerAction("start", "开始");
        registerAction("finish", "完成");
        registerAction("general", "通用反馈");
        registerAction("recommendation", "推荐项反馈");
        registerAction("record_field", "病历字段反馈");
        registerAction("session", "整页评分反馈");
        registerAction("procedure", "处置建议反馈", "处置反馈");
        registerAction("lab_test", "检验项目反馈", "检验反馈");
        registerAction("examination", "检查项目反馈", "检查反馈");

        registerTitle("chat", "AI 对话请求", "聊天请求");
        registerTitle("chat_stream", "AI 流式对话请求");
        registerTitle("transcribe", "语音转写请求");
        registerTitle("realtime", "实时语音识别");
        registerTitle("speech_transcribe", "语音转写链路");
        registerTitle("speech_realtime", "实时语音识别链路");
        registerTitle("regional_runtime_initialized", "区域化运行时初始化完成");
        registerTitle("register", "终端注册");
        registerTitle("register_device", "终端注册");
        registerTitle("bootstrap", "拉取启动配置");
        registerTitle("get_bootstrap", "拉取启动配置");
        registerTitle("heartbeat", "终端心跳");
        registerTitle("procedure", "处置建议反馈");
        registerTitle("lab_test", "检验项目反馈");
        registerTitle("examination", "检查项目反馈");
        registerTitle("general", "通用反馈");
        registerTitle("recommendation", "推荐项反馈");
        registerTitle("record_field", "病历字段反馈");
        registerTitle("session", "整页评分反馈");
    }

    public void enrich(AiOpLog log) {
        if (log == null) {
            return;
        }
        String displayModule = resolveModuleLabel(log.getNaModule());
        String displayAction = resolveActionLabel(log.getOpAction());
        String displayTitle = resolveTitleLabel(log.getOpTitle(), log.getOpAction(), displayAction);
        log.setDisplayModule(displayModule);
        log.setDisplayAction(displayAction);
        log.setDisplayTitle(displayTitle);
        log.setDisplaySourceModule(resolveSourceModuleLabel(log.getSourceModule()));
        log.setDisplayScene(resolveSceneLabel(log.getSceneCode()));
    }

    public Collection<String> lookupModuleCodes(String text) {
        return lookupCodes(moduleAliases, text);
    }

    public Collection<String> lookupActionCodes(String text) {
        LinkedHashSet<String> matches = new LinkedHashSet<String>(lookupCodes(actionAliases, text));
        matches.addAll(matchStructuredActions(text));
        return matches;
    }

    public Collection<String> lookupSourceModuleCodes(String text) {
        return lookupCodes(sourceModuleAliases, text);
    }

    public Collection<String> lookupSceneCodes(String text) {
        return lookupCodes(sceneAliases, text);
    }

    public Collection<String> lookupTitleCodes(String text) {
        LinkedHashSet<String> matches = new LinkedHashSet<String>(lookupCodes(titleAliases, text));
        matches.addAll(matchStructuredActions(text));
        return matches;
    }

    public String resolveModuleLabel(String code) {
        String text = trimToNull(code);
        if (text == null) {
            return null;
        }
        String label = moduleLabels.get(text);
        return label != null ? label : text;
    }

    public String resolveActionLabel(String code) {
        String text = trimToNull(code);
        if (text == null) {
            return null;
        }
        String label = actionLabels.get(text);
        if (label != null) {
            return label;
        }
        label = resolveStructuredActionLabel(text);
        return label != null ? label : text;
    }

    public String resolveSourceModuleLabel(String code) {
        String text = trimToNull(code);
        if (text == null) {
            return null;
        }
        String label = sourceModuleLabels.get(text);
        if (label == null) {
            label = moduleLabels.get(text);
        }
        return label != null ? label : text;
    }

    public String resolveSceneLabel(String code) {
        String text = trimToNull(code);
        if (text == null) {
            return null;
        }
        String label = sceneLabels.get(text);
        return label != null ? label : text;
    }

    public String resolveTitleLabel(String title, String action, String displayAction) {
        String text = trimToNull(title);
        if (text == null) {
            return firstNonBlank(displayAction, resolveActionLabel(action));
        }
        if (containsChinese(text)) {
            return text;
        }
        String direct = titleLabels.get(text);
        if (direct != null) {
            return direct;
        }
        String structured = resolveStructuredActionLabel(text);
        if (structured != null) {
            return structured;
        }
        String actionLabel = firstNonBlank(displayAction, resolveActionLabel(action));
        if (actionLabel != null && (text.equals(trimToNull(action)) || actionLabel.contains(text) || titleLabels.containsKey(text))) {
            return actionLabel;
        }
        return text;
    }

    private void registerModule(String code, String label, String... aliases) {
        register(moduleLabels, moduleAliases, code, label, aliases);
    }

    private void registerAction(String code, String label, String... aliases) {
        register(actionLabels, actionAliases, code, label, aliases);
    }

    private void registerSourceModule(String code, String label, String... aliases) {
        register(sourceModuleLabels, sourceModuleAliases, code, label, aliases);
    }

    private void registerScene(String code, String label, String... aliases) {
        register(sceneLabels, sceneAliases, code, label, aliases);
    }

    private void registerTitle(String code, String label, String... aliases) {
        register(titleLabels, titleAliases, code, label, aliases);
    }

    private void register(Map<String, String> labels,
                          Map<String, Set<String>> aliases,
                          String code,
                          String label,
                          String... extraAliases) {
        labels.put(code, label);
        addAlias(aliases, code, code);
        addAlias(aliases, code, label);
        if (extraAliases != null) {
            for (String alias : extraAliases) {
                addAlias(aliases, code, alias);
            }
        }
    }

    private void addAlias(Map<String, Set<String>> aliases, String code, String alias) {
        String normalizedAlias = normalize(alias);
        if (normalizedAlias == null) {
            return;
        }
        Set<String> values = aliases.get(normalizedAlias);
        if (values == null) {
            values = new LinkedHashSet<String>();
            aliases.put(normalizedAlias, values);
        }
        values.add(code);
    }

    private Collection<String> lookupCodes(Map<String, Set<String>> aliases, String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return Collections.emptyList();
        }
        Set<String> direct = aliases.get(normalized);
        if (direct != null && !direct.isEmpty()) {
            return new ArrayList<String>(direct);
        }
        LinkedHashSet<String> matches = new LinkedHashSet<String>();
        for (Map.Entry<String, Set<String>> entry : aliases.entrySet()) {
            if (entry.getKey().contains(normalized) || normalized.contains(entry.getKey())) {
                matches.addAll(entry.getValue());
            }
        }
        return matches;
    }

    private Collection<String> matchStructuredActions(String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<String>();
        for (String action : Arrays.asList(
            "request_reference:diagnosis",
            "request_reference:medicine",
            "request_reference:medication",
            "request_reference:examination",
            "request_reference:lab_test",
            "request_reference:procedure",
            "reference_feedback:diagnosis",
            "reference_feedback:medicine",
            "reference_feedback:medication",
            "reference_feedback:examination",
            "reference_feedback:lab_test",
            "reference_feedback:procedure"
        )) {
            String label = resolveStructuredActionLabel(action);
            String normalizedLabel = normalize(label);
            if (normalizedLabel != null && (normalizedLabel.contains(normalized) || normalized.contains(normalizedLabel))) {
                matches.add(action);
            }
        }
        return matches;
    }

    private String resolveStructuredActionLabel(String action) {
        String text = trimToNull(action);
        if (text == null) {
            return null;
        }
        String candidate = text;
        if (candidate.startsWith("reference_feedback:")) {
            return "接收" + referenceTargetLabel(candidate.substring("reference_feedback:".length())) + "引用回执";
        }
        if (candidate.startsWith("reference_feedback_")) {
            return "接收" + referenceTargetLabel(candidate.substring("reference_feedback_".length())) + "引用回执";
        }
        if (candidate.startsWith("request_reference:")) {
            return "发起" + referenceTargetLabel(candidate.substring("request_reference:".length())) + "引用";
        }
        if (candidate.startsWith("request_reference_")) {
            return "发起" + referenceTargetLabel(candidate.substring("request_reference_".length())) + "引用";
        }
        return null;
    }

    private String referenceTargetLabel(String code) {
        String text = trimToNull(code);
        if (text == null) {
            return "业务";
        }
        if ("diagnosis".equals(text)) {
            return "诊断";
        }
        if ("medicine".equals(text) || "medication".equals(text) || "drug".equals(text)) {
            return "用药";
        }
        if ("lab_test".equals(text)) {
            return "检验项目";
        }
        if ("procedure".equals(text)) {
            return "处置项目";
        }
        if ("advice".equals(text) || "treatment".equals(text)) {
            return "治疗建议";
        }
        if ("examination".equals(text) || "item".equals(text)) {
            return "检查项目";
        }
        return text;
    }

    private String normalize(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        return text.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String text = trimToNull(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private boolean containsChinese(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(i));
            if (Character.UnicodeScript.HAN == script) {
                return true;
            }
        }
        return false;
    }
}