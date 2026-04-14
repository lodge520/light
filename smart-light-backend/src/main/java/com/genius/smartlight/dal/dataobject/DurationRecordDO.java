package com.genius.smartlight.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("duration_record")
public class DurationRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_code")
    private String deviceCode;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("duration_value")
    private Long durationValue;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}