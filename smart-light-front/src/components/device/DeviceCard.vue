<template>
  <div class="lamp-card">
    <div class="card-header clickable-header" @click="openDetailModal">
      <div class="device-title-block">
        <h3>{{ displayNameText }}</h3>
        <p class="last-seen-under-name">
          上次在线：{{ !device.online ? (lastSeenText || '未知') : '当前在线' }}
        </p>
      </div>

      <span class="status-badge" :class="{ online: device.online, offline: !device.online }">
        {{ device.online ? '在线' : '离线' }}
      </span>
    </div>


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

    <label class="field-label">面料：{{ localForm.fabric || '未设置' }}</label>

    <div
      class="color-box"
      :style="{
        background: rgbStyle,
        color: textColor,
      }"
    >
      {{ "RGB(" + (localForm.mainColorRgb || '暂无主色') + ")" }}
    </div>

    <div class="card-actions">
      <button class="btn-secondary" @click="resetForm">重置</button>
      <button class="btn-danger" :disabled="deleting" @click="handleDelete">
        {{ deleting ? '删除中...' : '删除' }}
      </button>
    </div>
  </div>

<Transition name="detail-overlay-fade">
  <div
    v-if="showDetailModal"
    class="device-detail-overlay"
    @click.self="closeDetailModal"
  >
    <Transition name="detail-card-pop" appear>
      <div class="device-detail-modal">
        <div class="detail-modal-header">
          <div>
            <h3>{{ displayNameText }}</h3>
            <p class="detail-subtitle">{{ device.online ? '在线' : '离线' }}</p>
          </div>
          <button class="detail-close-btn" @click="closeDetailModal">×</button>
        </div>

        <div class="detail-info-item">
          <span class="detail-label">设备类型</span>
          <span class="detail-value">{{ displayDeviceType }}</span>
        </div>

        <div class="detail-info-item">
          <span class="detail-label">IP</span>
          <span class="detail-value">{{ localForm.ip || '未设置' }}</span>
        </div>

        <label class="modal-label">用户命名</label>
        <input
          v-model.trim="localForm.displayName"
          class="modal-input"
          type="text"
          placeholder="如 橱窗灯1"
        />

        <label class="modal-label">设备编号</label>
        <input
          v-model.trim="localForm.deviceNo"
          class="modal-input"
          type="text"
          placeholder="如 LIGHT-001"
        />

        <div class="detail-modal-actions">
          <button class="btn-secondary" @click="closeDetailModal">取消</button>
          <button class="btn-primary" @click="saveDeviceBaseInfo">保存</button>
        </div>
      </div>
    </Transition>
  </div>
</Transition>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
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
  chipId: '',
  ip: '',
  displayName: '',
  deviceType: '',
  deviceNo: '',
  brightness: 50,
  temp: 4000,
  autoMode: false,
  recommendedBrightness: 50,
  recommendedTemp: 4000,
  fabric: '',
  mainColorRgb: '',
})

const showDetailModal = ref(false)

function openDetailModal() {
  showDetailModal.value = true
}

function closeDetailModal() {
  showDetailModal.value = false
}

function saveDeviceBaseInfo() {
  emitRealtimeUpdate()
  showDetailModal.value = false
}

const lastSeenText = computed(() => {
  const value = props.device.lastSeen
  if (!value) return ''

  const timestamp = value < 1e12 ? value * 1000 : value
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''

  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')

  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
})

function syncFromProps() {
  localForm.chipId = props.device.chipId
  localForm.ip = props.device.ip || ''
  localForm.displayName = props.device.displayName || ''
  localForm.deviceType = props.device.deviceType || ''
  localForm.deviceNo = props.device.deviceNo || ''
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
      chipId: localForm.chipId,
      ip: localForm.ip || '',
      displayName: localForm.displayName || '',
      deviceType: localForm.deviceType || '',
      deviceNo: localForm.deviceNo || '',
      brightness: localForm.brightness ?? 50,
      temp: localForm.temp ?? 4000,
      autoMode: localForm.autoMode ?? false,
      recommendedBrightness: localForm.recommendedBrightness ?? 50,
      recommendedTemp: localForm.recommendedTemp ?? 4000,
      fabric: localForm.fabric || '',
      mainColorRgb: localForm.mainColorRgb || '',
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
  const targetName = displayNameText.value || displayDeviceNo.value || '该设备'
  if (!window.confirm(`确认删除设备 ${targetName} 吗？`)) return
  emit('delete', props.device.id)
}

const displayNameText = computed(() => {
  return props.device.displayName?.trim() || props.device.deviceNo?.trim() || '未命名设备'
})

const displayDeviceNo = computed(() => {
  return props.device.deviceNo?.trim() || '未设置'
})

const displayDeviceType = computed(() => {
  return props.device.deviceType?.trim() || '未知'
})

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