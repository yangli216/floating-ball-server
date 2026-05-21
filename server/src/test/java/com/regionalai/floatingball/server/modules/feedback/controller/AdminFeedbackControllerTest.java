package com.regionalai.floatingball.server.modules.feedback.controller;

import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.exception.GlobalExceptionHandler;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackDetailResponse;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackListItem;
import com.regionalai.floatingball.server.modules.feedback.dto.FeedbackListQuery;
import com.regionalai.floatingball.server.modules.feedback.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminFeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminFeedbackController(feedbackService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void listShouldNormalizeInvalidPagingAndReturnWrappedFeedbacks() throws Exception {
        AdminFeedbackListItem item = new AdminFeedbackListItem();
        item.setFeedbackId("FB001");
        item.setScore(4);
        item.setComment("回答整体可用");
        item.setSourceModule("voice_record_field");
        item.setDisplaySourceModule("语音病例字段");
        item.setRevisionNo(2);
        item.setLatest(true);

        when(feedbackService.list(any(FeedbackListQuery.class)))
            .thenReturn(new PageResponse<AdminFeedbackListItem>(1, 10, 1, Collections.singletonList(item)));

        mockMvc.perform(get("/admin/api/feedbacks")
                .param("current", "0")
                .param("size", "-1")
                .param("sourceModule", "语音问诊")
                .param("includeHistory", "true")
                .header("X-Request-Id", "RID-feedback-list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-feedback-list"))
            .andExpect(jsonPath("$.data.current").value(1))
            .andExpect(jsonPath("$.data.size").value(10))
            .andExpect(jsonPath("$.data.records[0].feedbackId").value("FB001"))
            .andExpect(jsonPath("$.data.records[0].displaySourceModule").value("语音病例字段"))
            .andExpect(jsonPath("$.data.records[0].latest").value(true));

        ArgumentCaptor<FeedbackListQuery> queryCaptor = ArgumentCaptor.forClass(FeedbackListQuery.class);
        verify(feedbackService).list(queryCaptor.capture());
        assertEquals(1, queryCaptor.getValue().getCurrent());
        assertEquals(10, queryCaptor.getValue().getSize());
        assertEquals("语音问诊", queryCaptor.getValue().getSourceModule());
        assertEquals(Boolean.TRUE, queryCaptor.getValue().getIncludeHistory());
    }

    @Test
    void detailShouldReturnWrappedTimeline() throws Exception {
        AdminFeedbackDetailResponse response = new AdminFeedbackDetailResponse();
        AdminFeedbackDetailResponse.FeedbackDetail detail = new AdminFeedbackDetailResponse.FeedbackDetail();
        detail.setFeedbackId("FB001");
        detail.setDisplaySourceModule("智能问诊页");
        detail.setRevisionNo(1);
        detail.setLatest(true);
        response.setFeedback(detail);

        when(feedbackService.detail("FB001")).thenReturn(response);

        mockMvc.perform(get("/admin/api/feedbacks/FB001")
                .header("X-Request-Id", "RID-feedback-detail"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-feedback-detail"))
            .andExpect(jsonPath("$.data.feedback.feedbackId").value("FB001"))
            .andExpect(jsonPath("$.data.feedback.displaySourceModule").value("智能问诊页"))
            .andExpect(jsonPath("$.data.feedback.latest").value(true));
    }

    @Test
    void detailShouldUseGlobalErrorEnvelopeWhenMissing() throws Exception {
        when(feedbackService.detail("MISSING")).thenThrow(new BusinessException("反馈记录不存在"));

        mockMvc.perform(get("/admin/api/feedbacks/MISSING")
                .header("X-Request-Id", "RID-feedback-missing"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BIZ-001"))
            .andExpect(jsonPath("$.requestId").value("RID-feedback-missing"))
            .andExpect(jsonPath("$.message").value("反馈记录不存在"));
    }
}
