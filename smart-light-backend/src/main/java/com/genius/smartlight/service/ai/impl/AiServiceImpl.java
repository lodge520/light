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
import java.util.Locale;

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
    public FabricRecognizeRespVO fabricRecognize(String chipId, MultipartFile file) {
        long totalStart = System.currentTimeMillis();
        String filename = file == null ? "" : file.getOriginalFilename();
        long fileSize = file == null ? 0L : file.getSize();
        log.info("fabricRecognize start chipId={} filename={} fileSize={}", chipId, filename, fileSize);
        try {
        validateFile(file);

        // 1. Python 完成 SegFormer 分割 + ViT 面料识别
        long pythonStart = System.currentTimeMillis();
        FabricRecognizeRespVO result;
        try {
            result = fabricAiClient.recognize(file, chipId);
        } finally {
            log.info("fabricRecognize cost step=pythonRecognize chipId={} filename={} fileSize={} costMs={}",
                    chipId, filename, fileSize, System.currentTimeMillis() - pythonStart);
        }

        // 2. Java 主色提取：优先使用透明背景 PNG，只统计衣服像素
        MainColorResult colorResult;
        long mainColorStart = System.currentTimeMillis();
        try {
            String maskedBase64 = result.getClothMaskedPngBase64();

            if (maskedBase64 != null && !maskedBase64.isBlank()) {
                byte[] maskedBytes = java.util.Base64.getDecoder().decode(maskedBase64);
                colorResult = mainColorService.extract(new java.io.ByteArrayInputStream(maskedBytes));
            } else {
                colorResult = mainColorService.extract(file.getInputStream());
            }
        } catch (Exception e) {
            log.warn("fabricRecognize main color extract failed chipId={} filename={} fileSize={}",
                    chipId, filename, fileSize, e);
            colorResult = new MainColorResult("128,128,128", 60, 4500);
        } finally {
            log.info("fabricRecognize cost step=mainColorExtract chipId={} filename={} fileSize={} costMs={}",
                    chipId, filename, fileSize, System.currentTimeMillis() - mainColorStart);
        }

        MainColorResult adjustedColorResult = applyFabricAdjustment(colorResult, result.getLabel());

        result.setMainColorRgb(adjustedColorResult.getMainColorRgb());
        result.setRecommendedBrightness(adjustedColorResult.getRecommendedBrightness());
        result.setRecommendedTemp(adjustedColorResult.getRecommendedTemp());

        long updateStart = System.currentTimeMillis();
        try {
            if (chipId != null && !chipId.isBlank()) {
                updateDeviceAiResult(chipId, result);
            }
        } finally {
            log.info("fabricRecognize cost step=updateDeviceAndPushState chipId={} filename={} fileSize={} costMs={} skipped={}",
                    chipId, filename, fileSize, System.currentTimeMillis() - updateStart,
                    chipId == null || chipId.isBlank());
        }

        long wsStart = System.currentTimeMillis();
        try {
            webSocketPushService.pushFabricRecognize(chipId, file.getOriginalFilename(), result);
        } finally {
            log.info("fabricRecognize cost step=pushFabricRecognize chipId={} filename={} fileSize={} costMs={}",
                    chipId, filename, fileSize, System.currentTimeMillis() - wsStart);
        }

        // 主色已经算完，避免返回体太大，可以不把透明 PNG 返回给前端
        result.setClothMaskedPngBase64(null);

        return result;
        } finally {
            log.info("fabricRecognize cost step=total chipId={} filename={} fileSize={} costMs={}",
                    chipId, filename, fileSize, System.currentTimeMillis() - totalStart);
        }
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

    private MainColorResult applyFabricAdjustment(MainColorResult colorResult, String fabric) {
        MainColorResult baseResult = colorResult == null
                ? new MainColorResult("128,128,128", 60, 4500)
                : colorResult;

        int brightness = baseResult.getRecommendedBrightness() == null
                ? 60
                : baseResult.getRecommendedBrightness();
        int temp = baseResult.getRecommendedTemp() == null
                ? 4500
                : baseResult.getRecommendedTemp();

        String normalizedFabric = normalizeFabric(fabric);
        if (normalizedFabric.contains("cotton")) {
            brightness += 5;
            temp += 100;
        } else if (normalizedFabric.contains("polyester")) {
            brightness -= 5;
            temp += 150;
        } else if (normalizedFabric.contains("wool") || normalizedFabric.contains("cashmere")) {
            brightness -= 3;
            temp -= 250;
        }

        return new MainColorResult(
                baseResult.getMainColorRgb(),
                clamp(brightness, 30, 95),
                clamp(temp, 2700, 6500)
        );
    }

    private String normalizeFabric(String fabric) {
        return fabric == null ? "" : fabric.trim().toLowerCase(Locale.ROOT);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
