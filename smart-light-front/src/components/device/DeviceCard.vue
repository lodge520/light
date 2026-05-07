<template>
  <div class="lamp-card">
    <div class="card-header clickable-header" @click="handleHeaderClick">
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
      {{ fabricLoading ? '识别中...' : '上传服装图片' }}
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

<Teleport to="body">
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

        <section class="firmware-section">
          <h4>固件升级</h4>

          <label class="modal-label">固件通道</label>
          <BaseSelect
            v-model="firmwareChannel"
            :options="firmwareChannelOptions"
            :disabled="otaStarting || otaStatusValue === 'updating'"
          />

          <div class="firmware-info-grid">
            <div class="firmware-info-item">
              <span>当前固件</span>
              <strong>{{ firmwareVersionText }}</strong>
            </div>

            <div class="firmware-info-item">
              <span>OTA状态</span>
              <strong>{{ otaStatusText }}</strong>
            </div>
          </div>

          <div v-if="otaCheckResult" class="ota-result">
            <div>{{ otaUpdateText }}</div>
            <div v-if="otaCheckResult.changelog" class="modal-hint">
              更新说明：{{ otaCheckResult.changelog }}
            </div>
          </div>

          <p v-if="otaMessage" class="modal-hint">{{ otaMessage }}</p>

          <div class="detail-modal-actions ota-actions">
            <button class="btn-secondary" :disabled="otaChecking" @click="handleCheckFirmwareUpdate">
              {{ otaChecking ? '检查中...' : '检查更新' }}
            </button>
            <button
              class="btn-primary"
              :disabled="!canStartOta"
              @click="handleStartOtaUpdate"
            >
              {{ otaStarting ? '下发中...' : '确认更新' }}
            </button>
          </div>
        </section>

        <div class="detail-info-item">
          <span class="detail-label">IP</span>
          <span class="detail-value">{{ localForm.ip || '未设置' }}</span>
        </div>

       <label class="modal-label">所属分区</label>
        <input
          v-model.trim="localForm.displayName"
          class="modal-input"
          type="text"
          placeholder="如 新品展示区、橱窗区、主通道区"
        />

        <label class="modal-label">分区内编号</label>
        <input
          v-model.trim="localForm.deviceNo"
          class="modal-input"
          type="text"
          inputmode="numeric"
          pattern="[1-9][0-9]*"
          placeholder="从 1 开始，如 1、2、3"
        />

        <p class="modal-hint">
          编号从 1 开始，同一分区内不能重复。
        </p>

        <p v-if="deviceNoError" class="modal-error">
          {{ deviceNoError }}
        </p>

        <div class="detail-modal-actions">
          <button class="btn-secondary" @click="closeDetailModal">取消</button>
          <button class="btn-primary" @click="saveDeviceBaseInfo">保存</button>
        </div>
      </div>
      </Transition>
    </div>
  </Transition>
</Teleport>
<Teleport to="body">
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
          :src="annotatedImageSrc"
          alt="服装区域分割结果"
        />

        <div class="detail-modal-actions">
          <button class="btn-secondary" @click="closeClothPreviewModal">关闭</button>
        </div>
      </div>
      </Transition>
    </div>
  </Transition>
</Teleport>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import BaseSelect from '../common/BaseSelect.vue'
import type {
  DeviceCreatePayload,
  DeviceItem,
  FirmwareChannel,
  OtaCheckResult,
} from '../../types/device'
import { fabricRecognize } from '../../api/ai'
import {
  setFlowUpload,
  locateDevice,
  checkFirmwareUpdate,
  startOtaUpdate,
} from '../../api/device'

const props = defineProps<{
  device: DeviceItem
  deleting?: boolean
  allDevices?: DeviceItem[]
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
function normalizeBase64ImageSrc(value: string) {
  const rawValue = value.trim()
  if (!rawValue) {
    return ''
  }

  const dataUriMatch = rawValue.match(/^(data:image\/[a-zA-Z0-9.+-]+;base64,)([\s\S]*)$/i)
  if (dataUriMatch) {
    const base64Value = dataUriMatch[2].replace(/\s/g, '')
    return base64Value ? `${dataUriMatch[1]}${base64Value}` : ''
  }

  const base64Value = rawValue.replace(/\s/g, '')

  if (!base64Value) {
    return ''
  }

  return `data:image/jpeg;base64,${base64Value}`
}

const annotatedImageSrc = computed(() => {
  const value = annotatedImageBase64.value
  if (!value) {
    return ''
  }
  return normalizeBase64ImageSrc(value)
})
const clothDetected = ref<boolean | null>(null)
const showClothPreviewModal = ref(false)
const firmwareChannel = ref<FirmwareChannel>('stable')
const otaChecking = ref(false)
const otaStarting = ref(false)
const otaCheckResult = ref<OtaCheckResult | null>(null)
const otaMessage = ref('')

const firmwareChannelOptions = [
  { label: '正式版', value: 'stable' },
  { label: '测试版', value: 'test' },
]

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

const locating = ref(false)

async function silentLocateDevice() {
  const chipId = localForm.chipId || props.device.chipId

  if (!chipId) return
  if (locating.value) return

  locating.value = true

  try {
    await locateDevice(chipId)
  } catch (error) {
    console.warn('静默定位失败，可能设备离线：', error)
  } finally {
    locating.value = false
  }
}

function handleHeaderClick() {
  openDetailModal()
  silentLocateDevice()
}

const deviceNoError = computed(() => {
  const zoneName = localForm.displayName?.trim()
  const deviceNo = localForm.deviceNo?.trim()

  if (!zoneName) {
    return '所属分区不能为空'
  }

  if (!deviceNo) {
    return '分区内编号不能为空'
  }

  if (!/^[1-9]\d*$/.test(deviceNo)) {
    return '分区内编号必须是从 1 开始的正整数'
  }

  const duplicated = (props.allDevices || []).some(item => {
    if (item.id === props.device.id) return false

    const sameZone = (item.displayName || '').trim() === zoneName
    const sameNo = (item.deviceNo || '').trim() === deviceNo

    return sameZone && sameNo
  })

  if (duplicated) {
    return `「${zoneName}」分区内已经存在编号 ${deviceNo}`
  }

  return ''
})

function saveDeviceBaseInfo() {
  if (deviceNoError.value) {
    window.alert(deviceNoError.value)
    return
  }

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

const firmwareVersionText = computed(() => {
  const version = props.device.firmwareVersion || 'unknown'
  const code = props.device.firmwareVersionCode
  return code == null ? version : `${version} (${code})`
})

const otaStatusValue = computed(() => props.device.otaStatus || 'idle')

const otaStatusText = computed(() => {
  const map: Record<string, string> = {
    idle: '空闲',
    updating: '更新中',
    success: '更新成功',
    failed: '更新失败',
  }
  return map[otaStatusValue.value] || otaStatusValue.value
})

const otaUpdateText = computed(() => {
  const result = otaCheckResult.value
  if (!result) return ''
  if (!result.latestVersion) return '当前通道暂无可用固件'
  if (!result.hasUpdate) {
    return '当前已是该通道最新版本'
  }
  return `发现新版本 ${result.latestVersion}`
})

const canStartOta = computed(() => {
  return Boolean(
    otaCheckResult.value?.hasUpdate &&
    otaCheckResult.value.firmwareId &&
    !otaChecking.value &&
    !otaStarting.value &&
    otaStatusValue.value !== 'updating',
  )
})

async function handleCheckFirmwareUpdate() {
  if (!localForm.chipId) return
  otaChecking.value = true
  otaMessage.value = ''
  otaCheckResult.value = null

  try {
    otaCheckResult.value = await checkFirmwareUpdate(localForm.chipId, firmwareChannel.value)
  } catch (error) {
    console.error('checkFirmwareUpdate error =', error)
    otaMessage.value = '检查更新失败'
  } finally {
    otaChecking.value = false
  }
}

async function handleStartOtaUpdate() {
  if (!localForm.chipId || !otaCheckResult.value?.firmwareId) return

  const target = otaCheckResult.value.latestVersion || 'selected firmware'
  if (!window.confirm(`确认更新到 ${target} 吗？`)) return

  otaStarting.value = true
  otaMessage.value = ''

  try {
    otaCheckResult.value = await startOtaUpdate(
      localForm.chipId,
      otaCheckResult.value.firmwareId,
      firmwareChannel.value,
    )
    otaMessage.value = 'OTA更新指令已下发'
  } catch (error) {
    console.error('startOtaUpdate error =', error)
    otaMessage.value = 'OTA更新指令下发失败'
  } finally {
    otaStarting.value = false
  }
}

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
  firmwareChannel.value = props.device.firmwareChannel === 'test' ? 'test' : 'stable'
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
    window.alert('设备缺少 chipId，无法上传面料识别图片')
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
    const message = error instanceof Error && error.message
      ? error.message
      : '面料识别失败，请稍后重试'
    window.alert(message)
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
  const zoneName = props.device.displayName?.trim() || '未分区'
  const deviceNo = props.device.deviceNo?.trim()

  if (deviceNo) {
    return `${zoneName} · 灯具-${deviceNo}`
  }

  return zoneName
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

<style scoped>

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.device-title-block {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.device-title-block h3 {
  margin: 0;
}

.last-seen-under-name {
  margin: 6px 0 0;
  font-size: 14px;
  color: #8a8a8a;
  line-height: 1.4;
}

.status-badge {
  flex-shrink: 0;
  white-space: nowrap;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.online {
  background: #e8f7ed;
  color: #18a058;
}

.status-badge.offline {
  background: #fef0f0;
  color: #f56c6c;
}


.clickable-header {
  cursor: pointer;
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.clickable-header:hover {
  transform: translateY(-1px);
  opacity: 0.96;
}

.device-detail-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.device-detail-modal {
  position: relative;
  z-index: 2001;
  width: 420px;
  max-width: 92vw;
  max-height: 88vh;
  overflow: auto;
  background: #fff;
  border-radius: 20px;
  padding: 22px;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.18);
}

.detail-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-modal-header h3 {
  margin: 0;
}

.detail-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #8a8a8a;
}

.detail-close-btn {
  border: none;
  background: transparent;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  color: #333;
}

.detail-info-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 12px;
}

.detail-label {
  color: #64748b;
}

.detail-value {
  color: #0f172a;
  font-weight: 600;
}

.detail-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.firmware-section {
  margin: 14px 0;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.firmware-section h4 {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
}

.firmware-info-grid {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.firmware-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 9px 10px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #eef2f7;
}

.firmware-info-item span {
  color: #64748b;
  font-size: 13px;
}

.firmware-info-item strong {
  color: #0f172a;
  font-size: 13px;
  text-align: right;
  word-break: break-all;
}

.ota-result {
  margin-top: 10px;
  padding: 9px 10px;
  border-radius: 10px;
  background: #eef4ff;
  color: #2563eb;
  font-size: 13px;
  line-height: 1.5;
}

.ota-actions {
  margin-top: 12px;
}
/* 遮罩：只淡入淡出 */
.detail-overlay-fade-enter-active,
.detail-overlay-fade-leave-active {
  transition: opacity 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.detail-overlay-fade-enter-from,
.detail-overlay-fade-leave-to {
  opacity: 0;
}

.detail-overlay-fade-enter-to,
.detail-overlay-fade-leave-from {
  opacity: 1;
}

.lamp-card,
.placeholder-card {
  background: var(--card-bg);
  border-radius: var(--border-radius);
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.lamp-card h3 {
  font-size: 18px;
  margin-bottom: 12px;
}

.color-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
  min-width: 180px;
  height: 40px;
  text-align: center;
  border-radius: 12px;
  border: 1px solid #ccc;
  box-shadow: inset 0 0 4px rgba(0, 0, 0, 0.08);
  padding: 0 12px;
}

.lamp-card input[type='range'] {
  width: 100%;
  margin-top: 6px;
}

.lamp-card input[type='range']:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.field-label {
  display: block;
  margin-top: 12px;
  margin-bottom: 6px;
  font-size: 14px;
  color: #606266;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: #606266;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

/* 卡片：单独弹入弹出 */
.detail-card-pop-enter-active {
  transition:
    opacity 420ms cubic-bezier(0.2, 0.8, 0.2, 1),
    transform 420ms cubic-bezier(0.2, 0.8, 0.2, 1),
    filter 420ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.detail-card-pop-leave-active {
  transition:
    opacity 240ms cubic-bezier(0.4, 0, 1, 1),
    transform 240ms cubic-bezier(0.4, 0, 1, 1),
    filter 240ms cubic-bezier(0.4, 0, 1, 1);
}

.detail-card-pop-enter-from {
  opacity: 0;
  transform: translateY(24px) scale(0.94);
  filter: blur(10px);
}

.detail-card-pop-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.detail-card-pop-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.detail-card-pop-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.98);
  filter: blur(8px);
}

.ai-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  margin-bottom: 4px;
}

.hidden-file-input {
  display: none;
}

.btn-ai {
  border: none;
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 13px;
  cursor: pointer;
  background: #eef4ff;
  color: #1677ff;
  transition: all 0.2s ease;
}


.btn-ai:hover {
  background: #dbeafe;
}

.btn-ai.active {
  background: #fff1f0;
  color: #f53f3f;
}

.btn-ai:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-preview {
  margin-left: 8px;
}

.cloth-preview-modal {
  position: relative;
  z-index: 2001;
  width: min(760px, 92vw);
  max-height: 88vh;
  overflow: auto;
  background: #fff;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.24);
}

.cloth-preview-image {
  width: 100%;
  max-height: 560px;
  object-fit: contain;
  border-radius: 14px;
  margin-top: 14px;
  background: #f6f7f9;
}
.modal-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}

.modal-error {
  margin: 8px 0 0;
  padding: 8px 10px;
  border-radius: 10px;
  background: #fff1f0;
  color: #f53f3f;
  font-size: 13px;
  line-height: 1.5;
}

.modal-label {
  display: block;
  margin: 14px 0 7px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.modal-input {
  width: 100%;
  box-sizing: border-box;
  padding: 11px 12px;
  border-radius: 12px;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
  color: #0f172a;
  font-size: 14px;
  outline: none;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.modal-input:focus {
  border-color: #409eff;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.14);
}

.modal-input::placeholder {
  color: #94a3b8;
}
:global(body:has(.app-container.night-mode)) .device-detail-overlay {
  background: rgba(2, 6, 23, 0.68);
}

:global(body:has(.app-container.night-mode)) .device-detail-modal,
:global(body:has(.app-container.night-mode)) .cloth-preview-modal {
  background: rgba(15, 23, 42, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: rgba(226, 232, 240, 0.9);
  box-shadow: 0 26px 70px rgba(0, 0, 0, 0.5);
  filter: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

:global(body:has(.app-container.night-mode)) .detail-modal-header h3,
:global(body:has(.app-container.night-mode)) .firmware-section h4,
:global(body:has(.app-container.night-mode)) .detail-value,
:global(body:has(.app-container.night-mode)) .firmware-info-item strong {
  color: rgba(248, 250, 252, 0.96);
}

:global(body:has(.app-container.night-mode)) .detail-subtitle,
:global(body:has(.app-container.night-mode)) .detail-label,
:global(body:has(.app-container.night-mode)) .modal-label,
:global(body:has(.app-container.night-mode)) .modal-hint,
:global(body:has(.app-container.night-mode)) .firmware-info-item span {
  color: rgba(203, 213, 225, 0.72);
}

:global(body:has(.app-container.night-mode)) .detail-close-btn {
  color: rgba(226, 232, 240, 0.9);
}

:global(body:has(.app-container.night-mode)) .detail-info-item,
:global(body:has(.app-container.night-mode)) .firmware-section,
:global(body:has(.app-container.night-mode)) .firmware-info-item {
  background: rgba(15, 23, 42, 0.62);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

:global(body:has(.app-container.night-mode)) .modal-input {
  background: rgba(15, 23, 42, 0.76);
  border-color: rgba(148, 163, 184, 0.28);
  color: rgba(226, 232, 240, 0.92);
}

:global(body:has(.app-container.night-mode)) .modal-input:focus {
  background: rgba(15, 23, 42, 0.86);
  border-color: rgba(96, 165, 250, 0.72);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.18);
}

:global(body:has(.app-container.night-mode)) .modal-input::placeholder {
  color: rgba(203, 213, 225, 0.58);
}

:global(body:has(.app-container.night-mode)) .ota-result {
  background: rgba(30, 64, 175, 0.24);
  border: 1px solid rgba(96, 165, 250, 0.22);
  color: #bfdbfe;
}

:global(body:has(.app-container.night-mode)) .modal-error {
  background: rgba(127, 29, 29, 0.26);
  border: 1px solid rgba(248, 113, 113, 0.22);
  color: #fecaca;
}

:global(body:has(.app-container.night-mode)) .cloth-preview-image {
  background: rgba(15, 23, 42, 0.62);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

:global(body:has(.app-container.night-mode)) .btn-secondary {
  background: rgba(30, 41, 59, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.24);
  color: rgba(226, 232, 240, 0.9);
}

:global(body:has(.app-container.night-mode)) .btn-danger {
  background: rgba(127, 29, 29, 0.28);
  color: #fecaca;
}

:global(.app-container.night-mode) .lamp-card,
:global(.app-container.night-mode) .placeholder-card {
  background: rgba(15, 23, 42, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.18);
  color: rgba(226, 232, 240, 0.88);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35);
}

:global(.app-container.night-mode) .lamp-card h3,
:global(.app-container.night-mode) .device-title-block h3 {
  color: rgba(248, 250, 252, 0.96);
}

:global(.app-container.night-mode) .last-seen-under-name,
:global(.app-container.night-mode) .field-label,
:global(.app-container.night-mode) .checkbox-row {
  color: rgba(203, 213, 225, 0.72);
}

:global(.app-container.night-mode) .status-badge.online {
  background: rgba(6, 95, 70, 0.28);
  border: 1px solid rgba(52, 211, 153, 0.22);
  color: #a7f3d0;
}

:global(.app-container.night-mode) .status-badge.offline {
  background: rgba(127, 29, 29, 0.28);
  border: 1px solid rgba(248, 113, 113, 0.22);
  color: #fecaca;
}

:global(.app-container.night-mode) .btn-ai {
  background: rgba(30, 41, 59, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.24);
  color: rgba(226, 232, 240, 0.9);
}

:global(.app-container.night-mode) .btn-ai:hover {
  background: rgba(37, 99, 235, 0.26);
  border-color: rgba(96, 165, 250, 0.45);
  color: #bfdbfe;
}

:global(.app-container.night-mode) .btn-ai.active {
  background: rgba(127, 29, 29, 0.28);
  border-color: rgba(248, 113, 113, 0.22);
  color: #fecaca;
}

:global(.app-container.night-mode) .color-box {
  border-color: rgba(148, 163, 184, 0.28);
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.22);
}

</style>
