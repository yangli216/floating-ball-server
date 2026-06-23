package com.regionalai.floatingball.server.modules.feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.db.DatabaseDialect;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import com.regionalai.floatingball.server.modules.audit.mapper.AiOpLogMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitRequest;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitResponse;
import com.regionalai.floatingball.server.modules.feedback.dto.FeedbackListQuery;
import com.regionalai.floatingball.server.modules.feedback.entity.AiFeedback;
import com.regionalai.floatingball.server.modules.feedback.mapper.AiFeedbackMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private AiFeedbackMapper aiFeedbackMapper;

    @Mock
    private AiOpLogMapper aiOpLogMapper;

    private FeedbackService feedbackService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        feedbackService = new FeedbackService(aiFeedbackMapper, aiOpLogMapper, objectMapper, new DatabaseDialect(DatabaseDialect.Kind.ORACLE));
    }

    @Test
    void submitShouldCreateNewRevisionAndDowngradePreviousLatest() throws Exception {
        AiDevice device = new AiDevice();
        device.setIdDevice("device-1");
        device.setIdOrg("ORG-1");

        AiFeedback latest = new AiFeedback();
        latest.setIdFeedback("feedback-1");
        latest.setIdFeedbackRoot("feedback-1");
        latest.setRevisionNo(1);

        when(aiFeedbackMapper.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any())).thenReturn(latest);
        when(aiFeedbackMapper.insert(any(AiFeedback.class))).thenAnswer(invocation -> {
            AiFeedback feedback = invocation.getArgument(0);
            feedback.setIdFeedback("feedback-2");
            return 1;
        });

        ClientFeedbackSubmitRequest request = new ClientFeedbackSubmitRequest();
        request.setScore(4);
        request.setComment("修订后的反馈");
        request.setSourceModule("view:consultation");
        request.setKind("general");
        request.setSeverity("low");
        request.setChainContext(buildChainContext("consult-1::view:consultation", "feedback-1", 2));

        ClientFeedbackSubmitResponse response = feedbackService.submit(device, request);

        assertEquals("feedback-2", response.getFeedbackId());

        org.mockito.ArgumentCaptor<AiFeedback> insertCaptor = org.mockito.ArgumentCaptor.forClass(AiFeedback.class);
        verify(aiFeedbackMapper).insert(insertCaptor.capture());
        AiFeedback inserted = insertCaptor.getValue();
        assertEquals("consult-1::view:consultation", inserted.getFeedbackScopeKey());
        assertEquals("feedback-1", inserted.getIdFeedbackRoot());
        assertEquals("feedback-1", inserted.getPreviousFeedbackId());
        assertEquals(Integer.valueOf(2), inserted.getRevisionNo());
        assertEquals("1", inserted.getFgLatest());

        verify(aiFeedbackMapper, times(2)).update(isNull(), org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any());
        verify(aiFeedbackMapper, never()).updateById(any(AiFeedback.class));
    }

    @Test
    void submitShouldRetryOnceWhenLatestScopeUniqueConflictHappens() {
        AiDevice device = new AiDevice();
        device.setIdDevice("device-1");
        device.setIdOrg("ORG-1");

        AiFeedback firstLatest = new AiFeedback();
        firstLatest.setIdFeedback("feedback-1");
        firstLatest.setIdFeedbackRoot("feedback-1");
        firstLatest.setRevisionNo(1);
        AiFeedback secondLatest = new AiFeedback();
        secondLatest.setIdFeedback("feedback-2");
        secondLatest.setIdFeedbackRoot("feedback-1");
        secondLatest.setRevisionNo(2);

        when(aiFeedbackMapper.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any()))
            .thenReturn(firstLatest)
            .thenReturn(secondLatest);
        when(aiFeedbackMapper.insert(any(AiFeedback.class)))
            .thenThrow(new DuplicateKeyException("uk_c_ai_feedback_latest_scope"))
            .thenAnswer(invocation -> {
                AiFeedback feedback = invocation.getArgument(0);
                feedback.setIdFeedback("feedback-3");
                return 1;
            });

        ClientFeedbackSubmitRequest request = new ClientFeedbackSubmitRequest();
        request.setScore(4);
        request.setComment("并发修订后的反馈");
        request.setChainContext(buildChainContext("consult-1::view:consultation", "feedback-1", 2));

        ClientFeedbackSubmitResponse response = feedbackService.submit(device, request);

        assertEquals("feedback-3", response.getFeedbackId());
        org.mockito.ArgumentCaptor<AiFeedback> insertCaptor = org.mockito.ArgumentCaptor.forClass(AiFeedback.class);
        verify(aiFeedbackMapper, times(2)).insert(insertCaptor.capture());
        assertEquals("feedback-2", insertCaptor.getAllValues().get(1).getPreviousFeedbackId());
        assertEquals(Integer.valueOf(3), insertCaptor.getAllValues().get(1).getRevisionNo());
    }

    @Test
    void submitShouldReturnBusinessErrorWhenLatestScopeConflictRepeats() {
        AiDevice device = new AiDevice();
        device.setIdDevice("device-1");

        AiFeedback latest = new AiFeedback();
        latest.setIdFeedback("feedback-1");
        latest.setIdFeedbackRoot("feedback-1");
        latest.setRevisionNo(1);

        when(aiFeedbackMapper.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any()))
            .thenReturn(latest);
        when(aiFeedbackMapper.insert(any(AiFeedback.class)))
            .thenThrow(new DuplicateKeyException("uk_c_ai_feedback_latest_scope"));

        ClientFeedbackSubmitRequest request = new ClientFeedbackSubmitRequest();
        request.setScore(4);
        request.setComment("重复冲突");
        request.setChainContext(buildChainContext("consult-1::view:consultation", "feedback-1", 2));

        BusinessException ex = assertThrows(BusinessException.class, () -> feedbackService.submit(device, request));

        assertEquals("反馈提交冲突，请稍后重试", ex.getMessage());
        verify(aiFeedbackMapper, times(2)).insert(any(AiFeedback.class));
    }

    @Test
    void listShouldDefaultToLatestOnlyAndExposeRevisionFields() throws Exception {
        AiFeedback latest = new AiFeedback();
        latest.setIdFeedback("feedback-2");
        latest.setFgActive("1");
        latest.setFgLatest("1");
        latest.setRevisionNo(2);
        latest.setCommentText("最新反馈");
        latest.setKind("general");
        latest.setSeverity("low");
        latest.setSourceModule("view:consultation");
        latest.setIdFeedbackRoot("feedback-1");
        latest.setChainContextJson(objectMapper.writeValueAsString(Collections.singletonMap("feedbackScopeKey", "consult-1::view:consultation")));

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback>(1, 10);
        page.setRecords(Arrays.asList(latest));
        page.setTotal(1);
        when(aiFeedbackMapper.selectPage(
            any(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any()))
            .thenReturn(page);

        FeedbackListQuery query = new FeedbackListQuery();
        query.setCurrent(1);
        query.setSize(10);

        feedbackService.list(query);
        verify(aiFeedbackMapper).selectPage(
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback>>any(),
            argThat(wrapper -> String.valueOf(wrapper.getCustomSqlSegment()).contains("fg_latest")));

        com.regionalai.floatingball.server.common.api.PageResponse<com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackListItem> result = feedbackService.list(query);
        assertEquals(1, result.getRecords().size());
        assertEquals(Integer.valueOf(2), result.getRecords().get(0).getRevisionNo());
        assertTrue(Boolean.TRUE.equals(result.getRecords().get(0).getLatest()));
        assertEquals("智能问诊页", result.getRecords().get(0).getDisplaySourceModule());

        clearInvocations(aiFeedbackMapper);
        FeedbackListQuery includeHistoryQuery = new FeedbackListQuery();
        includeHistoryQuery.setIncludeHistory(true);
        feedbackService.list(includeHistoryQuery);
        verify(aiFeedbackMapper).selectPage(
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback>>any(),
            argThat(wrapper -> !String.valueOf(wrapper.getCustomSqlSegment()).contains("fg_latest")));
    }

    @Test
    void listShouldApplyMultiScoreFilterAndIgnoreInvalidValues() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(aiFeedbackMapper.selectPage(
            any(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any()))
            .thenReturn(page);

        FeedbackListQuery query = new FeedbackListQuery();
        query.setScores(Arrays.asList(1, 3, 3, 6, null, 5));

        feedbackService.list(query);

        verify(aiFeedbackMapper).selectPage(
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback>>any(),
            argThat(wrapper -> {
                String segment = String.valueOf(wrapper.getCustomSqlSegment());
                if (!(wrapper instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper)) {
                    return false;
                }
                @SuppressWarnings("unchecked")
                com.baomidou.mybatisplus.core.conditions.AbstractWrapper<AiFeedback, ?, ?> abstractWrapper =
                    (com.baomidou.mybatisplus.core.conditions.AbstractWrapper<AiFeedback, ?, ?>) wrapper;
                Map<String, Object> params = abstractWrapper.getParamNameValuePairs();
                return segment.contains("score IN")
                    && params.containsValue(1)
                    && params.containsValue(3)
                    && params.containsValue(5)
                    && !params.containsValue(6);
            }));
    }

    @Test
    void detailShouldExposeDisplayFieldsForFeedbackAndTimeline() {
        AiFeedback feedback = new AiFeedback();
        feedback.setIdFeedback("feedback-1");
        feedback.setFgActive("1");
        feedback.setSourceModule("voice_record_field");
        feedback.setFeedbackTime(java.time.LocalDateTime.of(2026, 5, 1, 10, 0, 0));

        AiOpLog log = new AiOpLog();
        log.setSdLogType("ai_proxy");
        log.setNaModule("llm");
        log.setOpAction("chat");
        log.setOpTitle("chat");
        log.setSourceModule("llm");
        log.setDesOp("chat");
        log.setOpResult("1");
        log.setOperationTime(java.time.LocalDateTime.of(2026, 5, 1, 9, 59, 0));
        log.setPayloadJson("{}");

        when(aiFeedbackMapper.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any()))
            .thenReturn(feedback);
        when(aiOpLogMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiOpLog>>any()))
            .thenReturn(Collections.singletonList(log));

        com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackDetailResponse response = feedbackService.detail("feedback-1");

        assertEquals("语音病例字段", response.getFeedback().getDisplaySourceModule());
        assertEquals("AI 对话请求", response.getTimeline().get(0).getTitle());
        assertEquals("AI 对话代理", response.getTimeline().get(0).getDisplaySourceModule());
    }

    @Test
    void detailShouldUsePgCompatiblePayloadSearchForGaussdbTimeline() {
        feedbackService = new FeedbackService(aiFeedbackMapper, aiOpLogMapper, objectMapper, new DatabaseDialect(DatabaseDialect.Kind.OPENGAUSS));
        AiFeedback feedback = new AiFeedback();
        feedback.setIdFeedback("feedback-1");
        feedback.setFgActive("1");
        feedback.setIdDevice("device-1");
        feedback.setTraceId("trace-1");
        feedback.setFeedbackTime(java.time.LocalDateTime.of(2026, 5, 1, 10, 0, 0));

        when(aiFeedbackMapper.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiFeedback>>any()))
            .thenReturn(feedback);
        when(aiOpLogMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<AiOpLog>>any()))
            .thenReturn(Collections.emptyList());

        feedbackService.detail("feedback-1");

        verify(aiOpLogMapper).selectList(argThat(wrapper -> {
            String segment = String.valueOf(wrapper.getCustomSqlSegment());
            return segment.contains("POSITION")
                && segment.contains("payload_json")
                && !segment.toLowerCase().contains("dbms_lob");
        }));
    }

    @Test
    void submitShouldRejectInvalidScoreBlankCommentAndNonImageScreenshot() {
        ClientFeedbackSubmitRequest missingComment = new ClientFeedbackSubmitRequest();
        missingComment.setScore(3);

        BusinessException missingCommentEx = assertThrows(
            BusinessException.class,
            () -> feedbackService.submit(new AiDevice(), missingComment)
        );
        assertEquals("反馈说明不能为空", missingCommentEx.getMessage());

        ClientFeedbackSubmitRequest invalidScore = new ClientFeedbackSubmitRequest();
        invalidScore.setScore(6);
        invalidScore.setComment("评分超出范围");

        BusinessException invalidScoreEx = assertThrows(
            BusinessException.class,
            () -> feedbackService.submit(new AiDevice(), invalidScore)
        );
        assertEquals("评分必须在 1 到 5 分之间", invalidScoreEx.getMessage());

        ClientFeedbackSubmitRequest invalidScreenshot = new ClientFeedbackSubmitRequest();
        invalidScreenshot.setScore(4);
        invalidScreenshot.setComment("截图不是图片");
        ClientFeedbackSubmitRequest.ScreenshotPayload screenshot = new ClientFeedbackSubmitRequest.ScreenshotPayload();
        screenshot.setDataUrl("data:text/plain;base64,SGVsbG8=");
        invalidScreenshot.setScreenshot(screenshot);

        BusinessException invalidScreenshotEx = assertThrows(
            BusinessException.class,
            () -> feedbackService.submit(new AiDevice(), invalidScreenshot)
        );
        assertEquals("截图必须为图片 dataUrl", invalidScreenshotEx.getMessage());
        verify(aiFeedbackMapper, never()).insert(any(AiFeedback.class));
    }

    @Test
    void submitFirstRevisionShouldBackfillRootAndDefaultFields() throws Exception {
        AiDevice device = new AiDevice();
        device.setIdDevice("device-1");
        device.setIdOrg("ORG-1");

        when(aiFeedbackMapper.insert(any(AiFeedback.class))).thenAnswer(invocation -> {
            AiFeedback feedback = invocation.getArgument(0);
            feedback.setIdFeedback("feedback-1");
            return 1;
        });

        ClientFeedbackSubmitRequest request = new ClientFeedbackSubmitRequest();
        request.setScore(5);
        request.setComment(" 首次反馈 ");
        request.setKind("unknown-kind");
        request.setSeverity("unknown-severity");
        request.setTraceId("trace-1");
        request.setTags(Arrays.asList(" 准确 ", "准确", "", "及时"));

        ClientFeedbackSubmitResponse response = feedbackService.submit(device, request);

        assertEquals("feedback-1", response.getFeedbackId());
        org.mockito.ArgumentCaptor<AiFeedback> captor = org.mockito.ArgumentCaptor.forClass(AiFeedback.class);
        verify(aiFeedbackMapper).insert(captor.capture());
        AiFeedback inserted = captor.getValue();
        assertEquals("general", inserted.getKind());
        assertEquals("medium", inserted.getSeverity());
        assertEquals("settings_feedback", inserted.getSourceModule());
        assertEquals("1", inserted.getHasTrace());
        assertEquals("首次反馈", inserted.getCommentText());
        assertEquals(Arrays.asList("准确", "及时"), objectMapper.readValue(inserted.getTagsJson(), java.util.List.class));
        assertEquals("feedback-1", inserted.getIdFeedbackRoot());
        verify(aiFeedbackMapper).updateById(inserted);
    }

    private Map<String, Object> buildChainContext(String scopeKey, String previousFeedbackId, int revision) {
        Map<String, Object> chainContext = new LinkedHashMap<String, Object>();
        chainContext.put("consultationId", "consult-1");
        chainContext.put("feedbackScopeKey", scopeKey);
        chainContext.put("feedbackRevision", revision);
        chainContext.put("previousFeedbackId", previousFeedbackId);
        return chainContext;
    }
}
