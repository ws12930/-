<template>
  <div class="leaves" aria-hidden="true">
    <span
      v-for="f in particles"
      :key="f.id"
      class="leaf"
      :style="f.style"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * 青禾叶片粒子：缓缓飘落的叶形光点，呼应看板娘「青禾」。
 * 颜色在青禾 / 薄荷 / 酸绿之间随机，尺寸与摇摆幅度各不相同。
 */
const COLORS = [
  'linear-gradient(135deg, #dce6e1, #8fd0ae)',
  'linear-gradient(135deg, #c2d6cc, #6d907f)',
  'linear-gradient(135deg, #eaffb0, #b9d94d)'
]

const particles = ref([])
let timer = null

function spawn() {
  const id = Date.now() + Math.random()
  particles.value.push({
    id,
    style: {
      left: Math.random() * 100 + 'vw',
      width: 7 + Math.random() * 8 + 'px',
      height: 7 + Math.random() * 8 + 'px',
      animationDuration: 7 + Math.random() * 7 + 's',
      animationDelay: '0s',
      background: COLORS[Math.floor(Math.random() * COLORS.length)]
    }
  })
  // 限制粒子总量，避免 DOM 无限增长
  if (particles.value.length > 44) particles.value.splice(0, particles.value.length - 44)
}

onMounted(() => {
  timer = setInterval(spawn, 420)
})
onBeforeUnmount(() => clearInterval(timer))
</script>
