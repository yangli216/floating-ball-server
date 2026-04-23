package com.regionalai.floatingball.server.common.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {

    @TableField("fg_active")
    private String fgActive;

    @TableField(value = "insert_time", fill = FieldFill.INSERT)
    private LocalDateTime insertTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
