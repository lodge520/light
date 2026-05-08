package com.genius.smartlight.service.lighteffect.impl;

import com.genius.smartlight.convert.device.DeviceConvert;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.service.lighteffect.LightEffectService;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.lighteffect.LightEffectStateReqVO;
import com.genius.smartlight.vo.lighteffect.LightEffectStateRespVO;
import com.genius.smartlight.websocket.WebSocketPushService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LightEffectServiceImpl implements LightEffectService {

    private static final String EFFECT_WAVE = "wave";
    private static final String SCOPE_ALL = "all";
    private static final int MIN_TEMP = 2700;
    private static final int MAX_TEMP = 6500;
    private static final int MIN_INTERVAL_MS = 1000;
    private static final int MAX_INTERVAL_MS = 6000;
    private static final int BASE_INTERVAL_MS = 2500;

    private final DeviceMapper deviceMapper;
    private final WebSocketPushService webSocketPushService;

    private final Object lock = new Object();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "light-effect-wave-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> waveFuture;
    private LightEffectStateRespVO state = defaultState();

    @Override
    public LightEffectStateRespVO getState() {
        synchronized (lock) {
            return copyState(state);
        }
    }

    @Override
    public LightEffectStateRespVO saveState(LightEffectStateReqVO reqVO) {
        LightEffectStateRespVO nextState;
        boolean shouldRunWave;

        synchronized (lock) {
            state = mergeState(state, reqVO);
            nextState = copyState(state);
            shouldRunWave = Boolean.TRUE.equals(state.getEnabled()) && EFFECT_WAVE.equals(state.getEffect());
        }

        if (shouldRunWave) {
            applyWaveTick();
            restartWaveScheduler(nextState.getSpeed());
        } else {
            stopWaveScheduler();
        }

        LightEffectStateRespVO latest = getState();
        webSocketPushService.pushLightEffectState(latest);
        return latest;
    }

    @Override
    public LightEffectStateRespVO close() {
        LightEffectStateRespVO closed;

        synchronized (lock) {
            state.setEnabled(false);
            state.setUpdateTime(LocalDateTime.now());
            closed = copyState(state);
        }

        stopWaveScheduler();
        webSocketPushService.pushLightEffectState(closed);
        return closed;
    }

    @PreDestroy
    public void destroy() {
        stopWaveScheduler();
        scheduler.shutdownNow();
    }

    private void restartWaveScheduler(Double speed) {
        synchronized (lock) {
            stopWaveSchedulerLocked();
            int intervalMs = resolveIntervalMs(speed);
            waveFuture = scheduler.scheduleAtFixedRate(this::safeApplyWaveTick, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    private void stopWaveScheduler() {
        synchronized (lock) {
            stopWaveSchedulerLocked();
        }
    }

    private void stopWaveSchedulerLocked() {
        if (waveFuture != null) {
            waveFuture.cancel(false);
            waveFuture = null;
        }
    }

    private void safeApplyWaveTick() {
        try {
            LightEffectStateRespVO latest = applyWaveTick();
            if (Boolean.TRUE.equals(latest.getEnabled()) && EFFECT_WAVE.equals(latest.getEffect())) {
                webSocketPushService.pushLightEffectState(latest);
            }
        } catch (Exception e) {
            log.error("Wave light effect tick failed", e);
        }
    }

    private LightEffectStateRespVO applyWaveTick() {
        LightEffectStateRespVO snapshot;
        synchronized (lock) {
            if (!Boolean.TRUE.equals(state.getEnabled()) || !EFFECT_WAVE.equals(state.getEffect())) {
                return copyState(state);
            }
            snapshot = copyState(state);
        }

        List<DeviceDO> devices = findTargetDevices(snapshot.getSelectedScope());
        int brightness = clamp(snapshot.getBrightness(), 0, 100);
        double phaseIndex = safeDouble(snapshot.getPhaseIndex(), 0D);
        double phaseGap = safeDouble(snapshot.getPhaseGap(), 0.8D);

        for (int index = 0; index < devices.size(); index++) {
            DeviceDO device = devices.get(index);
            int temp = resolveWaveTemp(snapshot, phaseIndex, index, phaseGap);

            device.setBrightness(brightness);
            device.setTemp(temp);
            device.setAutoMode(false);
            device.setRecommendedBrightness(brightness);
            device.setRecommendedTemp(temp);
            device.setUpdateTime(LocalDateTime.now());
            deviceMapper.updateById(device);

            DeviceRespVO respVO = DeviceConvert.convert(device);
            webSocketPushService.pushState(respVO);
            if (device.getChipId() != null && !device.getChipId().isBlank()) {
                webSocketPushService.pushStateToDevice(device.getChipId(), respVO);
            }
        }

        synchronized (lock) {
            if (Boolean.TRUE.equals(state.getEnabled()) && EFFECT_WAVE.equals(state.getEffect())) {
                state.setPhaseIndex(safeDouble(state.getPhaseIndex(), 0D) + 1D);
                state.setUpdateTime(LocalDateTime.now());
            }
            return copyState(state);
        }
    }

    private int resolveWaveTemp(LightEffectStateRespVO snapshot, double phaseIndex, int deviceIndex, double phaseGap) {
        int baseTemp = clamp(snapshot.getBaseTemp(), MIN_TEMP, MAX_TEMP);
        int range = clamp(snapshot.getRange(), 0, 1200);
        double value = baseTemp + Math.sin(phaseIndex + deviceIndex * phaseGap) * range;
        return clamp((int) Math.round(value), MIN_TEMP, MAX_TEMP);
    }

    private List<DeviceDO> findTargetDevices(String selectedScope) {
        List<DeviceDO> devices = deviceMapper.selectList(null);

        String scope = normalizeScope(selectedScope);
        return devices.stream()
                .filter(this::isLightDevice)
                .filter(device -> SCOPE_ALL.equals(scope) || scope.equals(normalizeScope(device.getDisplayName())))
                .sorted(Comparator
                        .comparing((DeviceDO device) -> normalizeScope(device.getDisplayName()))
                        .thenComparingInt(device -> parseDeviceNo(device.getDeviceNo()))
                        .thenComparing(device -> device.getChipId() == null ? "" : device.getChipId()))
                .toList();
    }

    private boolean isLightDevice(DeviceDO device) {
        String deviceType = device.getDeviceType();
        if (deviceType == null) {
            return false;
        }
        String normalized = deviceType.trim().toLowerCase(Locale.ROOT);
        return "lamp".equals(normalized) || "camlamp".equals(normalized);
    }

    private LightEffectStateRespVO mergeState(LightEffectStateRespVO current, LightEffectStateReqVO reqVO) {
        LightEffectStateRespVO next = copyState(current == null ? defaultState() : current);
        if (reqVO == null) {
            next.setUpdateTime(LocalDateTime.now());
            return next;
        }

        next.setEffect(normalizeEffect(reqVO.getEffect()));
        if (reqVO.getEnabled() != null) {
            next.setEnabled(reqVO.getEnabled());
        }
        if (reqVO.getBaseTemp() != null) {
            next.setBaseTemp(clamp(reqVO.getBaseTemp(), MIN_TEMP, MAX_TEMP));
        }
        if (reqVO.getRange() != null) {
            next.setRange(clamp(reqVO.getRange(), 0, 1200));
        }
        if (reqVO.getSpeed() != null) {
            next.setSpeed(clamp(reqVO.getSpeed(), 0.2D, 5D));
        }
        if (reqVO.getBrightness() != null) {
            next.setBrightness(clamp(reqVO.getBrightness(), 0, 100));
        }
        if (reqVO.getPhaseIndex() != null) {
            next.setPhaseIndex(reqVO.getPhaseIndex());
        }
        if (reqVO.getPhaseGap() != null) {
            next.setPhaseGap(clamp(reqVO.getPhaseGap(), 0D, 3D));
        }
        if (reqVO.getSelectedScope() != null && !reqVO.getSelectedScope().isBlank()) {
            next.setSelectedScope(reqVO.getSelectedScope().trim());
        }

        next.setUpdateTime(LocalDateTime.now());
        return next;
    }

    private LightEffectStateRespVO defaultState() {
        LightEffectStateRespVO respVO = new LightEffectStateRespVO();
        respVO.setEffect(EFFECT_WAVE);
        respVO.setEnabled(false);
        respVO.setBaseTemp(3800);
        respVO.setRange(500);
        respVO.setSpeed(1D);
        respVO.setBrightness(70);
        respVO.setPhaseIndex(0D);
        respVO.setPhaseGap(0.8D);
        respVO.setSelectedScope(SCOPE_ALL);
        respVO.setUpdateTime(LocalDateTime.now());
        return respVO;
    }

    private LightEffectStateRespVO copyState(LightEffectStateRespVO source) {
        LightEffectStateRespVO target = new LightEffectStateRespVO();
        target.setEffect(source.getEffect());
        target.setEnabled(source.getEnabled());
        target.setBaseTemp(source.getBaseTemp());
        target.setRange(source.getRange());
        target.setSpeed(source.getSpeed());
        target.setBrightness(source.getBrightness());
        target.setPhaseIndex(source.getPhaseIndex());
        target.setPhaseGap(source.getPhaseGap());
        target.setSelectedScope(source.getSelectedScope());
        target.setUpdateTime(source.getUpdateTime());
        return target;
    }

    private String normalizeEffect(String effect) {
        if (effect == null || effect.isBlank()) {
            return EFFECT_WAVE;
        }
        return effect.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return SCOPE_ALL;
        }
        return scope.trim().toLowerCase(Locale.ROOT);
    }

    private int resolveIntervalMs(Double speed) {
        double normalizedSpeed = speed == null || speed <= 0 ? 1D : speed;
        return clamp((int) Math.round(BASE_INTERVAL_MS / normalizedSpeed), MIN_INTERVAL_MS, MAX_INTERVAL_MS);
    }

    private double safeDouble(Double value, double fallback) {
        return value == null || !Double.isFinite(value) ? fallback : value;
    }

    private int parseDeviceNo(String value) {
        if (value == null || value.isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private int clamp(Integer value, int min, int max) {
        int next = value == null ? min : value;
        return Math.min(Math.max(next, min), max);
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
}
