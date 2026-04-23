package com.genius.smartlight.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.integration.ai.FabricAiClient;
import com.genius.smartlight.integration.ai.PersonDetectClient;
import com.genius.smartlight.service.ai.AiService;
import com.genius.smartlight.service.ai.MainColorService;
import com.genius.smartlight.service.ai.MainColorResult;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {
    private final DeviceMapper deviceMapper;
    private final FabricAiClient fabricAiClient;
    private final PersonDetectClient personDetectClient;
    private final WebSocketPushService webSocketPushService;
    private final MainColorService mainColorService;
    @Override
    public FabricRecognizeRespVO fabricRecognize(String id, MultipartFile file) {
        validateFile(file);

        // 1. Python 完成 SegFormer 分割 + ViT 面料识别
        FabricRecognizeRespVO result = fabricAiClient.recognize(file);

        // 2. Java 主色提取：优先使用透明背景 PNG，只统计衣服像素
        MainColorResult colorResult;
        try {
            String maskedBase64 = result.getClothMaskedPngBase64();

            if (maskedBase64 != null && !maskedBase64.isBlank()) {
                byte[] maskedBytes = java.util.Base64.getDecoder().decode(maskedBase64);
                colorResult = mainColorService.extract(new java.io.ByteArrayInputStream(maskedBytes));
            } else {
                colorResult = mainColorService.extract(file.getInputStream());
            }
        } catch (Exception e) {
            colorResult = new MainColorResult("128,128,128", 60, 4500);
        }

        result.setMainColorRgb(colorResult.getMainColorRgb());
        result.setRecommendedBrightness(colorResult.getRecommendedBrightness());
        result.setRecommendedTemp(colorResult.getRecommendedTemp());

        if (id != null && !id.isBlank()) {
            updateDeviceAiResult(id, result);
        }

        webSocketPushService.pushFabricRecognize(id, file.getOriginalFilename(), result);

        // 主色已经算完，避免返回体太大，可以不把透明 PNG 返回给前端
        result.setClothMaskedPngBase64(null);

        return result;
    }

    @Override
    public PersonDetectRespVO personDetect(String id, MultipartFile file) {
        validateFile(file);
        PersonDetectRespVO result = personDetectClient.detect(file);
        webSocketPushService.pushPersonDetect(id, file.getOriginalFilename(), result);
        return result;
    }

    private void updateDeviceAiResult(String chipId, FabricRecognizeRespVO result) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
        );

        if (device == null) {
            return;
        }

        device.setFabric(result.getLabel());
        device.setMainColorRgb(result.getMainColorRgb());
        device.setRecommendedBrightness(result.getRecommendedBrightness());
        device.setRecommendedTemp(result.getRecommendedTemp());
        device.setUpdateTime(LocalDateTime.now());

        deviceMapper.updateById(device);

        DeviceRespVO respVO = buildDeviceRespVO(device);

        // 推送给前端浏览器
        webSocketPushService.pushState(respVO);

        // 推送给对应设备 /ws/device
        webSocketPushService.pushStateToDevice(chipId, respVO);
    }

    private DeviceRespVO buildDeviceRespVO(DeviceDO device) {
        DeviceRespVO respVO = new DeviceRespVO();

        respVO.setId(device.getId());
        respVO.setChipId(device.getChipId());
        respVO.setDeviceType(device.getDeviceType());
        respVO.setDeviceNo(device.getDeviceNo());
        respVO.setDisplayName(device.getDisplayName());
        respVO.setIp(device.getIp());
        respVO.setBrightness(device.getBrightness());
        respVO.setTemp(device.getTemp());
        respVO.setAutoMode(device.getAutoMode());
        respVO.setRecommendedBrightness(device.getRecommendedBrightness());
        respVO.setRecommendedTemp(device.getRecommendedTemp());
        respVO.setFabric(device.getFabric());
        respVO.setMainColorRgb(device.getMainColorRgb());
        respVO.setCreateTime(device.getCreateTime());
        respVO.setUpdateTime(device.getUpdateTime());

        return respVO;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
    }
}