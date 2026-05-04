package com.genius.smartlight.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.convert.device.DeviceConvert;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.dataobject.OtaFirmwareDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.dal.mysql.OtaFirmwareMapper;
import com.genius.smartlight.service.device.DeviceOtaService;
import com.genius.smartlight.vo.device.DeviceOtaCheckRespVO;
import com.genius.smartlight.vo.device.DeviceOtaStartReqVO;
import com.genius.smartlight.websocket.DeviceSessionManager;
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DeviceOtaServiceImpl implements DeviceOtaService {

    private static final String CHANNEL_STABLE = "stable";
    private static final String CHANNEL_TEST = "test";
    private static final String OTA_STATUS_UPDATING = "updating";

    private final DeviceMapper deviceMapper;
    private final OtaFirmwareMapper otaFirmwareMapper;
    private final DeviceSessionManager deviceSessionManager;
    private final WebSocketPushService webSocketPushService;
    private final ObjectMapper objectMapper;

    @Override
    public DeviceOtaCheckRespVO checkUpdate(String chipId, String channel) {
        DeviceDO device = getDeviceByChipId(chipId);
        String currentChannel = normalizeChannel(device.getFirmwareChannel());
        String targetChannel = resolveTargetChannel(channel, currentChannel);
        OtaFirmwareDO firmware = findLatestFirmware(device, targetChannel);
        return buildCheckResp(device, firmware, currentChannel, targetChannel);
    }

    @Override
    public DeviceOtaCheckRespVO startUpdate(String chipId, DeviceOtaStartReqVO reqVO) {
        DeviceDO device = getDeviceByChipId(chipId);
        String currentChannel = normalizeChannel(device.getFirmwareChannel());
        String targetChannel = resolveTargetChannel(reqVO == null ? null : reqVO.getChannel(), currentChannel);
        OtaFirmwareDO firmware = resolveFirmware(device, reqVO, targetChannel);

        if (firmware == null) {
            throw new ServiceException("No enabled firmware found");
        }
        if (!Boolean.TRUE.equals(firmware.getEnabled())) {
            throw new ServiceException("Firmware is disabled");
        }
        if (!safeEquals(device.getDeviceType(), firmware.getDeviceType())) {
            throw new ServiceException("Firmware device type does not match");
        }
        if (!safeEquals(targetChannel, normalizeChannel(firmware.getChannel()))) {
            throw new ServiceException("Firmware channel does not match target channel");
        }
        validateFileUrl(firmware.getFileUrl());

        int currentCode = device.getFirmwareVersionCode() == null ? 0 : device.getFirmwareVersionCode();
        int targetCode = firmware.getVersionCode() == null ? 0 : firmware.getVersionCode();
        if (!isUpdatable(currentChannel, currentCode, targetChannel, targetCode)) {
            throw new ServiceException("Current firmware is already up to date");
        }
        if (!deviceSessionManager.isOnline(chipId)) {
            throw new ServiceException("Device is offline");
        }

        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "ota_update");
        msg.put("url", firmware.getFileUrl());
        msg.put("version", firmware.getVersion());
        msg.put("versionCode", firmware.getVersionCode());
        msg.put("channel", targetChannel);
        if (firmware.getMd5() != null && !firmware.getMd5().isBlank()) {
            msg.put("md5", firmware.getMd5());
        }

        boolean sent = webSocketPushService.pushRawToDevice(chipId, msg.toString());
        if (!sent) {
            throw new ServiceException("OTA command send failed");
        }

        device.setOtaStatus(OTA_STATUS_UPDATING);
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);
        webSocketPushService.pushState(DeviceConvert.convert(device));

        return buildCheckResp(device, firmware, currentChannel, targetChannel);
    }

    private DeviceDO getDeviceByChipId(String chipId) {
        if (chipId == null || chipId.isBlank()) {
            throw new ServiceException("chipId cannot be empty");
        }
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
        );
        if (device == null) {
            throw new ServiceException("Device not found");
        }
        return device;
    }

    private OtaFirmwareDO resolveFirmware(DeviceDO device, DeviceOtaStartReqVO reqVO, String targetChannel) {
        if (reqVO != null && reqVO.getFirmwareId() != null) {
            OtaFirmwareDO firmware = otaFirmwareMapper.selectById(reqVO.getFirmwareId());
            if (firmware == null) {
                throw new ServiceException("Firmware not found");
            }
            return firmware;
        }
        return findLatestFirmware(device, targetChannel);
    }

    private OtaFirmwareDO findLatestFirmware(DeviceDO device, String targetChannel) {
        return otaFirmwareMapper.selectOne(
                new LambdaQueryWrapper<OtaFirmwareDO>()
                        .eq(OtaFirmwareDO::getDeviceType, device.getDeviceType())
                        .eq(OtaFirmwareDO::getChannel, targetChannel)
                        .eq(OtaFirmwareDO::getEnabled, true)
                        .orderByDesc(OtaFirmwareDO::getVersionCode)
                        .last("limit 1")
        );
    }

    private DeviceOtaCheckRespVO buildCheckResp(DeviceDO device, OtaFirmwareDO firmware, String currentChannel, String targetChannel) {
        DeviceOtaCheckRespVO respVO = new DeviceOtaCheckRespVO();
        respVO.setChipId(device.getChipId());
        respVO.setDeviceType(device.getDeviceType());
        respVO.setChannel(targetChannel);
        respVO.setCurrentVersion(device.getFirmwareVersion());
        respVO.setCurrentVersionCode(device.getFirmwareVersionCode());
        respVO.setOtaStatus(device.getOtaStatus());

        if (firmware != null) {
            respVO.setFirmwareId(firmware.getId());
            respVO.setLatestVersion(firmware.getVersion());
            respVO.setLatestVersionCode(firmware.getVersionCode());
            respVO.setFileUrl(firmware.getFileUrl());
            respVO.setMd5(firmware.getMd5());
            respVO.setChangelog(firmware.getChangelog());
            int currentCode = device.getFirmwareVersionCode() == null ? 0 : device.getFirmwareVersionCode();
            int latestCode = firmware.getVersionCode() == null ? 0 : firmware.getVersionCode();
            respVO.setHasUpdate(isUpdatable(currentChannel, currentCode, targetChannel, latestCode));
        } else {
            respVO.setHasUpdate(false);
        }
        return respVO;
    }

    private String resolveTargetChannel(String channel, String currentChannel) {
        if (channel == null || channel.isBlank()) {
            return currentChannel;
        }
        return normalizeChannel(channel);
    }

    private boolean isUpdatable(String currentChannel, int currentCode, String targetChannel, int targetCode) {
        if (targetCode <= 0) {
            return false;
        }
        if (!safeEquals(currentChannel, targetChannel)) {
            return true;
        }
        return targetCode > currentCode;
    }

    private String normalizeChannel(String channel) {
        String value = channel == null ? "" : channel.trim().toLowerCase(Locale.ROOT);
        if (CHANNEL_TEST.equals(value)) {
            return CHANNEL_TEST;
        }
        return CHANNEL_STABLE;
    }

    private boolean safeEquals(String a, String b) {
        return a != null && b != null && a.equals(b);
    }

    private void validateFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ServiceException("Firmware file_url cannot be empty");
        }

        try {
            URI uri = URI.create(fileUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new ServiceException("Firmware file_url must be http or https");
            }
            if (host == null || host.isBlank()) {
                throw new ServiceException("Firmware file_url host cannot be empty");
            }

            String lowerHost = host.toLowerCase(Locale.ROOT);
            if ("localhost".equals(lowerHost)
                    || "127.0.0.1".equals(lowerHost)
                    || "::1".equals(lowerHost)
                    || "0:0:0:0:0:0:0:1".equals(lowerHost)) {
                throw new ServiceException("Firmware file_url must be reachable by ESP8266, not localhost");
            }
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Firmware file_url is invalid");
        }
    }
}
