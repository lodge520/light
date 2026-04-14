<template>
  <div class="chart-card">
    <div class="card-title">光照历史曲线</div>

    <div v-if="!hasData" class="empty-block">暂无光照历史数据</div>

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

interface LuxDataset {
  label: string
  data: Array<number | null>
}

const props = defineProps<{
  labels: string[]
  datasets: LuxDataset[]
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart | null = null

const colorPalette = [
  '#409EFF',
  '#67C23A',
  '#E6A23C',
  '#F56C6C',
  '#909399',
  '#9C27B0',
  '#00ACC1',
  '#FF7043',
  '#8D6E63',
  '#26A69A',
]

const hasData = computed(() => {
  return props.labels.length > 0 && props.datasets.length > 0
})

function renderChart() {
  if (!canvasRef.value || !hasData.value) return

  if (chart) chart.destroy()

  chart = new Chart(canvasRef.value, {
    type: 'line',
    data: {
      labels: props.labels,
      datasets: props.datasets.map((item, index) => {
        const color = colorPalette[index % colorPalette.length]

        return {
          label: item.label,
          data: item.data,
          tension: 0.35,
          borderColor: color,
          backgroundColor: color,
          pointBackgroundColor: color,
          pointBorderColor: color,
          pointRadius: 4,
          pointHoverRadius: 6,
          borderWidth: 2,
          spanGaps: true,
        }
      }),
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
  () => [props.labels, props.datasets],
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