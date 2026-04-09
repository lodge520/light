package com.genius.smartlight.service.lux.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.convert.lux.LuxConvert;
import com.genius.smartlight.dal.dataobject.LuxRecordDO;
import com.genius.smartlight.dal.mysql.LuxRecordMapper;
import com.genius.smartlight.service.lux.LuxService;
import com.genius.smartlight.vo.lux.LuxCreateReqVO;
import com.genius.smartlight.vo.lux.LuxRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LuxServiceImpl implements LuxService {

    private final LuxRecordMapper luxRecordMapper;

    @Override
    public Long createLuxRecord(LuxCreateReqVO reqVO) {
        LuxRecordDO record = LuxConvert.convert(reqVO);
        if (record.getCollectTime() == null) {
            record.setCollectTime(LocalDateTime.now());
        }
        record.setCreateTime(LocalDateTime.now());
        luxRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public LuxRespVO getLatestLuxRecord(String deviceCode) {
        LuxRecordDO record = luxRecordMapper.selectOne(
                new LambdaQueryWrapper<LuxRecordDO>()
                        .eq(LuxRecordDO::getDeviceCode, deviceCode)
                        .orderByDesc(LuxRecordDO::getCollectTime)
                        .last("limit 1")
        );
        if (record == null) {
            throw new ServiceException("未找到该设备的光照记录");
        }
        return LuxConvert.convert(record);
    }

    @Override
    public List<LuxRespVO> getLuxRecordList(String deviceCode) {
        List<LuxRecordDO> list = luxRecordMapper.selectList(
                new LambdaQueryWrapper<LuxRecordDO>()
                        .eq(LuxRecordDO::getDeviceCode, deviceCode)
                        .orderByDesc(LuxRecordDO::getCollectTime)
        );
        return list.stream().map(LuxConvert::convert).toList();
    }
}