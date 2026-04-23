package com.regionalai.floatingball.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_user")
public class AiUser extends BaseEntity {

    @TableId(value = "id_user", type = IdType.ASSIGN_UUID)
    private String idUser;

    @TableField("cd_user")
    private String cdUser;

    @TableField("na_user")
    private String naUser;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("id_org")
    private String idOrg;

    @TableField("sd_status")
    private String sdStatus;
}
