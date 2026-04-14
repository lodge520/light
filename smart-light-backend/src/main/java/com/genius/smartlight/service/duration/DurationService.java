package com.genius.smartlight.service.duration;

import com.genius.smartlight.vo.duration.DurationCreateReqVO;
import com.genius.smartlight.vo.duration.DurationDeviceSummaryRespVO;
import com.genius.smartlight.vo.duration.DurationRespVO;
import com.genius.smartlight.vo.duration.DurationSumRespVO;

import java.time.LocalDate;
import java.util.List;

public interface DurationService {

    Long createOrIncrease(DurationCreateReqVO reqVO);

    DurationRespVO getByDeviceCodeAndDate(String deviceCode, LocalDate statDate);

    List<DurationRespVO> getListByDeviceCode(String deviceCode);

    List<DurationRespVO> getListByDateRange(String deviceCode, LocalDate startDate, LocalDate endDate);

    DurationSumRespVO getSumByDateRange(String deviceCode, LocalDate startDate, LocalDate endDate);

    List<DurationDeviceSummaryRespVO> getDeviceSummaryByDateRange(LocalDate startDate, LocalDate endDate);
}