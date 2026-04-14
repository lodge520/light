package com.genius.smartlight.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.genius.smartlight.dal.dataobject.LuxRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LuxRecordMapper extends BaseMapper<LuxRecordDO> {
}