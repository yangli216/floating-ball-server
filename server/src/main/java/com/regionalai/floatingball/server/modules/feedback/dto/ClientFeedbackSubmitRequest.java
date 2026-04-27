package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
public class ClientFeedbackSubmitRequest {

    private String sessionId;
    private String traceId;
    private String sourceModule;

    /**
     * 反馈类型：general / recommendation / record_field / session
     */
    private String kind;

    /**
     * 严重度：low / medium / high
     */
    private String severity;

    /**
     * 反馈医生 ID（来自客户端握手 urt.userRoleDepts）
     */
    private String doctorId;

    private String doctorName;

    private String orgName;

    private String deptId;

    private String deptName;

    /**
     * 问题标签（预定义或自定义），不超过 20 个
     */
    private List<String> tags;

    /**
     * 是否包含医生修正内容（语音 record_field 反馈使用）
     */
    private Boolean hasCorrection;

    @Min(value = 1, message = "评分不能低于 1")
    @Max(value = 5, message = "评分不能高于 5")
    private Integer score;

    @NotBlank(message = "反馈说明不能为空")
    private String comment;

    @Valid
    private ScreenshotPayload screenshot;

    private Map<String, Object> chainContext;

    @Data
    public static class ScreenshotPayload {
        private String fileName;
        private String mimeType;
        private String dataUrl;
    }
}
