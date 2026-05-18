package com.regionalai.floatingball.server.modules.device.entity;

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
@TableName("c_ai_device")
public class AiDevice extends BaseEntity {

    @TableId(value = "id_device", type = IdType.ASSIGN_UUID)
    private String idDevice;

    @TableField("cd_device")
    private String cdDevice;

    @TableField("na_device")
    private String naDevice;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;

    @TableField("id_bind_user")
    private String idBindUser;

    @TableField("device_token")
    private String deviceToken;

    @TableField("sd_status")
    private String sdStatus;

    @TableField("dt_last_heartbeat")
    private LocalDateTime dtLastHeartbeat;

    @TableField("dt_registered")
    private LocalDateTime dtRegistered;

    @TableField("client_version")
    private String clientVersion;

    @TableField("os_info")
    private String osInfo;

    @TableField("device_public_key")
    private String devicePublicKey;
}
