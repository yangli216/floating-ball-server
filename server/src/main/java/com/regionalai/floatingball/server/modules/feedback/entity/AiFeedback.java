package com.regionalai.floatingball.server.modules.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_feedback")
public class AiFeedback extends BaseEntity {

    @TableId(value = "id_feedback", type = IdType.ASSIGN_UUID)
    private String idFeedback;

    @TableField("id_device")
    private String idDevice;

    @TableField("id_org")
    private String idOrg;

    @TableField("na_org")
    private String naOrg;

    @TableField("id_doctor")
    private String idDoctor;

    @TableField("na_doctor")
    private String naDoctor;

    @TableField("id_dept")
    private String idDept;

    @TableField("na_dept")
    private String naDept;

    @TableField("session_id")
    private String sessionId;

    @TableField("trace_id")
    private String traceId;

    @TableField("source_module")
    private String sourceModule;

    @TableField("kind")
    private String kind;

    @TableField("severity")
    private String severity;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("has_correction")
    private String hasCorrection;

    @TableField("has_trace")
    private String hasTrace;

    @TableField("score")
    private Integer score;

    @TableField("comment_text")
    private String commentText;

    @TableField("screenshot_file_name")
    private String screenshotFileName;

    @TableField("screenshot_mime_type")
    private String screenshotMimeType;

    @TableField("screenshot_data_url")
    private String screenshotDataUrl;

    @TableField("chain_context_json")
    private String chainContextJson;

    @TableField("feedback_time")
    private LocalDateTime feedbackTime;
}
