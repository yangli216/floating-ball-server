package com.regionalai.floatingball.server.modules.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_security_rejection_log")
public class SecurityRejectionLog extends BaseEntity {

    @TableId(value = "id_log", type = IdType.ASSIGN_UUID)
    private String idLog;

    @TableField("rejection_type")
    private String rejectionType;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_path")
    private String requestPath;

    @TableField("client_ip")
    private String clientIp;

    @TableField("id_device")
    private String idDevice;

    @TableField("cd_device")
    private String cdDevice;

    @TableField("id_org")
    private String idOrg;

    @TableField("request_id")
    private String requestId;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("reject_detail")
    private String rejectDetail;

    @TableField("has_signature")
    private String hasSignature;

    @TableField("timestamp_header")
    private String timestampHeader;

    @TableField("nonce_header")
    private String nonceHeader;

    @TableField("client_version")
    private String clientVersion;

    @TableField("update_channel")
    private String updateChannel;
}
