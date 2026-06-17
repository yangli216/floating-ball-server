package com.regionalai.floatingball.server.modules.lisresult.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hi_ods_apply")
public class HiOdsApply {

    @TableId(value = "id_apply", type = IdType.INPUT)
    private String idApply;

    @TableField("na_apply")
    private String naApply;

    @TableField("sd_disp")
    private String sdDisp;

    @TableField("cd_apply")
    private String cdApply;

    @TableField("sd_business")
    private String sdBusiness;

    @TableField("id_apply_sim")
    private String idApplySim;

    @TableField("na_apply_sim")
    private String naApplySim;

    @TableField("na_apply_group")
    private String naApplyGroup;

    @TableField("id_vis")
    private String idVis;

    @TableField("id_reg")
    private String idReg;

    @TableField("id_pi")
    private String idPi;

    @TableField("ids_diag")
    private String idsDiag;

    @TableField("nas_diag")
    private String nasDiag;

    @TableField("disease")
    private String disease;

    @TableField("na_disease")
    private String naDisease;

    @TableField("purpose")
    private String purpose;

    @TableField("remark")
    private String remark;

    @TableField("id_doc_exec")
    private String idDocExec;

    @TableField("na_doc_exec")
    private String naDocExec;

    @TableField("id_dept_exec")
    private String idDeptExec;

    @TableField("na_dept_exec")
    private String naDeptExec;

    @TableField("id_part")
    private String idPart;

    @TableField("na_part")
    private String naPart;

    @TableField("id_cli")
    private String idCli;

    @TableField("id_result")
    private String idResult;

    @TableField("sd_apply")
    private String sdApply;

    @TableField("fg_urgent")
    private String fgUrgent;

    @TableField("id_register")
    private String idRegister;

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

    @TableField("dt_exec")
    private LocalDateTime dtExec;

    @TableField("is_poct")
    private String isPoct;

    @TableField("stipulate")
    private String stipulate;

    @TableField("des_prob")
    private String desProb;

    @TableField("des_cur_die")
    private String desCurDie;

    @TableField("complete_check")
    private String completeCheck;

    @TableField("fg_digital")
    private String fgDigital;

    @TableField("fg_ct_reduct")
    private String fgCtReduct;

    @TableField("fg_day_first")
    private String fgDayFirst;
}
