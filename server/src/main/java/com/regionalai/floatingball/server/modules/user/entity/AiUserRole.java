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
@TableName("c_ai_user_role")
public class AiUserRole extends BaseEntity {

    @TableId(value = "id_user_role", type = IdType.ASSIGN_UUID)
    private String idUserRole;

    @TableField("id_user")
    private String idUser;

    @TableField("id_role")
    private String idRole;
}
