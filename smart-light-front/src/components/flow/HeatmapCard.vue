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

function toDisplaySeconds(value: number) {
  if (!Number.isFinite(value)) return 0
  return value > 10000 ? value / 1000 : value
}

function formatSeconds(value: number) {
  return Math.round(toDisplaySeconds(value))
}

function getMaxValue() {
  return Math.max(...props.rows.map(item => toDisplaySeconds(item.totalDuration)), 1)
}

function getPercent(value: number) {
  const max = getMaxValue()
  const current = toDisplaySeconds(value)
  return Math.max(8, Math.round((current / max) * 100))
}

function getCircleSize(value: number) {
  const max = getMaxValue()
  const current = toDisplaySeconds(value)
  const minSize = 48
  const maxSize = 110
  return Math.round(minSize + (current / max) * (maxSize - minSize))
}

function getCircleOpacity(value: number) {
  const max = getMaxValue()
  const current = toDisplaySeconds(value)
  const minOpacity = 0.45
  const maxOpacity = 0.95
  return minOpacity + (current / max) * (maxOpacity - minOpacity)
}
</script>

<style scoped>
.heatmap-list {
  display: grid;
  gap: 12px;
}

.heatmap-row {
  display: grid;
  grid-template-columns: 100px 1fr 80px;
  gap: 12px;
  align-items: center;
}

.heatmap-label,
.heatmap-value {
  font-size: 14px;
  color: #606266;
}

.heatmap-bar-wrap {
  height: 14px;
  background: #f2f3f5;
  border-radius: 999px;
  overflow: hidden;
}

.heatmap-bar {
  height: 100%;
  background: linear-gradient(90deg, #ffb347 0%, #ff6b6b 100%);
  border-radius: 999px;
}

.heat-circle-section {
  margin-top: 24px;
}

.heat-circle-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 14px;
}

.heat-circle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 18px;
  align-items: end;
}

.heat-circle-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.heat-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #ffcc80 0%, #ff7043 55%, #ef5350 100%);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(239, 83, 80, 0.28);
  min-width: 48px;
  min-height: 48px;
}

.heat-circle-device {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  text-align: center;
}

.heat-circle-time {
  font-size: 12px;
  color: #606266;
  text-align: center;
}
</style>