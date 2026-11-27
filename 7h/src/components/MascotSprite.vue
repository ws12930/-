<template>
  <!--
    桌宠的「身体」渲染器：把 sprite sheet 逐帧画到 canvas 上。
    为什么用 canvas 而不是 <img> + CSS 动画：
      * 状态机需要「第几帧 / 播完没有 / 单次播放后停下」这些信息，CSS 动画给不了；
      * 同一套代码还要能吃 GIF（网上开源的 Shimeji 素材大多是 GIF / PNG 序列），
        GIF 必须解码成帧才能受状态机控制。
    两个 canvas 叠在一起：主层画当前剪辑，淡出层保存切换前的最后一帧。
    状态切换时旧帧在上层渐隐、新帧在下层出现，等价于一次交叉淡化（参考 Shimeji
    「动作首尾相接」与 dsh-pet「动画无缝衔接」的做法）。
  -->
  <span class="mc-stage">
    <canvas ref="canvasEl" class="mc-canvas" aria-hidden="true"></canvas>
    <canvas ref="fadeEl" class="mc-canvas mc-canvas-fade" aria-hidden="true"></canvas>
  </span>
</template>

<script setup>
/**
 * 帧播放器。两种素材格式共用一套播放逻辑：
 *   1. sprite sheet（默认）：横向条带，第 i 帧 = 裁 [i*cellW, 0, (i+1)*cellW, cellH]
 *   2. GIF：按需动态 import gifuct-js 解码（MIT，https://github.com/matt-way/gifuct-js），
 *      只有真的用到 .gif 素材时才会加载这个依赖，默认路径零开销。
 *
 * 本轮改造：
 * - 素材统一走 mascot/assets.js 的缓存（ImageBitmap 优先，预载与播放器共用一次解码）；
 * - 切换 src 时不再硬切：旧帧先复制到淡出层，新帧解码完成后再淡出旧帧；
 * - 保持「按目标帧延迟自调度」，唤醒次数 == 素材帧率，与屏幕刷新率解耦；
 * - 掉队超过 3 帧直接拉回基准、不补帧；标签页隐藏时彻底停表。
 */
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { CELL } from '../mascot/animations'
import { loadSheet } from '../mascot/assets'

const props = defineProps({
  src: { type: String, default: '' },
  frames: { type: Number, default: 1 },
  fps: { type: Number, default: 8 },
  loop: { type: Boolean, default: true },
  paused: { type: Boolean, default: false },
  playbackRate: { type: Number, default: 1 },
  fadeMs: { type: Number, default: 0 },
  cellW: { type: Number, default: CELL.w },
  cellH: { type: Number, default: CELL.h }
})
const emit = defineEmits(['ended'])

const canvasEl = ref(null)
const fadeEl = ref(null)
let ctx = null
let fadeCtx = null
let timerId = null // 「下一帧到期」的一次性定时器
let fadeTimer = null
let token = 0
let sheetImg = null
let gif = null // { frames: [...], canvas, ctx, w, h }
let frame = 0
let nextAt = 0
let ended = false
let lastDrawn = -1
let hidden = false

/**
 * 开发期统计：wakes = 调度唤醒次数，draws = 实际重绘次数。
 * 生产构建里 import.meta.env.DEV 为 false，整段会被 Vite 摇掉。
 */
const stats = { wakes: 0, draws: 0 }
if (import.meta.env.DEV && typeof window !== 'undefined') window.__mascotSpriteStats = stats

function syncCanvas() {
  const c = canvasEl.value
  if (!c) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const w = Math.round(props.cellW * dpr)
  const h = Math.round(props.cellH * dpr)
  if (c.width !== w || c.height !== h) {
    c.width = w
    c.height = h
  }
  ctx = c.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.imageSmoothingEnabled = true
  ctx.imageSmoothingQuality = 'high'

  const f = fadeEl.value
  if (f) {
    if (f.width !== w || f.height !== h) {
      f.width = w
      f.height = h
    }
    fadeCtx = f.getContext('2d')
    fadeCtx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
}

/** 把当前画面 1:1 复制到淡出层（像素尺寸相同，不经过 dpr 变换） */
function captureFade() {
  const c = canvasEl.value
  const f = fadeEl.value
  if (!c || !f || !fadeCtx || lastDrawn < 0) return false
  fadeCtx.setTransform(1, 0, 0, 1, 0, 0)
  fadeCtx.clearRect(0, 0, f.width, f.height)
  fadeCtx.drawImage(c, 0, 0)
  return true
}

/** 旧帧淡出：先把淡出层点亮，下一帧再挂 transition 收到 0 */
function revealFade() {
  const f = fadeEl.value
  if (!f || !(props.fadeMs > 0)) return
  f.style.transition = 'none'
  f.style.opacity = '1'
  // 强制 reflow，保证下一行挂上的 transition 能生效
  void f.offsetWidth
  f.style.transition = `opacity ${props.fadeMs}ms ease-out`
  f.style.opacity = '0'
  window.clearTimeout(fadeTimer)
  fadeTimer = window.setTimeout(() => {
    f.style.opacity = '0'
  }, props.fadeMs + 40)
}

/** 把任意尺寸的图按「等比 + 底边对齐」画进单元格 */
function drawFit(source, sw, sh) {
  if (!ctx) return
  const { cellW: cw, cellH: ch } = props
  const s = Math.min(cw / sw, ch / sh, 1.5)
  const dw = sw * s
  const dh = sh * s
  ctx.clearRect(0, 0, cw, ch)
  ctx.drawImage(source, (cw - dw) / 2, ch - dh, dw, dh)
}

function drawSheet() {
  if (!ctx || !sheetImg) return
  const { cellW, cellH } = props
  const f = Math.min(frame, Math.max(0, props.frames - 1))
  ctx.clearRect(0, 0, cellW, cellH)
  ctx.drawImage(sheetImg, f * cellW, 0, cellW, cellH, 0, 0, cellW, cellH)
}

function drawGif() {
  if (!ctx || !gif) return
  const g = gif
  const f = g.frames[Math.min(frame, g.frames.length - 1)]
  // 上一帧的 disposal 要在绘制下一帧之前处理（GIF 规范：disposal 作用于当前帧显示完之后）
  if (g.pendingDisposal === 2) {
    g.gctx.clearRect(f.dims.left, f.dims.top, f.dims.width, f.dims.height)
  } else if (g.pendingDisposal === 3 && g.prevSnapshot) {
    g.gctx.putImageData(g.prevSnapshot, 0, 0)
  }
  g.pendingDisposal = f.disposal
  g.prevSnapshot = g.pendingDisposal === 3 ? g.gctx.getImageData(0, 0, g.w, g.h) : null
  g.gctx.putImageData(f.patch, f.dims.left, f.dims.top)
  drawFit(g.canvas, g.w, g.h)
}

/** 解码 GIF：gifuct-js 只负责「拆帧」，合成 / 定时由这里接管 */
async function decodeGif(url) {
  const [mod, buf] = await Promise.all([
    import('gifuct-js'),
    fetch(url).then((r) => {
      if (!r.ok) throw new Error('GIF 加载失败：' + url)
      return r.arrayBuffer()
    })
  ])
  // v2 是函数式 API（parseGIF + decompressFrames），旧版才是 new GIF()；
  // 这里两种都兼容，避免以后升级 / 降级依赖时静默失效
  const api = mod.default && mod.default.parseGIF ? mod.default : mod
  const parsed = api.parseGIF(buf)
  const raw = api.decompressFrames(parsed, true)
  const w = parsed.lsd.width
  const h = parsed.lsd.height
  const off = document.createElement('canvas')
  off.width = w
  off.height = h
  return {
    w,
    h,
    canvas: off,
    gctx: off.getContext('2d'),
    pendingDisposal: 0,
    prevSnapshot: null,
    frames: raw.map((f) => ({
      patch: new ImageData(new Uint8ClampedArray(f.patch), f.dims.width, f.dims.height),
      dims: f.dims,
      delay: Math.max(20, f.delay || 100),
      disposal: f.disposalType || 0
    }))
  }
}

function totalFrames() {
  return gif ? gif.frames.length : Math.max(1, props.frames)
}

function isLooping() {
  // GIF 自带循环信息，交给素材自身决定；sprite sheet 用组件的 loop
  return gif ? true : props.loop
}

/** 当前帧应当停留多久（ms）。倍速直接除进去，不额外叠一层定时器 */
function frameDelay() {
  const base = gif
    ? gif.frames[Math.min(frame, gif.frames.length - 1)].delay
    : 1000 / Math.max(1, props.fps)
  return base / Math.max(0.1, props.playbackRate)
}

function draw() {
  if (gif) drawGif()
  else drawSheet()
  lastDrawn = frame
  stats.draws += 1
}

function stop() {
  if (timerId != null) {
    window.clearTimeout(timerId)
    timerId = null
  }
}

/** 只按「距离下一帧还有多久」排一次定时器，不再每帧唤醒 */
function arm() {
  const wait = Math.max(0, nextAt - performance.now())
  timerId = window.setTimeout(onTick, wait)
}

function onTick() {
  timerId = null
  stats.wakes += 1
  const now = performance.now()

  // 掉队太多就放弃追赶：补帧只会让画面「瞬移」，
  // 而动作时长由状态机独立计时，不补帧不会让动作卡住。
  if (now - nextAt > frameDelay() * 3) nextAt = now

  const total = totalFrames()
  if (frame + 1 >= total) {
    if (isLooping()) {
      frame = 0
    } else {
      frame = total - 1
      if (lastDrawn !== frame) draw()
      if (!ended) {
        ended = true
        emit('ended')
      }
      return
    }
  } else {
    frame += 1
  }

  if (lastDrawn !== frame) draw()
  nextAt += frameDelay()
  arm()
}

function play() {
  stop()
  if (hidden || props.paused) return
  if (totalFrames() <= 1) return
  if (frame >= totalFrames() - 1 && !isLooping()) return
  ended = false
  nextAt = performance.now() + frameDelay()
  arm()
}

async function load() {
  const my = ++token
  stop()
  const url = props.src
  if (!url) return

  // 先留一张旧帧快照：新图解码期间主层继续显示旧画面，解码完成后再淡出
  const canFade = props.fadeMs > 0 && props.frames > 1 && !props.paused
  const hasSnap = canFade && captureFade()

  try {
    if (/\.gif($|\?)/i.test(url)) {
      const decoded = await decodeGif(url)
      if (my !== token) return
      gif = decoded
      sheetImg = null
    } else {
      const im = await loadSheet(url)
      if (my !== token) return
      if (!im) {
        // 素材坏了不能把桌宠整个拖垮：保持上一帧画面，控制台留一条线索
        console.warn('[mascot] 帧素材加载失败', url)
        return
      }
      sheetImg = im
      gif = null
    }
  } catch (e) {
    console.warn('[mascot] 帧素材加载失败', e)
    return
  }
  frame = 0
  ended = false
  lastDrawn = -1
  draw()
  play()
  if (hasSnap) revealFade()
}

function restart() {
  frame = 0
  ended = false
  lastDrawn = -1
  draw()
  play()
}

function onVisibility() {
  hidden = document.hidden
  if (hidden) stop()
  else play()
}

onMounted(async () => {
  syncCanvas()
  hidden = document.hidden
  document.addEventListener('visibilitychange', onVisibility)
  await nextTick()
  load()
})

onBeforeUnmount(() => {
  token += 1
  stop()
  window.clearTimeout(fadeTimer)
  document.removeEventListener('visibilitychange', onVisibility)
})

watch(() => props.src, load)

watch(
  () => [props.frames, props.fps, props.loop],
  () => {
    if (gif) return
    if (frame > props.frames - 1) frame = Math.max(0, props.frames - 1)
    lastDrawn = -1
    draw()
    play() // fps 变了要按新帧延迟重新起表
  }
)

watch(
  () => props.paused,
  (v) => {
    if (v) stop()
    else play()
  }
)

// 倍速变化即时生效：重排一次基准即可，不叠加调度链
watch(() => props.playbackRate, play)

defineExpose({ restart, stats })
</script>
