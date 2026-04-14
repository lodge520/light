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