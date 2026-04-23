<template>
  <div class="smart-card">
    <div class="smart-header">
      <div>
        <h2 class="smart-title">📶 设备 SmartConfig 配网</h2>
        <p class="smart-desc">
          用于给未联网的 ESP8266 / ESP32 设备配置 WiFi，让设备自动接入当前网络。
        </p>
      </div>

      <span class="smart-status" :class="{ active: configing }">
        {{ configing ? '配网中' : '待配网' }}
      </span>
    </div>

    <div class="smart-steps">
      <div class="smart-step">
        <span>1</span>
        <p>设备进入 SmartConfig 模式</p>
      </div>
      <div class="smart-step">
        <span>2</span>
        <p>手机连接 2.4G WiFi</p>
      </div>
      <div class="smart-step">
        <span>3</span>
        <p>输入密码并开始配网</p>
      </div>
    </div>

    <div class="smart-form">
      <div class="form-row">
        <label>当前 WiFi</label>
        <div class="wifi-row">
          <input v-model="ssid" readonly placeholder="请先获取当前 WiFi" />
          <button class="btn-secondary" :disabled="configing" @click="getCurrentWifi">
            获取 WiFi
          </button>
        </div>
      </div>

      <div class="form-row">
        <label>WiFi 密码</label>
        <input
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          placeholder="请输入当前 WiFi 密码"
        />
      </div>

      <div class="form-row">
        <label>服务器地址</label>
        <input
          v-model="serverHost"
          placeholder="例如 192.168.1.100 或 110.41.81.4"
        />
      </div>

      <div class="form-row">
        <label>服务器端口</label>
        <input
          v-model.number="serverPort"
          type="number"
          placeholder="例如 3000"
        />
      </div>
    </div>

    <div class="smart-actions">
      <button class="btn-primary" :disabled="configing" @click="startSmartConfig">
        {{ configing ? '配网中...' : '开始配网' }}
      </button>

      <button class="btn-secondary" :disabled="configing" @click="showPassword = !showPassword">
        {{ showPassword ? '隐藏密码' : '显示密码' }}
      </button>

      <button v-if="configing" class="btn-danger" @click="cancelSmartConfig">
        取消配网
      </button>
    </div>

    <div v-if="message" class="smart-message" :class="{ success, error: !success }">
      {{ message }}
    </div>

    <div class="smart-tips">
      <p>提示：SmartConfig 需要 App 真机环境和原生插件支持，普通浏览器 H5 页面无法完成配网。</p>
      <p>建议手机连接 2.4G WiFi，部分 ESP8266 / ESP32 不支持 5G WiFi。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const ssid = ref('')
const bssid = ref('')
const password = ref('')
const serverHost = ref('127.0.0.1')
const serverPort = ref(3000)

const configing = ref(false)
const message = ref('')
const success = ref(false)
const showPassword = ref(false)


function setMessage(msg: string, ok = false) {
  message.value = msg
  success.value = ok
}

function getEspTouchPlugin() {
  return window.AndroidSmartConfig || null
}

function getCurrentWifi() {
  const esptouch = getEspTouchPlugin()

  if (!esptouch) {
    setMessage('当前环境没有 SmartConfig 原生插件，请使用 App 真机运行。')
    return
  }

  setMessage('正在获取当前 WiFi...')

  try {
    esptouch.getWifiInfo((res: any) => {
      console.log('getWifiInfo:', res)

      const currentSsid = res?.ssid || res?.SSID || ''
      const currentBssid = res?.bssid || res?.BSSID || ''

      if (!currentSsid) {
        setMessage('获取 WiFi 失败，请确认手机已连接 WiFi，并开启定位 / 附近设备权限。')
        return
      }

      ssid.value = currentSsid
      bssid.value = currentBssid
      setMessage(`已获取当前 WiFi：${currentSsid}`, true)
    })
  } catch (e) {
    console.error(e)
    setMessage('调用获取 WiFi 接口失败，请检查插件方法名。')
  }
}

function startSmartConfig() {
  const esptouch = getEspTouchPlugin()

  if (!esptouch) {
    setMessage('当前环境没有 SmartConfig 原生插件，请使用 App 真机运行。')
    return
  }

  if (!ssid.value.trim()) {
    setMessage('请先获取当前 WiFi。')
    return
  }

  if (!password.value) {
    setMessage('请输入 WiFi 密码。')
    return
  }

  if (!serverHost.value.trim()) {
    setMessage('请输入服务器地址。')
    return
  }

  configing.value = true
  setMessage('正在配网，请保持设备处于 SmartConfig 模式...')

  const payload = {
    ssid: ssid.value.trim(),
    bssid: bssid.value,
    password: password.value,
    deviceCount: 1,
    broadcast: true,

    customData: JSON.stringify({
      serverHost: serverHost.value.trim(),
      serverPort: Number(serverPort.value) || 3000,
    }),
  }

  try {
    esptouch.smartConfig(payload, (res: any) => {
      console.log('smartConfig result:', res)

      const ok =
        res?.success === true ||
        res?.code === 200 ||
        res?.errCode === 0 ||
        res?.status === 'success'

      if (ok) {
        configing.value = false
        setMessage('配网成功，设备正在连接服务器，请稍后到设备列表查看上线状态。', true)
        return
      }

      const finished =
        res?.finish === true ||
        res?.done === true ||
        res?.status === 'fail' ||
        res?.error

      if (finished) {
        configing.value = false
        setMessage(res?.message || res?.msg || '配网失败，请确认 WiFi 密码和设备配网模式。')
      } else {
        setMessage(res?.message || res?.msg || '配网进行中...')
      }
    })
  } catch (e) {
    console.error(e)
    configing.value = false
    setMessage('调用 SmartConfig 失败，请检查插件方法名和参数格式。')
  }
}

function cancelSmartConfig() {
  const esptouch = getEspTouchPlugin()

  try {
    if (esptouch?.cancel) {
      esptouch.cancel()
    }

    if (esptouch?.stop) {
      esptouch.stop()
    }
  } catch (e) {
    console.error(e)
  }

  configing.value = false
  setMessage('已取消配网。')
}
</script>

<style scoped>
.smart-card {
  width: 100%;
}

.smart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.smart-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.smart-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}

.smart-status {
  flex-shrink: 0;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
}

.smart-status.active {
  color: #1d4ed8;
  background: #dbeafe;
}

.smart-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 18px 0;
}

.smart-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.smart-step span {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.smart-step p {
  margin: 0;
  font-size: 13px;
  color: #374151;
}

.smart-form {
  display: grid;
  gap: 12px;
}

.form-row {
  display: grid;
  grid-template-columns: 90px 1fr;
  align-items: center;
  gap: 12px;
}

.form-row label {
  font-size: 14px;
  color: #374151;
  font-weight: 600;
}

.form-row input {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  outline: none;
  font-size: 14px;
  background: #fff;
}

.form-row input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.wifi-row {
  display: flex;
  gap: 8px;
}

.wifi-row input {
  flex: 1;
}

.smart-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.btn-primary,
.btn-secondary,
.btn-danger {
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  color: #fff;
  background: #2563eb;
}

.btn-secondary {
  color: #1d4ed8;
  background: #eff6ff;
}

.btn-danger {
  color: #b91c1c;
  background: #fee2e2;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.smart-message {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
}

.smart-message.success {
  color: #047857;
  background: #ecfdf5;
}

.smart-message.error {
  color: #b91c1c;
  background: #fef2f2;
}

.smart-tips {
  margin-top: 14px;
  padding: 12px;
  border-radius: 12px;
  background: #fffbeb;
  color: #92400e;
  font-size: 12px;
  line-height: 1.6;
}

.smart-tips p {
  margin: 4px 0;
}

@media (max-width: 720px) {
  .smart-header {
    flex-direction: column;
  }

  .smart-steps {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .wifi-row {
    flex-direction: column;
  }
}
</style>