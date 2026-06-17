package com.regionalai.floatingball.server.modules.lisresult.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hi_ods_apply_pacs_report")
public class HiOdsApplyPacsReport {

    @TableId(value = "id_report", type = IdType.INPUT)
    private String idReport;

    @TableField("id_apply")
    private String idApply;

    @TableField("\"RESULT\"")
    private String result;

    @TableField("remark")
    private String remark;

    @TableField("clinical_impression")
    private String clinicalImpression;

    @TableField("negative_positive")
    private String negativePositive;

    @TableField("diagnostic_imaging")
    private String diagnosticImaging;

    @TableField("na_update_user")
    private String naUpdateUser;

    @TableField("na_insert_user")
    private String naInsertUser;

    @TableField("cd_study")
    private String cdStudy;

    @TableField("id_dept")
    private String idDept;

    @TableField("na_dept")
    private String naDept;

    @TableField("id_org")
    private String idOrg;

    @TableField("id_tet")
    private String idTet;

    @TableField("revision")
    private Integer revision;

    @TableField("insert_user")
    private String insertUser;

    @TableField("insert_time")
    private LocalDateTime insertTime;

    @TableField("update_user")
    private String updateUser;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
