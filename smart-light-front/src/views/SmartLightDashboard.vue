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
        <StoreSettingsPanel
          v-model="storeSettings"
          :store-name="currentStoreName"
          @logout="handleLogout"
          @open-store-settings="goStoreSettings"
        />
        <div class="settings-row">
          <DurationQueryPanel class="settings-half-card" />
          <ArmControlPanel
            class="settings-half-card"
            :devices="devices"
          />
        </div>

        <FlowMonitorPanel
          class="settings-full-card"
          :devices="devices"
        />
      </div>
       <SmartConfigPanel class="settings-full-card" />
    </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SidebarNav from '../components/layout/SidebarNav.vue'
import TopStatusBar from '../components/layout/TopStatusBar.vue'
import DeviceGrid from '../components/device/DeviceGrid.vue'
import DeviceAddModal from '../components/device/DeviceAddModal.vue'
import FlowMonitorPanel from '../components/settings/FlowMonitorPanel.vue'
import SmartConfigPanel from '../components/settings/SmartConfigPanel.vue'
import { useClock } from '../composables/useClock'
import { useWebSocket } from '../composables/useWebSocket'
import {
  createDevice,
  deleteDevice,
  getMyDeviceListApi,
  getOnlineList,
  updateDevice,
} from '../api/device'
import { getLatestLux } from '../api/lux'
import { getCurrentStoreApi } from '../api/store'
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
import { regions } from '../constants/china-region'
import { STORE_STYLE_MAP } from '../constants/store'
const router = useRouter()
const route = useRoute()

function getInitialTab(): DashboardTab {
  const tab = route.query.tab

  if (tab === 'main' || tab === 'flow' || tab === 'settings') {
    return tab
  }

  return 'main'
}

const activeTab = ref<DashboardTab>(getInitialTab())
const devices = ref<DeviceItem[]>([])
const loading = ref(false)
const creating = ref(false)
const deletingId = ref<number | null>(null)
const scanStatus = ref('未扫描')
const serverHost = ref('127.0.0.1')
const showAddDeviceModal = ref(false)
const currentStoreName = ref('')


const scannedDevices = ref<
  Array<{
    chipId: string
    ip: string
    deviceType?: string

    mac?: string
    added?: boolean
  }>
>([])

const storeSettingsReady = ref(false)

function handleLogout() {
  localStorage.removeItem('TOKEN')
  localStorage.removeItem('USER_INFO')
  localStorage.removeItem('storeSetup')
  localStorage.removeItem('REMEMBER_USERNAME')

  sessionStorage.removeItem('TOKEN')
  sessionStorage.removeItem('USER_INFO')
  sessionStorage.removeItem('storeSetup')

  router.replace('/login')
}

function goStoreSettings() {
  router.push('/store-profile')
}
const STYLE_TEMP_MAP: Record<string, number> = {
  HIGH_END: 3500,
  MASS_MARKET: 4000,
  FAST_FASHION: 4500,
}

function buildStoreTypeValue(storeStyle: string) {
  const label = STORE_STYLE_MAP[storeStyle] || '大众'
  const temp = STYLE_TEMP_MAP[storeStyle] || 4000
  return `${label},${temp}`
}

function buildStoreSizeValue(area: number | string | undefined) {
  const num = Number(area || 80)
  let label = '中型'
  if (num <= 60) label = '小型'
  if (num >= 150) label = '大型'
  return `${label},${num}`
}

function findRegionValue(provinceLabel: string, cityLabel: string) {
  for (const province of regions) {
    if (province.label !== provinceLabel) continue

    const city = province.cities.find(item => item.label === cityLabel)
    if (city) {
      return {
        province: province.value,
        provinceLabel: province.label,
        city: city.value,
        cityLabel: city.label,
      }
    }

    return {
      province: province.value,
      provinceLabel: province.label,
      city: '',
      cityLabel,
    }
  }

  return {
    province: '',
    provinceLabel: provinceLabel || '',
    city: '',
    cityLabel: cityLabel || '',
  }
}

async function loadCurrentStore() {
  try {
    const store = await getCurrentStoreApi()
    const region = findRegionValue(store.province, store.city)

    storeSettingsReady.value = false
    storeSettings.value = {
      ...storeSettings.value,
      region,
      storeType: buildStoreTypeValue(store.storeStyle),
      storeSize: buildStoreSizeValue(store.area),
    }

    currentStoreName.value = store.storeName || ''
    weatherText.value = `${store.province} · ${store.city}`
    envInfo.value.area = Number(store.area || 80)
    return true
  } catch (error: any) {
    console.error('loadCurrentStore error =', error)

    const msg = error?.response?.data?.msg || error?.message || ''
    if (msg.includes('当前用户未绑定店铺')) {
      router.push('/store-setup')
      return false
    }
    return false
  } finally {
    storeSettingsReady.value = true
  }
}

onMounted(async () => {
  const ok = await loadCurrentStore()
  if (!ok) return
  await loadDevices()
})

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
    if (!storeSettingsReady.value) return

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
  { deep: true },
)

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'main' || tab === 'flow' || tab === 'settings') {
      activeTab.value = tab
    }
  },
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
      getMyDeviceListApi(),
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

const wsUrl = computed(() => {
  const token = localStorage.getItem('TOKEN') || sessionStorage.getItem('TOKEN') || ''
  return `ws://${serverHost.value}:3000/ws?token=${encodeURIComponent(token)}`
})
const { connected } = useWebSocket(wsUrl, handleWsMessage)

watch(connected, (val) => {
  if (val) {
    scanStatus.value = '实时连接已建立'
    loadDevices()
  } else {
    scanStatus.value = 'WebSocket 未连接'
  }
})

onMounted(async () => {
  await loadCurrentStore()
  await loadDevices()
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

<style scoped>

.app-container {
  display: flex;
  min-height: 100vh;
}

.page-section {
  position: relative;
}

.section-space-top {
  margin-top: 16px;
}

.env-layout {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}

.env-card {
  flex: 1 1 48%;
  min-width: 300px;
  background: #fff;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.env-info {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

#metaInfo {
  display: grid;
  grid-template-columns: auto auto;
  column-gap: 30px;
  row-gap: 6px;
}

.lux-display {
  margin-top: 1em;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
}

.settings-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
  align-items: stretch;
}

.settings-half-card {
  min-width: 0;
  height: 100%;
}

.settings-full-card {
  width: 100%;
}

.night-mode {
  background: linear-gradient(180deg, #1f2329 0%, #14181f 100%);
  color: #e5eaf3;
}

.night-mode .main-content {
  background: transparent;
}

/* 夜间模式下的组件样式调整 */
.night-mode .env-card,
.night-mode .lamp-card,
.night-mode .settings-card,
.night-mode .placeholder-card,
.night-mode .empty-block,
.night-mode #controls,
.night-mode .sidebar {
  background: #23272f;
  color: #e5eaf3;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.22);
}

.night-mode .device-meta,
.night-mode .field-label,
.night-mode .checkbox-row,
.night-mode .settings-title,
.night-mode .lux-display,
.night-mode .readonly-box {
  color: #c9d1d9;
}

.night-mode .readonly-box,
.night-mode .date-input,
.night-mode .text-input,
.night-mode .region-input {
  background: #2b313a;
  border-color: #3a4452;
  color: #e5eaf3;
}

.night-mode .sidebar li.active,
.night-mode .sidebar li:hover {
  background: rgba(64, 158, 255, 0.18);
}


.scan-panel {
  margin: 20px 0 24px;
  padding: 20px 22px;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.scan-panel-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 16px;
}

.scan-empty {
  min-height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
}

.scan-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.scan-item {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 18px 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  border: 1px solid #dbeafe;
  border-radius: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.scan-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12);
}

.scan-item-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #334155;
  font-size: 15px;
  line-height: 1.5;
}

.scan-item-info div:first-child {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.scan-add-btn {
  align-self: flex-start;
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22);
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.scan-add-btn:hover {
  transform: translateY(-1px);
  opacity: 0.96;
}

.scan-add-btn:active {
  transform: translateY(0);
}

.scan-add-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.scan-item-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.scan-cancel-btn {
  align-self: flex-start;
  padding: 10px 18px;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, opacity 0.15s ease, border-color 0.15s ease;
}

.scan-cancel-btn:hover {
  transform: translateY(-1px);
  border-color: #94a3b8;
  opacity: 0.96;
}

.scan-panel {
  margin: 20px 0 24px;
  padding: 20px 22px;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 22px;
  box-shadow:
    0 12px 40px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(18px) saturate(1.08);
  -webkit-backdrop-filter: blur(18px) saturate(1.08);
}

.scan-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.scan-panel-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
}

.scan-clear-btn {
  padding: 8px 14px;
  border: 1px solid rgba(203, 213, 225, 0.95);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition:
    transform 180ms cubic-bezier(0.2, 0.8, 0.2, 1),
    box-shadow 180ms cubic-bezier(0.2, 0.8, 0.2, 1),
    opacity 180ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.scan-clear-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.scan-clear-btn:active {
  transform: scale(0.965);
}

.scan-empty {
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: rgba(248, 250, 252, 0.9);
  border: 1px dashed #cbd5e1;
  border-radius: 18px;
}

.scan-list {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.scan-item {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 18px 16px;
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.98) 0%, rgba(238, 246, 255, 0.98) 100%);
  border: 1px solid rgba(219, 234, 254, 0.95);
  border-radius: 18px;
  box-shadow:
    0 8px 24px rgba(59, 130, 246, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  transition:
    transform 220ms cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 220ms cubic-bezier(0.22, 1, 0.36, 1);
}

.scan-item:hover {
  transform: translateY(-2px);
  box-shadow:
    0 14px 30px rgba(59, 130, 246, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.scan-item-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #334155;
  font-size: 15px;
  line-height: 1.5;
}

.scan-item-info div:first-child {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.scan-item-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.scan-add-btn,
.scan-cancel-btn,
#controls > button,
.btn-confirm,
.btn-cancel {
  transition:
    transform 180ms cubic-bezier(0.2, 0.8, 0.2, 1),
    box-shadow 180ms cubic-bezier(0.2, 0.8, 0.2, 1),
    opacity 180ms cubic-bezier(0.2, 0.8, 0.2, 1);
  transform-origin: center;
}

.scan-add-btn:active,
.scan-cancel-btn:active,
#controls > button:active,
.btn-confirm:active,
.btn-cancel:active {
  transform: scale(0.965);
}

.scan-add-btn {
  align-self: flex-start;
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.22);
}

.scan-add-btn:hover {
  transform: translateY(-1px);
  opacity: 0.97;
}

.scan-cancel-btn {
  align-self: flex-start;
  padding: 10px 18px;
  border: 1px solid rgba(203, 213, 225, 0.95);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.scan-cancel-btn:hover {
  transform: translateY(-1px);
  opacity: 0.97;
}

/* iOS 风格：扫描面板 */
.ios-panel-enter-active {
  transition:
    opacity 420ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 420ms cubic-bezier(0.22, 1, 0.36, 1),
    filter 420ms cubic-bezier(0.22, 1, 0.36, 1);
  will-change: opacity, transform, filter;
}

.ios-panel-leave-active {
  transition:
    opacity 260ms cubic-bezier(0.4, 0, 1, 1),
    transform 260ms cubic-bezier(0.4, 0, 1, 1),
    filter 260ms cubic-bezier(0.4, 0, 1, 1);
  will-change: opacity, transform, filter;
}

.ios-panel-enter-from {
  opacity: 0;
  transform: translateY(18px) scale(0.965);
  filter: blur(8px);
}

.ios-panel-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.ios-panel-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.ios-panel-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
  filter: blur(6px);
}

.text-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
}
@media (max-width: 768px) {
  .app-container {
    flex-direction: column;
  }

  .main-content {
    padding: 12px;
  }
}
</style>