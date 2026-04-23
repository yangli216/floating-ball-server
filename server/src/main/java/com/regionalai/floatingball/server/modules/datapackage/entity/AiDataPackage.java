package com.regionalai.floatingball.server.modules.datapackage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.regionalai.floatingball.server.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_ai_data_package")
public class AiDataPackage extends BaseEntity {

    @TableId(value = "id_package", type = IdType.ASSIGN_UUID)
    private String idPackage;

    @TableField("cd_package")
    private String cdPackage;

    @TableField("na_package")
    private String naPackage;

    @TableField("sd_package_type")
    private String sdPackageType;

    @TableField("version_num")
    private String versionNum;

    @TableField("content_json")
    private String contentJson;

    @TableField("sd_status")
    private String sdStatus;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_region")
    private String idRegion;
}
