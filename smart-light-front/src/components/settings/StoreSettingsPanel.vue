<template>
  <div class="settings-card">
    <h2 class="settings-title">⚙️ 店铺设置</h2>

    <div class="selector-box">
      <ProvinceCityPicker
        :model-value="localRegion"
        @update:modelValue="handleRegionChange"
      />

      <select v-model="localStoreType" class="date-input" @change="emitChange">
        <option
          v-for="item in storeTypeOptions"
          :key="item.value"
          :value="item.value"
        >
          {{ item.label }}
        </option>
      </select>

      <select v-model="localStoreSize" class="date-input" @change="emitChange">
        <option
          v-for="item in storeSizeOptions"
          :key="item.value"
          :value="item.value"
        >
          {{ item.label }}
        </option>
      </select>

      <button type="button" class="btn-primary" @click="toggleMode">
        {{ isNightMode ? '切换到日间模式' : '切换到夜间模式' }}
      </button>
      <button type="button" class="btn-logout" @click="handleLogout">
        退出登录
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import ProvinceCityPicker from './ProvinceCityPicker.vue'
import type { RegionValue } from '../../constants/china-region'
import { STORE_STYLE_OPTIONS, STORE_STYLE_MAP } from '../../constants/store'

export interface StoreSettingsValue {
  region: RegionValue
  storeType: string
  storeSize: string
  isNightMode: boolean
}

const props = defineProps<{
  modelValue: StoreSettingsValue
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: StoreSettingsValue): void
  (e: 'logout'): void
}>()

function handleLogout() {
  emit('logout')
}

const STYLE_TEMP_MAP: Record<string, number> = {
  HIGH_END: 3500,
  MASS_MARKET: 4000,
  FAST_FASHION: 4500,
}

const storeTypeOptions = STORE_STYLE_OPTIONS.map(item => ({
  label: `${item.label}店（${STYLE_TEMP_MAP[item.value]}K）`,
  value: `${STORE_STYLE_MAP[item.value]},${STYLE_TEMP_MAP[item.value]}`,
}))

/**
 * 这里先用固定档位，和你父页面 parseStoreSize / buildStoreSizeValue 对齐
 * 如果后面想支持任意面积，建议把这个 select 改成 input
 */
const storeSizeOptions = [
  { label: '40㎡（小型）', value: '小型,40' },
  { label: '80㎡（中型）', value: '中型,80' },
  { label: '150㎡（大型）', value: '大型,150' },
]

const localRegion = ref<RegionValue>({ ...props.modelValue.region })
const localStoreType = ref(props.modelValue.storeType)
const localStoreSize = ref(props.modelValue.storeSize)
const isNightMode = ref(props.modelValue.isNightMode)

watch(
  () => props.modelValue,
  (val) => {
    localRegion.value = { ...val.region }
    localStoreType.value = val.storeType
    localStoreSize.value = val.storeSize
    isNightMode.value = val.isNightMode
  },
  { deep: true },
)

function emitChange() {
  emit('update:modelValue', {
    region: { ...localRegion.value },
    storeType: localStoreType.value,
    storeSize: localStoreSize.value,
    isNightMode: isNightMode.value,
  })
}

function handleRegionChange(value: RegionValue) {
  localRegion.value = { ...value }
  emitChange()
}

function toggleMode() {
  isNightMode.value = !isNightMode.value
  emitChange()
}
</script>