package com.regionalai.floatingball.server.modules.lisresult.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hi_ods_apply_lis_report")
public class HiOdsApplyLisReport {

    @TableId(value = "id_report", type = IdType.INPUT)
    private String idReport;

    @TableField("id_apply")
    private String idApply;

    @TableField("id_result")
    private String idResult;

    @TableField("resultid")
    private String resultid;

    @TableField("na_result")
    private String naResult;

    @TableField("test_result")
    private String testResult;

    @TableField("result_qualitative")
    private String resultQualitative;

    @TableField("reference_range")
    private String referenceRange;

    @TableField("reference_low")
    private String referenceLow;

    @TableField("reference_high")
    private String referenceHigh;

    @TableField("result_unit")
    private String resultUnit;

    @TableField("result_hint")
    private String resultHint;

    @TableField("cd_result")
    private String cdResult;

    @TableField("instrument_code")
    private String instrumentCode;

    @TableField("instrument_name")
    private String instrumentName;

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

    @TableField("ctr")
    private String ctr;

    @TableField("id_report_group")
    private String idReportGroup;
}
