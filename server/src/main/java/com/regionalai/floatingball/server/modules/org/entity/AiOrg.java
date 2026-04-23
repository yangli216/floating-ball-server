package com.regionalai.floatingball.server.modules.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_org")
public class AiOrg extends BaseEntity {

    @TableId(value = "id_org", type = IdType.ASSIGN_UUID)
    private String idOrg;

    @TableField("cd_org")
    private String cdOrg;

    @TableField("na_org")
    private String naOrg;

    @TableField("id_parent")
    private String idParent;

    @TableField("id_region")
    private String idRegion;

    @TableField("sd_org_type")
    private String sdOrgType;

    @TableField("sd_status")
    private String sdStatus;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("des_org")
    private String desOrg;
}
