package com.regionalai.floatingball.server.modules.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_role")
public class AiRole extends BaseEntity {

    @TableId(value = "id_role", type = IdType.ASSIGN_UUID)
    private String idRole;

    @TableField("cd_role")
    private String cdRole;

    @TableField("na_role")
    private String naRole;

    @TableField("des_role")
    private String desRole;

    @TableField("sd_status")
    private String sdStatus;
}
