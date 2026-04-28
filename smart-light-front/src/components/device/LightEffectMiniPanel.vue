<template>
  <div class="light-effect-mini-card">
    <div class="mini-title">🌊 灯效</div>

   <label class="mini-label">范围</label>
<BaseSelect
  v-model="scope"
  :options="scopeOptions"
  placeholder="请选择范围"
/>

    <label class="mini-label">基础色温</label>
    <input
      v-model.number="baseTemp"
      class="mini-input"
      type="number"
      min="2700"
      max="6500"
    />

    <label class="mini-label">波动范围</label>
    <input
      v-model.number="range"
      class="mini-input"
      type="number"
      min="0"
      max="1200"
    />

    <label class="mini-label">亮度</label>
    <input
      v-model.number="brightness"
      class="mini-input"
      type="number"
      min="0"
      max="100"
    />

    <button
      class="mini-btn start"
      :disabled="submitting || targetDevices.length === 0"
      @click="startWave"
    >
      开启流水
    </button>

    <button
      class="mini-btn stop"
      :disabled="submitting || targetDevices.length === 0"
      @click="stopWave"
    >
      停止
    </button>

    <div class="mini-status">
      {{ statusText }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { sendLightEffect } from '../../api/device'
import type { DeviceItem } from '../../types/device'
import BaseSelect from '../common/BaseSelect.vue'
const props = defineProps<{
  devices: DeviceItem[]
}>()

const scope = ref('all')
const baseTemp = ref(3800)
const range = ref(500)
const brightness = ref(70)
const speed = ref(1)
const submitting = ref(false)
const statusText = ref('未启动')

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

  if (scope.value === 'all') {
    return [...list].sort((a, b) => {
      const zoneA = getZoneName(a)
      const zoneB = getZoneName(b)
      const zoneCompare = zoneA.localeCompare(zoneB, 'zh-Hans-CN')
      if (zoneCompare !== 0) return zoneCompare

      return Number(a.deviceNo || 9999) - Number(b.deviceNo || 9999)
    })
  }

  return list
    .filter(device => getZoneName(device) === scope.value)
    .sort((a, b) => Number(a.deviceNo || 9999) - Number(b.deviceNo || 9999))
})

function getZoneName(device: DeviceItem) {
  return device.displayName?.trim() || '未分区'
}

function clamp(num: number, min: number, max: number) {
  return Math.min(Math.max(num, min), max)
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

    statusText.value = `已开启 ${targetDevices.value.length} 盏`
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

    statusText.value = '已停止'
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
  width: 160px;
  box-sizing: border-box;
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.12);
}

.mini-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 900;
  color: #0f172a;
}

.mini-label {
  display: block;
  margin: 10px 0 5px;
  font-size: 12px;
  font-weight: 800;
  color: #64748b;
}

.mini-select,
.mini-input {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 9px;
  border-radius: 10px;
  border: 1px solid #dbe3ef;
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
  font-size: 13px;
  outline: none;
}

.mini-btn {
  width: 100%;
  margin-top: 10px;
  border: none;
  border-radius: 999px;
  padding: 9px 10px;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.mini-btn.start {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22);
}

.mini-btn.stop {
  background: rgba(64, 158, 255, 0.12);
  color: #2563eb;
}

.mini-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.mini-status {
  margin-top: 10px;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}
</style>