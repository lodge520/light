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

<div class="ai-actions">
  <template v-if="isLamp">
    <input
      ref="fabricInputRef"
      class="hidden-file-input"
      type="file"
      accept="image/*"
      @change="handleFabricFileChange"
    />

    <button
      class="btn-ai"
      :disabled="fabricLoading"
      @click.stop="openFabricUpload"
    >
      {{ fabricLoading ? '识别中...' : '上传图片识别面料' }}
    </button>
    <button
      v-if="annotatedImageBase64"
      class="btn-ai btn-preview"
      type="button"
      @click.stop="openClothPreviewModal"
    >
      查看分割图
    </button>
  </template>

  <template v-if="isCamLamp">
    <button
      class="btn-ai"
      :class="{ active: flowEnabled }"
      :disabled="flowLoading"
      @click.stop="handleToggleFlowUpload"
    >
      {{
        flowLoading
          ? '下发中...'
          : flowEnabled
            ? '停止人流监测'
            : '开启人流监测'
      }}
    </button>
  </template>
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
<Transition name="detail-overlay-fade">
  <div
    v-if="showClothPreviewModal"
    class="device-detail-overlay"
    @click.self="closeClothPreviewModal"
  >
    <Transition name="detail-card-pop" appear>
      <div class="cloth-preview-modal">
        <div class="detail-modal-header">
          <div>
            <h3>服装区域分割结果</h3>
            <p class="detail-subtitle">
              {{ clothDetected === false ? '未检测到明确服装区域，已使用回退结果' : '已分割服装区域' }}
            </p>
          </div>
          <button class="detail-close-btn" @click="closeClothPreviewModal">×</button>
        </div>

        <img
          class="cloth-preview-image"
          :src="`data:image/jpeg;base64,${annotatedImageBase64}`"
          alt="服装区域分割结果"
        />

        <div class="detail-modal-actions">
          <button class="btn-secondary" @click="closeClothPreviewModal">关闭</button>
        </div>
      </div>
    </Transition>
  </div>
</Transition>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { DeviceCreatePayload, DeviceItem } from '../../types/device'
import { fabricRecognize } from '../../api/ai'
import { setFlowUpload } from '../../api/device'

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
const fabricInputRef = ref<HTMLInputElement | null>(null)
const fabricLoading = ref(false)
const flowLoading = ref(false)
const flowEnabled = ref(false)
const annotatedImageBase64 = ref('')
const clothDetected = ref<boolean | null>(null)
const showClothPreviewModal = ref(false)

function openClothPreviewModal() {
  showClothPreviewModal.value = true
}

function closeClothPreviewModal() {
  showClothPreviewModal.value = false
}

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
  flowEnabled.value = Boolean(
  (props.device as any).flowEnabled ??
  (props.device as any).flowAutoUpload ??
  false
  )
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

function openFabricUpload() {
  fabricInputRef.value?.click()
}

const MAX_IMAGE_SIZE = 20 * 1024 * 1024 // 20MB

async function handleFabricFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) return

  if (file.size > MAX_IMAGE_SIZE) {
    window.alert('图片大小不能超过 20MB，请压缩后再上传')
    input.value = ''
    return
  }

  if (!localForm.chipId) {
    input.value = ''
    return
  }

  fabricLoading.value = true

  try {
    const result = await fabricRecognize(file, localForm.chipId)

    const fabricName = result.fabric || result.label || ''

    if (fabricName) {
      localForm.fabric = fabricName
    }

    if (result.mainColorRgb !== undefined) {
      localForm.mainColorRgb = result.mainColorRgb || ''
    }

    if (result.recommendedBrightness !== undefined) {
      localForm.recommendedBrightness = result.recommendedBrightness
    }

    if (result.recommendedTemp !== undefined) {
      localForm.recommendedTemp = result.recommendedTemp
    }

    if (result.annotatedImageBase64) {
    annotatedImageBase64.value = result.annotatedImageBase64
    }

    if (result.clothDetected !== undefined) {
      clothDetected.value = result.clothDetected
    }

    emitRealtimeUpdate()
  } catch (error) {
    console.error('面料识别失败：', error)
  } finally {
    fabricLoading.value = false
    input.value = ''
  }
}

async function handleToggleFlowUpload() {
  if (!localForm.chipId) {
    //window.alert('设备缺少芯片ID，无法下发人流监测命令')
    return
  }

  const nextEnabled = !flowEnabled.value
  flowLoading.value = true

  try {
    await setFlowUpload(localForm.chipId, nextEnabled)

    flowEnabled.value = nextEnabled

    //window.alert(nextEnabled ? '已开启人流监测' : '已停止人流监测')
  } catch (error) {
    console.error('人流监测命令下发失败：', error)
    //window.alert('人流监测命令下发失败')
  } finally {
    flowLoading.value = false
  }
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
const normalizedDeviceType = computed(() => {
  return String(localForm.deviceType || props.device.deviceType || '')
    .replace(/[-_\s]/g, '')
    .toLowerCase()
})

const isLamp = computed(() => normalizedDeviceType.value === 'lamp')
const isCamLamp = computed(() => normalizedDeviceType.value === 'camlamp')

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