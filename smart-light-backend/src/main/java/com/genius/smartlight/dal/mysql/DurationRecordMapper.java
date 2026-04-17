package com.genius.smartlight.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.genius.smartlight.dal.dataobject.DurationRecordDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface DurationRecordMapper extends BaseMapper<DurationRecordDO> {

    @Insert("""
        INSERT INTO duration_record
        (chip_id, stat_date, duration_value, create_time, update_time)
        VALUES
        (#{chipId}, #{statDate}, #{durationValue}, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
        duration_value = duration_value + VALUES(duration_value),
        update_time = NOW()
        """)
    int insertOrIncrease(@Param("chipId") String chipId,
                         @Param("statDate") LocalDate statDate,
                         @Param("durationValue") Long durationValue);
}