package com.genius.smartlight.service.duration.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.convert.duration.DurationConvert;
import com.genius.smartlight.dal.dataobject.DurationRecordDO;
import com.genius.smartlight.dal.mysql.DurationRecordMapper;
import com.genius.smartlight.service.duration.DurationService;
import com.genius.smartlight.vo.duration.DurationCreateReqVO;
import com.genius.smartlight.vo.duration.DurationDeviceSummaryRespVO;
import com.genius.smartlight.vo.duration.DurationRespVO;
import com.genius.smartlight.vo.duration.DurationSumRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DurationServiceImpl implements DurationService {

    private final DurationRecordMapper durationRecordMapper;

    @Override
    public Long createOrIncrease(DurationCreateReqVO reqVO) {
        DurationRecordDO exist = durationRecordMapper.selectOne(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getDeviceCode, reqVO.getDeviceCode())
                        .eq(DurationRecordDO::getStatDate, reqVO.getStatDate())
        );

        if (exist != null) {
            exist.setDurationValue(exist.getDurationValue() + reqVO.getDurationValue());
            exist.setUpdateTime(LocalDateTime.now());
            durationRecordMapper.updateById(exist);
            return exist.getId();
        }

        DurationRecordDO record = DurationConvert.convert(reqVO);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        durationRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public DurationRespVO getByDeviceCodeAndDate(String deviceCode, LocalDate statDate) {
        DurationRecordDO record = durationRecordMapper.selectOne(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getDeviceCode, deviceCode)
                        .eq(DurationRecordDO::getStatDate, statDate)
        );
        if (record == null) {
            throw new ServiceException("未找到该设备当天的停留时长记录");
        }
        return DurationConvert.convert(record);
    }

    @Override
    public List<DurationRespVO> getListByDeviceCode(String deviceCode) {
        List<DurationRecordDO> list = durationRecordMapper.selectList(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getDeviceCode, deviceCode)
                        .orderByDesc(DurationRecordDO::getStatDate)
        );
        return list.stream().map(DurationConvert::convert).toList();
    }

    @Override
    public List<DurationRespVO> getListByDateRange(String deviceCode, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }

        List<DurationRecordDO> list = durationRecordMapper.selectList(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getDeviceCode, deviceCode)
                        .between(DurationRecordDO::getStatDate, startDate, endDate)
                        .orderByAsc(DurationRecordDO::getStatDate)
        );

        return list.stream().map(DurationConvert::convert).toList();
    }

    @Override
    public DurationSumRespVO getSumByDateRange(String deviceCode, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }

        List<DurationRecordDO> list = durationRecordMapper.selectList(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getDeviceCode, deviceCode)
                        .between(DurationRecordDO::getStatDate, startDate, endDate)
        );

        long total = list.stream()
                .mapToLong(DurationRecordDO::getDurationValue)
                .sum();

        DurationSumRespVO respVO = new DurationSumRespVO();
        respVO.setDeviceCode(deviceCode);
        respVO.setStartDate(startDate);
        respVO.setEndDate(endDate);
        respVO.setTotalDuration(total);
        return respVO;
    }

    @Override
    public List<DurationDeviceSummaryRespVO> getDeviceSummaryByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }

        List<DurationRecordDO> list = durationRecordMapper.selectList(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .between(DurationRecordDO::getStatDate, startDate, endDate)
                        .orderByAsc(DurationRecordDO::getDeviceCode)
        );

        Map<String, Long> summaryMap = list.stream()
                .collect(Collectors.groupingBy(
                        DurationRecordDO::getDeviceCode,
                        Collectors.summingLong(DurationRecordDO::getDurationValue)
                ));

        return summaryMap.entrySet().stream().map(entry -> {
            DurationDeviceSummaryRespVO respVO = new DurationDeviceSummaryRespVO();
            respVO.setDeviceCode(entry.getKey());
            respVO.setTotalDuration(entry.getValue());
            return respVO;
        }).toList();
    }
}