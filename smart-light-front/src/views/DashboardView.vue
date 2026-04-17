<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentStoreApi } from '../api/store'
import { getMyDeviceListApi } from '../api/device'
import { STORE_STYLE_MAP } from '../constants/store'
import type { StoreInfo } from '../types/store'
import type { DeviceItem } from '../types/device'

const router = useRouter()

const username = ref('-')
const loading = ref(false)
const errorMsg = ref('')

const storeInfo = ref<StoreInfo | null>(null)
const deviceList = ref<DeviceItem[]>([])

const storeStyleText = computed(() => {
  const style = storeInfo.value?.storeStyle
  if (!style) return '-'
  return STORE_STYLE_MAP[style] || style
})

onMounted(() => {
  username.value = window.localStorage.getItem('USERNAME') || '-'
  loadPageData()
})

async function loadPageData() {
  errorMsg.value = ''

  try {
    loading.value = true

    const [store, devices] = await Promise.all([
      getCurrentStoreApi(),
      getMyDeviceListApi(),
    ])

    storeInfo.value = store
    deviceList.value = devices || []
  } catch (error: any) {
    errorMsg.value =
      error?.response?.data?.msg || error?.message || '页面加载失败'
  } finally {
    loading.value = false
  }
}

function logout() {
  window.localStorage.removeItem('TOKEN')
  window.localStorage.removeItem('USERNAME')
  window.localStorage.removeItem('STORE_NAME')
  router.push('/login')
}
</script>

<template>
  <div class="dashboard-page">
    <div class="top-bar">
      <div>
        <h2>首页</h2>
        <p>当前用户：{{ username }}</p>
      </div>
      <button class="logout-btn" @click="logout">退出登录</button>
    </div>

    <div v-if="loading" class="panel">加载中...</div>
    <div v-else-if="errorMsg" class="panel error-text">{{ errorMsg }}</div>

    <template v-else>
      <div class="panel" v-if="storeInfo">
        <h3>店铺信息</h3>
        <div class="store-grid">
          <div class="info-item">
            <span class="label">店铺名称</span>
            <span class="value">{{ storeInfo.storeName }}</span>
          </div>
          <div class="info-item">
            <span class="label">店铺风格</span>
            <span class="value">{{ storeStyleText }}</span>
          </div>
          <div class="info-item">
            <span class="label">店铺面积</span>
            <span class="value">{{ storeInfo.area }} ㎡</span>
          </div>
          <div class="info-item">
            <span class="label">地区</span>
            <span class="value">{{ storeInfo.province }} {{ storeInfo.city }}</span>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="device-header">
          <h3>设备列表</h3>
          <span>共 {{ deviceList.length }} 台</span>
        </div>

        <div v-if="!deviceList.length" class="empty-text">
          当前店铺暂无设备
        </div>

        <div v-else class="device-grid">
          <div v-for="item in deviceList" :key="item.id" class="device-card">
            <div class="device-title-row">
              <h4>{{ item.displayName || item.deviceNo || item.chipId }}</h4>
              <span class="device-type">{{ item.deviceType || '未知类型' }}</span>
            </div>

            <p><strong>芯片ID：</strong>{{ item.chipId }}</p>
            <p><strong>设备编号：</strong>{{ item.deviceNo || '-' }}</p>
            <p><strong>IP：</strong>{{ item.ip || '-' }}</p>
            <p><strong>亮度：</strong>{{ item.brightness ?? '-' }}</p>
            <p><strong>色温：</strong>{{ item.temp ?? '-' }}</p>
            <p><strong>自动模式：</strong>{{ item.autoMode ? '开启' : '关闭' }}</p>
            <p><strong>推荐亮度：</strong>{{ item.recommendedBrightness ?? '-' }}</p>
            <p><strong>推荐色温：</strong>{{ item.recommendedTemp ?? '-' }}</p>
            <p><strong>面料：</strong>{{ item.fabric || '-' }}</p>
            <p><strong>主色：</strong>{{ item.mainColorRgb || '-' }}</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 24px;
  box-sizing: border-box;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.top-bar h2 {
  margin: 0 0 8px;
}

.top-bar p {
  margin: 0;
  color: #666;
}

.logout-btn {
  height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 10px;
  background: #ff4d4f;
  color: #fff;
  cursor: pointer;
}

.panel {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.store-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}

.label {
  display: block;
  font-size: 13px;
  color: #888;
  margin-bottom: 8px;
}

.value {
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.device-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.device-card {
  border: 1px solid #eef1f5;
  border-radius: 14px;
  padding: 16px;
  background: #fafcff;
}

.device-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.device-title-row h4 {
  margin: 0;
  font-size: 16px;
}

.device-type {
  background: #e8f3ff;
  color: #1677ff;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 999px;
}

.device-card p {
  margin: 8px 0;
  font-size: 14px;
  color: #444;
}

.empty-text {
  color: #888;
  text-align: center;
  padding: 24px 0;
}

.error-text {
  color: #e74c3c;
}
</style>