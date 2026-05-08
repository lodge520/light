<template>
  <div class="chart-card heat-card">
    <div class="card-title">热区时长分布</div>

    <div v-if="heatItems.length === 0" class="empty-block">暂无热区时长数据</div>

    <template v-else>
      <div class="heat-legend" aria-label="热区图例">
        <span><i class="legend-dot legend-cold"></i>冷区</span>
        <span><i class="legend-dot legend-mid"></i>中等</span>
        <span><i class="legend-dot legend-hot"></i>热区</span>
      </div>

      <div class="heat-zone-grid">
        <button
          v-for="item in heatItems"
          :key="item.chipId"
          class="heat-zone-item"
          type="button"
          :title="`${item.name} | 停留时长 ${item.durationText} | 占比 ${item.percentText}`"
          @click="selectedChipId = selectedChipId === item.chipId ? '' : item.chipId"
        >
          <span v-if="item.rank <= 3" class="rank-badge" :class="`rank-${item.rank}`">Top {{ item.rank }}</span>
          <span
            class="heat-bubble"
            :style="{
              width: `${item.size}px`,
              height: `${item.size}px`,
              background: item.background,
              boxShadow: item.shadow,
            }"
          >
            <span class="heat-bubble-value">{{ item.shortDurationText }}</span>
          </span>
          <span class="heat-zone-name">{{ item.name }}</span>
          <span class="heat-zone-time">{{ item.durationText }}</span>
          <span class="heat-zone-percent">{{ item.percentText }}</span>

          <span v-if="selectedChipId === item.chipId" class="heat-detail-popover">
            <strong>{{ item.name }}</strong>
            <span>停留时长：{{ item.durationText }}</span>
            <span>占比：{{ item.percentText }}</span>
          </span>
        </button>
      </div>

      <div class="heat-rank-list">
        <div
          v-for="item in heatItems"
          :key="`${item.chipId}-bar`"
          class="heat-rank-row"
        >
          <div class="heat-rank-meta">
            <span class="heat-rank-index">#{{ item.rank }}</span>
            <span class="heat-rank-name">{{ item.name }}</span>
          </div>
          <div class="heat-bar-track">
            <div
              class="heat-bar-fill"
              :style="{
                width: `${Math.max(item.ratio * 100, 4)}%`,
                background: item.background,
              }"
            />
          </div>
          <div class="heat-rank-duration">{{ item.durationText }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { DurationSummaryItem } from '../../types/duration'

const props = defineProps<{
  rows: DurationSummaryItem[]
}>()

const selectedChipId = ref('')

function toDisplaySeconds(value: number) {
  if (!Number.isFinite(value)) return 0
  return value > 10000 ? value / 1000 : value
}

function formatDuration(seconds: number) {
  const rounded = Math.round(seconds)
  if (rounded < 60) {
    return `${rounded}秒`
  }

  const minutes = Math.floor(rounded / 60)
  const remainSeconds = rounded % 60
  if (minutes < 60) {
    return remainSeconds > 0 ? `${minutes}分${remainSeconds}秒` : `${minutes}分`
  }

  const hours = Math.floor(minutes / 60)
  const remainMinutes = minutes % 60
  return remainMinutes > 0 ? `${hours}小时${remainMinutes}分` : `${hours}小时`
}

function interpolateColor(start: string, end: string, ratio: number) {
  const parse = (hex: string) => [1, 3, 5].map(index => Number.parseInt(hex.slice(index, index + 2), 16))
  const [sr, sg, sb] = parse(start)
  const [er, eg, eb] = parse(end)
  const mix = (a: number, b: number) => Math.round(a + (b - a) * ratio)
  return `rgb(${mix(sr, er)}, ${mix(sg, eg)}, ${mix(sb, eb)})`
}

function getHeatColor(ratio: number) {
  if (ratio <= 0.33) {
    return interpolateColor('#38bdf8', '#22d3ee', ratio / 0.33)
  }

  if (ratio <= 0.66) {
    return interpolateColor('#22d3ee', '#facc15', (ratio - 0.33) / 0.33)
  }

  return interpolateColor('#facc15', '#ef4444', (ratio - 0.66) / 0.34)
}

const heatItems = computed(() => {
  const validRows = props.rows
    .map(item => ({
      chipId: item.chipId,
      name: item.chipId || '未命名热区',
      seconds: toDisplaySeconds(item.totalDuration),
    }))
    .filter(item => item.seconds > 0)
    .sort((a, b) => b.seconds - a.seconds)

  const maxDuration = Math.max(...validRows.map(item => item.seconds), 1)
  const totalDuration = validRows.reduce((sum, item) => sum + item.seconds, 0)

  return validRows.map((item, index) => {
    const ratio = Math.min(item.seconds / maxDuration, 1)
    const color = getHeatColor(ratio)
    const shadowOpacity = 0.16 + ratio * 0.28
    const size = Math.round(34 + ratio * 42)

    return {
      ...item,
      rank: index + 1,
      ratio,
      size,
      background: `radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.72), ${color} 42%, ${color} 100%)`,
      shadow: `0 10px ${Math.round(18 + ratio * 18)}px rgba(${ratio > 0.66 ? '239, 68, 68' : '14, 165, 233'}, ${shadowOpacity})`,
      durationText: formatDuration(item.seconds),
      shortDurationText: formatDuration(item.seconds),
      percentText: totalDuration > 0 ? `${Math.round((item.seconds / totalDuration) * 100)}%` : '0%',
    }
  })
})
</script>

<style scoped>
.heat-card {
  overflow: visible;
}

.heat-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-bottom: 18px;
  color: #606266;
  font-size: 13px;
}

.heat-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  display: inline-block;
}

.legend-cold {
  background: #38bdf8;
}

.legend-mid {
  background: #facc15;
}

.legend-hot {
  background: #ef4444;
}

.heat-zone-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  gap: 16px;
  align-items: stretch;
}

.heat-zone-item {
  position: relative;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  background: #fbfcff;
  padding: 14px 10px 12px;
  min-height: 162px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  color: inherit;
}

.heat-zone-item:hover,
.heat-zone-item:focus-visible {
  border-color: #c7d2fe;
  outline: none;
}

.rank-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  border-radius: 999px;
  padding: 3px 7px;
  font-size: 11px;
  font-weight: 700;
  background: #eef2ff;
  color: #4f46e5;
}

.rank-1 {
  background: #fee2e2;
  color: #dc2626;
}

.rank-2 {
  background: #fef3c7;
  color: #d97706;
}

.rank-3 {
  background: #e0f2fe;
  color: #0284c7;
}

.heat-bubble {
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(15, 23, 42, 0.25);
  transition: transform 0.18s ease;
}

.heat-zone-item:hover .heat-bubble,
.heat-zone-item:focus-visible .heat-bubble {
  transform: translateY(-2px) scale(1.04);
}

.heat-bubble-value {
  font-size: 13px;
  white-space: nowrap;
}

.heat-zone-name {
  max-width: 100%;
  color: #303133;
  font-weight: 700;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.heat-zone-time,
.heat-zone-percent {
  color: #606266;
  font-size: 12px;
}

.heat-detail-popover {
  position: absolute;
  z-index: 2;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%);
  width: max-content;
  max-width: 220px;
  border-radius: 10px;
  background: rgba(17, 24, 39, 0.94);
  color: #fff;
  padding: 9px 10px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.22);
  display: grid;
  gap: 4px;
  font-size: 12px;
  text-align: left;
}

.heat-rank-list {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.heat-rank-row {
  display: grid;
  grid-template-columns: minmax(110px, 140px) 1fr auto;
  gap: 12px;
  align-items: center;
}

.heat-rank-meta {
  min-width: 0;
  display: flex;
  gap: 8px;
  align-items: center;
}

.heat-rank-index {
  color: #909399;
  font-size: 12px;
  font-weight: 700;
}

.heat-rank-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
  font-size: 13px;
}

.heat-bar-track {
  height: 10px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.heat-bar-fill {
  height: 100%;
  border-radius: inherit;
}

.heat-rank-duration {
  color: #606266;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .heat-zone-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .heat-zone-item {
    min-height: 150px;
    padding-inline: 8px;
  }

  .heat-rank-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .heat-rank-duration {
    justify-self: end;
  }

  .heat-detail-popover {
    left: 8px;
    right: 8px;
    bottom: calc(100% + 6px);
    transform: none;
    width: auto;
  }
}
</style>
