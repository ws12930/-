<template>
  <div
    ref="rootEl"
    class="mascot"
    :class="[`side-${side}`, { dragging, mini }]"
    :style="rootStyle"
  >
    <!-- 最小化：缩成一颗小圆点 -->
    <button
      v-if="mini"
      class="mc-dot"
      type="button"
      title="青禾在这里（点击展开）"
      @click="toggleMini(false)"
    >
      <img :src="FACES[0].img" alt="青禾" />
    </button>

    <template v-else>
      <!-- 悬停名片 -->
      <Transition name="card">
        <div v-if="showCard && !dragging" class="mc-card">
          <p class="mc-card-name">青禾</p>
          <p class="mc-card-role">博客看板娘</p>
          <p class="mc-card-quote">「慢慢写，好好说」</p>
        </div>
      </Transition>

      <Transition name="bub">
        <p v-if="showBubble" class="mc-bubble">{{ line }}</p>
      </Transition>

      <button
        ref="petEl"
        class="mc-body"
        :class="idleClass"
        type="button"
        :title="`看板娘 青禾 · ${face.label}（点我说话 · 双击收起）`"
        :aria-label="`看板娘青禾 · ${face.label}`"
        @pointerdown="onDown"
        @click="onTap"
        @dblclick="toggleMini(true)"
        @mouseenter="showCard = true"
        @mouseleave="showCard = false"
      >
        <img :src="face.img" :alt="face.label" draggable="false" />
      </button>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'

import mWelcome from '../assets/mascot/mascot-welcome.png'
import mNofight from '../assets/mascot/mascot-nofight.png'
import mWriting from '../assets/mascot/mascot-writing.png'

/**
 * 桌宠版看板娘「青禾」：
 * - 可拖拽（位置记忆，靠近左右边缘自动吸附）
 * - 待机小动作（张望 / 蹦跳 / 摇摆）
 * - 随机冒话 + 分时段问候
 * - 悬停出名片，双击收成小圆点
 */
const NAME = '青禾'

const FACES = [
  {
    img: mWelcome,
    label: '欢迎呀',
    lines: [`欢迎来到 7h 呀～我是${NAME}`, `新朋友你好，我是${NAME}！`, '逛累了就歇一会儿吧']
  },
  {
    img: mNofight,
    label: '不许吵架',
    lines: [`评论区要文明，不许吵架哦！——${NAME}`, '有话好好说嘛～', '理性发言最帅啦']
  },
  {
    img: mWriting,
    label: '加油写文',
    lines: [`今天也要好好写文，${NAME}给你加油～`, '想到什么就先记下来吧', '你的文章值得被看到']
  }
]

/** 分时段问候（青禾的开场白） */
function daypartLine() {
  const h = new Date().getHours()
  if (h >= 5 && h < 11) return '早上好呀，今天的灵感来了吗？'
  if (h >= 11 && h < 14) return '中午啦，吃完饭慢慢逛～'
  if (h >= 14 && h < 18) return '下午好！写累了记得休息眼睛哦'
  if (h >= 18 && h < 23) return `晚上好呀，${NAME}陪你慢慢逛`
  return '夜深了，写完这段就早点休息吧'
}

const IDLE_MS = 8000
const BUBBLE_MS = 6200
const STORE_KEY = 'qh-mascot-v1'

const route = useRoute()

const idx = ref(0)
const line = ref('')
const showBubble = ref(false)
const showCard = ref(false)
const mini = ref(false)

// ---- 位置记忆与吸附 ----
const pos = ref(null) // { x, y } px（元素左上角）
const side = ref('right')
const dragging = ref(false)

const rootStyle = computed(() => {
  if (!pos.value) return {}
  return { left: `${pos.value.x}px`, top: `${pos.value.y}px`, right: 'auto', bottom: 'auto' }
})

const petEl = ref(null)
const rootEl = ref(null)

let rotateTimer = null
let bubbleTimer = null
let idleTimer = null
let speakTimer = null

const face = computed(() => FACES[idx.value])

// ---- 待机小动作 ----
const idleClass = ref('')
const IDLE_ANIMS = ['is-peek', 'is-hop', 'is-sway']

function playIdle() {
  if (mini.value || dragging.value) return
  idleClass.value = ''
  // 下一帧再加类，保证连续同类动画能重放
  requestAnimationFrame(() => {
    idleClass.value = IDLE_ANIMS[Math.floor(Math.random() * IDLE_ANIMS.length)]
    setTimeout(() => (idleClass.value = ''), 2600)
  })
  idleTimer = setTimeout(playIdle, 6000 + Math.random() * 9000)
}

// ---- 说话 ----
function pick(pool) {
  return pool[Math.floor(Math.random() * pool.length)]
}

function speak(text) {
  line.value = text || pick(face.value.lines)
  showBubble.value = true
  clearTimeout(bubbleTimer)
  bubbleTimer = setTimeout(() => (showBubble.value = false), BUBBLE_MS)
}

/** 随机冒话：25-50s 一次，三分之一概率说时段问候 */
function scheduleChatter() {
  clearTimeout(speakTimer)
  speakTimer = setTimeout(() => {
    if (!mini.value && !document.hidden && !dragging.value) {
      speak(Math.random() < 0.34 ? daypartLine() : undefined)
    }
    scheduleChatter()
  }, 25000 + Math.random() * 25000)
}

function next() {
  idx.value = (idx.value + 1) % FACES.length
  speak()
  restartRotate()
}

function restartRotate() {
  clearInterval(rotateTimer)
  rotateTimer = setInterval(() => {
    if (!mini.value && !document.hidden) idx.value = (idx.value + 1) % FACES.length
  }, IDLE_MS)
}

// ---- 拖拽 ----
const MOVE_THRESHOLD = 6
let downInfo = null // { px, py, x0, y0, moved }

function onDown(e) {
  if (mini.value) return
  const base = pos.value || defaultPos()
  downInfo = { px: e.clientX, py: e.clientY, x0: base.x, y0: base.y, moved: false }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onUp)
}

function defaultPos() {
  // 默认停在右下角：x 以 css right:20px 为准，这里换算为 left
  const el = rootEl.value
  const w = el ? el.offsetWidth : 130
  const h = el ? el.offsetHeight : 190
  return { x: window.innerWidth - w - 20, y: window.innerHeight - h - 14 }
}

function onMove(e) {
  if (!downInfo) return
  const dx = e.clientX - downInfo.px
  const dy = e.clientY - downInfo.py
  if (!downInfo.moved && Math.hypot(dx, dy) > MOVE_THRESHOLD) {
    downInfo.moved = true
    dragging.value = true
    showCard.value = false
  }
  if (downInfo.moved) {
    const el = rootEl.value
    const w = el ? el.offsetWidth : 130
    const h = el ? el.offsetHeight : 190
    const x = Math.min(Math.max(downInfo.x0 + dx, 8), window.innerWidth - w - 8)
    const y = Math.min(Math.max(downInfo.y0 + dy, 56), window.innerHeight - h - 8)
    pos.value = { x, y }
  }
}

function onUp() {
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', onUp)
  if (downInfo && downInfo.moved) {
    snap()
    saveState()
    // 拖完抱怨一句
    speak(pick(['放手吧，我就在这儿待着～', '这里视野不错！', `被你拎来拎去的啦——${NAME}`]))
  }
  downInfo = null
  dragging.value = false
}

/** 靠近左右边缘 72px 内吸附；并始终保证不出屏 */
function snap() {
  const p = pos.value
  if (!p) return
  const el = rootEl.value
  const w = el ? el.offsetWidth : 130
  const mid = p.x + w / 2
  side.value = mid < window.innerWidth / 2 ? 'left' : 'right'
  const edgeL = 20
  const edgeR = window.innerWidth - w - 20
  if (Math.abs(p.x - edgeL) < 72) pos.value = { ...p, x: edgeL }
  else if (Math.abs(p.x - edgeR) < 72) pos.value = { ...p, x: edgeR }
}

// ---- 点击 / 双击 ----
let tapTimer = null
function onTap() {
  if (dragging.value) return
  // 单击延迟执行，留给双击机会
  clearTimeout(tapTimer)
  tapTimer = setTimeout(() => {
    if (!mini.value) next()
  }, 260)
}

function toggleMini(v) {
  clearTimeout(tapTimer)
  mini.value = v
  saveState()
  if (!v) speak(`我回来啦～`)
}

// ---- 持久化 ----
function saveState() {
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify({ pos: pos.value, side: side.value, mini: mini.value }))
  } catch (e) { /* 忽略 */ }
}

function loadState() {
  try {
    const s = JSON.parse(localStorage.getItem(STORE_KEY) || 'null')
    if (s && s.pos && typeof s.pos.x === 'number') {
      pos.value = s.pos
      side.value = s.side || 'right'
      mini.value = !!s.mini
    }
  } catch (e) { /* 忽略 */ }
}

// 窗口尺寸变化时把看板娘拉回屏内
function onResize() {
  if (!pos.value) return
  const el = rootEl.value
  const w = el ? el.offsetWidth : 130
  const h = el ? el.offsetHeight : 190
  pos.value = {
    x: Math.min(Math.max(pos.value.x, 8), Math.max(8, window.innerWidth - w - 8)),
    y: Math.min(Math.max(pos.value.y, 56), Math.max(56, window.innerHeight - h - 8))
  }
}

onMounted(() => {
  loadState()
  // 未拖过：保持默认右下角（css 定位）；拖过才用 left/top
  if (!pos.value) side.value = 'right'
  window.addEventListener('resize', onResize)
  speak(daypartLine())
  restartRotate()
  playIdle()
  scheduleChatter()
})

onBeforeUnmount(() => {
  clearInterval(rotateTimer)
  clearTimeout(bubbleTimer)
  clearTimeout(speakTimer)
  clearTimeout(idleTimer)
  clearTimeout(tapTimer)
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', onUp)
  window.removeEventListener('resize', onResize)
})

// 路由切换时偶尔换个表情，别在每页都一个样
watch(
  () => route.path,
  () => {
    if (!mini.value && Math.random() < 0.6) {
      idx.value = (idx.value + 1) % FACES.length
    }
  }
)
</script>

<style scoped>
.mascot {
  position: fixed;
  right: 20px;
  bottom: 14px;
  z-index: 45;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  pointer-events: none;
  transition: left 0.28s ease, top 0.28s ease;
}
.mascot.dragging { transition: none; }

/* 浅色气泡 */
.mc-bubble {
  max-width: 230px;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.55;
  color: #1b1d20;
  background: linear-gradient(135deg, #f4ede2 0%, #fbf8f1 100%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 14px 14px 4px 14px;
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.45);
  text-align: right;
}
.side-left .mc-bubble { border-radius: 14px 14px 14px 4px; text-align: left; }

/* 悬停名片 */
.mc-card {
  padding: 10px 16px;
  text-align: right;
  color: #eef1f0;
  background: rgba(18, 22, 25, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.mc-card-name {
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 2px;
  background: var(--grad-title);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.mc-card-role { margin-top: 2px; font-size: 12px; color: #aab4ae; letter-spacing: 1px; }
.mc-card-quote { margin-top: 5px; font-size: 12px; color: #cfd8d3; }
.card-enter-active, .card-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.card-enter-from, .card-leave-to { opacity: 0; transform: translateY(4px); }

.mc-body {
  pointer-events: auto;
  width: 112px;
  padding: 0;
  border: none;
  background: none;
  cursor: grab;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.5));
  animation: mc-float 3.4s ease-in-out infinite;
  transition: transform 0.2s ease;
  touch-action: none;
  user-select: none;
}
.mascot.dragging .mc-body { cursor: grabbing; animation: none; }
.mc-body:hover { transform: scale(1.06); }
.mc-body:active { transform: scale(0.97); }
.mc-body img { display: block; width: 100%; height: auto; user-select: none; -webkit-user-drag: none; }

@keyframes mc-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}

/* ---- 待机小动作 ---- */
/* 张望：探头看左右 */
.mc-body.is-peek { animation: mc-peek 2.2s ease-in-out; }
@keyframes mc-peek {
  0%, 100% { transform: rotate(0) translateY(0); }
  25% { transform: rotate(-6deg) translateY(-4px); }
  55% { transform: rotate(6deg) translateY(-4px); }
  80% { transform: rotate(-3deg) translateY(0); }
}
/* 蹦一下 */
.mc-body.is-hop { animation: mc-hop 1.6s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes mc-hop {
  0%, 100% { transform: translateY(0) scaleY(1); }
  18% { transform: translateY(2px) scaleY(0.92); }
  40% { transform: translateY(-26px) scaleY(1.06); }
  62% { transform: translateY(0) scaleY(0.96); }
  78% { transform: translateY(-6px) scaleY(1.02); }
}
/* 摇摆 */
.mc-body.is-sway { animation: mc-sway 2.4s ease-in-out; }
@keyframes mc-sway {
  0%, 100% { transform: rotate(0); }
  30% { transform: rotate(5deg); }
  70% { transform: rotate(-5deg); }
}

/* ---- 最小化圆点 ---- */
.mc-dot {
  pointer-events: auto;
  width: 42px;
  height: 42px;
  padding: 0;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(16, 20, 24, 0.85);
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.5);
  transition: transform 0.2s ease;
}
.mc-dot:hover { transform: scale(1.12); }
.mc-dot img { display: block; width: 100%; height: 100%; object-fit: cover; }

/* 气泡进出 */
.bub-enter-active, .bub-leave-active { transition: opacity 0.28s ease, transform 0.28s ease; }
.bub-enter-from, .bub-leave-to { opacity: 0; transform: translateY(6px); }

@media (max-width: 700px) {
  .mascot { right: 12px; bottom: 10px; gap: 8px; }
  .mc-body { width: 78px; }
  .mc-bubble { max-width: 170px; font-size: 12px; padding: 8px 11px; }
  .mc-card { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .mc-body { animation: none !important; }
}
</style>
