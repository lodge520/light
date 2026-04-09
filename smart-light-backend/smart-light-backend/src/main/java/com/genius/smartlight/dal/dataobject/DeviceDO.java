package com.genius.smartlight.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device")
public class DeviceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_code")
    private String deviceCode;

    @TableField("ip")
    private String ip;

    @TableField("brightness")
    private Integer brightness;

    @TableField("temp")
    private Integer temp;

    @TableField("auto_mode")
    private Boolean autoMode;

    @TableField("recommended_brightness")
    private Integer recommendedBrightness;

    @TableField("recommended_temp")
    private Integer recommendedTemp;

    @TableField("fabric")
    private String fabric;

    @TableField("main_color_rgb")
    private String mainColorRgb;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}