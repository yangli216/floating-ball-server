package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientFeedbackSubmitResponse {
    private String feedbackId;
    private String status;
}
