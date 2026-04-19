package com.genius.smartlight.service.lux.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.convert.lux.LuxConvert;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.dataobject.LuxRecordDO;
import com.genius.smartlight.dal.dataobject.StoreDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.dal.mysql.LuxRecordMapper;
import com.genius.smartlight.dal.mysql.StoreMapper;
import com.genius.smartlight.security.SecurityUtils;
import com.genius.smartlight.service.lux.LuxService;
import com.genius.smartlight.vo.lux.LuxCreateReqVO;
import com.genius.smartlight.vo.lux.LuxRespVO;
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LuxServiceImpl implements LuxService {

    private final LuxRecordMapper luxRecordMapper;
    private final DeviceMapper deviceMapper;
    private final StoreMapper storeMapper;
    private final WebSocketPushService webSocketPushService;

    @Override
    public Long createLuxRecord(LuxCreateReqVO reqVO) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, reqVO.getChipId())
        );

        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        if (device.getStoreId() == null) {
            throw new ServiceException("设备未绑定店铺");
        }

        LuxRecordDO record = LuxConvert.convert(reqVO);
        record.setChipId(device.getChipId());
        record.setDeviceId(device.getId());
        record.setStoreId(device.getStoreId());

        if (record.getCollectTime() == null) {
            record.setCollectTime(LocalDateTime.now());
        }
        record.setCreateTime(LocalDateTime.now());

        luxRecordMapper.insert(record);

        webSocketPushService.pushLux(LuxConvert.convert(record));
        return record.getId();
    }

    @Override
    public LuxRespVO getLatestLuxRecord(String chipId) {
        Long currentStoreId = getCurrentStoreId();
        ensureDeviceInCurrentStore(chipId, currentStoreId);

        LuxRecordDO record = luxRecordMapper.selectOne(
                new LambdaQueryWrapper<LuxRecordDO>()
                        .eq(LuxRecordDO::getChipId, chipId)
                        .eq(LuxRecordDO::getStoreId, currentStoreId)
                        .orderByDesc(LuxRecordDO::getCollectTime)
                        .last("limit 1")
        );

        if (record == null) {
            throw new ServiceException("未找到该设备的光照记录");
        }
        return LuxConvert.convert(record);
    }

    @Override
    public List<LuxRespVO> getLuxRecordList(String chipId) {
        Long currentStoreId = getCurrentStoreId();
        ensureDeviceInCurrentStore(chipId, currentStoreId);

        List<LuxRecordDO> list = luxRecordMapper.selectList(
                new LambdaQueryWrapper<LuxRecordDO>()
                        .eq(LuxRecordDO::getChipId, chipId)
                        .eq(LuxRecordDO::getStoreId, currentStoreId)
                        .orderByDesc(LuxRecordDO::getCollectTime)
        );

        return list.stream().map(LuxConvert::convert).toList();
    }

    private Long getCurrentStoreId() {
        Long userId = SecurityUtils.getCurrentUserId();

        StoreDO store = storeMapper.selectOne(
                new LambdaQueryWrapper<StoreDO>()
                        .eq(StoreDO::getUserId, userId)
        );

        if (store == null) {
            throw new ServiceException("当前用户未绑定店铺");
        }

        return store.getId();
    }

    private void ensureDeviceInCurrentStore(String chipId, Long currentStoreId) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
                        .eq(DeviceDO::getStoreId, currentStoreId)
        );

        if (device == null) {
            throw new ServiceException("无权查看该设备数据");
        }
    }
}