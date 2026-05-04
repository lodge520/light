<template>
  <div class="layout-card">
    <div class="layout-header">
      <div>
        <h2>店铺灯具布局</h2>
        <p>拖动灯具进入分区，系统会自动生成分区内排序</p>
      </div>

      <div class="layout-actions">
        <button class="reset-layout-btn" @click="addZone">新增分区</button>
        <button class="reset-layout-btn" @click="resetLayout">重置布局</button>
        <button
          class="locate-btn"
          type="button"
          :disabled="!selectedDevice"
          @click.stop="handleLocateSelected"
        >
          {{ selectedDevice ? `定位：${getSelectedDeviceLabel(selectedDevice)}` : '先选择灯具' }}
        </button>
        <button class="save-layout-btn" :disabled="saving" @click="saveLayout">
          {{ saving ? '保存中...' : '保存分区排序' }}
        </button>
      </div>
    </div>

    <div ref="stageRef" class="store-stage">
      <img
        class="store-bg"
        src="/backgrounds/store-layout.png"
        alt="店铺布局"
        draggable="false"
      />

      <!-- 分区 -->
      <div
        v-for="zone in zones"
        :key="zone.id"
        class="zone-box"
        :class="{ active: activeZoneId === zone.id }"
        :style="getZoneStyle(zone)"
        @pointerdown="handleZonePointerDown($event, zone)"
      >
        <input
          v-model.trim="zone.name"
          class="zone-name-input"
          @pointerdown.stop
          @input="saveZones"
        />

        <div class="zone-count">
          {{ getDevicesByZone(zone.name).length }} 盏灯
        </div>

        <button
          class="zone-delete-btn"
          type="button"
          title="删除分区"
          @pointerdown.stop
          @click.stop="deleteZone(zone)"
        > ×
        </button>
        <div
          class="zone-resize"
          @pointerdown.stop="handleZoneResize($event, zone)"
        ></div>
      </div>

      <!-- 灯具 -->
      <div
        v-for="(device, index) in layoutDevices"
        :key="device.id || device.chipId"
        class="lamp-node"
       :class="{
          active: draggingKey === getKey(device),
          selected: selectedDeviceId === device.id,
        }"
        :style="getNodeStyle(device, index)"
        @pointerdown="handleLampPointerDown($event, device)"
      >
        <div class="lamp-icon">💡</div>

        <div class="lamp-info">
          <strong>{{ getLampTitle(device, index) }}</strong>
          <span>{{ getLampSubText(device) }}</span>
        </div>
        <div v-if="selectedDeviceId === device.id" class="selected-badge">
          已选中
        </div>
      </div>
    </div>

    <div class="layout-tips">
      <div
        v-for="zone in zones"
        :key="zone.id"
        class="zone-order-row"
      >
        <strong>{{ zone.name }}</strong>
        <span
          v-for="device in getDevicesByZone(zone.name)"
          :key="device.id"
        >
          灯具-{{ getLocalDeviceNo(device) }} · {{ device.chipId }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { updateDevice, locateDevice } from '../../api/device'
import type { DeviceCreatePayload, DeviceItem } from '../../types/device'

const props = defineProps<{
  devices: DeviceItem[]
}>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

type Position = {
  x: number
  y: number
}

type Zone = {
  id: string
  name: string
  x: number
  y: number
  width: number
  height: number
}

type LocalDeviceState = {
  zoneName: string
  deviceNo: string
}

const stageRef = ref<HTMLElement | null>(null)
const positions = ref<Record<string, Position>>({})
const deviceState = ref<Record<number, LocalDeviceState>>({})
const zones = ref<Zone[]>([])
const draggingKey = ref('')
const activeZoneId = ref('')
const saving = ref(false)
const selectedDeviceId = ref<number | null>(null)

const selectedDevice = computed(() => {
  if (selectedDeviceId.value == null) return null
  return layoutDevices.value.find(device => device.id === selectedDeviceId.value) || null
})
const POSITION_STORAGE_KEY = 'SMART_LIGHT_LAYOUT_POSITIONS'
const ZONE_STORAGE_KEY = 'SMART_LIGHT_LAYOUT_ZONES'

function getKey(device: DeviceItem) {
  return String(device.id || device.chipId || device.deviceNo || '')
}

const layoutDevices = computed(() => props.devices || [])

function createId() {
  return `zone-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function clamp(num: number, min: number, max: number) {
  return Math.min(Math.max(num, min), max)
}

function getSelectedDeviceLabel(device: DeviceItem) {
  const no = getLocalDeviceNo(device)
  const zoneName = deviceState.value[device.id]?.zoneName || '未分区'

  if (no) {
    return `${zoneName} · 灯具-${no}`
  }

  return `${zoneName} · ${device.chipId || '未知灯具'}`
}

async function handleLocateSelected() {
  if (!selectedDevice.value) {
    alert('请先点击或拖动选择一盏灯')
    return
  }

  await handleLocate(selectedDevice.value)
}

async function handleLocate(device: DeviceItem) {
  if (!device.chipId) return

  try {
    const ok = await locateDevice(device.chipId)

    if (!ok) {
      alert('设备离线，无法定位')
      return
    }
  } catch (error) {
    console.error('定位灯具失败 =', error)
    alert('设备离线或连接不可用，无法定位')
  }
}

function loadPositions() {
  try {
    const raw = localStorage.getItem(POSITION_STORAGE_KEY)
    if (!raw) return
    positions.value = JSON.parse(raw)
  } catch (error) {
    console.warn('灯具布局读取失败', error)
  }
}

function syncDeviceStateByPosition() {
  for (const device of layoutDevices.value) {
    const key = getKey(device)
    const pos = positions.value[key]

    if (!pos) continue

    const zone = getZoneAtPosition(pos)

    if (zone) {
      deviceState.value[device.id] = {
        zoneName: zone.name,
        deviceNo: deviceState.value[device.id]?.deviceNo || '',
      }
    }
  }

  refreshAllZoneOrder()
}
function savePositions() {
  localStorage.setItem(POSITION_STORAGE_KEY, JSON.stringify(positions.value))
}

function loadZones() {
  try {
    const raw = localStorage.getItem(ZONE_STORAGE_KEY)

    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        zones.value = parsed
        activeZoneId.value = zones.value[0].id
        return
      }
    }
  } catch (error) {
    console.warn('分区布局读取失败', error)
  }

  zones.value = [
    {
      id: createId(),
      name: '新品展示区',
      x: 8,
      y: 12,
      width: 36,
      height: 34,
    },
    {
      id: createId(),
      name: '主通道区',
      x: 54,
      y: 18,
      width: 36,
      height: 30,
    },
  ]

  activeZoneId.value = zones.value[0].id
  saveZones()
}

function saveZones() {
  localStorage.setItem(ZONE_STORAGE_KEY, JSON.stringify(zones.value))
}

function initDeviceState() {
  const nextState: Record<number, LocalDeviceState> = {}

  for (const device of layoutDevices.value) {
    nextState[device.id] = {
      zoneName: device.displayName || '未分区',
      deviceNo: device.deviceNo || '',
    }
  }

  deviceState.value = nextState
}

function initDefaultPositions() {
  layoutDevices.value.forEach((device, index) => {
    const key = getKey(device)
    if (positions.value[key]) return

    positions.value[key] = {
      x: 18 + (index % 3) * 26,
      y: 22 + Math.floor(index / 3) * 24,
    }
  })

  savePositions()
}

function getZoneStyle(zone: Zone) {
  return {
    left: `${zone.x}%`,
    top: `${zone.y}%`,
    width: `${zone.width}%`,
    height: `${zone.height}%`,
  }
}

function getNodeStyle(device: DeviceItem, index: number) {
  const key = getKey(device)
  const pos = positions.value[key] || {
    x: 18 + (index % 3) * 26,
    y: 24 + Math.floor(index / 3) * 22,
  }

  return {
    left: `${pos.x}%`,
    top: `${pos.y}%`,
  }
}

function getLampTitle(device: DeviceItem, index: number) {
  const no = getLocalDeviceNo(device)
  return no ? `灯具-${no}` : `灯具${index + 1}`
}

function getLampSubText(device: DeviceItem) {
  const zoneName = deviceState.value[device.id]?.zoneName || '未分区'
  const online = device.online ? '在线' : '离线'
  return `${zoneName} · ${online}`
}

function getLocalDeviceNo(device: DeviceItem) {
  return deviceState.value[device.id]?.deviceNo || device.deviceNo || ''
}

function getZoneAtPosition(pos: Position) {
  return zones.value.find(zone => {
    return (
      pos.x >= zone.x &&
      pos.x <= zone.x + zone.width &&
      pos.y >= zone.y &&
      pos.y <= zone.y + zone.height
    )
  })
}

function isPositionInsideZone(pos: Position, zone: Zone) {
  return (
    pos.x >= zone.x &&
    pos.x <= zone.x + zone.width &&
    pos.y >= zone.y &&
    pos.y <= zone.y + zone.height
  )
}

function getDevicesInZone(zone: Zone) {
  return layoutDevices.value.filter(device => {
    const stateZoneName = deviceState.value[device.id]?.zoneName
    const key = getKey(device)
    const pos = positions.value[key]

    return stateZoneName === zone.name || (pos ? isPositionInsideZone(pos, zone) : false)
  })
}

function deleteZone(zone: Zone) {
  const confirmed = window.confirm(`确认删除分区「${zone.name}」吗？该分区内灯具会变为未分区。`)
  if (!confirmed) return

  const affectedDevices = getDevicesInZone(zone)

  affectedDevices.forEach(device => {
    deviceState.value[device.id] = {
      zoneName: '未分区',
      deviceNo: '',
    }
  })

  zones.value = zones.value.filter(item => item.id !== zone.id)

  if (activeZoneId.value === zone.id) {
    activeZoneId.value = zones.value[0]?.id || ''
  }

  saveZones()
  savePositions()
}

function getDevicesByZone(zoneName: string) {
  return [...layoutDevices.value]
    .filter(device => deviceState.value[device.id]?.zoneName === zoneName)
    .sort((a, b) => {
      const posA = positions.value[getKey(a)] || { x: 0, y: 0 }
      const posB = positions.value[getKey(b)] || { x: 0, y: 0 }

      if (Math.abs(posA.x - posB.x) > 5) {
        return posA.x - posB.x
      }

      return posA.y - posB.y
    })
}

function refreshZoneOrder(zoneName: string) {
  const list = getDevicesByZone(zoneName)

  list.forEach((device, index) => {
    deviceState.value[device.id] = {
      zoneName,
      deviceNo: String(index + 1),
    }
  })
}

function refreshAllZoneOrder() {
  for (const zone of zones.value) {
    refreshZoneOrder(zone.name)
  }
}

function addZone() {
  const index = zones.value.length + 1

  const zone: Zone = {
    id: createId(),
    name: `分区${index}`,
    x: 16 + ((index - 1) % 3) * 18,
    y: 16 + Math.floor((index - 1) / 3) * 18,
    width: 30,
    height: 26,
  }

  zones.value.push(zone)
  activeZoneId.value = zone.id
  saveZones()
}

function handleZonePointerDown(event: PointerEvent, zone: Zone) {
  const stage = stageRef.value
  if (!stage) return

  activeZoneId.value = zone.id

  const rect = stage.getBoundingClientRect()
  const startX = ((event.clientX - rect.left) / rect.width) * 100
  const startY = ((event.clientY - rect.top) / rect.height) * 100

  const offsetX = startX - zone.x
  const offsetY = startY - zone.y

  const originZoneX = zone.x
  const originZoneY = zone.y

  // 关键：记录分区内灯具的初始位置
  const affectedDevices = getDevicesInZone(zone)
  const affectedStartPositions: Record<string, Position> = {}

  affectedDevices.forEach(device => {
    const key = getKey(device)
    const pos = positions.value[key]

    if (pos) {
      affectedStartPositions[key] = {
        x: pos.x,
        y: pos.y,
      }
    }
  })

  function move(moveEvent: PointerEvent) {
    const x = ((moveEvent.clientX - rect.left) / rect.width) * 100
    const y = ((moveEvent.clientY - rect.top) / rect.height) * 100

    const nextZoneX = clamp(x - offsetX, 0, 100 - zone.width)
    const nextZoneY = clamp(y - offsetY, 0, 100 - zone.height)

    const dx = nextZoneX - originZoneX
    const dy = nextZoneY - originZoneY

    zone.x = nextZoneX
    zone.y = nextZoneY

    Object.entries(affectedStartPositions).forEach(([key, pos]) => {
      positions.value[key] = {
        x: clamp(pos.x + dx, 5, 95),
        y: clamp(pos.y + dy, 8, 92),
      }
    })
  }

  function up() {
    refreshZoneOrder(zone.name)
    saveZones()
    savePositions()

    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }

  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function handleZoneResize(event: PointerEvent, zone: Zone) {
  event.preventDefault()

  const stage = stageRef.value
  if (!stage) return

  activeZoneId.value = zone.id

  const rect = stage.getBoundingClientRect()

  function move(moveEvent: PointerEvent) {
    const x = ((moveEvent.clientX - rect.left) / rect.width) * 100
    const y = ((moveEvent.clientY - rect.top) / rect.height) * 100

    zone.width = clamp(x - zone.x, 18, 100 - zone.x)
    zone.height = clamp(y - zone.y, 16, 100 - zone.y)
  }

  function up() {
    saveZones()

    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }

  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function handleLampPointerDown(event: PointerEvent, device: DeviceItem) {
   const stage = stageRef.value
  if (!stage) return

  selectedDeviceId.value = device.id

  const key = getKey(device)
  draggingKey.value = key

  const rect = stage.getBoundingClientRect()
  const target = event.currentTarget as HTMLElement
  target.setPointerCapture(event.pointerId)

  function move(moveEvent: PointerEvent) {
    const x = ((moveEvent.clientX - rect.left) / rect.width) * 100
    const y = ((moveEvent.clientY - rect.top) / rect.height) * 100

    positions.value[key] = {
      x: clamp(x, 5, 95),
      y: clamp(y, 8, 92),
    }
  }

  function up() {
    const pos = positions.value[key]
    const oldZoneName = deviceState.value[device.id]?.zoneName || ''

    if (pos) {
      const zone = getZoneAtPosition(pos)

      if (zone) {
        deviceState.value[device.id] = {
          zoneName: zone.name,
          deviceNo: deviceState.value[device.id]?.deviceNo || '',
        }

        refreshZoneOrder(zone.name)

        if (oldZoneName && oldZoneName !== zone.name) {
          refreshZoneOrder(oldZoneName)
        }
      }
    }

    draggingKey.value = ''
    savePositions()

    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }

  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function resetLayout() {
  positions.value = {}
  zones.value = []

  localStorage.removeItem(POSITION_STORAGE_KEY)
  localStorage.removeItem(ZONE_STORAGE_KEY)

  loadZones()

  nextTick(() => {
    initDefaultPositions()
    initDeviceState()
  })
}


function buildUpdatePayload(
  device: DeviceItem,
  displayName: string,
  deviceNo: string,
): DeviceCreatePayload {
  return {
    chipId: device.chipId || '',
    ip: device.ip || '',
    displayName, // 这里就是分区名
    deviceType: device.deviceType || '',
    deviceNo, // 这里就是分区内编号
    brightness: device.brightness ?? 50,
    temp: device.temp ?? 4000,
    autoMode: device.autoMode ?? false,
    recommendedBrightness: device.recommendedBrightness ?? 50,
    recommendedTemp: device.recommendedTemp ?? 4000,
    fabric: device.fabric || '',
    mainColorRgb: device.mainColorRgb || '',
  }
}

async function saveLayout() {
  saving.value = true

  try {
    // 1. 保存前先根据灯具位置重新计算分区和排序
    syncDeviceStateByPosition()

    // 2. 关键：做快照，避免保存过程中被 watch / WebSocket 重置
    const stateSnapshot: Record<number, LocalDeviceState> = JSON.parse(
      JSON.stringify(deviceState.value)
    )

    const deviceSnapshot: DeviceItem[] = layoutDevices.value.map(device => ({
      ...device,
    }))

    console.log('最终保存快照 stateSnapshot =', stateSnapshot)

    for (const device of deviceSnapshot) {
      const state = stateSnapshot[device.id]
      if (!state) continue

      const nextDisplayName = state.zoneName || ''
      const nextDeviceNo = state.deviceNo || ''

      console.log('发送设备更新 =', {
        id: device.id,
        chipId: device.chipId,
        oldDisplayName: device.displayName,
        oldDeviceNo: device.deviceNo,
        nextDisplayName,
        nextDeviceNo,
      })

      await updateDevice(
        device.id,
        buildUpdatePayload(device, nextDisplayName, nextDeviceNo),
      )
    }

    alert('分区排序已保存')
    emit('saved')
  } catch (error) {
    console.error('save layout error =', error)
    alert('保存分区排序失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => props.devices,
  () => {
    if (saving.value) return

    nextTick(() => {
      if (saving.value) return

      initDefaultPositions()
      initDeviceState()
    })
  },
  { deep: true },
)
onMounted(() => {
  loadPositions()
  loadZones()

  nextTick(() => {
    initDefaultPositions()
    initDeviceState()
  })
})
</script>

<style scoped>
.layout-card {
  margin:0;
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.14);
}

.layout-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.layout-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
  color: #111827;
  letter-spacing: -0.02em;
}

.layout-header p {
  margin: 7px 0 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.layout-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.reset-layout-btn,
.save-layout-btn,
.locate-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  min-height: 40px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    background 0.16s ease,
    opacity 0.16s ease;
}

.reset-layout-btn:hover,
.save-layout-btn:hover,
.locate-btn:hover {
  transform: translateY(-1px);
}

.reset-layout-btn {
  background: rgba(64, 158, 255, 0.12);
  color: #2563eb;
}

.reset-layout-btn:hover {
  background: rgba(64, 158, 255, 0.2);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.12);
}

.locate-btn {
  background: rgba(245, 158, 11, 0.18);
  color: #d97706;
}

.locate-btn:hover {
  background: rgba(245, 158, 11, 0.26);
  box-shadow: 0 8px 18px rgba(217, 119, 6, 0.14);
}

.locate-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.save-layout-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.24);
}

.save-layout-btn:hover {
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.3);
}

.save-layout-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.zone-delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.14);
  color: #dc2626;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition:
    background 0.16s ease,
    transform 0.16s ease;
}

.zone-delete-btn:hover {
  background: rgba(239, 68, 68, 0.24);
  transform: scale(1.08);
}

.store-stage {
  position: relative;
  width: 100%;
  height: 420px;
  overflow: hidden;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid rgba(226, 232, 240, 0.9);
}


.store-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(2px) brightness(0.72) saturate(0.9);
  transform: scale(1.02);
  user-select: none;
  pointer-events: none;
}

.zone-box {
  position: absolute;
  z-index: 2;
  padding: 12px;
  border-radius: 20px;
  background: rgba(37, 99, 235, 0.12);
  border: 2px solid rgba(37, 99, 235, 0.55);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.18),
    0 12px 32px rgba(15, 23, 42, 0.16);
  cursor: grab;
  user-select: none;
  touch-action: none;
}

.zone-box.active {
  background: rgba(37, 99, 235, 0.2);
  border-color: rgba(37, 99, 235, 0.95);
  box-shadow:
    0 0 0 4px rgba(37, 99, 235, 0.16),
    0 18px 42px rgba(37, 99, 235, 0.2);
}

.zone-name-input {
  width: 140px;
  max-width: 90%;
  border: none;
  outline: none;
  border-radius: 999px;
  padding: 7px 12px;
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}

.zone-count {
  display: inline-flex;
  margin-top: 10px;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.zone-resize {
  position: absolute;
  right: 7px;
  bottom: 7px;
  width: 15px;
  height: 15px;
  border-right: 3px solid rgba(37, 99, 235, 0.6);
  border-bottom: 3px solid rgba(37, 99, 235, 0.6);
  cursor: nwse-resize;
}

.lamp-node {
  position: absolute;
  z-index: 5;
  transform: translate(-50%, -50%);
  min-width: 150px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 13px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  border: 2px solid rgba(255, 255, 255, 0.95);
  box-shadow:
    0 12px 30px rgba(15, 23, 42, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  cursor: grab;
  user-select: none;
  touch-action: none;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.lamp-node.active {
  cursor: grabbing;
  transform: translate(-50%, -50%) scale(1.08);
  background: #ffffff;
  border-color: rgba(245, 158, 11, 0.95);
  box-shadow:
    0 0 0 5px rgba(245, 158, 11, 0.22),
    0 20px 44px rgba(15, 23, 42, 0.34);
  z-index: 20;
}

.lamp-icon {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #92400e;
  font-size: 18px;
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.25);
}

.lamp-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.lamp-info strong {
  font-size: 14px;
  color: #1f2937;
}

.lamp-info span {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.layout-tips {
  display: grid;
  gap: 10px;
  margin-top: 14px;
  font-size: 13px;
  color: #64748b;
}

.zone-order-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.zone-order-row strong {
  color: #1e293b;
}

.zone-order-row span {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.82);
  color: #475569;
}

.lamp-node.selected {
  border-color: rgba(37, 99, 235, 0.95);
  background: #ffffff;
  box-shadow:
    0 0 0 5px rgba(59, 130, 246, 0.2),
    0 18px 40px rgba(37, 99, 235, 0.3);
}

.selected-badge {
  position: absolute;
  top: -10px;
  right: -8px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.28);
}

@media (max-width: 1200px) {
  .layout-header {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
  }

  .layout-header h2 {
    white-space: nowrap;
  }

  .layout-header p {
    max-width: 100%;
  }

  .layout-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .reset-layout-btn,
  .save-layout-btn,
  .locate-btn {
    flex: 0 1 auto;
  }

  .locate-btn {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

@media (max-width: 768px) {
  .layout-card {
    padding: 14px;
    border-radius: 18px;
  }

  .layout-header {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
  }

  .layout-actions {
    width: 100%;
    justify-content: flex-start;
    gap: 8px;
  }

  .reset-layout-btn,
  .save-layout-btn,
  .locate-btn {
    padding: 9px 13px;
    min-height: 38px;
    font-size: 13px;
  }

  .locate-btn {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .store-stage {
    height: 320px;
  }

  .lamp-node {
    min-width: 112px;
    padding: 8px 10px;
    gap: 7px;
  }

  .lamp-icon {
    width: 30px;
    height: 30px;
    flex-basis: 30px;
    font-size: 15px;
  }

  .lamp-info strong {
    font-size: 12px;
  }

  .lamp-info span {
    display: block;
    font-size: 10px;
  }

  .zone-name-input {
    width: 100px;
    font-size: 12px;
  }
}

:global(.app-container.night-mode) .layout-card {
  background: rgba(15, 23, 42, 0.82);
  border-color: rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35);
  filter: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

:global(.app-container.night-mode) .layout-header h2,
:global(.app-container.night-mode) .lamp-info strong,
:global(.app-container.night-mode) .zone-order-row strong {
  color: rgba(248, 250, 252, 0.96);
}

:global(.app-container.night-mode) .layout-header p,
:global(.app-container.night-mode) .layout-tips,
:global(.app-container.night-mode) .lamp-info span,
:global(.app-container.night-mode) .zone-order-row span {
  color: rgba(203, 213, 225, 0.72);
}

:global(.app-container.night-mode) .store-stage {
  background: rgba(2, 6, 23, 0.78);
  border-color: rgba(148, 163, 184, 0.22);
}

:global(.app-container.night-mode) .store-bg {
  filter: blur(2px) brightness(0.58) saturate(0.82);
}

:global(.app-container.night-mode) .reset-layout-btn,
:global(.app-container.night-mode) .zone-order-row span {
  background: rgba(30, 41, 59, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.24);
  color: rgba(226, 232, 240, 0.9);
}

:global(.app-container.night-mode) .locate-btn {
  background: rgba(120, 53, 15, 0.26);
  color: #fde68a;
}

:global(.app-container.night-mode) .zone-box {
  background: rgba(37, 99, 235, 0.2);
  border-color: rgba(96, 165, 250, 0.72);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.36);
}

:global(.app-container.night-mode) .zone-name-input,
:global(.app-container.night-mode) .zone-count,
:global(.app-container.night-mode) .lamp-node {
  background: rgba(15, 23, 42, 0.9);
  border-color: rgba(148, 163, 184, 0.3);
  color: rgba(248, 250, 252, 0.96);
}

:global(.app-container.night-mode) .lamp-node.active,
:global(.app-container.night-mode) .lamp-node.selected {
  background: rgba(15, 23, 42, 0.94);
  border-color: rgba(251, 191, 36, 0.92);
  box-shadow:
    0 0 0 5px rgba(251, 191, 36, 0.18),
    0 18px 44px rgba(0, 0, 0, 0.46);
}

</style>
