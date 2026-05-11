package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminFeedbackListItem {
    private String feedbackId;
    private Integer score;
    private String comment;
    private String sourceModule;
    private String kind;
    private String severity;
    private List<String> tags;
    private Boolean hasCorrection;
    private Boolean hasTrace;
    private Boolean hasScreenshot;

    /** 反馈目标摘要（如"推荐诊断：高血压"、"病例字段：主诉"），由 chainContext 解析得出，便于运营快速识别 */
    private String targetSummary;
    /** 反馈目标类型：diagnosis / medication / exam / lab / procedure / chiefComplaint / pastHistory / session 等 */
    private String targetType;

    private String idDoctor;
    private String naDoctor;
    private String idDept;
    private String naDept;
    private String idOrg;
    private String naOrg;

    private String traceId;
    private String sessionId;
    private String idDevice;
    private Integer revisionNo;
    private Boolean latest;
    private LocalDateTime createdAt;
}
