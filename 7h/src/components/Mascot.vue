<template>
  <div
    ref="rootEl"
    class="mascot"
    :class="[`side-${side}`, { dragging, mini, muted, sleeping, hovering }]"
    :style="rootStyle"
  >
    <!-- 最小化：缩成一颗小圆点 -->
    <button
      v-if="mini"
      class="mc-dot"
      type="button"
      title="青禾在这里（点击展开）"
      @click="toggleMini(false)"
      @contextmenu.prevent="openMenu"
    >
      <img :src="FACES[0].img" alt="青禾" />
      <span v-if="muted" class="mc-dot-flag"></span>
    </button>

    <template v-else>
      <!-- 浮层：绝对定位，弹出/消失都不占布局，避免身体被顶得上下跳 -->
      <div class="mc-overlays">
        <!-- 说话气泡：点一下收起 -->
        <Transition name="bub">
          <p
            v-if="showBubble"
            class="mc-bubble"
            title="点一下收起"
            @click="dismissBubble"
          >{{ line }}</p>
        </Transition>

        <!-- 悬停名片 -->
        <Transition name="card">
          <div v-if="showCard && !dragging" class="mc-card">
            <p class="mc-card-name">青禾</p>
            <p class="mc-card-role">博客看板娘</p>
            <p class="mc-card-quote">「慢慢写，好好说」</p>
            <p v-if="meName" class="mc-card-me">hi，{{ meName }}</p>
          </div>
        </Transition>

        <!-- 右键菜单 -->
        <Transition name="menu">
          <div v-if="menuOpen" class="mc-menu" @mouseleave="menuOpen = false">
            <button type="button" @click="menuAction(next)">换个表情</button>
            <button type="button" @click="menuAction(toggleMute)">
              {{ muted ? '取消静音' : '暂时静音' }}
            </button>
            <button type="button" @click="menuAction(resetPos)">回到右下角</button>
            <button type="button" @click="menuAction(() => toggleMini(true))">收起来</button>
          </div>
        </Transition>
      </div>

      <button
        ref="petEl"
        class="mc-body"
        :class="idleClass"
        type="button"
        :title="`看板娘 青禾 · ${face.label}（点我说话 · 双击收起 · 右键菜单）`"
        :aria-label="`看板娘青禾 · ${face.label}`"
        @pointerdown="onDown"
        @click="onTap"
        @dblclick="toggleMini(true)"
        @contextmenu.prevent="openMenu"
        @mouseenter="onEnter"
        @mouseleave="onLeave"
      >
        <img :key="face.label" :src="face.img" :alt="face.label" draggable="false" />
        <span v-if="sleeping" class="mc-zzz">Zzz</span>
        <span v-if="muted" class="mc-mute">静音</span>
      </button>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { readMe } from '../utils/author'

import mWelcome from '../assets/mascot/mascot-welcome.png'
import mNofight from '../assets/mascot/mascot-nofight.png'
import mWriting from '../assets/mascot/mascot-writing.png'

/**
 * 桌宠版看板娘「青禾」
 * - 可拖拽（位置记忆，靠边自动吸附，rAF 节流）
 * - 待机小动作（张望 / 蹦跳 / 摇摆）
 * - 按页面场景说话：登录、写文章、读文章、社区……
 * - 分时段问候、深夜打瞌睡
 * - 悬停名片（带当前用户名）、右键菜单、双击收成小圆点
 * - 页面切到后台自动停掉所有定时器
 */
const NAME = '青禾'

const FACES = [
  {
    img: mWelcome,
    label: '欢迎呀',
    lines: [
      `欢迎来到 7h 呀～我是${NAME}`,
      `新朋友你好，我是${NAME}！`,
      '逛累了就歇一会儿吧',
      '今天也来逛逛啦',
      '我在这儿陪你，慢慢看',
      '有想找的就用上面的搜索框'
    ]
  },
  {
    img: mNofight,
    label: '不许吵架',
    lines: [
      `评论区要文明，不许吵架哦！——${NAME}`,
      '有话好好说嘛～',
      '理性发言最帅啦',
      '意见不同很正常，别上头',
      '看到不友善的可以举报给我'
    ]
  },
  {
    img: mWriting,
    label: '加油写文',
    lines: [
      `今天也要好好写文，${NAME}给你加油～`,
      '想到什么就先记下来吧',
      '你的文章值得被看到',
      '写不出来就先写个大概，之后再改',
      '标题想好了吗？可以先占个位',
      '一段一段来，别急着追求完美'
    ]
  }
]

/** 各页面的场景台词 */
const SCENE_LINES = {
  login: [
    '登录以后就能写文章啦',
    '账号密码填一下，我在这儿等你',
    '忘记密码的话点下面那个链接'
  ],
  register: [
    '注册一下就有自己的主页啦',
    '昵称可以之后再改，别纠结太久',
    '密码记得设复杂一点哦'
  ],
  forgot: ['别急，按步骤来就能找回', '验证完就能重设密码啦'],
  home: ['今天想写点什么？', '首页有新的动态，往下翻翻看', '累了就先歇会儿，我帮你看着'],
  blog: ['慢慢挑，好文章值得细读', '看到喜欢的就点个赞吧', '用标签筛选会快一点'],
  mine: ['这里都是你写的文章', '草稿也可以随时改', '要不要再写一篇？'],
  post: ['这篇读完了吗？', '有想法就在评论区聊聊', '看到这里了，进度不错呀'],
  editor: ['写吧写吧，我在这儿陪你', '记得时不时保存一下', '写不出来就先写个大概，之后再改'],
  profile: ['换个新头像试试？', '写一句签名吧，别人能看到', '资料填全一点，更容易被关注'],
  messages: ['有新消息吗？', '回消息的时候别急', '想找人聊聊就搜一下昵称'],
  notifications: ['看看谁来找过你', '有未读的话顺手处理一下'],
  community: ['遇到合拍的人就关注吧', '这里可以认识新朋友', '互动多了，社区就热闹了'],
  user: ['这是别人的主页哦', '喜欢的话就关注一下']
}

/**
 * 场景 -> 表情：0 欢迎 / 1 不许吵架 / 2 加油写文
 * 让表情跟着页面语义走，而不是随机乱切，表达才立得住。
 */
const SCENE_FACE = {
  login: 0,
  register: 0,
  forgot: 0,
  home: 0,
  blog: 0,
  profile: 0,
  user: 0,
  post: 1,
  community: 1,
  messages: 1,
  notifications: 1,
  editor: 2,
  mine: 2
}

const POSTED = ['发布成功！这下大家都能看到了', '写完了，辛苦啦～', '恭喜，又一篇完成']
const DRAFTED = ['草稿存好啦，随时回来改', '先存着，慢慢改']

const GRABBED = ['哎呀，轻点拎～', '把我放哪儿呀？', '诶——我还没站稳呢']

/** 分时段问候 */
function daypartLine() {
  const h = new Date().getHours()
  if (h >= 5 && h < 8) return '早，起得真早呀'
  if (h >= 8 && h < 11) return '上午好，今天状态怎么样？'
  if (h >= 11 && h < 14) return '中午啦，先吃饭再写吧'
  if (h >= 14 && h < 18) return '下午好，写累了记得看看窗外'
  if (h >= 18 && h < 22) return `晚上好呀，${NAME}陪你慢慢逛`
  if (h >= 22) return '有点晚了，别熬太狠'
  return '深夜了，写完这段就早点休息吧'
}

const STORE_KEY = 'qh-mascot-v2'
const IDLE_MS = 8000
const BUBBLE_MS = 6200
const SAVE_NUDGE_MS = 180000

const route = useRoute()

const idx = ref(0)
const line = ref('')
const lastLine = ref('')
const showBubble = ref(false)
const showCard = ref(false)
const mini = ref(false)
const muted = ref(false)
const menuOpen = ref(false)
const meName = ref('')
const hour = ref(new Date().getHours())

const sleeping = computed(() => hour.value >= 23 || hour.value < 6)
/** 鼠标停在身上：立刻定住，方便点选/拖拽 */
const hovering = ref(false)

// ---- 位置记忆与吸附 ----
const pos = ref(null) // { x, y } px（元素左上角）
const side = ref('right')
const dragging = ref(false)
let userExpanded = false

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
let hourTimer = null
let saveNudgeTimer = null
let rafId = null

const face = computed(() => FACES[idx.value])

function pick(pool) {
  return pool[Math.floor(Math.random() * pool.length)]
}

/** 避免连续两句重复：同一个池子里优先换一句 */
function pickFresh(pool) {
  if (!pool || !pool.length) return ''
  if (pool.length === 1) return pool[0]
  let s = pick(pool)
  for (let i = 0; i < 6 && s === lastLine.value; i++) s = pick(pool)
  return s
}

/** 场景对应的表情 */
function faceForScene(name) {
  return SCENE_FACE[name] ?? 0
}

/** 说当前场景的话；没有场景台词就退回当前表情的台词 */
function speakScene() {
  const scene = SCENE_LINES[route.name] || []
  speak(scene.length ? pickFresh(scene) : pickFresh(face.value.lines))
}

// ---- 待机小动作 ----
const idleClass = ref('')
const IDLE_ANIMS = ['is-peek', 'is-hop', 'is-sway']

function playIdle() {
  // 悬停、拖拽、收起时一律不做小动作，免得目标乱跑点不中
  if (mini.value || dragging.value || hovering.value) return
  idleClass.value = ''
  requestAnimationFrame(() => {
    idleClass.value = IDLE_ANIMS[Math.floor(Math.random() * IDLE_ANIMS.length)]
    setTimeout(() => (idleClass.value = ''), 2600)
  })
  idleTimer = setTimeout(playIdle, 6000 + Math.random() * 9000)
}

/** 鼠标移入：停掉小动作与漂浮，让指针稳稳落在身上 */
function onEnter() {
  hovering.value = true
  showCard.value = true
  clearTimeout(idleTimer)
  idleClass.value = ''
}

/** 鼠标移出：恢复待机动作 */
function onLeave() {
  hovering.value = false
  showCard.value = false
  clearTimeout(idleTimer)
  idleTimer = setTimeout(playIdle, 1500)
}

// ---- 说话 ----
function speak(text) {
  if (muted.value) return
  const t = text || pickFresh(face.value.lines)
  if (!t) return
  lastLine.value = t
  line.value = t
  showBubble.value = true
  clearTimeout(bubbleTimer)
  bubbleTimer = setTimeout(() => (showBubble.value = false), BUBBLE_MS)
}

function dismissBubble() {
  clearTimeout(bubbleTimer)
  showBubble.value = false
}

/** 随机冒话：25-50s 一次，优先说当前场景的话 */
function scheduleChatter() {
  clearTimeout(speakTimer)
  speakTimer = setTimeout(() => {
    if (!mini.value && !document.hidden && !dragging.value) {
      const r = Math.random()
      if (r < 0.3) speak(daypartLine())
      else if (r < 0.62) {
        const scene = SCENE_LINES[route.name] || []
        speak(scene.length ? pickFresh(scene) : '')
      } else speak('')
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
    // 悬停时不自动换表情，避免点到一半脸变了
    if (!mini.value && !document.hidden && !hovering.value) {
      idx.value = (idx.value + 1) % FACES.length
    }
  }, IDLE_MS)
}

// ---- 后台暂停：切走标签页就停表，回来再起 ----
function pauseAll() {
  clearInterval(rotateTimer)
  clearTimeout(bubbleTimer)
  clearTimeout(speakTimer)
  clearTimeout(idleTimer)
  clearTimeout(saveNudgeTimer)
}

function resumeAll() {
  restartRotate()
  playIdle()
  scheduleChatter()
  scheduleSaveNudge()
}

function onVisibility() {
  if (document.hidden) pauseAll()
  else if (!mini.value) resumeAll()
}

// ---- 场景联动 ----
function refreshMe() {
  try {
    meName.value = readMe()?.nickname || ''
  } catch {
    meName.value = ''
  }
}

/** 阅读进度：文章页读到 85% 夸一句 */
let readFired = false
let scrollRaf = null
function onScroll() {
  if (route.name !== 'post' || readFired) return
  if (scrollRaf) return
  scrollRaf = requestAnimationFrame(() => {
    scrollRaf = null
    const total = document.documentElement.scrollHeight - window.innerHeight
    if (total > 0 && window.scrollY / total > 0.85) {
      readFired = true
      speak('快读完啦，还剩一点点～')
    }
  })
}

/** 编辑器页待久了提醒保存 */
function scheduleSaveNudge() {
  clearTimeout(saveNudgeTimer)
  if (route.name !== 'editor') return
  saveNudgeTimer = setTimeout(() => {
    if (route.name === 'editor' && !mini.value && !document.hidden) {
      speak('写这么多啦，记得点一下保存哦')
    }
  }, SAVE_NUDGE_MS)
}

function onBlogSaved(e) {
  // 编辑器保存后会派发 blog:saved，detail.status 是字符串状态，DRAFT 才算草稿
  const st = String(e?.detail?.status ?? '').toUpperCase()
  speak(pickFresh(st === 'DRAFT' ? DRAFTED : POSTED))
}

// ---- 拖拽 ----
const MOVE_THRESHOLD = 6
let downInfo = null
let pending = null

function onDown(e) {
  if (mini.value) return
  const base = pos.value || defaultPos()
  downInfo = { px: e.clientX, py: e.clientY, x0: base.x, y0: base.y, moved: false }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onUp)
}

function defaultPos() {
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
    // 刚被拎起来：给一句即时反馈，别等放手才说话
    speak(pickFresh(GRABBED))
  }
  if (downInfo.moved) {
    const el = rootEl.value
    const w = el ? el.offsetWidth : 130
    const h = el ? el.offsetHeight : 190
    pending = {
      x: Math.min(Math.max(downInfo.x0 + dx, 8), Math.max(8, window.innerWidth - w - 8)),
      y: Math.min(Math.max(downInfo.y0 + dy, 56), Math.max(56, window.innerHeight - h - 8))
    }
    if (!rafId) {
      rafId = requestAnimationFrame(() => {
        rafId = null
        if (pending) pos.value = pending
      })
    }
  }
}

function onUp() {
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', onUp)
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = null
    if (pending) pos.value = pending
  }
  pending = null
  if (downInfo && downInfo.moved) {
    snap()
    saveState()
    speak(pickFresh(['放手吧，我就在这儿待着～', '这里视野不错！', `被你拎来拎去的啦——${NAME}`]))
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

// ---- 点击 / 双击 / 菜单 ----
let tapTimer = null
function onTap() {
  if (dragging.value) return
  clearTimeout(tapTimer)
  tapTimer = setTimeout(() => {
    if (mini.value) return
    // 单击：优先说这个页面该说的话，表情也回到场景对应的那张
    idx.value = faceForScene(route.name)
    speakScene()
  }, 260)
}

function openMenu() {
  menuOpen.value = !menuOpen.value
  showCard.value = false
}

function menuAction(fn) {
  menuOpen.value = false
  fn()
}

function toggleMute() {
  muted.value = !muted.value
  saveState()
  if (!muted.value) speak('我又能说话啦')
}

function toggleMini(v) {
  clearTimeout(tapTimer)
  mini.value = v
  saveState()
  if (v) pauseAll()
  else {
    userExpanded = true
    resumeAll()
    speak('我回来啦～')
  }
}

function resetPos() {
  pos.value = null
  side.value = 'right'
  saveState()
  speak('回到原位啦')
}

// ---- 持久化 ----
function saveState() {
  try {
    localStorage.setItem(
      STORE_KEY,
      JSON.stringify({ pos: pos.value, side: side.value, mini: mini.value, muted: muted.value })
    )
  } catch (e) { /* 忽略 */ }
}

function loadState() {
  try {
    const s = JSON.parse(localStorage.getItem(STORE_KEY) || 'null')
    if (s) {
      if (s.pos && typeof s.pos.x === 'number') pos.value = s.pos
      side.value = s.side || 'right'
      mini.value = !!s.mini
      muted.value = !!s.muted
    }
  } catch (e) { /* 忽略 */ }
}

// 窗口尺寸变化时把看板娘拉回屏内；小屏自动收起（用户手动展开过就不再自动收）
let resizeTimer = null
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (!userExpanded && window.innerWidth < 700) {
      mini.value = true
      return
    }
    if (!pos.value) return
    const el = rootEl.value
    const w = el ? el.offsetWidth : 130
    const h = el ? el.offsetHeight : 190
    pos.value = {
      x: Math.min(Math.max(pos.value.x, 8), Math.max(8, window.innerWidth - w - 8)),
      y: Math.min(Math.max(pos.value.y, 56), Math.max(56, window.innerHeight - h - 8))
    }
  }, 200)
}

onMounted(() => {
  loadState()
  refreshMe()
  // 小屏首次进来默认收成圆点，别挡内容
  if (window.innerWidth < 700) mini.value = true
  if (!pos.value) side.value = 'right'

  // 图片预解码，避免首次切换表情时闪一下
  FACES.forEach((f) => {
    const im = new Image()
    im.src = f.img
  })

  window.addEventListener('resize', onResize)
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('storage', refreshMe)
  window.addEventListener('blog:saved', onBlogSaved)
  document.addEventListener('visibilitychange', onVisibility)

  hourTimer = setInterval(() => (hour.value = new Date().getHours()), 60000)

  if (!mini.value) {
    idx.value = faceForScene(route.name)
    speak(meName.value ? `${meName.value}，${daypartLine()}` : daypartLine())
    resumeAll()
  }
})

onBeforeUnmount(() => {
  pauseAll()
  clearTimeout(tapTimer)
  clearTimeout(resizeTimer)
  clearInterval(hourTimer)
  if (rafId) cancelAnimationFrame(rafId)
  if (scrollRaf) cancelAnimationFrame(scrollRaf)
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', onUp)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('storage', refreshMe)
  window.removeEventListener('blog:saved', onBlogSaved)
  document.removeEventListener('visibilitychange', onVisibility)
})

// 路由切换：换个表情 + 说一句这个场景的话
watch(
  () => route.path,
  () => {
    refreshMe()
    readFired = false
    if (mini.value) return
    // 换页面：表情跟着场景走，再配一句场景台词
    idx.value = faceForScene(route.name)
    const scene = SCENE_LINES[route.name] || []
    if (scene.length) {
      const t = pickFresh(scene)
      speak(meName.value && Math.random() < 0.4 ? `${meName.value}，${t}` : t)
    }
    scheduleSaveNudge()
  }
)
</script>

<style scoped>
.mascot {
  position: fixed;
  right: 20px;
  bottom: 14px;
  z-index: 46;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  pointer-events: none;
  transition: left 0.28s ease, top 0.28s ease;
}

/* 气泡 / 名片 / 菜单统一挂在浮层里，不参与布局：
   否则它们一出现就会把身体顶下去，鼠标脱靶后浮层消失、身体弹回，来回抖。 */
.mc-overlays {
  position: absolute;
  bottom: 100%;
  right: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  width: max-content;
  max-width: 240px;
  margin-bottom: 8px;
}
.side-left .mc-overlays {
  right: auto;
  left: 0;
  align-items: flex-start;
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
  pointer-events: auto;
  cursor: pointer;
}
.mc-bubble:hover { filter: brightness(1.03); }
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
.mc-card-me {
  margin-top: 6px;
  padding-top: 6px;
  font-size: 12px;
  color: #9fd8bd;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.card-enter-active, .card-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.card-enter-from, .card-leave-to { opacity: 0; transform: translateY(4px); }

/* 右键菜单 */
.mc-menu {
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  min-width: 116px;
  padding: 6px;
  background: rgba(18, 22, 25, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 10px;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.mc-menu button {
  padding: 7px 10px;
  font-size: 12.5px;
  text-align: right;
  color: #dfe6e2;
  background: none;
  border: none;
  border-radius: 7px;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease;
}
.mc-menu button:hover { background: rgba(159, 216, 189, 0.14); color: #9fd8bd; }
.menu-enter-active, .menu-leave-active { transition: opacity 0.16s ease, transform 0.16s ease; }
.menu-enter-from, .menu-leave-to { opacity: 0; transform: translateY(4px); }

.mc-body {
  position: relative;
  pointer-events: auto;
  width: 112px;
  padding: 0;
  border: none;
  background: none;
  cursor: grab;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.5));
  animation: mc-float 3.4s ease-in-out infinite;
  transition: transform 0.2s ease, filter 0.4s ease;
  touch-action: none;
  user-select: none;
}
/* 被拎起来：歪一点、抬一点，配合即时台词，手感更明确 */
.mascot.dragging .mc-body {
  cursor: grabbing;
  animation: none;
  transform: rotate(-5deg) scale(1.08);
  filter: drop-shadow(0 18px 26px rgba(0, 0, 0, 0.58));
}
/* 悬停：漂浮与小动作就地冻结（用 paused 而不是 none，避免突兀归位），
   只保留轻微放大作为反馈，指针不会再被甩掉 */
.mascot.hovering .mc-body { animation-play-state: paused; }
.mascot.hovering .mc-zzz { animation-play-state: paused; }
/* 悬停反馈改用 filter：动画处于 paused 时 transform 会被动画值覆盖，改 transform 是无效的 */
.mc-body:hover { filter: drop-shadow(0 12px 24px rgba(0, 0, 0, 0.55)) brightness(1.07); }
.mc-body:active { filter: drop-shadow(0 6px 14px rgba(0, 0, 0, 0.5)) brightness(0.94); }

/* 扩大点击热区：图片四周 -10px 也算身体，好点很多 */
.mc-body::after {
  content: '';
  position: absolute;
  inset: -10px;
}
.mc-body img { display: block; width: 100%; height: auto; user-select: none; -webkit-user-drag: none; }

/* 表情切换：靠 :key 重挂载触发淡入，避免硬切一帧 */
.mc-body img { animation: mc-face-in 0.34s ease both; }
@keyframes mc-face-in {
  0% { opacity: 0; transform: scale(0.9) translateY(3px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

/* 深夜：压暗一点，配 Zzz */
.mascot.sleeping .mc-body img { filter: brightness(0.78) saturate(0.75); }
.mascot.sleeping .mc-body { animation: mc-float 6s ease-in-out infinite; }
.mc-zzz {
  position: absolute;
  top: 2px;
  right: -6px;
  font-size: 13px;
  font-weight: 500;
  color: #cfe3d8;
  letter-spacing: 1px;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.6);
  animation: mc-zzz 3s ease-in-out infinite;
}
@keyframes mc-zzz {
  0%, 100% { opacity: 0; transform: translateY(4px) scale(0.9); }
  40% { opacity: 1; transform: translateY(-6px) scale(1); }
  70% { opacity: 0; transform: translateY(-12px) scale(1.05); }
}

/* 静音标记 */
.mc-mute {
  position: absolute;
  right: -4px;
  bottom: 6px;
  padding: 1px 6px;
  font-size: 11px;
  color: #dfe6e2;
  background: rgba(18, 22, 25, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
}
.mc-dot-flag {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 12px;
  height: 12px;
  background: rgba(18, 22, 25, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
}

@keyframes mc-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}

/* ---- 待机小动作 ---- */
.mc-body.is-peek { animation: mc-peek 2.2s ease-in-out; }
@keyframes mc-peek {
  0%, 100% { transform: rotate(0) translateY(0); }
  25% { transform: rotate(-6deg) translateY(-4px); }
  55% { transform: rotate(6deg) translateY(-4px); }
  80% { transform: rotate(-3deg) translateY(0); }
}
.mc-body.is-hop { animation: mc-hop 1.6s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes mc-hop {
  0%, 100% { transform: translateY(0) scaleY(1); }
  18% { transform: translateY(2px) scaleY(0.92); }
  40% { transform: translateY(-26px) scaleY(1.06); }
  62% { transform: translateY(0) scaleY(0.96); }
  78% { transform: translateY(-6px) scaleY(1.02); }
}
.mc-body.is-sway { animation: mc-sway 2.4s ease-in-out; }
@keyframes mc-sway {
  0%, 100% { transform: rotate(0); }
  30% { transform: rotate(5deg); }
  70% { transform: rotate(-5deg); }
}

/* ---- 最小化圆点 ---- */
.mc-dot {
  position: relative;
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

/* 系统的「最佳性能」会让浏览器上报 prefers-reduced-motion: reduce，
   此前在此把桌宠动画整个停掉，看起来像卡住了。这里保留动作，只放慢节奏。 */
@media (prefers-reduced-motion: reduce) {
  .mc-body { animation-duration: 6s !important; }
  .mc-zzz { animation-duration: 5s !important; }
}
</style>
