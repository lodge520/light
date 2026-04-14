<template>
  <div class="chart-card">
    <div class="card-title">温度 / 人流趋势</div>

    <div v-if="!hasData" class="empty-block">暂无温度 / 人流历史数据</div>

    <div v-else class="chart-canvas-wrap">
      <canvas ref="canvasRef"></canvas>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Legend,
  Tooltip,
} from 'chart.js'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

Chart.register(
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Legend,
  Tooltip,
)

const props = defineProps<{
  labels: string[]
  tempSeries: number[]
  peopleSeries: number[]
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart | null = null

const hasData = computed(() => {
  return props.labels.length > 0 && (props.tempSeries.length > 0 || props.peopleSeries.length > 0)
})

function renderChart() {
  if (!canvasRef.value || !hasData.value) return

  if (chart) {
    chart.destroy()
  }

  chart = new Chart(canvasRef.value, {
    type: 'line',
    data: {
      labels: props.labels,
      datasets: [
        {
          label: '温度',
          data: props.tempSeries,
          tension: 0.35,
          borderColor: '#409EFF',
          backgroundColor: '#409EFF',
          pointBackgroundColor: '#409EFF',
          pointBorderColor: '#409EFF',
          pointRadius: 4,
          pointHoverRadius: 6,
          borderWidth: 2,
          spanGaps: true,
        },
        {
          label: '人流',
          data: props.peopleSeries,
          tension: 0.35,
          borderColor: '#67C23A',
          backgroundColor: '#67C23A',
          pointBackgroundColor: '#67C23A',
          pointBorderColor: '#67C23A',
          pointRadius: 4,
          pointHoverRadius: 6,
          borderWidth: 2,
          spanGaps: true,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'nearest',
        intersect: false,
      },
      plugins: {
        legend: {
          position: 'top',
        },
      },
    },
  })
}

onMounted(renderChart)

watch(
  () => [props.labels, props.tempSeries, props.peopleSeries],
  () => {
    if (chart) {
      chart.destroy()
      chart = null
    }
    renderChart()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  if (chart) chart.destroy()
})
</script>