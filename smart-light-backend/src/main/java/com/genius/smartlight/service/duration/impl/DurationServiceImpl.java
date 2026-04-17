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
import com.genius.smartlight.websocket.WebSocketPushService;
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

    private final WebSocketPushService webSocketPushService;
    private final DurationRecordMapper durationRecordMapper;

    @Override
    public Long createOrIncrease(DurationCreateReqVO reqVO) {
        if (reqVO.getChipId() == null || reqVO.getChipId().isBlank()) {
            throw new ServiceException("chipId不能为空");
        }
        if (reqVO.getDurationValue() == null || reqVO.getDurationValue() < 0) {
            throw new ServiceException("durationValue不能为空且不能小于0");
        }
        if (reqVO.getStatDate() == null) {
            reqVO.setStatDate(LocalDate.now());
        }

        durationRecordMapper.insertOrIncrease(
                reqVO.getChipId(),
                reqVO.getStatDate(),
                reqVO.getDurationValue()
        );

        DurationRecordDO latest = durationRecordMapper.selectOne(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getChipId, reqVO.getChipId())
                        .eq(DurationRecordDO::getStatDate, reqVO.getStatDate())
                        .last("limit 1")
        );

        if (latest == null) {
            throw new ServiceException("时长记录保存成功，但查询结果失败");
        }

        webSocketPushService.pushDuration(DurationConvert.convert(latest));
        return latest.getId();
    }

    @Override
    public DurationRespVO getByChipIdAndDate(String chipId, LocalDate statDate) {
        DurationRecordDO record = durationRecordMapper.selectOne(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getChipId, chipId)
                        .eq(DurationRecordDO::getStatDate, statDate)
        );
        if (record == null) {
            throw new ServiceException("未找到该设备当天的停留时长记录");
        }
        return DurationConvert.convert(record);
    }

    @Override
    public List<DurationRespVO> getListByChipId(String chipId) {
        List<DurationRecordDO> list = durationRecordMapper.selectList(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getChipId, chipId)
                        .orderByDesc(DurationRecordDO::getStatDate)
        );
        return list.stream().map(DurationConvert::convert).toList();
    }

    @Override
    public List<DurationRespVO> getListByDateRange(String chipId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }

        List<DurationRecordDO> list = durationRecordMapper.selectList(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getChipId, chipId)
                        .between(DurationRecordDO::getStatDate, startDate, endDate)
                        .orderByAsc(DurationRecordDO::getStatDate)
        );

        return list.stream().map(DurationConvert::convert).toList();
    }

    @Override
    public DurationSumRespVO getSumByDateRange(String chipId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }

        List<DurationRecordDO> list = durationRecordMapper.selectList(
                new LambdaQueryWrapper<DurationRecordDO>()
                        .eq(DurationRecordDO::getChipId, chipId)
                        .between(DurationRecordDO::getStatDate, startDate, endDate)
        );

        long total = list.stream()
                .mapToLong(DurationRecordDO::getDurationValue)
                .sum();

        DurationSumRespVO respVO = new DurationSumRespVO();
        respVO.setChipId(chipId);
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
                        .orderByAsc(DurationRecordDO::getChipId)
        );

        Map<String, Long> summaryMap = list.stream()
                .filter(item -> item.getChipId() != null && !item.getChipId().isBlank())
                .collect(Collectors.groupingBy(
                        DurationRecordDO::getChipId,
                        Collectors.summingLong(item -> item.getDurationValue() == null ? 0 : item.getDurationValue())
                ));

        return summaryMap.entrySet().stream().map(entry -> {
            DurationDeviceSummaryRespVO respVO = new DurationDeviceSummaryRespVO();
            respVO.setChipId(entry.getKey());
            respVO.setTotalDuration(entry.getValue());
            return respVO;
        }).toList();
    }
}