<template>
  <div class="chart-card">
    <div class="card-title">热区时长分布</div>

    <div v-if="rows.length === 0" class="empty-block">暂无热区数据</div>

    <template v-else>
      <div class="heatmap-list">
        <div
          v-for="item in rows"
          :key="item.chipId"
          class="heatmap-row"
        >
          <div class="heatmap-label">{{ item.chipId }}</div>
          <div class="heatmap-bar-wrap">
            <div
              class="heatmap-bar"
              :style="{ width: `${getPercent(item.totalDuration)}%` }"
            />
          </div>
          <div class="heatmap-value">{{ formatSeconds(item.totalDuration) }} 秒</div>
        </div>
      </div>

      <div class="heat-circle-section">
        <div class="heat-circle-title">热区圆形分布</div>

        <div class="heat-circle-grid">
          <div
            v-for="item in rows"
            :key="`${item.chipId}-circle`"
            class="heat-circle-item"
          >
            <div
              class="heat-circle"
              :style="{
                width: `${getCircleSize(item.totalDuration)}px`,
                height: `${getCircleSize(item.totalDuration)}px`,
                opacity: getCircleOpacity(item.totalDuration),
              }"
            >
              {{ formatSeconds(item.totalDuration) }}
            </div>
            <div class="heat-circle-device">{{ item.chipId }}</div>
            <div class="heat-circle-time">{{ formatSeconds(item.totalDuration) }} 秒</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { DurationSummaryItem } from '../../types/duration'

const props = defineProps<{
  rows: DurationSummaryItem[]
}>()

function formatSeconds(value: number) {
  if (!Number.isFinite(value)) return 0
  return value > 10000 ? Math.round(value / 1000) : Math.round(value)
}

function getMaxValue() {
  return Math.max(...props.rows.map(item => item.totalDuration), 1)
}

function getPercent(value: number) {
  const max = getMaxValue()
  return Math.max(8, Math.round((value / max) * 100))
}

function getCircleSize(value: number) {
  const max = getMaxValue()
  const minSize = 48
  const maxSize = 110
  return Math.round(minSize + (value / max) * (maxSize - minSize))
}

function getCircleOpacity(value: number) {
  const max = getMaxValue()
  const minOpacity = 0.45
  const maxOpacity = 0.95
  return minOpacity + (value / max) * (maxOpacity - minOpacity)
}
</script>