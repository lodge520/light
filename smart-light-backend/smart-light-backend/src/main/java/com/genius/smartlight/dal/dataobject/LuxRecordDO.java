package com.genius.smartlight.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lux_record")
public class LuxRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_code")
    private String deviceCode;

    @TableField("lux_value")
    private Double luxValue;

    @TableField("collect_time")
    private LocalDateTime collectTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}