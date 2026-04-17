<template>
  <Transition name="modal-overlay-fade" appear @after-leave="handleOverlayAfterLeave">
    <div v-if="overlayVisible" class="modal-overlay" @click.self="handleClose">
      <Transition name="ios-modal-card" appear @after-leave="handleCardAfterLeave">
        <div v-if="visible" class="modal-card">
          <h3>{{ initialData ? '添加扫描到的设备' : '手动添加设备' }}</h3>

            <template v-if="initialData">
              <div class="readonly-grid compact-readonly-grid">
                <div class="readonly-item">
                  <div class="readonly-label">设备编码</div>
                  <div class="readonly-value">{{ form.chipId || '未获取' }}</div>
                </div>

                <div class="readonly-item">
                  <div class="readonly-label">设备类型</div>
                  <div class="readonly-value">{{ form.deviceType || '未知' }}</div>
                </div>

                <div class="readonly-item readonly-item-full">
                  <div class="readonly-label">设备 IP</div>
                  <div class="readonly-value">{{ form.ip || '未获取' }}</div>
                </div>
              </div>
            </template>

            <template v-else>
              <label class="modal-label">设备名称</label>
              <input
                v-model.trim="form.chipId"
                class="modal-input"
                type="text"
                placeholder="如 lamp2"
              />

              <label class="modal-label">设备 IP</label>
              <input
                v-model.trim="form.ip"
                class="modal-input"
                type="text"
                placeholder="如 192.168.1.105"
              />
            </template>

            <label class="modal-label">显示名称</label>
            <input
              v-model.trim="form.displayName"
              class="modal-input"
              type="text"
              placeholder="如 橱窗灯1"
            />

            <label class="modal-label">设备编号</label>
            <input
              v-model.trim="form.deviceNo"
              class="modal-input"
              type="text"
              placeholder="如 LIGHT-001"
            />

          <div class="modal-actions">
            <button class="btn-confirm" :disabled="submitting" @click="submit">
              {{ submitting ? '提交中...' : '确认添加' }}
            </button>
            <button class="btn-cancel" :disabled="submitting" @click="handleClose">
              取消
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import type { DeviceCreatePayload } from '../../types/device'

type DeviceAddInitialData = {
  chipId?: string
  ip?: string
  deviceType?: string
  displayName?: string
  deviceNo?: string
} | null

const props = defineProps<{
  submitting?: boolean
  initialData?: DeviceAddInitialData
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', value: DeviceCreatePayload): void
}>()

const overlayVisible = ref(false)
const visible = ref(false)
const closing = ref(false)

const form = reactive<DeviceCreatePayload>({
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

function resetForm() {
  form.chipId = ''
  form.ip = ''
  form.displayName = ''
  form.deviceType = ''
  form.deviceNo = ''
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
  form.deviceType = data.deviceType?.trim?.() || ''
  form.deviceNo = data.deviceNo?.trim?.() || ''
  form.displayName =
    data.displayName?.trim?.() ||
    buildDefaultDisplayName(data.chipId)
}

function submit() {
  if (!form.chipId) return
  emit('submit', { ...form })
}

function handleClose() {
  if (closing.value) return
  closing.value = true
  visible.value = false
}

function handleCardAfterLeave() {
  overlayVisible.value = false
}

function handleOverlayAfterLeave() {
  emit('close')
  closing.value = false
}

onMounted(() => {
  fillFormByInitialData(props.initialData)
  overlayVisible.value = true
  nextTick(() => {
    visible.value = true
  })
})

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