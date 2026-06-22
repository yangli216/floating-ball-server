package com.regionalai.floatingball.server.modules.recommendationpreference.entity;

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
@TableName("c_ai_rec_pref_event")
public class AiRecommendationPreferenceEvent extends BaseEntity {

    @TableId(value = "id_event", type = IdType.ASSIGN_UUID)
    private String idEvent;

    @TableField("id_device")
    private String idDevice;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;

    @TableField("recommendation_type")
    private String recommendationType;

    @TableField("action_code")
    private String actionCode;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("item_key")
    private String itemKey;

    @TableField("item_id")
    private String itemId;

    @TableField("item_code")
    private String itemCode;

    @TableField("item_name")
    private String itemName;

    @TableField("fg_selected")
    private String fgSelected;

    @TableField("fg_primary")
    private String fgPrimary;

    @TableField("trace_id")
    private String traceId;

    @TableField("consultation_id")
    private String consultationId;

    @TableField("session_id")
    private String sessionId;

    @TableField("source_module")
    private String sourceModule;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("id_doctor")
    private String idDoctor;

    @TableField("na_doctor")
    private String naDoctor;

    @TableField("id_dept")
    private String idDept;

    @TableField("na_dept")
    private String naDept;

    @TableField("prompt_version")
    private String promptVersion;

    @TableField("template_version")
    private String templateVersion;

    @TableField("model_version")
    private String modelVersion;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("event_time")
    private LocalDateTime eventTime;
}
