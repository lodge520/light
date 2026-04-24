<template>
  <div class="time-container">
    <div class="current-time">{{ currentTime }}</div>
    <div class="time-row">
      <p id="weatherDisplay">{{ weatherText }}</p>
      <div class="current-week">{{ weekInfo }}</div>
      <div class="current-date">{{ dateInfo }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  currentTime: string
  weekInfo: string
  dateInfo: string
  weatherText?: string
}>()
</script>

<style scoped>
.time-container {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-bottom: 24px;
}

.current-time {
  font-size: 2rem;
  font-weight: bold;
  line-height: 1;
}

.time-row {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  color: #666;
}
@media (max-width: 768px) {
  .time-container {
    align-items: flex-start;
  }

  .time-row {
    flex-wrap: wrap;
  }
}
</style>