<template>
  <div class="app-container" :class="{ 'night-mode': storeSettings.isNightMode }">
    <SidebarNav v-model="activeTab" />

    <div class="main-content">
      <section v-show="activeTab === 'main'" class="page-section">
        <TopStatusBar
          :current-time="currentTime"
          :week-info="weekInfo"
          :date-info="dateInfo"
          :weather-text="weatherText"
        />

        <div class="env-layout card-section section-space-top">
          <div class="env-card">
            <h4>实时环境参数</h4>
            <div class="env-info">
              <div>温度：{{ envInfo.temp }} °C</div>
              <div>人流量：{{ envInfo.people }} 人</div>
              <div>面积：{{ envInfo.area }} ㎡</div>
            </div>
          </div>

          <div class="env-card">
            <div id="metaInfo">
              <div>{{ holidayInfo }}</div>
              <div>{{ workdayInfo }}</div>
            </div>
            <div id="luxDisplay" class="lux-display">
              {{ latestLuxText }}
            </div>
          </div>
        </div>

        <h1>智能灯控</h1>

        <div id="controls">
          <button :disabled="scanning" @click="handleScan">
            {{ scanning ? '扫描中...' : '扫描设备' }}
          </button>
          <button @click="openManualAdd">手动添加设备</button>
          <label>
            服务器地址：
            <input v-model.trim="serverHost" type="text" placeholder="127.0.0.1" />
          </label>
          <div id="scanStatus">
            {{ connected ? `WS 已连接 · ${scanStatus}` : `WS 未连接 · ${scanStatus}` }}
          </div>
        </div>

<Transition name="ios-panel">
  <div v-if="scanning || scannedDevices.length > 0" class="scan-panel">
    <div class="scan-panel-header">
      <div class="scan-panel-title">
        {{ scanning ? '扫描结果（进行中）' : '扫描结果（已结束）' }}
      </div>

      <button
        v-if="scannedDevices.length > 0"
        class="scan-clear-btn"
        @click="scannedDevices = []"
      >
        清空结果
      </button>
    </div>

    <div v-if="scannedDevices.length === 0" class="scan-empty">
      正在等待设备广播...
    </div>

    <TransitionGroup name="ios-card" tag="div" class="scan-list">
      <div
        v-for="(item, index) in scannedDevices"
        :key="`${item.chipId || 'unknown'}-${index}`"
        class="scan-item"
      >
        <div class="scan-item-info">
          <div>{{ item.chipId }}</div>
          <div>IP：{{ item.ip || '未知' }}</div>
          <div>类型：{{ item.deviceType || '未知' }}</div>
        </div>

        <div class="scan-item-actions">
          <button class="scan-add-btn" @click="openAddFromScan(item)">
            添加设备
          </button>
          <button class="scan-cancel-btn" @click="removeScannedDevice(item.chipId)">
            取消
          </button>
        </div>
      </div>
    </TransitionGroup>
  </div>
</Transition>
        <DeviceGrid
          :devices="devices"
          :loading="loading"
          :deleting-id="deletingId"
          @refresh="loadDevices"
          @update-realtime="handleRealtimeUpdate"
          @delete="handleDeleteDevice"
        />

      </section> 


  <DeviceAddModal
    v-if="showAddDeviceModal"
    :submitting="creating"
    :initial-data="pendingScannedDevice"
    @close="closeAddDeviceModal"
    @submit="handleCreateDevice"
  />

      <section v-show="activeTab === 'flow'" class="page-section">
        <FlowOverview
          :devices="devices"
          :latest-lux="latestLux"
          :current-area="envInfo.area"
        />
      </section>

      <section v-show="activeTab === 'settings'" class="page-section">
        <div class="settings-layout">
          <StoreSettingsPanel v-model="storeSettings" />
          <DurationQueryPanel />
          <ArmControlPanel :devices="devices" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import SidebarNav from '../components/layout/SidebarNav.vue'
import TopStatusBar from '../components/layout/TopStatusBar.vue'
import DeviceGrid from '../components/device/DeviceGrid.vue'
import DeviceAddModal from '../components/device/DeviceAddModal.vue'
import { useClock } from '../composables/useClock'
import { useWebSocket } from '../composables/useWebSocket'
import {
  createDevice,
  deleteDevice,
  getDeviceList,
  getOnlineList,
  updateDevice,
} from '../api/device'
import { getLatestLux } from '../api/lux'
import type {
  DashboardTab,
  DeviceCreatePayload,
  DeviceItem,
  DeviceOnlineItem,
} from '../types/device'
import DurationQueryPanel from '../components/settings/DurationQueryPanel.vue'
import ArmControlPanel from '../components/settings/ArmControlPanel.vue'
import StoreSettingsPanel from '../components/settings/StoreSettingsPanel.vue'
import type { StoreSettingsValue } from '../components/settings/StoreSettingsPanel.vue'
import FlowOverview from '../components/flow/FlowOverview.vue'

const activeTab = ref<DashboardTab>('main')
const devices = ref<DeviceItem[]>([])
const loading = ref(false)
const creating = ref(false)
const deletingId = ref<number | null>(null)
const scanStatus = ref('未扫描')
const serverHost = ref('127.0.0.1')
const showAddDeviceModal = ref(false)

const scannedDevices = ref<
  Array<{
    chipId: string
    ip: string
    deviceType?: string

    mac?: string
    added?: boolean
  }>
>([])

function removeScannedDevice(chipId: string) {
  scannedDevices.value = scannedDevices.value.filter(item => item.chipId !== chipId)

  if (scanning.value) {
    scanStatus.value = `扫描中，已发现 ${scannedDevices.value.length} 台待添加设备`
  } else {
    scanStatus.value = `扫描结束，发现 ${scannedDevices.value.length} 台待添加设备`
  }
}

const scanning = ref(false)
const pendingScannedDevice = ref<{
  chipId: string
  ip: string
  deviceType?: string
  deviceNo?: string
} | null>(null)

let scanTimer: number | null = null

const { currentTime, dateInfo, weekInfo } = useClock()

const weatherText = ref('天气信息待接入')
const holidayInfo = ref('是否节假日：否')
const workdayInfo = ref('是否工作日：是')
const latestLuxText = ref('💡 光照值等待更新中...')
const latestLux = ref<number | null>(null)

const envInfo = ref({
  temp: 22,
  people: 0,
  area: 80,
})

function parseStoreType(value: string) {
  const [label, temp] = value.split(',')
  return {
    label,
    temp: Number(temp || 4000),
  }
}

function parseStoreSize(value: string) {
  const [label, area] = value.split(',')
  return {
    label,
    area: Number(area || 80),
  }
}

const storeSettings = ref<StoreSettingsValue>({
  region: {
    province: 'hunan',
    provinceLabel: '湖南省',
    city: '28.1894,112.9861',
    cityLabel: '长沙市',
  },
  storeType: '高端,3500',
  storeSize: '高端,80',
  isNightMode: false,
})

watch(
  storeSettings,
  async (val) => {
    const storeTypeInfo = parseStoreType(val.storeType)
    const storeSizeInfo = parseStoreSize(val.storeSize)

    weatherText.value = `${val.region.provinceLabel} · ${val.region.cityLabel}`
    envInfo.value.area = storeSizeInfo.area
    envInfo.value.temp = val.isNightMode ? 20 : 24

    for (const device of devices.value) {
      if (!device.autoMode) continue

      const nextPayload: DeviceCreatePayload = {
      chipId: device.chipId || '',
      ip: device.ip || '',
      displayName: device.displayName || '',
      brightness: device.brightness ?? 50,
      temp: device.temp ?? 4000,
      autoMode: device.autoMode ?? false,
      recommendedBrightness: device.recommendedBrightness ?? 50,
      recommendedTemp: storeTypeInfo.temp ?? 4000,
      fabric: device.fabric || '',
      mainColorRgb: device.mainColorRgb || '',
    }

      try {
        await updateDevice(device.id, nextPayload)
        device.recommendedTemp = storeTypeInfo.temp
      } catch (error) {
        console.error('sync recommendedTemp error =', error)
      }
    }
  },
  { deep: true, immediate: true },
)

function mergeDeviceOnline(deviceList: DeviceItem[], onlineList: DeviceOnlineItem[]) {
  const onlineMap = new Map(
    (onlineList || []).map(item => [item.chipId, item]),
  )

  return (deviceList || []).map(device => {
    const onlineInfo = onlineMap.get(device.chipId)
    return {
      ...device,
      online: onlineInfo?.online ?? false,
      lastSeen: onlineInfo?.lastSeen,
      ip: onlineInfo?.ip || device.ip,
    }
  })
}

async function loadDevices() {
  loading.value = true
  scanStatus.value = scanning.value ? '扫描中（10秒）...' : '加载中...'

  try {
    const [deviceList, onlineList] = await Promise.all([
      getDeviceList(),
      getOnlineList(),
    ])

    devices.value = mergeDeviceOnline(deviceList, onlineList)

    if (!scanning.value) {
      scanStatus.value = `已加载 ${devices.value.length} 台设备`
    }

    await loadLatestLux()
  } catch (error) {
    console.error('loadDevices error =', error)
    scanStatus.value = '设备加载失败'
  } finally {
    loading.value = false
  }
}

async function loadLatestLux() {
  try {
    if (devices.value.length === 0) {
      latestLux.value = null
      latestLuxText.value = '💡 光照值等待更新中...'
      return
    }

    for (const device of devices.value) {
      try {
        const record = await getLatestLux(device.chipId)

        if (record && record.luxValue != null) {
          latestLux.value = record.luxValue
          latestLuxText.value = `💡 光照值：${record.luxValue} lux`
          return
        }
      } catch (error) {
        console.warn(`device ${device.chipId} has no lux record`)
      }
    }

    latestLux.value = null
    latestLuxText.value = '💡 暂无光照数据'
  } catch (error) {
    console.error('loadLatestLux error =', error)
    latestLuxText.value = '💡 光照数据加载失败'
  }
}

function handleScan() {
  scannedDevices.value = []
  scanning.value = true
  scanStatus.value = '扫描中（10秒）...'

  if (scanTimer) {
    window.clearTimeout(scanTimer)
    scanTimer = null
  }

  scanTimer = window.setTimeout(() => {
    scanning.value = false
    scanStatus.value = `扫描结束，发现 ${scannedDevices.value.length} 台待添加设备`
    scanTimer = null
  }, 10000)
}

function openManualAdd() {
  pendingScannedDevice.value = null
  showAddDeviceModal.value = true
}

function openAddFromScan(device: {
  chipId: string
  ip: string
  deviceType?: string
  deviceNo?: string
}) {
  pendingScannedDevice.value = {
    chipId: device.chipId || '',
    ip: device.ip || '',
    deviceType: device.deviceType || '',
    deviceNo: device.deviceNo || '',
  }
  showAddDeviceModal.value = true
}

function closeAddDeviceModal() {
  showAddDeviceModal.value = false
  pendingScannedDevice.value = null
}

async function handleCreateDevice(payload: DeviceCreatePayload) {
  creating.value = true
  try {
    await createDevice(payload)

    showAddDeviceModal.value = false
    pendingScannedDevice.value = null

    scannedDevices.value = scannedDevices.value.filter(
      item => item.chipId !== payload.chipId,
    )

    await loadDevices()
  } catch (error) {
    console.error('createDevice error =', error)
    alert('添加设备失败')
  } finally {
    creating.value = false
  }
}

const updateTimerMap = new Map<number, number>()

function handleRealtimeUpdate({ id, payload }: { id: number; payload: DeviceCreatePayload }) {
  const oldTimer = updateTimerMap.get(id)
  if (oldTimer) {
    window.clearTimeout(oldTimer)
  }

  const timer = window.setTimeout(async () => {
    try {
      await updateDevice(id, payload)

      const index = devices.value.findIndex(item => item.id === id)
      if (index >= 0) {
        devices.value[index] = {
          ...devices.value[index],
          ...payload,
        }
      }
    } catch (error) {
      console.error('realtime update error =', error)
    } finally {
      updateTimerMap.delete(id)
    }
  }, 150)

  updateTimerMap.set(id, timer)
}

async function handleDeleteDevice(id: number) {
  deletingId.value = id
  try {
    await deleteDevice(id)
    await loadDevices()
  } catch (error) {
    console.error('deleteDevice error =', error)
    alert('删除设备失败')
  } finally {
    deletingId.value = null
  }
}

function updateDeviceByIncoming(incoming: Partial<DeviceItem>) {
  const index = devices.value.findIndex(item => {
    if (incoming.id != null && item.id === incoming.id) return true
    if (incoming.chipId && item.chipId === incoming.chipId) return true
    return false
  })

  if (index < 0) return

  devices.value[index] = {
    ...devices.value[index],
    ...incoming,
  }
}

function handleWsMessage(message: any) {
  if (!message?.type) return

  if (message.type === 'state' && message.data) {
    updateDeviceByIncoming(message.data)
    return
  }

  if (message.type === 'onlineStatus' && message.data) {
    updateDeviceByIncoming({
      chipId: message.data.chipId,
      ip: message.data.ip,
      online: message.data.online,
      lastSeen: message.data.lastSeen,
    })
    return
  }

  if (message.type === 'deviceDeleted' && message.data?.id) {
    devices.value = devices.value.filter(item => item.id !== message.data.id)
    return
  }

  if (message.type === 'lux') {
    const luxValue = Number(
      message?.data?.luxValue ??
      message?.data?.lux ??
      message?.value ??
      0
    )

    latestLux.value = luxValue
    latestLuxText.value = `💡 光照值：${luxValue} lux`
    console.log('lux message =', message)
    return
  }

  if (message.type === 'announce' && message.data) {
    if (!scanning.value) return

    const chipId = String(
      message.data.chipId ??
      message.data.deviceCode ??
      ''
    ).trim()

    if (!chipId) return

    const alreadyAdded = devices.value.some(item => item.chipId === chipId)
    if (alreadyAdded) return

    const added = Boolean(message.data.added)
    if (added) return

    const exists = scannedDevices.value.some(item => item.chipId === chipId)
    if (exists) return

    scannedDevices.value = [
      ...scannedDevices.value,
      {
        chipId,
        ip: String(message.data.ip ?? '').trim(),
        deviceType: String(message.data.deviceType ?? '').trim(),
        mac: String(message.data.mac ?? '').trim(),
        added: false,
      },
    ]

    scanStatus.value = `扫描中，已发现 ${scannedDevices.value.length} 台待添加设备`
    return
  }
}

const wsUrl = computed(() => `ws://${serverHost.value}:3000/ws`)
const { connected } = useWebSocket(wsUrl, handleWsMessage)

watch(connected, (val) => {
  if (val) {
    scanStatus.value = '实时连接已建立'
    loadDevices()
  } else {
    scanStatus.value = 'WebSocket 未连接'
  }
})

onMounted(() => {
  loadDevices()
})

onBeforeUnmount(() => {
  if (scanTimer) {
    window.clearTimeout(scanTimer)
    scanTimer = null
  }

  updateTimerMap.forEach(timer => {
    window.clearTimeout(timer)
  })
  updateTimerMap.clear()
})
</script>