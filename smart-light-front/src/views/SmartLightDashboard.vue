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
          <button @click="handleScan">扫描设备</button>
          <button @click="showAddDeviceModal = true">手动添加设备</button>
          <label>
            服务器地址：
            <input v-model.trim="serverHost" type="text" placeholder="127.0.0.1" />
          </label>
          <div id="scanStatus">
            {{ connected ? `WS 已连接 · ${scanStatus}` : `WS 未连接 · ${scanStatus}` }}
          </div>
        </div>

        <DeviceGrid
          :devices="devices"
          :loading="loading"
          :deleting-id="deletingId"
          @refresh="loadDevices"
          @update-realtime="handleRealtimeUpdate"
          @delete="handleDeleteDevice"
        />

        <DeviceAddModal
          v-if="showAddDeviceModal"
          :submitting="creating"
          @close="showAddDeviceModal = false"
          @submit="handleCreateDevice"
        />
      </section>

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
import { computed, onMounted, ref, watch } from 'vue'
import SidebarNav from '../components/layout/SidebarNav.vue'
import TopStatusBar from '../components/layout/TopStatusBar.vue'
import DeviceGrid from '../components/device/DeviceGrid.vue'
import DeviceAddModal from '../components/device/DeviceAddModal.vue'
import { useClock } from '../composables/useClock'
import { useWebSocket } from '../composables/useWebSocket'
import { createDevice, deleteDevice, getDeviceList, getOnlineList, updateDevice } from '../api/device'
import type { DashboardTab, DeviceCreatePayload, DeviceItem, DeviceOnlineItem } from '../types/device'
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

import { getLatestLux } from '../api/lux'
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

    // 1. 城市显示联动
    weatherText.value = `${val.region.provinceLabel} · ${val.region.cityLabel}`

    // 2. 面积联动
    envInfo.value.area = storeSizeInfo.area

    // 3. 温度联动：这里先用一个简单展示值
    envInfo.value.temp = val.isNightMode ? 20 : 24

    // 4. 自动模式设备的推荐色温联动
    for (const device of devices.value) {
      if (!device.autoMode) continue

      const nextPayload: DeviceCreatePayload = {
        chipId: device.chipId,
        ip: device.ip,
        brightness: device.brightness,
        temp: device.temp,
        autoMode: device.autoMode,
        recommendedBrightness: device.recommendedBrightness,
        recommendedTemp: storeTypeInfo.temp,
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
  const onlineMap = new Map(onlineList.map(item => [item.chipId, item]))

  return deviceList.map(device => {
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
  scanStatus.value = '加载中...'

  try {
    const [deviceList, onlineList] = await Promise.all([
      getDeviceList(),
      getOnlineList(),
    ])

    devices.value = mergeDeviceOnline(deviceList, onlineList)
    scanStatus.value = `已加载 ${devices.value.length} 台设备`

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
        // 当前设备没光照记录就继续找下一台
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
  loadDevices()
}

async function handleCreateDevice(payload: DeviceCreatePayload) {
  creating.value = true
  try {
    await createDevice(payload)
    showAddDeviceModal.value = false
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

  if (message.type === 'announce') {
    loadDevices()
    return
  }
}

const wsUrl = computed(() => `ws://${serverHost.value}:3000/ws`)
const { connected } = useWebSocket(wsUrl, handleWsMessage)

watch(connected, (val) => {
  if (val) {
    scanStatus.value = '实时连接已建立'
    loadDevices()
  }
})

onMounted(() => {
  loadDevices()
})
</script>