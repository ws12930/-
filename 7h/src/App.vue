<template>
  <div class="app-shell">
    <BgCarousel />
    <div class="site-grid" aria-hidden="true"></div>
    <CursorGlow />
    <Leaves />
    <router-view />
    <Mascot />
  </div>
</template>

<script setup>
import BgCarousel from './components/BgCarousel.vue'
import CursorGlow from './components/CursorGlow.vue'
import Leaves from './components/Leaves.vue'
import Mascot from './components/Mascot.vue'
</script>

<style scoped>
.app-shell {
  height: 100%;
  position: relative;
  z-index: 1;
}
</style>
