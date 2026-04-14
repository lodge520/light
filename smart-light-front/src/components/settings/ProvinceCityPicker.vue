<template>
  <div class="region-picker">
    <div class="region-input" @click="openPopup">
      <span v-if="displayText">{{ displayText }}</span>
      <span v-else class="region-placeholder">请选择省份 / 城市</span>
      <span class="region-arrow">▾</span>
    </div>

    <div v-if="visible" class="region-mask" @click.self="closePopup">
      <div class="region-dialog">
        <div class="region-dialog-header">
          <div class="region-dialog-title">选择省市</div>
          <button class="region-close" @click="closePopup">×</button>
        </div>

        <div class="region-dialog-body">
          <div class="region-province-list">
            <div
              v-for="province in regions"
              :key="province.value"
              class="region-province-item"
              :class="{ active: currentProvince?.value === province.value }"
              @click="selectProvince(province.value)"
            >
              {{ province.label }}
            </div>
          </div>

          <div class="region-city-list">
            <div class="region-city-title">
              {{ currentProvince?.label || '请选择省份' }}
            </div>

            <div class="region-city-grid">
              <div
                v-for="city in currentCities"
                :key="city.value"
                class="region-city-item"
                :class="{ active: modelValue.city === city.value }"
                @click="selectCity(city.value)"
              >
                {{ city.label }}
              </div>
            </div>
          </div>
        </div>

        <div class="region-dialog-footer">
          <button class="btn-secondary" @click="closePopup">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { regions } from '../../constants/china-region'
import type { RegionValue } from '../../constants/china-region'

const props = defineProps<{
  modelValue: RegionValue
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: RegionValue): void
}>()

const visible = ref(false)
const selectedProvinceValue = ref(props.modelValue.province || regions[0].value)

watch(
  () => props.modelValue.province,
  (val) => {
    if (val) {
      selectedProvinceValue.value = val
    }
  },
)

const currentProvince = computed(() => {
  return regions.find(item => item.value === selectedProvinceValue.value) || regions[0]
})

const currentCities = computed(() => currentProvince.value?.cities || [])

const displayText = computed(() => {
  if (!props.modelValue.provinceLabel || !props.modelValue.cityLabel) return ''
  return `${props.modelValue.provinceLabel} / ${props.modelValue.cityLabel}`
})

function openPopup() {
  visible.value = true
}

function closePopup() {
  visible.value = false
}

function selectProvince(provinceValue: string) {
  selectedProvinceValue.value = provinceValue
}

function selectCity(cityValue: string) {
  const province = currentProvince.value
  if (!province) return

  const city = province.cities.find(item => item.value === cityValue)
  if (!city) return

  emit('update:modelValue', {
    province: province.value,
    provinceLabel: province.label,
    city: city.value,
    cityLabel: city.label,
  })

  closePopup()
}
</script>