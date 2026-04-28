<template>
  <div class="settings-card gimbal-panel">
    <div class="panel-header">
      <div>
        <h2 class="settings-title">🎯 云台控制</h2>
        <p class="panel-desc">
          控制摄像头云台或灯光云台方向，支持服装采集与人流追踪预设姿态
        </p>
      </div>
    </div>

    <div class="form-row">
      <label>选择设备：</label>
      <BaseSelect
        v-model="selectedDeviceCode"
        :options="cameraDeviceOptions"
        placeholder="请选择摄像头灯设备"
      />
    </div>

    <div class="form-row">
      <label>控制对象：</label>
      <div class="target-tabs">
        <button
          class="target-tab"
          :class="{ active: controlTarget === 'camera' }"
          @click="controlTarget = 'camera'"
        >
          摄像头云台
        </button>
        <button
          class="target-tab"
          :class="{ active: controlTarget === 'lamp' }"
          @click="controlTarget = 'lamp'"
        >
          灯光云台
        </button>
      </div>
    </div>

    <div class="form-row">
      <label>动作速度：</label>
      <div class="speed-tabs">
        <button
          v-for="item in speedOptions"
          :key="item.value"
          class="speed-tab"
          :class="{ active: speed === item.value }"
          @click="speed = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="gimbal-layout">
      <div class="direction-pad">
        <button
          class="dir-btn up"
          :disabled="submitting"
          @click="send('up')"
        >
          ⬆
        </button>

        <button
          class="dir-btn left"
          :disabled="submitting"
          @click="send('left')"
        >
          ⬅
        </button>

        <button
          class="dir-btn center"
          :disabled="submitting"
          @click="send('center')"
        >
          居中
        </button>

        <button
          class="dir-btn right"
          :disabled="submitting"
          @click="send('right')"
        >
          ➡
        </button>

        <button
          class="dir-btn down"
          :disabled="submitting"
          @click="send('down')"
        >
          ⬇
        </button>
      </div>

      <div class="preset-panel">
        <button
          class="preset-btn"
          :disabled="submitting"
          @click="send('cloth')"
        >
          <strong>服装采集姿态</strong>
          <span>摄像头朝向灯下服装区域</span>
        </button>

        <button
          class="preset-btn"
          :disabled="submitting"
          @click="send('flow')"
        >
          <strong>人流追踪姿态</strong>
          <span>摄像头朝向顾客活动区域</span>
        </button>

        <button
          class="preset-btn"
          :disabled="submitting"
          @click="send('home')"
        >
          <strong>归位</strong>
          <span>云台回到默认初始角度</span>
        </button>
      </div>
    </div>

    <div class="result-block">
      <div v-if="errorText" class="error-text">{{ errorText }}</div>
      <div v-else class="device-meta">{{ statusText }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { armControl } from '../../api/device'
import type { DeviceItem } from '../../types/device'
import BaseSelect from '../common/BaseSelect.vue'

const props = defineProps<{
  devices: DeviceItem[]
}>()

type ControlTarget = 'camera' | 'lamp'
type GimbalSpeed = 'slow' | 'normal' | 'fast'

const selectedDeviceCode = ref('')
const controlTarget = ref<ControlTarget>('camera')
const speed = ref<GimbalSpeed>('normal')
const submitting = ref(false)
const errorText = ref('')
const statusText = ref('请选择设备后发送云台控制指令')

const speedOptions: { label: string; value: GimbalSpeed }[] = [
  { label: '慢', value: 'slow' },
  { label: '中', value: 'normal' },
  { label: '快', value: 'fast' },
]

const cameraDevices = computed(() => {
  return (props.devices || []).filter(device => {
    const type = String(device.deviceType || '')
      .replace(/[-_\s]/g, '')
      .toLowerCase()

    return type === 'camlamp'
  })
})

const cameraDeviceOptions = computed(() => {
  return cameraDevices.value.map(device => ({
    label: buildDeviceLabel(device),
    value: getDeviceCode(device),
  }))
})

function getDeviceCode(device: Partial<DeviceItem> | any) {
  return String(device?.chipId || device?.deviceCode || '').trim()
}

function buildDeviceLabel(device: DeviceItem) {
  const zoneName = device.displayName || '未分区'
  const no = device.deviceNo ? `灯具-${device.deviceNo}` : '未编号'
  const chipId = device.chipId || '未知芯片'

  return `${zoneName} · ${no} · ${chipId}`
}

function getActionText(action: string) {
  const map: Record<string, string> = {
    up: '上',
    down: '下',
    left: '左',
    right: '右',
    center: '居中',
    cloth: '服装采集姿态',
    flow: '人流追踪姿态',
    home: '归位',
  }

  return map[action] || action
}

function getTargetText(target: ControlTarget) {
  return target === 'camera' ? '摄像头云台' : '灯光云台'
}

async function send(action: string) {
  errorText.value = ''

  if (!selectedDeviceCode.value) {
    errorText.value = '请先选择设备'
    return
  }

  submitting.value = true

  try {
    const command = `${controlTarget.value}:${action}:${speed.value}`

    await armControl(selectedDeviceCode.value, command)

    statusText.value = `已发送：${getTargetText(controlTarget.value)} / ${getActionText(action)} / ${speed.value}`
  } catch (error) {
    console.error('gimbal control error =', error)
    errorText.value = '发送云台控制指令失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.gimbal-panel {
  overflow: visible;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-desc {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}

.form-row label {
  flex: 0 0 88px;
  color: #606266;
  font-size: 14px;
}

.target-tabs,
.speed-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.target-tab,
.speed-tab {
  border: 1px solid rgba(203, 213, 225, 0.95);
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.86);
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease;
}

.target-tab:hover,
.speed-tab:hover {
  transform: translateY(-1px);
}

.target-tab.active,
.speed-tab.active {
  background: rgba(64, 158, 255, 0.14);
  border-color: rgba(64, 158, 255, 0.55);
  color: #2563eb;
}

.gimbal-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.direction-pad {
  display: grid;
  grid-template-columns: repeat(3, 64px);
  grid-template-rows: repeat(3, 52px);
  gap: 8px;
  justify-content: center;
  align-content: center;
  padding: 14px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.dir-btn {
  border: none;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  color: #2563eb;
  font-size: 18px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    background 0.15s ease;
}

.dir-btn:hover {
  transform: translateY(-1px);
  background: #fff;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.14);
}

.dir-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.dir-btn.up {
  grid-column: 2;
  grid-row: 1;
}

.dir-btn.left {
  grid-column: 1;
  grid-row: 2;
}

.dir-btn.center {
  grid-column: 2;
  grid-row: 2;
  font-size: 13px;
}

.dir-btn.right {
  grid-column: 3;
  grid-row: 2;
}

.dir-btn.down {
  grid-column: 2;
  grid-row: 3;
}

.preset-panel {
  display: grid;
  gap: 10px;
}

.preset-btn {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 16px;
  padding: 13px 14px;
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    border-color 0.16s ease;
}

.preset-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(64, 158, 255, 0.45);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.1);
}

.preset-btn strong {
  display: block;
  color: #1e293b;
  font-size: 14px;
  font-weight: 900;
}

.preset-btn span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.result-block {
  margin-top: 14px;
}

.error-text {
  color: #f53f3f;
  font-size: 13px;
}

.device-meta {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .form-row label {
    flex: none;
  }

  .gimbal-layout {
    grid-template-columns: 1fr;
  }

  .direction-pad {
    grid-template-columns: repeat(3, minmax(56px, 1fr));
  }
}
</style>