<template>
  <div
    ref="rootRef"
    class="base-select"
    :class="{ open, disabled }"
  >
    <button
      type="button"
      class="select-trigger"
      :disabled="disabled"
      @click="toggleOpen"
    >
      <span class="select-text" :class="{ placeholder: !selectedOption }">
        {{ selectedOption ? selectedOption.label : placeholder }}
      </span>
      <span class="select-arrow">⌄</span>
    </button>

    <transition name="select-fade">
      <div v-if="open" class="select-dropdown">
        <div
          v-for="item in options"
          :key="String(item.value)"
          class="select-option"
          :class="{
            active: item.value === modelValue,
            disabled: item.disabled,
          }"
          @click="handleSelect(item)"
        >
          {{ item.label }}
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

export interface BaseSelectOption {
  label: string
  value: string | number
  disabled?: boolean
}

const props = withDefaults(
  defineProps<{
    modelValue: string | number | null | undefined
    options: BaseSelectOption[]
    placeholder?: string
    disabled?: boolean
  }>(),
  {
    placeholder: '请选择',
    disabled: false,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
  (e: 'change', value: string | number): void
}>()

const open = ref(false)
const rootRef = ref<HTMLElement | null>(null)

const selectedOption = computed(() => {
  return props.options.find(item => item.value === props.modelValue)
})

function toggleOpen() {
  if (props.disabled) return
  open.value = !open.value
}

function closeOpen() {
  open.value = false
}

function handleSelect(item: BaseSelectOption) {
  if (item.disabled) return
  emit('update:modelValue', item.value)
  emit('change', item.value)
  closeOpen()
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as Node | null
  if (!rootRef.value || !target) return
  if (!rootRef.value.contains(target)) {
    closeOpen()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.base-select {
  position: relative;
  width: 100%;
}

.select-trigger {
  width: 100%;
  min-height: 44px;
  padding: 0 42px 0 14px;
  border: 1px solid #dbe3f0;
  border-radius: 14px;
  background: #fff;
  color: #111827;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  box-sizing: border-box;
  transition: all 0.2s ease;
  position: relative;
}

.select-trigger:hover {
  border-color: #c7d2e3;
}

.open .select-trigger {
  border-color: #4f46e5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.08);
}

.select-text {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.select-text.placeholder {
  color: #9ca3af;
}

.select-arrow {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: #6b7280;
  transition: transform 0.2s ease;
  pointer-events: none;
}

.open .select-arrow {
  transform: translateY(-50%) rotate(180deg);
}

.select-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  width: 100%;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
  padding: 8px;
  z-index: 1000;
  max-height: 240px;
  overflow-y: auto;
  box-sizing: border-box;
}

.select-option {
  min-height: 42px;
  padding: 0 14px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  color: #111827;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.select-option:hover {
  background: #f3f4f6;
}

.select-option.active {
  background: linear-gradient(135deg, #4f46e5, #2563eb);
  color: #fff;
  font-weight: 600;
}

.select-option.disabled {
  color: #9ca3af;
  cursor: not-allowed;
}

.disabled .select-trigger {
  background: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
}

.select-fade-enter-active,
.select-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.select-fade-enter-from,
.select-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 768px) {
  .select-trigger {
    min-height: 46px;
    border-radius: 12px;
  }

  .select-dropdown {
    border-radius: 14px;
  }

  .select-option {
    min-height: 44px;
  }
}
</style>