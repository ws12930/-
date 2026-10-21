<template>
  <section
    class="banner"
    aria-label="首屏轮播"
    @mouseenter="pause"
    @mouseleave="resume"
    @touchstart.passive="onTouchStart"
    @touchend.passive="onTouchEnd"
  >
    <!-- 所有图层常驻，靠 opacity 交叉淡入淡出，复用背景轮播的切换质感 -->
    <div
      v-for="(item, i) in slides"
      :key="item.src"
      class="bn-layer"
      :class="{ active: i === current }"
      :style="{ backgroundImage: `url(${item.src})` }"
    />

    <div class="bn-scrim" />

    <div v-if="!slides[current].clean" class="bn-caption">
      <h2 class="bn-title">{{ slides[current].title }}</h2>
      <p class="bn-sub">{{ slides[current].sub }}</p>
    </div>

    <!-- 左右箭头：窄屏隐藏，改由手势滑动 -->
    <button class="bn-arrow prev" type="button" title="上一张" @click="prev">
      <span class="bn-chev"></span>
    </button>
    <button class="bn-arrow next" type="button" title="下一张" @click="next">
      <span class="bn-chev"></span>
    </button>

    <div class="bn-dots">
      <button
        v-for="(item, i) in slides"
        :key="`dot-${i}`"
        class="bn-dot"
        :class="{ on: i === current }"
        type="button"
        :title="item.title"
        :aria-label="item.title"
        :aria-current="i === current"
        @click="select(i)"
      />
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

import bannerWenming from '../assets/bg/banner-wenming.jpg'
import bgAurora from '../assets/bg/bg-qh-aurora.jpg'
import bgBamboo from '../assets/bg/bg-qh-bamboo.jpg'
import bgRipple from '../assets/bg/bg-qh-ripple.jpg'
import bgDune from '../assets/bg/bg-qh-dune.jpg'

/**
 * clean: 图片自带完整文案（如运营横幅），不再叠加左下角标题。
 */
const slides = [
  { src: bannerWenming, title: '文明访问', sub: '让每一次表达都更有温度', clean: true },
  { src: bgAurora, title: '禾光 · 夜辉', sub: '青禾色的夜空' },
  { src: bgBamboo, title: '晨雾 · 竹林', sub: '清新与宁静' },
  { src: bgRipple, title: '静水 · 微澜', sub: '涟漪与倒影' },
  { src: bgDune, title: '禾浪 · 起伏', sub: '流动的曲线' }
]

const ROTATE_MS = 5000
const SWIPE_THRESHOLD = 40

const current = ref(0)
const paused = ref(false)

let timer = null
let touchStartX = 0
let touchStartY = 0

function select(i) {
  current.value = (i + slides.length) % slides.length
}

function next() {
  select(current.value + 1)
}

function prev() {
  select(current.value - 1)
}

function startTimer() {
  stopTimer()
  timer = setInterval(next, ROTATE_MS)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function pause() {
  paused.value = true
  stopTimer()
}

function resume() {
  paused.value = false
  if (!reduceMotion() && !document.hidden) startTimer()
}

function reduceMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function onVisibility() {
  if (document.hidden) stopTimer()
  else if (!paused.value && !reduceMotion()) startTimer()
}

function onTouchStart(e) {
  const t = e.changedTouches[0]
  touchStartX = t.clientX
  touchStartY = t.clientY
  pause()
}

function onTouchEnd(e) {
  const t = e.changedTouches[0]
  const dx = t.clientX - touchStartX
  const dy = t.clientY - touchStartY
  // 纵向位移更大时判定为页面滚动，不抢手势
  if (Math.abs(dx) > SWIPE_THRESHOLD && Math.abs(dx) > Math.abs(dy)) {
    if (dx < 0) next()
    else prev()
  }
  resume()
}

onMounted(() => {
  if (!reduceMotion()) startTimer()
  document.addEventListener('visibilitychange', onVisibility)
})

onBeforeUnmount(() => {
  stopTimer()
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
.banner {
  position: relative;
  width: 100%;
  /* 高度与第一张运营横幅（1680x340，约 4.94:1）匹配：
     900px 宽容器下完整显示约需 182px，避免 cover 放大后裁掉两侧文案 */
  height: min(182px, 20vw);
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: var(--c-panel);
  touch-action: pan-y;
}

.bn-layer {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  opacity: 0;
  transform: scale(1.04);
  transition: opacity 0.9s ease, transform 6s ease-out;
  will-change: opacity, transform;
  filter: brightness(1.06) saturate(1.05);
}

.bn-layer.active {
  opacity: 1;
  transform: scale(1);
}

/* 只压住下半部分，保证标题可读同时画面能透出来 */
.bn-scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(10, 13, 16, 0.78) 0%, rgba(10, 13, 16, 0.25) 55%, rgba(10, 13, 16, 0.1) 100%);
}

.bn-caption {
  position: absolute;
  left: 26px;
  bottom: 30px;
  max-width: 70%;
}

.bn-title {
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 3px;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.75);
}

.bn-sub {
  margin-top: 6px;
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(232, 232, 240, 0.72);
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.7);
}

/* ===== 箭头 ===== */
.bn-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(10, 13, 16, 0.42);
  opacity: 0;
  transition: opacity 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.banner:hover .bn-arrow {
  opacity: 1;
}

.bn-arrow:hover {
  background: rgba(10, 13, 16, 0.72);
  border-color: rgba(255, 255, 255, 0.45);
}

.bn-arrow.prev { left: 12px; }
.bn-arrow.next { right: 12px; }

.bn-chev {
  width: 8px;
  height: 8px;
  border-left: 1.5px solid #e8e8f0;
  border-bottom: 1.5px solid #e8e8f0;
}

.bn-arrow.prev .bn-chev { transform: rotate(45deg); margin-left: 2px; }
.bn-arrow.next .bn-chev { transform: rotate(-135deg); margin-right: 2px; }

/* ===== 指示点 ===== */
.bn-dots {
  position: absolute;
  right: 22px;
  bottom: 20px;
  display: flex;
  gap: 8px;
}

.bn-dot {
  width: 9px;
  height: 9px;
  padding: 0;
  border-radius: 50%;
  background: rgba(232, 232, 240, 0.38);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.45);
  transition: background 0.25s ease, transform 0.25s ease;
}

.bn-dot:hover {
  background: rgba(232, 232, 240, 0.75);
}

.bn-dot.on {
  background: var(--c-ninja);
  transform: scale(1.3);
}

@media (max-width: 700px) {
  .banner { height: 150px; }

  .bn-caption { left: 16px; bottom: 26px; max-width: 78%; }
  .bn-title { font-size: 15px; letter-spacing: 2px; }
  .bn-sub { display: none; }

  /* 窄屏没有 hover，箭头交给手势，直接隐藏 */
  .bn-arrow { display: none; }

  .bn-dots { right: 14px; bottom: 14px; gap: 7px; }
  .bn-dot { width: 8px; height: 8px; }
}
</style>
