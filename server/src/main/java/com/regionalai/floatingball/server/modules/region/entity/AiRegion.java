package com.regionalai.floatingball.server.modules.region.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_region")
public class AiRegion extends BaseEntity {

    @TableId(value = "id_region", type = IdType.ASSIGN_UUID)
    private String idRegion;

    @TableField("cd_region")
    private String cdRegion;

    @TableField("na_region")
    private String naRegion;

    @TableField("id_parent")
    private String idParent;

    @TableField("sd_region_type")
    private String sdRegionType;

    @TableField("sd_status")
    private String sdStatus;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("des_region")
    private String desRegion;
}
