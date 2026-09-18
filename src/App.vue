<template>
  <div class="app-shell">
    <Flames />
    <router-view />
  </div>
</template>

<script setup>
import Flames from './components/Flames.vue'
</script>

<style scoped>
.app-shell {
  height: 100%;
  position: relative;
  z-index: 1;
}
</style>
