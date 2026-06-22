package com.regionalai.floatingball.server.modules.recommendationpreference.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_rec_pref_agg")
public class AiRecommendationPreferenceAggregate extends BaseEntity {

    @TableId(value = "id_agg", type = IdType.ASSIGN_UUID)
    private String idAgg;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;

    @TableField("id_dept")
    private String idDept;

    @TableField("id_doctor")
    private String idDoctor;

    @TableField("recommendation_type")
    private String recommendationType;

    @TableField("item_key")
    private String itemKey;

    @TableField("item_id")
    private String itemId;

    @TableField("item_code")
    private String itemCode;

    @TableField("item_name")
    private String itemName;

    @TableField("selected_count")
    private Integer selectedCount;

    @TableField("confirm_count")
    private Integer confirmCount;

    @TableField("manual_match_count")
    private Integer manualMatchCount;

    @TableField("preference_score")
    private BigDecimal preferenceScore;

    @TableField("last_event_time")
    private LocalDateTime lastEventTime;
}
