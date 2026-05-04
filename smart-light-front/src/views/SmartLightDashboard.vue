<template>
  <div class="app-container" :class="{ 'night-mode': storeSettings.isNightMode }">
    <SidebarNav v-model="activeTab" />

    <div class="main-content">
      <section v-show="activeTab === 'main'" class="page-section">
        <div class="dashboard-top-status">
          <div class="current-time">{{ currentTime }}</div>
          <div class="weather-status-row">
            <span>{{ weatherText }}</span>
            <span class="weather-svg-icon" :class="`weather-${weatherIconType}`" aria-hidden="true">
              <svg viewBox="0 0 36 36" focusable="false">
                <g v-if="weatherIconType === 'sunny'" class="weather-sunny">
                  <circle class="sun-core" cx="18" cy="18" r="6.4" />
                  <path class="sun-rays" d="M18 4.5v4M18 27.5v4M4.5 18h4M27.5 18h4M8.4 8.4l2.8 2.8M24.8 24.8l2.8 2.8M27.6 8.4l-2.8 2.8M11.2 24.8l-2.8 2.8" />
                </g>

                <g v-else>
                  <g v-if="weatherIconType === 'partly-cloudy'" class="weather-sun-small">
                    <circle class="sun-core" cx="13" cy="13" r="4.4" />
                    <path class="sun-rays" d="M13 5.5v2.4M13 18.1v2.4M5.5 13h2.4M18.1 13h2.4M7.7 7.7l1.7 1.7M16.6 16.6l1.7 1.7M18.3 7.7l-1.7 1.7M9.4 16.6l-1.7 1.7" />
                  </g>

                  <path class="cloud-shape" d="M10.9 25.7h15.2a5.4 5.4 0 0 0 .4-10.8 8 8 0 0 0-15.3-1.8 6.4 6.4 0 0 0-.3 12.6Z" />

                  <g v-if="weatherIconType === 'rain'" class="rain-lines">
                    <path d="M14 28.2l-1.7 3.1M21 28.2l-1.7 3.1M27 28l-1.5 2.8" />
                  </g>

                  <g v-if="weatherIconType === 'snow'" class="snow-marks">
                    <path d="M14 29.5v3M12.7 31h2.6M22 29.5v3M20.7 31h2.6" />
                  </g>

                  <g v-if="weatherIconType === 'fog'" class="fog-lines">
                    <path d="M8.5 28.6h19M10.8 32h14.4" />
                  </g>

                  <g v-if="weatherIconType === 'thunder'" class="thunder-bolt">
                    <path d="M19.2 27.4l-3 5h3l-1.1 3.1 4.3-5.2h-3.2l.3-2.9Z" />
                  </g>
                </g>
              </svg>
            </span>
            <span>{{ weekInfo }}</span>
            <span>{{ dateInfo }}</span>
          </div>
        </div>

        <div class="env-layout card-section section-space-top">
          <div class="env-card">
            <h4>实时概况</h4>
            <div class="stat-grid">
              <div class="stat-item">
                <span class="stat-label">温度</span>
                <strong class="stat-value">{{ formatWeatherMetric(envInfo.temp, '℃') }}</strong>
              </div>
              <div class="stat-item">
                <span class="stat-label">体感</span>
                <strong class="stat-value">{{ formatWeatherMetric(envInfo.apparentTemp, '℃') }}</strong>
              </div>
              <div class="stat-item">
                <span class="stat-label">湿度</span>
                <strong class="stat-value">{{ formatWeatherMetric(envInfo.humidity, '%') }}</strong>
              </div>
              <div class="stat-item">
                <span class="stat-label">人流量</span>
                <strong class="stat-value">{{ envInfo.people }} 人</strong>
              </div>
              <div class="stat-item">
                <span class="stat-label">面积</span>
                <strong class="stat-value">{{ envInfo.area }} ㎡</strong>
              </div>
            </div>
          </div>

          <div class="env-card">
            <div class="meta-grid">
              <div class="meta-item">
                <span class="stat-label">节假日</span>
                <strong class="stat-value">{{ holidayValue }}</strong>
              </div>
              <div class="meta-item">
                <span class="stat-label">工作日</span>
                <strong class="stat-value">{{ workdayValue }}</strong>
              </div>
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

       <div class="store-layout-row">
          <LightEffectMiniPanel
            class="store-effect-mini"
            :devices="devices"
          />

          <StoreLightLayout
            class="store-layout-main"
            :devices="devices"
            @saved="loadDevices"
          />
        </div>

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

    <section v-show="activeTab === 'firmware'" class="page-section">
      <FirmwareManagePanel />
    </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SidebarNav from '../components/layout/SidebarNav.vue'
import DeviceGrid from '../components/device/DeviceGrid.vue'
import DeviceAddModal from '../components/device/DeviceAddModal.vue'
import FlowMonitorPanel from '../components/settings/FlowMonitorPanel.vue'
import SmartConfigPanel from '../components/settings/SmartConfigPanel.vue'
import StoreLightLayout from '../components/device/StoreLightLayout.vue'
import LightEffectMiniPanel from '../components/device/LightEffectMiniPanel.vue'
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
import { getCurrentWeather } from '../api/weather'
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
import FirmwareManagePanel from '../components/firmware/FirmwareManagePanel.vue'
import { regions } from '../constants/china-region'
import { STORE_STYLE_MAP } from '../constants/store'
const router = useRouter()
const route = useRoute()

function getInitialTab(): DashboardTab {
  const tab = route.query.tab

  if (tab === 'main' || tab === 'flow' || tab === 'settings' || tab === 'firmware') {
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
const showAddDeviceModal = ref(false)
const currentStoreName = ref('')

const API_BASE = import.meta.env.VITE_API_BASE

const serverHost = computed(() => {
  return new URL(API_BASE).host
})

const wsUrl = computed(() => {
  const token = localStorage.getItem('TOKEN') || sessionStorage.getItem('TOKEN') || ''
  return `${API_BASE.replace(/^http/, 'ws')}/ws?token=${encodeURIComponent(token)}`
})

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
const NIGHT_MODE_STORAGE_KEY = 'SMART_LIGHT_NIGHT_MODE'

function readPersistedNightMode() {
  return localStorage.getItem(NIGHT_MODE_STORAGE_KEY) === '1'
}

function persistNightMode(value: boolean) {
  localStorage.setItem(NIGHT_MODE_STORAGE_KEY, value ? '1' : '0')
}

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
    if (!store?.id) {
      weatherText.value = '天气：暂无'
      envInfo.value.weather = '暂无'
      envInfo.value.temp = null
      envInfo.value.apparentTemp = null
      envInfo.value.humidity = null
      envInfo.value.weatherCode = null
      hasWeatherData.value = false
      return false
    }

    const region = findRegionValue(store.province || '', store.city || '')

    storeSettingsReady.value = false
    storeSettings.value = {
      ...storeSettings.value,
      region,
      storeType: buildStoreTypeValue(store.storeStyle || ''),
      storeSize: buildStoreSizeValue(store.area),
    }

    currentStoreName.value = store.storeName || ''
    currentStoreCityName.value = store.city || store.province || ''
    if (!hasWeatherData.value) {
      weatherText.value = '天气：暂无'
    }
    envInfo.value.area = Number(store.area || 80)
    await loadWeather(store.id)
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

const weatherText = ref('天气：暂无')
const holidayInfo = ref('是否节假日：否')
const workdayInfo = ref('是否工作日：是')
const latestLuxText = ref('光照值等待更新中...')
const latestLux = ref<number | null>(null)
const currentStoreCityName = ref('')
const envInfo = ref({
  temp: null as number | null,
  apparentTemp: null as number | null,
  humidity: null as number | null,
  weather: '暂无',
  weatherCode: null as number | null,
  windSpeed: null as number | null,
  people: 0,
  area: 80,
})

const hasWeatherData = ref(false)
type WeatherIconType = 'sunny' | 'partly-cloudy' | 'cloudy' | 'rain' | 'snow' | 'fog' | 'thunder'

const holidayValue = computed(() => extractInfoValue(holidayInfo.value))
const workdayValue = computed(() => extractInfoValue(workdayInfo.value))
const weatherIconType = computed(() => mapOpenMeteoCodeToWeatherIcon(envInfo.value.weatherCode))

function extractInfoValue(value: string) {
  const parts = value.split(/[：:]/)
  return (parts.length > 1 ? parts[parts.length - 1] : value).trim() || '--'
}

function formatWeatherMetric(value: number | null | undefined, unit: string) {
  if (!hasWeatherData.value || value === null || value === undefined || Number.isNaN(Number(value))) {
    return '暂无'
  }
  return `${Number(value).toFixed(1)}${unit}`
}

function buildWeatherSummary() {
  const city = currentStoreCityName.value.trim()
  const weather = envInfo.value.weather || '暂无'
  return city ? `${city} · ${weather}` : weather
}

function mapOpenMeteoCodeToWeatherIcon(weatherCode?: number | null): WeatherIconType {
  if (weatherCode === 0) return 'sunny'
  if (weatherCode === 1 || weatherCode === 2) return 'partly-cloudy'
  if (weatherCode === 3) return 'cloudy'
  if (weatherCode === 45 || weatherCode === 48) return 'fog'
  if ([51, 53, 55, 61, 63, 65, 80, 81, 82].includes(Number(weatherCode))) return 'rain'
  if ([71, 73, 75, 77].includes(Number(weatherCode))) return 'snow'
  if ([95, 96, 99].includes(Number(weatherCode))) return 'thunder'
  return 'cloudy'
}

async function loadWeather(storeId?: string) {
  if (!storeId) {
    weatherText.value = '天气：暂无'
    envInfo.value.weather = '暂无'
    hasWeatherData.value = false
    return
  }

  try {
    const weather = await getCurrentWeather(storeId)
    envInfo.value.temp = weather.temperature ?? envInfo.value.temp
    envInfo.value.apparentTemp = weather.apparentTemperature ?? null
    envInfo.value.humidity = weather.humidity ?? null
    envInfo.value.weather = weather.weatherText || '暂无'
    envInfo.value.weatherCode = weather.weatherCode ?? null
    envInfo.value.windSpeed = weather.windSpeed ?? null
    weatherText.value = buildWeatherSummary()
    hasWeatherData.value = true
  } catch (error) {
    console.error('load weather error =', error)
    weatherText.value = '天气：暂无'
    envInfo.value.weather = '暂无'
    envInfo.value.temp = null
    envInfo.value.apparentTemp = null
    envInfo.value.humidity = null
    envInfo.value.weatherCode = null
    hasWeatherData.value = false
  }
}

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
  isNightMode: readPersistedNightMode(),
})

watch(
  storeSettings,
  async (val) => {
    if (!storeSettingsReady.value) return

    const storeTypeInfo = parseStoreType(val.storeType)
    const storeSizeInfo = parseStoreSize(val.storeSize)

    if (!hasWeatherData.value) {
      const provinceLabel = val.region?.provinceLabel || ''
      const cityLabel = val.region?.cityLabel || ''
      currentStoreCityName.value = cityLabel || provinceLabel || ''
      weatherText.value = '天气：暂无'
    }
    envInfo.value.area = storeSizeInfo.area
    persistNightMode(val.isNightMode)

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
    if (tab === 'main' || tab === 'flow' || tab === 'settings' || tab === 'firmware') {
      activeTab.value = tab
    }
  },
)
function normalizeChipId(value?: string) {
  return String(value || '').trim().toUpperCase()
}

function mergeDeviceOnline(deviceList: DeviceItem[], onlineList: DeviceOnlineItem[]) {
  const onlineMap = new Map(
    (onlineList || []).map(item => [normalizeChipId(item.chipId), item]),
  )

  return (deviceList || []).map(device => {
    const onlineInfo = onlineMap.get(normalizeChipId(device.chipId))

    return {
      ...device,
      online: onlineInfo?.online === true,
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
      latestLuxText.value = '光照值等待更新中...'
      return
    }

    for (const device of devices.value) {
      try {
        const record = await getLatestLux(device.chipId)

        if (record && record.luxValue != null) {
          latestLux.value = record.luxValue
          latestLuxText.value = `光照值：${record.luxValue} lux`
          return
        }
      } catch (error) {
        console.warn(`device ${device.chipId} has no lux record`)
      }
    }

    latestLux.value = null
    latestLuxText.value = '暂无光照数据'
  } catch (error) {
    console.error('loadLatestLux error =', error)
    latestLuxText.value = '光照数据加载失败'
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
    latestLuxText.value = `光照值：${luxValue} lux`
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

const { connected } = useWebSocket(wsUrl, handleWsMessage)

watch(connected, (val) => {
  if (val) {
    scanStatus.value = '实时连接已建立'
    loadDevices()
  } else {
    scanStatus.value = 'WebSocket 未连接'
  }
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
  position: relative;
  isolation: isolate;
  display: block;
  min-height: 100vh;
  background: #eef4fb;
  overflow: visible;
}

.app-container::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: -2;
  background-image: url('/backgrounds/bg-day.png');
  background-size: cover;
  background-position: center right;
  background-repeat: no-repeat;
  opacity: 0.95;
  filter: blur(8px);
  transform: scale(1.02);
  pointer-events: none;
}

.app-container::after {
  content: "";
  position: fixed;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(
      90deg,
      rgba(245, 248, 252, 0.18) 0%,
      rgba(245, 248, 252, 0.08) 45%,
      rgba(245, 248, 252, 0.02) 100%
    );
  pointer-events: none;
}

.app-container.night-mode::before {
  background-image: url('/backgrounds/bg-night.png');
  opacity: 1;
}

.app-container.night-mode::after {
  background:
    linear-gradient(
      90deg,
      rgba(2, 6, 23, 0.42) 0%,
      rgba(2, 6, 23, 0.22) 55%,
      rgba(2, 6, 23, 0.08) 100%
    );
}

.page-section {
  position: relative;
}

.dashboard-top-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-bottom: 14px;
}

.current-time {
  color: #111827;
  font-size: 2rem;
  font-weight: 900;
  line-height: 1;
}

.weather-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 7px;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
}

.weather-svg-icon {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #64748b;
}

.weather-svg-icon svg {
  width: 36px;
  height: 36px;
  display: block;
}

.weather-svg-icon path,
.weather-svg-icon circle {
  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.weather-svg-icon .sun-core {
  fill: rgba(245, 158, 11, 0.18);
  stroke: #f59e0b;
}

.weather-svg-icon .sun-rays,
.weather-svg-icon.weather-sunny {
  color: #f59e0b;
}

.weather-svg-icon .weather-sun-small {
  color: #f59e0b;
}

.weather-svg-icon .cloud-shape {
  color: #64748b;
  fill: rgba(148, 163, 184, 0.14);
}

.weather-svg-icon .rain-lines {
  color: #3b82f6;
}

.weather-svg-icon .snow-marks {
  color: #60a5fa;
}

.weather-svg-icon .fog-lines {
  color: #94a3b8;
}

.weather-svg-icon .thunder-bolt path {
  color: #facc15;
  fill: rgba(250, 204, 21, 0.24);
}

.section-space-top {
  margin-top: 10px;
}

.env-layout {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 22px;
}

.env-card {
  flex: 1 1 48%;
  min-width: 300px;
  background: #fff;
  padding: 14px 16px;
  border-radius: 16px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.env-card h4 {
  margin: 0 0 10px;
  font-size: 17px;
}
:deep(.env-card),
:deep(.lamp-card),
:deep(.settings-card),
:deep(.placeholder-card),
:deep(.empty-block),
:deep(.scan-panel),
:deep(.chart-card),
:deep(.info-card),
:deep(#controls) {
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.10);
}
.env-info {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.stat-grid {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.stat-item {
  min-width: 0;
  padding: 0 18px 0 0;
  border-right: 1px solid rgba(203, 213, 225, 0.72);
}

.stat-item:last-child {
  padding-right: 0;
  border-right: none;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: center;
  width: 100%;
  gap: 0;
  margin-bottom: 14px;
}

.meta-item {
  min-width: 0;
  padding: 0 24px;
}

.meta-item:first-child {
  padding-left: 0;
}

.meta-item + .meta-item {
  border-left: 1px solid rgba(203, 213, 225, 0.72);
}

.meta-item:last-child {
  padding-right: 0;
}

.stat-label {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.2;
}

.stat-value {
  display: block;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.25;
  font-weight: 800;
}

#metaInfo {
  display: grid;
  grid-template-columns: auto auto;
  column-gap: 30px;
  row-gap: 6px;
}

.lux-display {
  margin-top: 10px;
  padding: 10px 12px;
  background: rgba(248, 250, 252, 0.72);
  border-radius: 13px;
  min-height: 44px;
  display: flex;
  align-items: center;
}

.page-section > h1 {
  margin: 26px 0 16px;
  color: #1f2937;
  font-size: 34px;
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: -0.02em;
}

#controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin: 0 0 28px;
  padding: 14px 18px;
  border-radius: 18px;
}

#controls > button {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  min-height: 40px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
}

#controls > button:disabled {
  opacity: 0.62;
  cursor: not-allowed;
  box-shadow: none;
}

#controls label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
}

#controls input {
  width: 178px;
  height: 36px;
  box-sizing: border-box;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 10px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.86);
  color: #334155;
  font-size: 14px;
}

#scanStatus {
  margin-left: auto;
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
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

/* 夜间模式：基础文字 */
.night-mode .device-meta,
.night-mode .field-label,
.night-mode .checkbox-row,
.night-mode .settings-title,
.night-mode .readonly-box {
  color: #c9d1d9;
}

/* 夜间模式：光照显示 */
.night-mode .lux-display {
  background: rgba(30, 41, 59, 0.72);
}

/* 夜间模式：侧边栏激活态 */
.night-mode :deep(.sidebar li.active),
.night-mode :deep(.sidebar li:hover) {
  background: rgba(64, 158, 255, 0.18);
}

/* 夜间模式：大卡片统一 */
.app-container.night-mode :deep(.env-card),
.app-container.night-mode :deep(.lamp-card),
.app-container.night-mode :deep(.settings-card),
.app-container.night-mode :deep(.placeholder-card),
.app-container.night-mode :deep(.empty-block),
.app-container.night-mode :deep(.scan-panel),
.app-container.night-mode :deep(.chart-card),
.app-container.night-mode :deep(.info-card),
.app-container.night-mode :deep(#controls),
.app-container.night-mode :deep(.sidebar) {
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.18);
  color: #e5e7eb;
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.35);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

/* 夜间模式：设置页内部小卡片 */
.app-container.night-mode :deep(.flow-card),
.app-container.night-mode :deep(.flow-data-item),
.app-container.night-mode :deep(.flow-chart-box),
.app-container.night-mode :deep(.empty-flow),
.app-container.night-mode :deep(.smart-step),
.app-container.night-mode :deep(.smart-status),
.app-container.night-mode :deep(.smart-message),
.app-container.night-mode :deep(.meta-chip) {
  background: rgba(15, 23, 42, 0.62) !important;
  border-color: rgba(148, 163, 184, 0.22) !important;
  color: #e5e7eb !important;
}

/* 夜间模式：重点文字 */
.app-container.night-mode :deep(.flow-device-name),
.app-container.night-mode :deep(.flow-data-item strong),
.app-container.night-mode :deep(.meta-value),
.app-container.night-mode :deep(.smart-title) {
  color: #f8fafc !important;
}

/* 夜间模式：辅助文字 */
.app-container.night-mode :deep(.flow-device-sub),
.app-container.night-mode :deep(.flow-data-item span),
.app-container.night-mode :deep(.meta-key),
.app-container.night-mode :deep(.smart-desc),
.app-container.night-mode :deep(.flow-chart-box),
.app-container.night-mode :deep(.smart-step p) {
  color: #94a3b8 !important;
}

/* 夜间模式：输入框、日期框、自定义下拉 */
.app-container.night-mode :deep(input),
.app-container.night-mode :deep(select),
.app-container.night-mode :deep(.date-input),
.app-container.night-mode :deep(.text-input),
.app-container.night-mode :deep(.region-input),
.app-container.night-mode :deep(.readonly-box),
.app-container.night-mode :deep(.select-trigger) {
  background: rgba(15, 23, 42, 0.76) !important;
  border-color: rgba(148, 163, 184, 0.28) !important;
  color: #e5e7eb !important;
}

.app-container.night-mode :deep(input::placeholder),
.app-container.night-mode :deep(.select-text.placeholder) {
  color: #64748b !important;
}

/* 夜间模式：自定义下拉展开面板 */
.app-container.night-mode :deep(.select-dropdown) {
  background: rgba(15, 23, 42, 0.96) !important;
  border-color: rgba(148, 163, 184, 0.24) !important;
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.45) !important;
}

.app-container.night-mode :deep(.select-option) {
  color: #e5e7eb !important;
}

.app-container.night-mode :deep(.select-option:hover) {
  background: rgba(30, 41, 59, 0.9) !important;
}

/* 夜间模式：状态标签 */
.app-container.night-mode :deep(.flow-status) {
  background: rgba(37, 99, 235, 0.22) !important;
  color: #93c5fd !important;
}

.app-container.night-mode :deep(.flow-status.active) {
  background: rgba(127, 29, 29, 0.3) !important;
  color: #fecaca !important;
}

/* 夜间模式：次级按钮 */
.app-container.night-mode :deep(.btn-secondary),
.app-container.night-mode :deep(.secondary-btn),
.app-container.night-mode :deep(.scan-cancel-btn) {
  background: rgba(30, 41, 59, 0.82) !important;
  border: 1px solid rgba(148, 163, 184, 0.24) !important;
  color: #e5e7eb !important;
}

/* 夜间模式：退出 / 危险按钮 */
.app-container.night-mode :deep(.btn-logout),
.app-container.night-mode :deep(.btn-danger) {
  background: rgba(127, 29, 29, 0.26) !important;
  color: #fecaca !important;
}

/* 夜间模式：SmartConfig 提示框 */
.app-container.night-mode :deep(.smart-tips) {
  background: rgba(120, 53, 15, 0.22) !important;
  border: 1px solid rgba(245, 158, 11, 0.18) !important;
  color: #fde68a !important;
}

.app-container.night-mode :deep(.smart-message.success) {
  background: rgba(6, 95, 70, 0.22) !important;
  color: #a7f3d0 !important;
}

.app-container.night-mode :deep(.smart-message.error) {
  background: rgba(127, 29, 29, 0.22) !important;
  color: #fecaca !important;
}

/* 夜间模式：高对比可读性补强 */
.app-container.night-mode :deep(.layout-card),
.app-container.night-mode :deep(.light-effect-mini-card),
.app-container.night-mode :deep(.smart-config-section),
.app-container.night-mode :deep(.smart-card),
.app-container.night-mode :deep(.direction-pad),
.app-container.night-mode :deep(.preset-btn),
.app-container.night-mode :deep(.slider-card),
.app-container.night-mode :deep(.scan-item),
.app-container.night-mode :deep(.firmware-section),
.app-container.night-mode :deep(.firmware-info-item),
.app-container.night-mode :deep(.detail-info-item),
.app-container.night-mode :deep(.readonly-item) {
  background: rgba(15, 23, 42, 0.72) !important;
  border-color: rgba(148, 163, 184, 0.18) !important;
  color: rgba(226, 232, 240, 0.88) !important;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35) !important;
}

.app-container.night-mode :deep(.smart-card),
.app-container.night-mode :deep(.layout-card),
.app-container.night-mode :deep(.light-effect-mini-card) {
  background: rgba(15, 23, 42, 0.82) !important;
}

.app-container.night-mode :deep(h1),
.app-container.night-mode :deep(h2),
.app-container.night-mode :deep(h3),
.app-container.night-mode :deep(h4),
.app-container.night-mode :deep(.card-title),
.app-container.night-mode :deep(.scan-panel-title),
.app-container.night-mode :deep(.layout-header h2),
.app-container.night-mode :deep(.mini-title),
.app-container.night-mode :deep(.device-title-block h3),
.app-container.night-mode :deep(.preset-btn strong),
.app-container.night-mode :deep(.slider-card-header),
.app-container.night-mode :deep(.firmware-section h4),
.app-container.night-mode :deep(.firmware-info-item strong),
.app-container.night-mode :deep(.detail-value),
.app-container.night-mode :deep(.readonly-value),
.app-container.night-mode :deep(.lamp-info strong),
.app-container.night-mode :deep(.zone-order-row strong) {
  color: rgba(248, 250, 252, 0.96) !important;
}

.app-container.night-mode :deep(.env-info),
.app-container.night-mode :deep(.stat-label),
.app-container.night-mode :deep(#metaInfo),
.app-container.night-mode :deep(#scanStatus),
.app-container.night-mode :deep(.scan-item-info),
.app-container.night-mode :deep(.field-label),
.app-container.night-mode :deep(.checkbox-row),
.app-container.night-mode :deep(.form-row label),
.app-container.night-mode :deep(.modal-label),
.app-container.night-mode :deep(.detail-label),
.app-container.night-mode :deep(.firmware-info-item span),
.app-container.night-mode :deep(.readonly-label),
.app-container.night-mode :deep(.mini-label),
.app-container.night-mode :deep(.lamp-info span),
.app-container.night-mode :deep(.zone-order-row span),
.app-container.night-mode :deep(.message-body) {
  color: rgba(226, 232, 240, 0.88) !important;
}

.app-container.night-mode :deep(.stat-item) {
  border-color: rgba(148, 163, 184, 0.22) !important;
}

.app-container.night-mode :deep(.meta-item) {
  border-color: rgba(148, 163, 184, 0.22) !important;
}

.app-container.night-mode :deep(.stat-value) {
  color: rgba(248, 250, 252, 0.96) !important;
}

.app-container.night-mode :deep(.panel-desc),
.app-container.night-mode :deep(.last-seen-under-name),
.app-container.night-mode :deep(.layout-header p),
.app-container.night-mode :deep(.mini-status),
.app-container.night-mode :deep(.preset-btn span),
.app-container.night-mode :deep(.device-meta),
.app-container.night-mode :deep(.detail-subtitle),
.app-container.night-mode :deep(.modal-hint),
.app-container.night-mode :deep(.scan-empty),
.app-container.night-mode :deep(.empty-block),
.app-container.night-mode :deep(.field-hint.placeholder) {
  color: rgba(203, 213, 225, 0.72) !important;
}

.app-container.night-mode :deep(input::placeholder),
.app-container.night-mode :deep(textarea::placeholder),
.app-container.night-mode :deep(.select-text.placeholder) {
  color: rgba(203, 213, 225, 0.58) !important;
}

.app-container.night-mode :deep(.scan-empty),
.app-container.night-mode :deep(.empty-block) {
  background: rgba(15, 23, 42, 0.58) !important;
  border-color: rgba(148, 163, 184, 0.22) !important;
}

.app-container.night-mode :deep(.speed-tab),
.app-container.night-mode :deep(.compact-btn),
.app-container.night-mode :deep(.shortcut-btn),
.app-container.night-mode :deep(.btn-light),
.app-container.night-mode :deep(.reset-layout-btn),
.app-container.night-mode :deep(.scan-clear-btn),
.app-container.night-mode :deep(.mini-btn.stop),
.app-container.night-mode :deep(.btn-ai) {
  background: rgba(30, 41, 59, 0.82) !important;
  border: 1px solid rgba(148, 163, 184, 0.24) !important;
  color: rgba(226, 232, 240, 0.9) !important;
  box-shadow: none !important;
}

.app-container.night-mode :deep(.speed-tab.active),
.app-container.night-mode :deep(.compact-btn.primary),
.app-container.night-mode :deep(.btn-ai:not(.active):hover),
.app-container.night-mode :deep(.reset-layout-btn:hover),
.app-container.night-mode :deep(.shortcut-btn:hover),
.app-container.night-mode :deep(.compact-btn:hover) {
  background: rgba(37, 99, 235, 0.26) !important;
  border-color: rgba(96, 165, 250, 0.45) !important;
  color: #bfdbfe !important;
}

.app-container.night-mode :deep(.dir-btn) {
  background: rgba(30, 41, 59, 0.88) !important;
  color: #93c5fd !important;
  border: 1px solid rgba(96, 165, 250, 0.22) !important;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.28) !important;
}

.app-container.night-mode :deep(.dir-btn:hover) {
  background: rgba(37, 99, 235, 0.28) !important;
}

.app-container.night-mode :deep(.field-hint:not(.placeholder)) {
  color: #fcd34d !important;
}

.app-container.night-mode :deep(.smart-status.active) {
  background: rgba(37, 99, 235, 0.28) !important;
  border-color: rgba(96, 165, 250, 0.36) !important;
  color: #bfdbfe !important;
}

.app-container.night-mode :deep(.smart-status.success),
.app-container.night-mode :deep(.status-badge.online) {
  background: rgba(6, 95, 70, 0.28) !important;
  border-color: rgba(52, 211, 153, 0.22) !important;
  color: #a7f3d0 !important;
}

.app-container.night-mode :deep(.smart-status.error),
.app-container.night-mode :deep(.status-badge.offline),
.app-container.night-mode :deep(.btn-ai.active) {
  background: rgba(127, 29, 29, 0.28) !important;
  border-color: rgba(248, 113, 113, 0.22) !important;
  color: #fecaca !important;
}

.app-container.night-mode :deep(.smart-status.warning),
.app-container.night-mode :deep(.smart-message.warning),
.app-container.night-mode :deep(.ota-result) {
  background: rgba(120, 53, 15, 0.26) !important;
  border-color: rgba(245, 158, 11, 0.24) !important;
  color: #fde68a !important;
}

.app-container.night-mode :deep(.status-badge) {
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.app-container.night-mode :deep(.color-box),
.app-container.night-mode :deep(.lux-display),
.app-container.night-mode :deep(.zone-order-row span) {
  border-color: rgba(148, 163, 184, 0.22) !important;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.22) !important;
}

.app-container.night-mode :deep(.store-stage) {
  background: rgba(2, 6, 23, 0.78) !important;
  border-color: rgba(148, 163, 184, 0.22) !important;
}

.app-container.night-mode :deep(.store-bg) {
  filter: blur(2px) brightness(0.58) saturate(0.82) !important;
}

.app-container.night-mode :deep(.zone-box) {
  background: rgba(37, 99, 235, 0.2) !important;
  border-color: rgba(96, 165, 250, 0.72) !important;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.36) !important;
}

.app-container.night-mode :deep(.zone-name-input),
.app-container.night-mode :deep(.zone-count),
.app-container.night-mode :deep(.lamp-node) {
  background: rgba(15, 23, 42, 0.9) !important;
  border-color: rgba(148, 163, 184, 0.3) !important;
  color: rgba(248, 250, 252, 0.96) !important;
}

.app-container.night-mode :deep(.lamp-node.active),
.app-container.night-mode :deep(.lamp-node.selected) {
  border-color: rgba(251, 191, 36, 0.92) !important;
  box-shadow:
    0 0 0 5px rgba(251, 191, 36, 0.18),
    0 18px 44px rgba(0, 0, 0, 0.46) !important;
}

/* 夜间模式：内容层禁止使用模糊/毛玻璃，避免整页发糊 */
.app-container.night-mode,
.app-container.night-mode .main-content,
.app-container.night-mode :deep(.sidebar),
.app-container.night-mode :deep(.env-card),
.app-container.night-mode :deep(.lamp-card),
.app-container.night-mode :deep(.settings-card),
.app-container.night-mode :deep(.placeholder-card),
.app-container.night-mode :deep(.empty-block),
.app-container.night-mode :deep(.scan-panel),
.app-container.night-mode :deep(.scan-item),
.app-container.night-mode :deep(.chart-card),
.app-container.night-mode :deep(.info-card),
.app-container.night-mode :deep(#controls),
.app-container.night-mode :deep(.layout-card),
.app-container.night-mode :deep(.light-effect-mini-card),
.app-container.night-mode :deep(.smart-config-section),
.app-container.night-mode :deep(.smart-card),
.app-container.night-mode :deep(.direction-pad),
.app-container.night-mode :deep(.preset-btn),
.app-container.night-mode :deep(.slider-card),
.app-container.night-mode :deep(.flow-card),
.app-container.night-mode :deep(.flow-data-item),
.app-container.night-mode :deep(.flow-chart-box),
.app-container.night-mode :deep(.firmware-section),
.app-container.night-mode :deep(.firmware-info-item),
.app-container.night-mode :deep(.detail-info-item),
.app-container.night-mode :deep(.readonly-item) {
  filter: none !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

.app-container.night-mode :deep(.smart-message.success) {
  background: rgba(6, 95, 70, 0.22) !important;
  color: #a7f3d0 !important;
}

.app-container.night-mode :deep(.smart-message.error) {
  background: rgba(127, 29, 29, 0.22) !important;
  color: #fecaca !important;
}

.app-container.night-mode .current-time {
  color: rgba(248, 250, 252, 0.96);
}

.app-container.night-mode .weather-status-row {
  color: rgba(226, 232, 240, 0.82);
}

.app-container.night-mode .weather-svg-icon {
  color: rgba(226, 232, 240, 0.85);
}

.app-container.night-mode .weather-svg-icon .sun-core,
.app-container.night-mode .weather-svg-icon .sun-rays,
.app-container.night-mode .weather-svg-icon .weather-sun-small {
  color: #fbbf24;
  stroke: #fbbf24;
}

.app-container.night-mode .weather-svg-icon .sun-core {
  fill: rgba(251, 191, 36, 0.2);
}

.app-container.night-mode .weather-svg-icon .cloud-shape {
  color: rgba(226, 232, 240, 0.85);
  fill: rgba(226, 232, 240, 0.1);
}

.app-container.night-mode .weather-svg-icon .rain-lines {
  color: #60a5fa;
}

.app-container.night-mode .weather-svg-icon .snow-marks {
  color: #bfdbfe;
}

.app-container.night-mode .weather-svg-icon .fog-lines {
  color: rgba(203, 213, 225, 0.78);
}

.app-container.night-mode .weather-svg-icon .thunder-bolt path {
  color: #fde047;
  fill: rgba(253, 224, 71, 0.22);
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
.settings-half-card,
.settings-full-card {
  position: relative;
  z-index: 1;
}

.settings-half-card:focus-within,
.settings-full-card:focus-within {
  z-index: 50;
}

.main-content {
  min-height: 100vh;
  margin-left: 228px;
  width: calc(100vw - 228px);
  box-sizing: border-box;
  padding: 24px 32px 48px 0;
  overflow-x: hidden;
}

.store-layout-row {
  display: grid;
  grid-template-columns: minmax(320px, 0.75fr) minmax(520px, 1.55fr);
  gap: 20px;
  align-items: stretch;
  margin: 20px 0 28px;
}

.store-layout-row > * {
  min-width: 0;
}

.store-effect-mini {
  width: 100%;
  height: 100%;
}

.store-layout-main {
  min-width: 0;
  height: 100%;
}

@media (max-width: 1360px) {
  .store-layout-row {
    grid-template-columns: minmax(340px, 0.95fr) minmax(460px, 1.15fr);
  }
}

@media (max-width: 1180px) {
  .store-layout-row {
    grid-template-columns: 1fr;
  }

  .store-effect-mini {
    position: static;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .store-layout-row {
    gap: 12px;
    margin: 14px 0 22px;
  }

  .stat-grid {
    gap: 10px;
  }

  .meta-grid {
    gap: 10px;
  }

  .stat-item,
  .meta-item {
    flex: 1 1 120px;
    padding-right: 10px;
  }

  #controls {
    gap: 10px;
    padding: 12px;
  }

  #controls label {
    width: 100%;
    align-items: flex-start;
    flex-direction: column;
  }

  #controls input {
    width: 100%;
  }

  #scanStatus {
    width: 100%;
    margin-left: 0;
  }
}
@media (max-width: 768px) {
   .main-content {
    width: 100%;
    margin-left: 0;
    padding: 12px;
    box-sizing: border-box;
  }

  .main-content {
    padding: 12px;
    margin-left: 0;
  }
}
@media (max-width: 900px) {
   .settings-row {
    grid-template-columns: 1fr;
  }

  .settings-half-card {
    position: relative;
  }

  .settings-half-card:focus-within {
    z-index: 80;
  }
}

/* 最终兜底：夜间模式内容层不允许模糊，背景图层除外 */
.app-container.night-mode,
.app-container.night-mode .main-content,
.app-container.night-mode :deep(.sidebar),
.app-container.night-mode :deep(.env-card),
.app-container.night-mode :deep(.lamp-card),
.app-container.night-mode :deep(.settings-card),
.app-container.night-mode :deep(.placeholder-card),
.app-container.night-mode :deep(.empty-block),
.app-container.night-mode :deep(.scan-panel),
.app-container.night-mode :deep(.scan-item),
.app-container.night-mode :deep(.chart-card),
.app-container.night-mode :deep(.info-card),
.app-container.night-mode :deep(#controls),
.app-container.night-mode :deep(.layout-card),
.app-container.night-mode :deep(.light-effect-mini-card),
.app-container.night-mode :deep(.smart-config-section),
.app-container.night-mode :deep(.smart-card),
.app-container.night-mode :deep(.direction-pad),
.app-container.night-mode :deep(.preset-btn),
.app-container.night-mode :deep(.slider-card),
.app-container.night-mode :deep(.flow-card),
.app-container.night-mode :deep(.flow-data-item),
.app-container.night-mode :deep(.flow-chart-box),
.app-container.night-mode :deep(.firmware-section),
.app-container.night-mode :deep(.firmware-info-item),
.app-container.night-mode :deep(.detail-info-item),
.app-container.night-mode :deep(.readonly-item) {
  filter: none !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}
</style>
