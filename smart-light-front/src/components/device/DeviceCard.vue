<template>
  <div class="lamp-card">
    <div class="card-header">
      <h3>{{ localForm.deviceCode }}</h3>
      <span class="status-badge" :class="{ online: device.online, offline: !device.online }">
        {{ device.online ? '在线' : '离线' }}
      </span>
    </div>

    <p class="device-meta">IP：{{ localForm.ip || '未设置' }}</p>
    <p class="device-meta">设备ID：{{ device.id }}</p>

    <label class="field-label">亮度：{{ displayBrightness }}</label>
    <input
      :value="sliderBrightnessValue"
      type="range"
      min="0"
      max="100"
      :disabled="!!localForm.autoMode"
      @input="handleBrightnessInput"
    />

    <label class="field-label">色温：{{ displayTemp }}</label>
    <input
      :value="sliderTempValue"
      type="range"
      min="2700"
      max="6500"
      step="100"
      :disabled="!!localForm.autoMode"
      @input="handleTempInput"
    />

    <label class="checkbox-row">
      <input
        v-model="localForm.autoMode"
        type="checkbox"
        @change="handleAutoModeChange"
      />
      自动模式
    </label>

    <label class="field-label">面料</label>
    <div class="readonly-box">
      {{ localForm.fabric || '未知' }}
    </div>

    <div
      class="color-box"
      :style="{
        background: rgbStyle,
        color: textColor,
      }"
    >
      {{ localForm.mainColorRgb || '暂无主色' }}
    </div>

    <div class="card-actions">
      <button class="btn-secondary" @click="resetForm">重置</button>
      <button class="btn-danger" :disabled="deleting" @click="handleDelete">
        {{ deleting ? '删除中...' : '删除' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { DeviceCreatePayload, DeviceItem } from '../../types/device'

const props = defineProps<{
  device: DeviceItem
  deleting?: boolean
}>()

const emit = defineEmits<{
  (e: 'update-realtime', value: { id: number; payload: DeviceCreatePayload }): void
  (e: 'delete', id: number): void
}>()

const localForm = reactive<DeviceCreatePayload>({
  deviceCode: '',
  ip: '',
  brightness: 50,
  temp: 4000,
  autoMode: false,
  recommendedBrightness: 50,
  recommendedTemp: 4000,
  fabric: '',
  mainColorRgb: '',
})

function syncFromProps() {
  localForm.deviceCode = props.device.deviceCode
  localForm.ip = props.device.ip || ''
  localForm.brightness = props.device.brightness ?? 50
  localForm.temp = props.device.temp ?? 4000
  localForm.autoMode = props.device.autoMode ?? false
  localForm.recommendedBrightness = props.device.recommendedBrightness ?? 50
  localForm.recommendedTemp = props.device.recommendedTemp ?? 4000
  localForm.fabric = props.device.fabric || ''
  localForm.mainColorRgb = props.device.mainColorRgb || ''
}

watch(
  () => props.device,
  () => {
    syncFromProps()
  },
  { immediate: true, deep: true },
)

function resetForm() {
  syncFromProps()
}

function emitRealtimeUpdate() {
  emit('update-realtime', {
    id: props.device.id,
    payload: {
      deviceCode: localForm.deviceCode,
      ip: localForm.ip,
      brightness: localForm.brightness,
      temp: localForm.temp,
      autoMode: localForm.autoMode,
      recommendedBrightness: localForm.recommendedBrightness,
      recommendedTemp: localForm.recommendedTemp,
    },
  })
}

function handleBrightnessInput(event: Event) {
  if (localForm.autoMode) return
  const target = event.target as HTMLInputElement
  localForm.brightness = Number(target.value)
  emitRealtimeUpdate()
}

function handleTempInput(event: Event) {
  if (localForm.autoMode) return
  const target = event.target as HTMLInputElement
  localForm.temp = Number(target.value)
  emitRealtimeUpdate()
}

function handleAutoModeChange() {
  emitRealtimeUpdate()
}

function handleDelete() {
  if (!window.confirm(`确认删除设备 ${props.device.deviceCode} 吗？`)) return
  emit('delete', props.device.id)
}

const displayBrightness = computed(() => {
  return localForm.autoMode
    ? (localForm.recommendedBrightness ?? localForm.brightness ?? 0)
    : (localForm.brightness ?? 0)
})

const displayTemp = computed(() => {
  return localForm.autoMode
    ? (localForm.recommendedTemp ?? localForm.temp ?? 4000)
    : (localForm.temp ?? 4000)
})

const sliderBrightnessValue = computed(() => {
  return localForm.autoMode
    ? (localForm.recommendedBrightness ?? localForm.brightness ?? 0)
    : (localForm.brightness ?? 0)
})

const sliderTempValue = computed(() => {
  return localForm.autoMode
    ? (localForm.recommendedTemp ?? localForm.temp ?? 4000)
    : (localForm.temp ?? 4000)
})

const rgbStyle = computed(() => {
  const raw = localForm.mainColorRgb
  if (!raw) return '#888'
  if (raw.startsWith('rgb')) return raw
  return `rgb(${raw})`
})

const textColor = computed(() => {
  const raw = localForm.mainColorRgb
  if (!raw) return '#fff'

  const nums = raw.match(/\d+/g)
  if (!nums || nums.length < 3) return '#fff'

  const [r, g, b] = nums.map(Number)
  const lum = 0.299 * r + 0.587 * g + 0.114 * b
  return lum > 186 ? '#000' : '#fff'
})
</script>