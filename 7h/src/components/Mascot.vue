<template>
  <!-- 关掉后只留一个极小的恢复入口，避免「隐藏了就再也找不回来」 -->
  <button v-if="!settings.enabled" class="mc-restore" type="button" @click="restore">显示青禾</button>

  <div
    v-else
    ref="rootEl"
    class="mascot"
    :class="[
      `side-${position.side.value}`,
      { dragging: position.dragging.value, mini, muted, sleeping, hovering, 'is-eco': settings.eco }
    ]"
    :style="rootStyle"
  >
    <!-- 浮层始终渲染：收起时右键 / 长按小圆点也能真正打开菜单（原来被 v-else 挡住了） -->
    <MascotOverlays
      :line="speech.line.value"
      :show-bubble="speech.showBubble.value"
      :show-card="showCard"
      :me-name="meName"
      :menu-open="menuOpen"
      :side="position.side.value"
      :below="overlayBelow"
      :dragging="position.dragging.value"
      :muted="muted"
      :mini="mini"
      :settings="settings"
      @dismiss-bubble="speech.dismissBubble()"
      @close-menu="closeMenu"
      @action="onMenuAction"
      @setting-change="changeSetting"
    />

    <p v-if="warn" class="mc-warn" role="status">{{ warn }}</p>

    <!-- 最小化：缩成一颗小圆点 -->
    <button
      v-if="mini"
      class="mc-dot"
      type="button"
      title="青禾在这里（点一下展开 · 长按或点 ⋯ 打开菜单）"
      aria-label="展开青禾"
      :aria-expanded="!mini"
      @click="onDotClick"
      @pointerdown="onDotDown"
      @pointermove="onDotMove"
      @contextmenu.prevent="openMenu($event.currentTarget)"
    >
      <img :src="face.img" alt="青禾" decoding="async" />
      <span v-if="muted" class="mc-dot-flag"></span>
    </button>

    <button
      v-else
      ref="petEl"
      class="mc-body"
      :class="idleClass"
      type="button"
      :title="`看板娘 青禾 · ${face.label}（点我说话 · 长按或点 ⋯ 打开菜单 · 双击收起）`"
      :aria-label="`看板娘青禾 · ${face.label}`"
      aria-haspopup="menu"
      :aria-expanded="menuOpen"
      aria-controls="mc-menu"
      @pointerdown="onDown"
      @click="onTap"
      @dblclick="toggleMini(true)"
      @contextmenu.prevent="openMenu($event.currentTarget)"
      @mouseenter="onEnter"
      @mouseleave="onLeave"
    >
      <img :key="face.label" :src="face.img" :alt="face.label" draggable="false" decoding="async" />
      <span v-if="sleeping" class="mc-zzz">Zzz</span>
      <span v-if="muted" class="mc-mute">静音</span>
    </button>

    <!-- 右键之外的显式入口：键盘 / 触屏都能到，菜单不再是「只有鼠标右键」才能开 -->
    <button
      class="mc-more"
      type="button"
      aria-label="青禾菜单"
      aria-haspopup="menu"
      :aria-expanded="menuOpen"
      aria-controls="mc-menu"
      @click.stop="openMenu($event.currentTarget)"
    >⋯</button>

    <!-- 游戏窗口自己 teleport 到 body，放在这里只是借宿，不影响桌宠布局。
         三个游戏各自独立开合，关掉即销毁 iframe，BGM 不会在后台接着响。 -->
    <GameStage :open="gameOpen" @close="closeGame" />
    <GameStage
      :open="riftOpen"
      title="裂锋对决"
      en="RIFT CLASH"
      :src="riftUrl"
      auto-focus
      foot-left="2D 格斗 · 单人打电脑 / 同键盘双人对战"
      foot-right="关掉窗口即重新开局，战绩不存档"
      @close="closeRift"
    />
    <GameStage
      :open="guanglunOpen"
      title="光轮"
      en="GUANGLUN"
      :src="guanglunUrl"
      auto-focus
      foot-left="双人轨迹对战 · 同屏对战"
      foot-right="关掉窗口即重新开局，战绩不存档"
      @close="closeGuanglun"
    />
  </div>
</template>

<script setup>
/**
 * 桌宠版看板娘「青禾」——组装层。
 *
 * 数据配置在 mascot/config.js，定时器 / 台词 / 位置 / 数据联动各在对应的 composable，
 * 浮层 UI 在 MascotOverlays.vue，样式在 assets/mascot.css。这里只负责把手感串起来：
 * 拖拽手势、单击与双击、菜单开关与焦点、路由联动、生命周期。
 *
 * 相比旧版修掉的确定缺陷（细节见各 composable 的注释）：
 * - 拖放手后 click 会绕过 dragging 判断多说一句 → suppressTap 消费掉；
 * - 右键 pointerdown 误进拖拽 → 只认左键；
 * - 定时器漏登记、后台没停干净 → 统一 timer registry；
 * - 恢复存档坐标不校验视口 → load() 立刻 clamp；
 * - 浮层在屏幕顶部被裁切 → 按可用空间向下翻转；
 * - 收起后菜单根本不渲染、触屏 / 键盘无入口 → 浮层常驻 + ⋯ 按钮 + 长按。
 *
 * 另外：本轮执行期间项目新增了第三个游戏「光轮」，当时 Mascot.vue 里只加了菜单按钮，
 * 函数 / GameStage / 样式都还没跟上；这里把它补全成和前两个游戏一致的完整链路。
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { readMe } from '../utils/author'
import { resolveRiftUrl, resolveGuanglunUrl } from '../utils/gameConfig'
import { EVT_SESSION_CHANGED, EVT_BLOG_SAVED } from '../utils/constants'
import GameStage from './GameStage.vue'
import MascotOverlays from './MascotOverlays.vue'
import {
  FACES, SCENE_LINES, GRABBED, DROPPED, daypartLine, facePool,
  DEFAULT_SETTINGS, SMALL_SCREEN_PX, LONG_PRESS_MS,
  IDLE_MS, TAP_DELAY_MS, MOVE_THRESHOLD, HOUR_TICK_MS,
  IDLE_CLEAR_MS, IDLE_MIN_MS, IDLE_JITTER_MS,
  RESIZE_DEBOUNCE_MS, MIN_GAP, TOP_LIMIT, OVERLAY_SAFE_GAP, OVERLAY_ESTIMATE_PX
} from '../mascot/config'
import { useMascotTimers } from '../mascot/useMascotTimers'
import { useMascotSpeech } from '../mascot/useMascotSpeech'
import { useMascotPosition } from '../mascot/useMascotPosition'
import { useMascotLive } from '../mascot/useMascotLive'
import '../assets/mascot.css'

const route = useRoute()
const rootEl = ref(null)
const petEl = ref(null)

const idx = ref(0)
const mini = ref(false)
const muted = ref(false)
const menuOpen = ref(false)
const gameOpen = ref(false)
const riftOpen = ref(false)
const guanglunOpen = ref(false)
const riftUrl = resolveRiftUrl()
const guanglunUrl = resolveGuanglunUrl()
const meName = ref('')
const hour = ref(new Date().getHours())
const hovering = ref(false)
const showCard = ref(false)
const idleClass = ref('')
const overlayBelow = ref(false)
const warn = ref('')
const settings = reactive({ ...DEFAULT_SETTINGS })

const pool = computed(() => facePool(settings.faceSet))
const face = computed(() => pool.value[idx.value % pool.value.length] || FACES[0])
const sleeping = computed(() => hour.value >= 23 || hour.value < 6)

const timers = useMascotTimers()
const speech = useMascotSpeech({ timers, getFace: () => face.value, isMuted: muted, isMini: mini, route })
const position = useMascotPosition({
  rootEl,
  getSnapshot: () => ({ mini: mini.value, muted: muted.value, settings: { ...settings } }),
  applySnapshot: (s) => {
    mini.value = s.mini
    muted.value = s.muted
    Object.assign(settings, { ...DEFAULT_SETTINGS, ...s.settings })
  },
  onStorageError: () => {
    warn.value = '浏览器存储不可用，桌宠设置本次不会保存'
    timers.timeout(() => (warn.value = ''), 6000)
  }
})
const live = useMascotLive({ timers, speak: speech.speak, isMuted: muted, isMini: mini })

const rootStyle = computed(() => ({
  ...position.posStyle.value,
  '--mc-size': `${settings.size}px`,
  '--mc-opacity': String(settings.opacity)
}))

// ---- 定时器总排程：pause 一次全清，resume 只从这一处重新起一套，杜绝叠链 ----
let rotateId = null
let resizeId = null
let idleNextId = null
let idleRafId = null
let idleClearId = null
let tapId = null
let scrollRaf = null
let readFired = false
let userExpanded = false

const IDLE_ANIMS = ['is-peek', 'is-hop', 'is-sway']

function restartRotate() {
  timers.clear(rotateId)
  rotateId = timers.interval(() => {
    if (!mini.value && !document.hidden && !hovering.value) {
      idx.value = (idx.value + 1) % pool.value.length
    }
  }, IDLE_MS)
}

function stopIdle() {
  timers.clear(idleRafId)
  timers.clear(idleClearId)
  timers.clear(idleNextId)
  idleClass.value = ''
}

function playIdle() {
  if (mini.value || position.dragging.value || hovering.value || settings.eco) return
  stopIdle()
  idleRafId = timers.raf(() => {
    idleClass.value = IDLE_ANIMS[Math.floor(Math.random() * IDLE_ANIMS.length)]
    idleClearId = timers.timeout(() => (idleClass.value = ''), IDLE_CLEAR_MS)
  })
  idleNextId = timers.timeout(playIdle, IDLE_MIN_MS + Math.random() * IDLE_JITTER_MS)
}

function scheduleAll() {
  hour.value = new Date().getHours()
  restartRotate()
  playIdle()
  speech.scheduleChatter()
  speech.scheduleSaveNudge()
  timers.interval(() => (hour.value = new Date().getHours()), HOUR_TICK_MS)
  live.start()
  syncScrollWatch()
}

function pauseAll() { timers.pause() }
function resumeAll() { timers.resume(); scheduleAll() }

function onVisibility() {
  if (document.hidden) pauseAll()
  // 隐藏桌宠时连定时器一起停，切回前台也别把轮询 / 待机动作偷偷启起来
  else if (!mini.value && settings.enabled) resumeAll()
  syncScrollWatch()
}

// ---- 悬停：定住小动作与漂浮，方便点选 / 拖拽 ----
function onEnter() {
  hovering.value = true
  showCard.value = true
  stopIdle()
}
function onLeave() {
  hovering.value = false
  showCard.value = false
  stopIdle()
  idleNextId = timers.timeout(playIdle, 1500)
}

// ---- 表情 / 场景 ----
function next() {
  idx.value = (idx.value + 1) % pool.value.length
  speech.speak()
  restartRotate()
}
function applySceneFace() {
  idx.value = speech.faceForScene(route.name) % pool.value.length
}

/** 阅读进度只在文章页挂 scroll 监听；文章读到 85% 夸一句 */
function onScroll() {
  if (route.name !== 'post' || readFired || scrollRaf) return
  scrollRaf = timers.raf(() => {
    scrollRaf = null
    const total = document.documentElement.scrollHeight - window.innerHeight
    if (total > 0 && window.scrollY / total > 0.85) {
      readFired = true
      speech.speak('快读完啦，还剩一点点～')
    }
  })
}
function syncScrollWatch() {
  window.removeEventListener('scroll', onScroll)
  if (route.name === 'post' && !document.hidden) {
    window.addEventListener('scroll', onScroll, { passive: true })
  }
}

// ---- 拖拽 / 长按 / 单击 / 双击 ----
let downInfo = null
let pending = null
let dragRaf = null
let longPressId = null
let longPressFired = false
let suppressTap = false
let dotStart = null

function onDown(e) {
  // 右键 pointerdown 会和 contextmenu 同时发生，原来会把菜单弹出后的移动也当成拖拽
  if (e.button !== 0) return
  if (mini.value) return
  suppressTap = false // 每次按下重置，避免上次未消费的标记吞掉这次单击
  longPressFired = false
  longPressId = timers.timeout(() => {
    longPressFired = true
    openMenu(petEl.value)
  }, LONG_PRESS_MS)
  const base = position.pos.value || position.defaultPos()
  downInfo = { px: e.clientX, py: e.clientY, x0: base.x, y0: base.y, moved: false }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onUp)
}

/** 小圆点也支持长按开菜单；圆点本身不参与拖拽（保持旧手感） */
function onDotDown(e) {
  if (e.button !== 0) return
  suppressTap = false
  longPressFired = false
  const trigger = e.currentTarget // 异步回调里 currentTarget 已失效，必须先存下来
  dotStart = { x: e.clientX, y: e.clientY }
  longPressId = timers.timeout(() => {
    longPressFired = true
    openMenu(trigger)
  }, LONG_PRESS_MS)
  window.addEventListener('pointermove', onDotMove)
  window.addEventListener('pointerup', onDotUp, { once: true })
}
function onDotMove(e) {
  if (!dotStart || longPressFired) return
  if (Math.hypot(e.clientX - dotStart.x, e.clientY - dotStart.y) > MOVE_THRESHOLD) {
    timers.clear(longPressId)
  }
}
function onDotUp() {
  timers.clear(longPressId)
  window.removeEventListener('pointermove', onDotMove)
  if (longPressFired) suppressTap = true
  dotStart = null
}
function onDotClick() {
  // 长按是想开菜单，抬手时浏览器补的那个 click 不该顺手把桌宠展开
  if (suppressTap) { suppressTap = false; return }
  toggleMini(false)
}

function onMove(e) {
  if (!downInfo || longPressFired) return
  const dx = e.clientX - downInfo.px
  const dy = e.clientY - downInfo.py
  if (!downInfo.moved && Math.hypot(dx, dy) > MOVE_THRESHOLD) {
    timers.clear(longPressId)
    downInfo.moved = true
    position.dragging.value = true
    showCard.value = false
    speech.speak(speech.pickFresh(GRABBED))
  }
  if (downInfo.moved) {
    const { w, h } = position.dims()
    pending = {
      x: Math.min(Math.max(downInfo.x0 + dx, MIN_GAP), Math.max(MIN_GAP, window.innerWidth - w - MIN_GAP)),
      y: Math.min(Math.max(downInfo.y0 + dy, TOP_LIMIT), Math.max(TOP_LIMIT, window.innerHeight - h - MIN_GAP))
    }
    if (!dragRaf) {
      dragRaf = timers.raf(() => {
        dragRaf = null
        if (pending) position.pos.value = position.clampPos(pending)
      })
    }
  }
}

function onUp() {
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', onUp)
  timers.clear(longPressId)
  if (dragRaf) {
    timers.clear(dragRaf)
    dragRaf = null
    if (pending) position.pos.value = position.clampPos(pending)
  }
  pending = null
  const moved = !!downInfo?.moved
  if (moved) {
    position.snap()
    position.save()
    speech.speak(speech.pickFresh(DROPPED))
  }
  // 拖拽 / 长按结束后浏览器仍会派发 click：这里立标记，让 onTap 消费掉这一次，
  // 否则放下后 260ms 又会冒出一句场景话，把刚说的「放手吧」顶掉。
  suppressTap = moved || longPressFired
  downInfo = null
  position.dragging.value = false
  updateOverlayPlacement()
}

function onTap() {
  if (position.dragging.value) return
  if (suppressTap) { suppressTap = false; return }
  timers.clear(tapId)
  tapId = timers.timeout(() => {
    if (mini.value) return
    applySceneFace()
    speech.speakScene()
  }, TAP_DELAY_MS)
}

// ---- 菜单：右键 / 长按 / ⋯ 按钮 / 键盘共用 ----
let menuTrigger = null
function openMenu(trigger) {
  menuOpen.value = !menuOpen.value
  showCard.value = false
  if (menuOpen.value) {
    menuTrigger = trigger && typeof trigger.focus === 'function' ? trigger : petEl.value
    updateOverlayPlacement()
  }
}
function closeMenu() {
  if (!menuOpen.value) return
  menuOpen.value = false
  menuTrigger?.focus?.()
}
function onMenuAction(key) {
  closeMenu()
  const actions = {
    playGame: () => (gameOpen.value = true),
    playRift: () => (riftOpen.value = true),
    playGuanglun: () => (guanglunOpen.value = true),
    next,
    toggleMute,
    resetPos,
    toggleMini: () => toggleMini(!mini.value),
    hide
  }
  actions[key]?.()
}
function onDocPointerDown(e) {
  if (menuOpen.value && !rootEl.value?.contains(e.target)) closeMenu()
}
function onGlobalKey(e) {
  if (e.key === 'Escape' && menuOpen.value) closeMenu()
}

function toggleMute() {
  muted.value = !muted.value
  position.save()
  if (!muted.value) speech.speak('我又能说话啦')
}
function resetPos() {
  position.reset()
  speech.speak('回到原位啦')
  updateOverlayPlacement()
}
function toggleMini(v) {
  timers.clear(tapId)
  mini.value = v
  position.save()
  if (v) pauseAll()
  else {
    userExpanded = true
    resumeAll()
    speech.speak('我回来啦～')
  }
}
function hide() {
  settings.enabled = false
  position.save()
  pauseAll()
  document.documentElement.classList.remove('eco-mode')
}
function restore() {
  settings.enabled = true
  position.save()
  resumeAll()
  speech.speak('我回来啦～')
}
function changeSetting({ key, value }) {
  if (!(key in settings)) return
  settings[key] = value
  if (key === 'faceSet') idx.value = 0
  if (key === 'eco') document.documentElement.classList.toggle('eco-mode', !!value)
  position.save()
  updateOverlayPlacement()
}
function closeGame() {
  gameOpen.value = false
  speech.speak('回来啦，玩得开心吗？')
}
function closeRift() {
  riftOpen.value = false
  speech.speak('拳头别挥到我这来哦')
}
function closeGuanglun() {
  guanglunOpen.value = false
  speech.speak('光轮停下来了，歇会儿吧')
}

/** 浮层朝上放不下时翻到身体下方：拖到屏幕顶部菜单也能完整可点 */
function updateOverlayPlacement() {
  nextTick(() => {
    const overlays = rootEl.value?.querySelector('.mc-overlays')
    const h = overlays?.offsetHeight || OVERLAY_ESTIMATE_PX
    const y = position.pos.value?.y ?? window.innerHeight - position.dims().h - 14
    overlayBelow.value = y < h + OVERLAY_SAFE_GAP
  })
}
watch([menuOpen, () => speech.showBubble.value, showCard], updateOverlayPlacement)

// ---- 用户昵称：storage 事件不覆盖本标签页，补一条自定义事件 ----
function refreshMe() {
  try {
    meName.value = readMe()?.nickname || ''
  } catch {
    meName.value = ''
  }
  live.resetSession()
}

// ---- 视口变化 ----
function onResize() {
  timers.clear(resizeId)
  resizeId = timers.timeout(() => {
    if (!userExpanded && window.innerWidth < SMALL_SCREEN_PX) {
      if (!mini.value) {
        mini.value = true
        pauseAll()
        position.save()
      }
      return
    }
    if (!position.pos.value) return
    position.pos.value = position.clampPos(position.pos.value)
    updateOverlayPlacement()
  }, RESIZE_DEBOUNCE_MS)
}

function onBlogSaved(e) {
  speech.speakSaved(e?.detail?.status)
}

// ---- 路由联动 ----
watch(
  () => route.path,
  () => {
    refreshMe()
    readFired = false
    live.resetRouteCounter()
    syncScrollWatch()
    if (mini.value) return
    applySceneFace()
    const lines = SCENE_LINES[route.name] || []
    if (lines.length) {
      const t = speech.pickFresh(lines)
      speech.speak(meName.value && Math.random() < 0.4 ? `${meName.value}，${t}` : t)
    }
    speech.scheduleSaveNudge()
  }
)

onMounted(() => {
  position.load() // 内部会 clamp 一次，并回填 mini / muted / settings
  refreshMe()
  if (window.innerWidth < SMALL_SCREEN_PX) mini.value = true
  if (!position.pos.value) position.side.value = 'right'
  document.documentElement.classList.toggle('eco-mode', settings.eco)

  // 只解码当前这一张：三张全量预解码会让首屏白等 872K（现已换 WebP，按需即可）
  const im = new Image()
  im.decoding = 'async'
  im.src = face.value.img

  window.addEventListener('resize', onResize)
  window.addEventListener('storage', refreshMe)
  window.addEventListener(EVT_SESSION_CHANGED, refreshMe)
  window.addEventListener(EVT_BLOG_SAVED, onBlogSaved)
  document.addEventListener('visibilitychange', onVisibility)
  document.addEventListener('keydown', onGlobalKey)
  document.addEventListener('pointerdown', onDocPointerDown, true)

  syncScrollWatch()
  updateOverlayPlacement()
  if (!mini.value && settings.enabled) {
    applySceneFace()
    speech.speak(meName.value ? `${meName.value}，${daypartLine()}` : daypartLine())
    resumeAll()
  }
})

onBeforeUnmount(() => {
  pauseAll()
  timers.clearAll()
  document.documentElement.classList.remove('eco-mode')
  window.removeEventListener('resize', onResize)
  window.removeEventListener('storage', refreshMe)
  window.removeEventListener(EVT_SESSION_CHANGED, refreshMe)
  window.removeEventListener(EVT_BLOG_SAVED, onBlogSaved)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', onUp)
  window.removeEventListener('pointermove', onDotMove)
  window.removeEventListener('pointerup', onDotUp)
  document.removeEventListener('visibilitychange', onVisibility)
  document.removeEventListener('keydown', onGlobalKey)
  document.removeEventListener('pointerdown', onDocPointerDown, true)
})
</script>
