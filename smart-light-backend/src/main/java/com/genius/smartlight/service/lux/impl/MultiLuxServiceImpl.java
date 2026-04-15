package com.genius.smartlight.service.lux.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.dataobject.LuxRecordDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.dal.mysql.LuxRecordMapper;
import com.genius.smartlight.service.lux.MultiLuxService;
import com.genius.smartlight.vo.lux.MultiLuxRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MultiLuxServiceImpl implements MultiLuxService {

    private final DeviceMapper deviceMapper;
    private final LuxRecordMapper luxMapper;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public MultiLuxRespVO getMultiLux() {
        List<DeviceDO> devices = deviceMapper.selectList(new LambdaQueryWrapper<>());

        Map<String, List<LuxRecordDO>> deviceLuxMap = new LinkedHashMap<>();
        SortedSet<String> labelSet = new TreeSet<>();

        for (DeviceDO device : devices) {
            List<LuxRecordDO> luxList = luxMapper.selectList(
                    new LambdaQueryWrapper<LuxRecordDO>()
                            .eq(LuxRecordDO::getDeviceCode, device.getDeviceCode())
                            .orderByAsc(LuxRecordDO::getCollectTime)
                            .orderByAsc(LuxRecordDO::getCreateTime)
            );

            if (luxList == null || luxList.isEmpty()) {
                continue;
            }

            List<LuxRecordDO> lastLuxList = luxList.size() > 12
                    ? luxList.subList(luxList.size() - 12, luxList.size())
                    : luxList;

            deviceLuxMap.put(device.getDeviceCode(), lastLuxList);

            for (LuxRecordDO lux : lastLuxList) {
                labelSet.add(formatLabel(lux));
            }
        }

        MultiLuxRespVO respVO = new MultiLuxRespVO();
        List<String> labels = new ArrayList<>(labelSet);
        respVO.setLabels(labels);

        List<MultiLuxRespVO.Dataset> datasets = new ArrayList<>();

        for (Map.Entry<String, List<LuxRecordDO>> entry : deviceLuxMap.entrySet()) {
            String deviceCode = entry.getKey();
            List<LuxRecordDO> luxList = entry.getValue();

            Map<String, Double> pointMap = new HashMap<>();
            for (LuxRecordDO lux : luxList) {
                pointMap.put(formatLabel(lux), lux.getLuxValue());
            }

            MultiLuxRespVO.Dataset dataset = new MultiLuxRespVO.Dataset();
            dataset.setLabel(deviceCode);

            List<Double> data = new ArrayList<>();
            for (String label : labels) {
                data.add(pointMap.getOrDefault(label, null));
            }

            dataset.setData(data);
            datasets.add(dataset);
        }

        respVO.setDatasets(datasets);
        return respVO;
    }

    private String formatLabel(LuxRecordDO lux) {
        LocalDateTime time = lux.getCollectTime() != null ? lux.getCollectTime() : lux.getCreateTime();
        if (time == null) {
            return "-";
        }
        return time.format(TIME_FORMATTER);
    }
}