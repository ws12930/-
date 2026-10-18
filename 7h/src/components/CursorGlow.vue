<template>
  <div ref="el" class="cursor-glow" aria-hidden="true"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/** 光标辉光：一团跟随鼠标的青禾柔光（样式在 main.css 全局定义） */
const el = ref(null)

function onMove(e) {
  if (!el.value) return
  el.value.style.transform = `translate(${e.clientX - 280}px, ${e.clientY - 280}px)`
}

onMounted(() => {
  window.addEventListener('pointermove', onMove, { passive: true })
})
onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onMove)
})
</script>
