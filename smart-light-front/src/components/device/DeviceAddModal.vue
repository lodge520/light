<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <Transition name="ios-modal-card" appear>
      <div class="modal-card">
        <h3>{{ initialData ? '添加扫描到的设备' : '手动添加设备' }}</h3>

        <label class="modal-label">设备编码</label>
        <input
          v-model.trim="form.chipId"
          class="modal-input"
          type="text"
          placeholder="如 lamp2"
        />

        <label class="modal-label">显示名称</label>
        <input
          v-model.trim="form.displayName"
          class="modal-input"
          type="text"
          placeholder="如 门口摄像灯 / 展台主灯"
        />

        <label class="modal-label">设备 IP</label>
        <input
          v-model.trim="form.ip"
          class="modal-input"
          type="text"
          placeholder="如 192.168.1.105"
        />

        <div class="modal-actions">
          <button class="btn-confirm" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中...' : '确认添加' }}
          </button>
          <button class="btn-cancel" :disabled="submitting" @click="emit('close')">
            取消
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { DeviceCreatePayload } from '../../types/device'

type DeviceAddInitialData = {
  chipId?: string
  ip?: string
  deviceType?: string
  displayName?: string
} | null

const props = defineProps<{
  submitting?: boolean
  initialData?: DeviceAddInitialData
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', value: DeviceCreatePayload): void
}>()

const form = reactive<DeviceCreatePayload>({
  chipId: '',
  ip: '',
  displayName: '',
  brightness: 50,
  temp: 4000,
  autoMode: false,
  recommendedBrightness: 50,
  recommendedTemp: 4000,
  fabric: '',
  mainColorRgb: '',
})

function resetForm() {
  form.chipId = ''
  form.ip = ''
  form.displayName = ''
  form.brightness = 50
  form.temp = 4000
  form.autoMode = false
  form.recommendedBrightness = 50
  form.recommendedTemp = 4000
  form.fabric = ''
  form.mainColorRgb = ''
}

function buildDefaultDisplayName(chipId?: string) {
  const code = chipId?.trim?.() || ''
  if (!code) return ''
  return `设备-${code}`
}

function fillFormByInitialData(data?: DeviceAddInitialData) {
  resetForm()
  if (!data) return

  form.chipId = data.chipId?.trim?.() || ''
  form.ip = data.ip?.trim?.() || ''
  form.displayName =
    data.displayName?.trim?.() ||
    buildDefaultDisplayName(data.chipId)
}

function submit() {
  if (!form.chipId) return
  emit('submit', { ...form })
}

watch(
  () => props.initialData,
  (val) => {
    fillFormByInitialData(val)
  },
  { immediate: true },
)

watch(
  () => props.submitting,
  (val, oldVal) => {
    if (oldVal === true && val === false) {
      fillFormByInitialData(props.initialData)
    }
  },
)
</script>