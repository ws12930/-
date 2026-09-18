<template>
  <div class="flames" aria-hidden="true">
    <span
      v-for="f in particles"
      :key="f.id"
      class="flame"
      :style="f.style"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const particles = ref([])
let timer = null

function spawn() {
  const leftNinja = Math.random() < 0.5
  const id = Date.now() + Math.random()
  particles.value.push({
    id,
    style: {
      left: Math.random() * 100 + 'vw',
      width: 8 + Math.random() * 12 + 'px',
      height: 10 + Math.random() * 14 + 'px',
      animationDuration: 4 + Math.random() * 4 + 's',
      animationDelay: '0s',
      background: leftNinja
        ? 'radial-gradient(circle at 30% 30%, #ffd24a, #ff6a00 60%, rgba(255,106,0,0))'
        : 'radial-gradient(circle at 30% 30%, #b9f4ff, #0090ff 60%, rgba(0,144,255,0))'
    }
  })
  // 限制粒子总量，避免 DOM 无限增长
  if (particles.value.length > 60) particles.value.splice(0, particles.value.length - 60)
}

onMounted(() => {
  timer = setInterval(spawn, 180)
})
onBeforeUnmount(() => clearInterval(timer))
</script>
