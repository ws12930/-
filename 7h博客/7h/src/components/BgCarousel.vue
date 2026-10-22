<template>
  <div class="bg-carousel" aria-hidden="true">
    <!-- 所有图层常驻 DOM，靠 opacity 做交叉淡入淡出，避免切换时闪白 -->
    <div
      v-for="(img, i) in images"
      :key="img"
      class="bg-layer"
      :class="{ active: i === current }"
      :style="{ backgroundImage: `url(${img})` }"
    />

    <!-- 压暗层：保证前景文字在任意背景上都可读 -->
    <div class="bg-scrim" />

    <!-- 指示点：hover 才浮现，平时不打扰 -->
    <div class="bg-dots">
      <button
        v-for="(img, i) in images"
        :key="`dot-${i}`"
        class="bg-dot"
        :class="{ on: i === current }"
        type="button"
        :title="titles[i]"
        @click="select(i)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

import { bgAutoPlay } from '../utils/bgState'
import bgAurora from '../assets/bg/bg-qh-aurora.jpg'
import bgBamboo from '../assets/bg/bg-qh-bamboo.jpg'
import bgRipple from '../assets/bg/bg-qh-ripple.jpg'
import bgDune from '../assets/bg/bg-qh-dune.jpg'

const images = [bgAurora, bgBamboo, bgRipple, bgDune]
const titles = ['禾光 · 夜辉', '晨雾 · 竹林', '静水 · 微澜', '禾浪 · 层叠']

const STORAGE_KEY = '7h-bg'
const ROTATE_MS = 9000

const current = ref(0)
let timer = null

// 只持久化"看到第几张"，刷新后接着轮换，不会每次跳回第一张
function loadIndex() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return 0
    const parsed = JSON.parse(raw)
    const idx = Number(parsed && parsed.index)
    return Number.isInteger(idx) && idx >= 0 && idx < images.length ? idx : 0
  } catch {
    return 0
  }
}

function saveIndex() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ index: current.value, v: 1 }))
  } catch {
    /* 隐私模式下 localStorage 可能不可写，忽略即可 */
  }
}

function next() {
  select((current.value + 1) % images.length)
}

function select(i) {
  current.value = i
  saveIndex()
}

function startTimer() {
  stopTimer()
  const slow = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  timer = setInterval(next, slow ? ROTATE_MS * 2 : ROTATE_MS)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

/**
 * 自动播放开关由外部接管：首页挂载了独立的首屏轮播栏，
 * 此时 bgAutoPlay 为 false，背景定格在当前图层，避免两个轮播同时动。
 *
 * 注意：不再理会 prefers-reduced-motion。系统开「最佳性能」时浏览器会上报该值，
 * 此前会让背景彻底停止轮换，这里保持轮换（低动效下仅把间隔拉长）。
 */
function syncTimer() {
  if (bgAutoPlay.value && !document.hidden) startTimer()
  else stopTimer()
}

// 切到后台标签页时停掉，回来再继续，避免无意义地占资源
function onVisibility() {
  if (document.hidden) stopTimer()
  else syncTimer()
}

onMounted(() => {
  current.value = loadIndex()
  syncTimer()

  document.addEventListener('visibilitychange', onVisibility)
})

watch(bgAutoPlay, syncTimer)

onBeforeUnmount(() => {
  stopTimer()
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
.bg-carousel {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--c-bg);
}

.bg-layer {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  opacity: 0;
  transform: scale(1.06);
  transition: opacity 1.6s ease, transform 9s ease-out;
  will-change: opacity, transform;
  /* 降饱和：背景只做氛围，绿意收敛，不让整屏发绿 */
  filter: brightness(0.98) saturate(0.72) contrast(1.02);
}

.bg-layer.active {
  opacity: 1;
  transform: scale(1);
}

/* 遮罩：只压住中间区域保证文字可读，四周留亮让画面能透出来 */
.bg-scrim {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 45%, rgba(10, 13, 16, 0.16), rgba(10, 13, 16, 0.6) 80%),
    linear-gradient(to bottom, rgba(10, 13, 16, 0.22), rgba(10, 13, 16, 0.42));
}

.bg-dots {
  position: absolute;
  right: 18px;
  bottom: 16px;
  display: flex;
  gap: 8px;
  pointer-events: auto;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.bg-carousel:hover .bg-dots {
  opacity: 1;
}

.bg-dot {
  width: 9px;
  height: 9px;
  padding: 0;
  border-radius: 50%;
  background: rgba(230, 242, 236, 0.35);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.4);
  transition: background 0.25s ease, transform 0.25s ease;
}

.bg-dot:hover {
  background: rgba(230, 242, 236, 0.7);
}

.bg-dot.on {
  background: var(--c-acid);
  transform: scale(1.25);
}
</style>
