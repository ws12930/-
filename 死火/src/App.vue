<template>
  <div class="app-shell">
    <BgCarousel />
    <Flames />
    <router-view />
  </div>
</template>

<script setup>
import BgCarousel from './components/BgCarousel.vue'
import Flames from './components/Flames.vue'
</script>

<style scoped>
.app-shell {
  height: 100%;
  position: relative;
  z-index: 1;
}
</style>
