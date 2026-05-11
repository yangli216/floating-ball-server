package com.regionalai.floatingball.server.modules.feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
        feedbackService = new FeedbackService(aiFeedbackMapper, aiOpLogMapper, objectMapper);
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

        clearInvocations(aiFeedbackMapper);
        FeedbackListQuery includeHistoryQuery = new FeedbackListQuery();
        includeHistoryQuery.setIncludeHistory(true);
        feedbackService.list(includeHistoryQuery);
        verify(aiFeedbackMapper).selectPage(
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiFeedback>>any(),
            argThat(wrapper -> !String.valueOf(wrapper.getCustomSqlSegment()).contains("fg_latest")));
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