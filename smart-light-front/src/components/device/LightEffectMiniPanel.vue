<template>
  <div class="light-effect-mini-card">
    <div class="mini-header">
      <div>
        <div class="mini-title">灯效控制</div>
        <div class="mini-subtitle">按区域快速调整灯光效果</div>
      </div>
    </div>

    <div class="scope-field">
      <span class="mini-label">范围</span>
      <BaseSelect
        v-model="selectedScope"
        :options="scopeOptions"
        placeholder="请选择范围"
      />
    </div>

    <div class="effect-action-grid">
      <button
        v-for="action in quickActions"
        :key="action.key"
        class="effect-action-btn"
        :class="[`effect-${action.key}`, { active: activeEffect === action.key }]"
        type="button"
        :disabled="submitting || targetDevices.length === 0"
        @click="handleQuickAction(action.key)"
      >
        <strong>{{ action.label }}</strong>
        <span>{{ action.desc }}</span>
      </button>
    </div>

    <div class="mini-status">
      {{ statusText }}
    </div>

    <Teleport to="body">
      <div
        v-if="showSettings"
        class="effect-modal-overlay"
        :class="{ 'effect-modal-night': isNightMode() }"
        @click.self="closeSettings"
      >
        <div class="effect-modal-card">
          <div class="effect-modal-header">
            <div>
              <h3>循环设置</h3>
              <p>调整当前范围内灯具的流水参数</p>
            </div>
            <button class="modal-close-btn" type="button" @click="closeSettings">关闭</button>
          </div>

          <div class="effect-form">
            <label class="form-field">
              <span class="mini-label">基础色温</span>
              <input
                v-model.number="baseTemp"
                class="mini-input"
                type="number"
                min="2700"
                max="6500"
              />
            </label>

            <label class="form-field">
              <span class="mini-label">波动范围</span>
              <input
                v-model.number="range"
                class="mini-input"
                type="number"
                min="0"
                max="1200"
              />
            </label>

            <label class="form-field">
              <span class="mini-label">亮度</span>
              <input
                v-model.number="brightness"
                class="mini-input"
                type="number"
                min="0"
                max="100"
              />
            </label>
          </div>

          <div class="effect-modal-actions">
            <button class="modal-btn secondary" type="button" @click="saveSettings">
              保存设置
            </button>
            <button
              class="modal-btn primary"
              type="button"
              :disabled="submitting || targetDevices.length === 0"
              @click="startWave"
            >
              开启循环
            </button>
            <button
              class="modal-btn danger"
              type="button"
              :disabled="submitting || targetDevices.length === 0"
              @click="stopWave"
            >
              停止循环
            </button>
            <button class="modal-btn ghost" type="button" @click="closeSettings">
              取消
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { sendLightEffect, updateDevice } from '../../api/device'
import type { DeviceCreatePayload, DeviceItem } from '../../types/device'
import BaseSelect from '../common/BaseSelect.vue'

const props = defineProps<{
  devices: DeviceItem[]
}>()

type ActiveEffect = 'warm' | 'neutral' | 'cool' | 'auto' | 'loop' | null
type QuickActionKey = NonNullable<ActiveEffect> | 'settings'

const selectedScope = ref('all')
const baseTemp = ref(3800)
const range = ref(500)
const brightness = ref(70)
const speed = ref(1)
const submitting = ref(false)
const showSettings = ref(false)
const activeEffect = ref<ActiveEffect>(null)
const statusText = ref('未启动')

const quickActions: Array<{ key: QuickActionKey; label: string; desc: string }> = [
  { key: 'warm', label: '暖光', desc: '3000K' },
  { key: 'neutral', label: '中性白', desc: '4000K' },
  { key: 'cool', label: '冷白', desc: '6000K' },
  { key: 'auto', label: '自动模式', desc: '智能调节' },
  { key: 'loop', label: '循环效果', desc: '动态流光' },
  { key: 'settings', label: '循环设置', desc: '参数配置' },
]

const zoneNames = computed(() => {
  const set = new Set<string>()

  for (const device of props.devices || []) {
    set.add(getZoneName(device))
  }

  return Array.from(set)
})

const scopeOptions = computed(() => {
  return [
    {
      label: '全局',
      value: 'all',
    },
    ...zoneNames.value.map(zone => ({
      label: zone,
      value: zone,
    })),
  ]
})

const targetDevices = computed(() => {
  const list = props.devices || []

  if (selectedScope.value === 'all') {
    return [...list].sort((a, b) => {
      const zoneA = getZoneName(a)
      const zoneB = getZoneName(b)
      const zoneCompare = zoneA.localeCompare(zoneB, 'zh-Hans-CN')
      if (zoneCompare !== 0) return zoneCompare

      return Number(a.deviceNo || 9999) - Number(b.deviceNo || 9999)
    })
  }

  return list
    .filter(device => getZoneName(device) === selectedScope.value)
    .sort((a, b) => Number(a.deviceNo || 9999) - Number(b.deviceNo || 9999))
})

function getZoneName(device: DeviceItem) {
  return device.displayName?.trim() || '未分区'
}

function clamp(num: number, min: number, max: number) {
  return Math.min(Math.max(num, min), max)
}

function buildDevicePayload(device: DeviceItem, next: Partial<DeviceCreatePayload>): DeviceCreatePayload {
  return {
    chipId: device.chipId || '',
    ip: device.ip || '',
    displayName: device.displayName || '',
    deviceType: device.deviceType || '',
    deviceNo: device.deviceNo || '',
    brightness: next.brightness ?? device.brightness ?? 70,
    temp: next.temp ?? device.temp ?? 4000,
    autoMode: next.autoMode ?? device.autoMode ?? false,
    recommendedBrightness: next.recommendedBrightness ?? device.recommendedBrightness ?? 70,
    recommendedTemp: next.recommendedTemp ?? device.recommendedTemp ?? 4000,
    fabric: device.fabric || '',
    mainColorRgb: device.mainColorRgb || '',
  }
}

async function handleQuickAction(action: QuickActionKey) {
  if (action === 'settings') {
    showSettings.value = true
    return
  }

  if (activeEffect.value === action) {
    await cancelEffect(action)
    return
  }

  if (action === 'loop') {
    await startWave()
    return
  }

  if (action === 'auto') {
    await applyDeviceMode({
      autoMode: true,
      recommendedBrightness: brightness.value,
      recommendedTemp: baseTemp.value,
    }, '自动模式', 'auto')
    return
  }

  const tempMap: Record<'warm' | 'neutral' | 'cool', number> = {
    warm: 3000,
    neutral: 4000,
    cool: 6000,
  }

  const labelMap: Record<'warm' | 'neutral' | 'cool', string> = {
    warm: '暖光',
    neutral: '中性白',
    cool: '冷白',
  }

  await applyDeviceMode({
    temp: tempMap[action],
    brightness: brightness.value,
    autoMode: false,
    recommendedTemp: tempMap[action],
    recommendedBrightness: brightness.value,
  }, labelMap[action], action)
}

async function cancelEffect(effect: NonNullable<ActiveEffect>) {
  if (effect === 'loop') {
    await stopWave()
    return
  }

  if (effect === 'auto') {
    await applyDeviceMode({ autoMode: false }, '自动模式已关闭', null)
    return
  }

  const labelMap: Record<'warm' | 'neutral' | 'cool', string> = {
    warm: '暖光',
    neutral: '中性白',
    cool: '冷白',
  }

  activeEffect.value = null
  statusText.value = `已取消${labelMap[effect]}`
}

async function applyDeviceMode(
  next: Partial<DeviceCreatePayload>,
  label: string,
  nextActiveEffect: ActiveEffect,
) {
  submitting.value = true

  try {
    for (const device of targetDevices.value) {
      if (!device.id) continue
      await updateDevice(device.id, buildDevicePayload(device, next))
      if (typeof next.temp === 'number') device.temp = next.temp
      if (typeof next.brightness === 'number') device.brightness = next.brightness
      if (typeof next.autoMode === 'boolean') device.autoMode = next.autoMode
      if (typeof next.recommendedTemp === 'number') device.recommendedTemp = next.recommendedTemp
      if (typeof next.recommendedBrightness === 'number') device.recommendedBrightness = next.recommendedBrightness
    }

    activeEffect.value = nextActiveEffect
    statusText.value = nextActiveEffect === null
      ? label
      : `${label}已应用 ${targetDevices.value.length} 盏`
  } catch (error) {
    console.error('apply device mode error =', error)
    statusText.value = nextActiveEffect === null ? '关闭失败' : `${label}应用失败`
  } finally {
    submitting.value = false
  }
}

function saveSettings() {
  baseTemp.value = clamp(baseTemp.value, 2700, 6500)
  range.value = clamp(range.value, 0, 1200)
  brightness.value = clamp(brightness.value, 0, 100)
  statusText.value = '循环设置已保存'
}

function closeSettings() {
  showSettings.value = false
}

function isNightMode() {
  return localStorage.getItem('SMART_LIGHT_NIGHT_MODE') === '1'
}

async function startWave() {
  submitting.value = true

  try {
    for (let index = 0; index < targetDevices.value.length; index++) {
      const device = targetDevices.value[index]
      if (!device.chipId) continue

      await sendLightEffect(device.chipId, {
        effect: 'wave',
        enabled: true,
        baseTemp: clamp(baseTemp.value, 2700, 6500),
        range: clamp(range.value, 0, 1200),
        speed: clamp(speed.value, 0.2, 5),
        brightness: clamp(brightness.value, 0, 100),
        phaseIndex: index,
        phaseGap: 0.8,
      })
    }

    activeEffect.value = 'loop'
    statusText.value = `循环已启动 ${targetDevices.value.length} 盏`
  } catch (error) {
    console.error('start wave effect error =', error)
    statusText.value = '开启失败'
  } finally {
    submitting.value = false
  }
}

async function stopWave() {
  submitting.value = true

  try {
    for (const device of targetDevices.value) {
      if (!device.chipId) continue

      await sendLightEffect(device.chipId, {
        effect: 'wave',
        enabled: false,
      })
    }

    activeEffect.value = null
    statusText.value = '循环已停止'
  } catch (error) {
    console.error('stop wave effect error =', error)
    statusText.value = '停止失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.light-effect-mini-card {
  width: 100%;
  min-height: 100%;
  box-sizing: border-box;
  padding: 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.10);
}

.mini-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.mini-title {
  font-size: 22px;
  font-weight: 900;
  color: #0f172a;
  line-height: 1.2;
}

.mini-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
  font-weight: 700;
}

.scope-field {
  display: block;
  margin-bottom: 12px;
}

.effect-action-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 13px;
}

.effect-action-btn {
  position: relative;
  min-height: 70px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  color: #1e293b;
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    border-color 0.16s ease,
    background 0.16s ease;
}

.effect-action-btn strong,
.effect-action-btn span {
  position: relative;
  z-index: 1;
  display: block;
}

.effect-action-btn strong {
  font-size: 15px;
  line-height: 1.15;
  font-weight: 900;
}

.effect-action-btn span {
  margin-top: 7px;
  color: rgba(71, 85, 105, 0.82);
  font-size: 12px;
  line-height: 1.15;
  font-weight: 800;
}

.effect-action-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 12px 22px rgba(37, 99, 235, 0.13);
}

.effect-action-btn.active {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.52);
  box-shadow:
    0 12px 26px rgba(37, 99, 235, 0.18),
    inset 0 0 0 1px rgba(255, 255, 255, 0.56);
}

.effect-action-btn.active::before {
  content: "已启用";
  position: absolute;
  right: 10px;
  bottom: 9px;
  z-index: 2;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #ffffff;
  font-size: 10px;
  line-height: 1.4;
  font-weight: 900;
  pointer-events: none;
}

.effect-warm {
  background: linear-gradient(135deg, rgba(255, 237, 213, 0.95), rgba(251, 191, 36, 0.28));
  border-color: rgba(245, 158, 11, 0.28);
}

.effect-warm.active {
  border-color: rgba(245, 158, 11, 0.75);
  box-shadow:
    0 12px 28px rgba(245, 158, 11, 0.25),
    0 0 0 2px rgba(245, 158, 11, 0.12);
}

.effect-neutral {
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.98), rgba(226, 232, 240, 0.65));
  border-color: rgba(148, 163, 184, 0.28);
}

.effect-neutral.active {
  border-color: rgba(100, 116, 139, 0.65);
  box-shadow:
    0 12px 26px rgba(100, 116, 139, 0.18),
    0 0 0 2px rgba(100, 116, 139, 0.1);
}

.effect-cool {
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(147, 197, 253, 0.32));
  border-color: rgba(59, 130, 246, 0.26);
}

.effect-cool.active {
  border-color: rgba(59, 130, 246, 0.7);
  box-shadow:
    0 12px 28px rgba(59, 130, 246, 0.22),
    0 0 0 2px rgba(59, 130, 246, 0.11);
}

.effect-auto {
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.96), rgba(129, 140, 248, 0.28));
  border-color: rgba(99, 102, 241, 0.24);
}

.effect-auto::after {
  content: "AUTO";
  position: absolute;
  right: 10px;
  top: 9px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.14);
  color: #4f46e5;
  font-size: 10px;
  font-weight: 900;
}

.effect-auto.active {
  border-color: rgba(99, 102, 241, 0.72);
  box-shadow:
    0 12px 28px rgba(99, 102, 241, 0.24),
    0 0 0 2px rgba(99, 102, 241, 0.12);
}

.effect-loop {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.16), rgba(168, 85, 247, 0.22), rgba(14, 165, 233, 0.16));
  border-color: rgba(99, 102, 241, 0.28);
}

.effect-loop::after {
  content: "";
  position: absolute;
  right: 12px;
  top: 14px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  border: 2px solid rgba(99, 102, 241, 0.48);
  border-left-color: transparent;
}

.effect-loop.active {
  position: relative;
  overflow: hidden;
  border-color: rgba(99, 102, 241, 0.72);
  background: linear-gradient(120deg, rgba(59, 130, 246, 0.22), rgba(168, 85, 247, 0.26), rgba(14, 165, 233, 0.22), rgba(59, 130, 246, 0.22));
  background-size: 220% 220%;
  animation: loopFlow 3s ease infinite;
  box-shadow: 0 14px 34px rgba(99, 102, 241, 0.28);
}

.effect-loop.active::before {
  content: "";
  position: absolute;
  left: 50%;
  top: 50%;
  width: 320%;
  aspect-ratio: 1 / 1;
  transform: translate(-50%, -50%) rotate(0deg);
  transform-origin: 50% 50%;
  z-index: 0;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    rgba(59, 130, 246, 0.12),
    rgba(168, 85, 247, 0.45),
    rgba(14, 165, 233, 0.38),
    rgba(59, 130, 246, 0.12)
  );
  animation: loopBorderSpin 3s linear infinite;
  opacity: 0.48;
  pointer-events: none;
}

.effect-loop.active::after {
  display: none;
}

.effect-loop.active > * {
  position: relative;
  z-index: 1;
}

.effect-settings {
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.92), rgba(219, 234, 254, 0.52));
  border-color: rgba(148, 163, 184, 0.24);
}

.effect-settings.active::before {
  display: none;
}

@keyframes loopFlow {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@keyframes loopBorderSpin {
  from {
    transform: translate(-50%, -50%) rotate(0deg);
  }
  to {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

.effect-action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.mini-status {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
  font-weight: 700;
}

.effect-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
}

.effect-modal-card {
  width: min(480px, 100%);
  max-height: min(640px, calc(100vh - 48px));
  overflow-y: auto;
  box-sizing: border-box;
  padding: 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.28);
}

.effect-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.effect-modal-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 900;
}

.effect-modal-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.modal-close-btn {
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.92);
  color: #475569;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.effect-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-field:last-child {
  grid-column: 1 / -1;
}

.form-field {
  min-width: 0;
}

.mini-label {
  display: block;
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 800;
  color: #64748b;
}

.mini-input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 10px;
  border-radius: 11px;
  border: 1px solid #dbe3ef;
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  font-size: 14px;
  outline: none;
}

.effect-modal-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.modal-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 15px;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.modal-btn.primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
}

.modal-btn.secondary {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
}

.modal-btn.danger {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
}

.modal-btn.ghost {
  background: rgba(100, 116, 139, 0.12);
  color: #475569;
}

.modal-btn:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

:global(.app-container.night-mode) .light-effect-mini-card {
  background: rgba(15, 23, 42, 0.72);
  border-color: rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
  filter: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

:global(.app-container.night-mode) .mini-title {
  color: rgba(248, 250, 252, 0.96);
}

:global(.app-container.night-mode) .mini-subtitle,
:global(.app-container.night-mode) .mini-status,
:global(.app-container.night-mode) .mini-label {
  color: rgba(203, 213, 225, 0.72);
}

:global(.app-container.night-mode) .effect-action-btn {
  background: rgba(30, 41, 59, 0.82);
  border-color: rgba(148, 163, 184, 0.22);
  color: rgba(226, 232, 240, 0.9);
  box-shadow: none;
}

:global(.app-container.night-mode) .effect-action-btn span {
  color: rgba(203, 213, 225, 0.72);
}

:global(.app-container.night-mode) .effect-action-btn:hover,
:global(.app-container.night-mode) .effect-action-btn.active {
  background: rgba(37, 99, 235, 0.24);
  border-color: rgba(96, 165, 250, 0.42);
  color: #bfdbfe;
}

:global(.app-container.night-mode) .effect-action-btn.active::before {
  background: rgba(191, 219, 254, 0.14);
  color: #dbeafe;
}

:global(.app-container.night-mode) .effect-action-btn.active span {
  color: rgba(219, 234, 254, 0.82);
}

:global(.app-container.night-mode) .effect-warm {
  background: linear-gradient(135deg, rgba(67, 42, 12, 0.9), rgba(245, 158, 11, 0.22));
  border-color: rgba(245, 158, 11, 0.34);
}

:global(.app-container.night-mode) .effect-warm.active {
  border-color: rgba(251, 191, 36, 0.66);
  box-shadow:
    0 12px 28px rgba(245, 158, 11, 0.25),
    0 0 0 2px rgba(251, 191, 36, 0.14);
}

:global(.app-container.night-mode) .effect-neutral {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.92), rgba(71, 85, 105, 0.42));
  border-color: rgba(148, 163, 184, 0.26);
}

:global(.app-container.night-mode) .effect-cool {
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(37, 99, 235, 0.26));
  border-color: rgba(96, 165, 250, 0.34);
}

:global(.app-container.night-mode) .effect-auto {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.92), rgba(99, 102, 241, 0.28));
  border-color: rgba(129, 140, 248, 0.34);
}

:global(.app-container.night-mode) .effect-auto::after {
  background: rgba(129, 140, 248, 0.22);
  color: #c7d2fe;
}

:global(.app-container.night-mode) .effect-loop {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.24), rgba(168, 85, 247, 0.28), rgba(14, 165, 233, 0.2));
  border-color: rgba(129, 140, 248, 0.4);
}

:global(.app-container.night-mode) .effect-loop::after {
  border-color: rgba(191, 219, 254, 0.68);
  border-left-color: transparent;
}

:global(.app-container.night-mode) .effect-loop.active {
  background: linear-gradient(120deg, rgba(37, 99, 235, 0.3), rgba(168, 85, 247, 0.34), rgba(14, 165, 233, 0.26), rgba(37, 99, 235, 0.3));
  background-size: 220% 220%;
  box-shadow: 0 14px 34px rgba(99, 102, 241, 0.32);
}

:global(.app-container.night-mode) .effect-settings {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.9), rgba(51, 65, 85, 0.58));
  border-color: rgba(148, 163, 184, 0.26);
}

.effect-modal-night .effect-modal-card {
  background: rgba(15, 23, 42, 0.94);
  border-color: rgba(148, 163, 184, 0.22);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.48);
}

.effect-modal-night .effect-modal-header h3 {
  color: rgba(248, 250, 252, 0.96);
}

.effect-modal-night .effect-modal-header p,
.effect-modal-night .mini-label {
  color: rgba(203, 213, 225, 0.72);
}

.effect-modal-night .mini-input {
  background: rgba(15, 23, 42, 0.76);
  border-color: rgba(148, 163, 184, 0.28);
  color: rgba(226, 232, 240, 0.92);
}

.effect-modal-night .modal-close-btn,
.effect-modal-night .modal-btn.ghost {
  background: rgba(30, 41, 59, 0.82);
  border-color: rgba(148, 163, 184, 0.24);
  color: rgba(226, 232, 240, 0.9);
}

.effect-modal-night .modal-btn.secondary {
  background: rgba(37, 99, 235, 0.24);
  color: #bfdbfe;
}

.effect-modal-night .modal-btn.danger {
  background: rgba(127, 29, 29, 0.28);
  color: #fecaca;
}

@media (max-width: 768px) {
  .effect-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 9px;
  }

  .effect-form {
    grid-template-columns: 1fr;
  }

  .effect-action-btn {
    min-height: 78px;
    padding: 11px 12px;
    border-radius: 16px;
  }

  .effect-action-btn strong {
    font-size: 14px;
  }

  .effect-action-btn span {
    margin-top: 6px;
    font-size: 11px;
  }

  .effect-action-btn.active::before {
    right: 8px;
    bottom: 7px;
    padding: 1px 6px;
    font-size: 9px;
  }

  .effect-modal-card {
    padding: 18px;
  }

  .effect-modal-actions {
    justify-content: stretch;
  }

  .modal-btn {
    flex: 1 1 140px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .effect-loop.active,
  .effect-loop.active::before {
    animation: none;
  }
}
</style>
