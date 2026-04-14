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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import ProvinceCityPicker from './ProvinceCityPicker.vue'
import type { RegionValue } from '../../constants/china-region'

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
}>()

const storeTypeOptions = [
  { label: '高端店（3500K）', value: '高端,3500' },
  { label: '大众店（4000K）', value: '中端,4000' },
  { label: '快销店（5000K）', value: '小型,5000' },
]

const storeSizeOptions = [
  { label: '40㎡', value: '小型,40' },
  { label: '60㎡', value: '中端,60' },
  { label: '80㎡', value: '高端,80' },
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