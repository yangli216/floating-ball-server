package com.regionalai.floatingball.server.modules.feedback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.GlobalExceptionHandler;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitRequest;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitResponse;
import com.regionalai.floatingball.server.modules.feedback.service.FeedbackService;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientFeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new ClientFeedbackController(feedbackService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void tearDown() {
        DeviceContextHolder.clear();
    }

    @Test
    void submitShouldUseDeviceContextAndWrapAcceptedResponse() throws Exception {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");
        DeviceContextHolder.set(device);

        ClientFeedbackSubmitRequest request = new ClientFeedbackSubmitRequest();
        request.setScore(5);
        request.setComment("建议很准确");
        request.setSourceModule("view:consultation");
        request.setKind("recommendation");

        when(feedbackService.submit(eq(device), any(ClientFeedbackSubmitRequest.class)))
            .thenReturn(new ClientFeedbackSubmitResponse("FB001", "accepted"));

        mockMvc.perform(post("/v1/client/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", "RID-client-feedback")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-client-feedback"))
            .andExpect(jsonPath("$.data.feedbackId").value("FB001"))
            .andExpect(jsonPath("$.data.status").value("accepted"));

        ArgumentCaptor<ClientFeedbackSubmitRequest> requestCaptor = ArgumentCaptor.forClass(ClientFeedbackSubmitRequest.class);
        verify(feedbackService).submit(eq(device), requestCaptor.capture());
        assertEquals("recommendation", requestCaptor.getValue().getKind());
    }

    @Test
    void submitShouldRejectInvalidScoreBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/v1/client/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", "RID-client-feedback-invalid")
                .content("{\"score\":0,\"comment\":\"评分非法\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION-001"))
            .andExpect(jsonPath("$.requestId").value("RID-client-feedback-invalid"))
            .andExpect(jsonPath("$.message").value("评分不能低于 1"));

        verifyNoInteractions(feedbackService);
    }
}
