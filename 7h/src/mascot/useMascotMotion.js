/**
 * 桌宠动作状态机（第二轮改造）。
 *
 * 相比上一版，这一版把「动作」从零散函数收成一条统一的调度链，参考了两套开源实现：
 *
 * 1. **Shimeji-ee**（BSD-2-Clause）的 Behavior / Action：
 *    高层行为结束后从带权重的 NextBehaviorList 抽下一步，Action 由「Pose + 速度 + 时长」组成。
 *    这里对应成 actions.js 的 `behaviorWeights()`（按页面 / 时段 / 无聊度调权重）
 *    + `ACTION_DEFS`（优先级、一次性、可打断），并由 `request()` 统一裁决。
 * 2. **dsh-pet**（MIT）的物理手感与状态：
 *    点击 Q 弹挤压、拖拽甩抛 + 边缘反弹、空闲打盹、动作首尾相接不硬切。
 *
 * 状态集：idle / sleep / walk / think / wave / jump / throw / celebrate / failed / land / drag
 * 优先级：drag(100) > land(60) > celebrate·failed(50) > throw(45) > jump(40)
 *         > think·wave(30) > walk(20) > sleep(5) > idle(0)
 *
 * 三条不可破坏的约束（沿用旧版语义）：
 *  - 用户的手永远优先：beginDrag() 直接清掉在途 rAF 与排程；
 *  - 不可打断的一次性动作播完再回 idle，高优先级事件进「待播队列」而不是丢失；
 *  - 所有定时器 / rAF 都走 useMascotTimers，收起、切后台、卸载时一起清。
 */
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { resolveClip, clipDuration } from './animations'
import {
  ACTION_DEFS,
  canStart,
  pickWeighted,
  behaviorWeights,
  filterCooled,
  createCooldowns,
  POKE_REACTIONS
} from './actions'
import {
  WALK_SPEED,
  WALK_MIN_MS,
  WALK_MAX_MS,
  WALK_MIN_PX,
  WALK_MAX_PX,
  WALK_LEAN,
  ROAM_MIN_MS,
  ROAM_MAX_MS,
  ROAM_RETRY_MS,
  JUMP_AFTER_WALK,
  JUMP_PX,
  JUMP_PX_TAP,
  HOLD_RELEASE_MS,
  MIN_GAP,
  SPEED_MIN,
  SPEED_MAX,
  SQUASH_MS,
  SQUASH_X,
  SQUASH_Y,
  HOVER_LEAN,
  BOREDOM_NAP_MS,
  BOREDOM_NAP_MS_NIGHT,
  NAP_MIN_MS,
  THROW_MIN_V,
  THROW_MAX_V,
  THROW_FRICTION,
  THROW_BOUNCE,
  THROW_MAX_MS,
  THROW_LIFT_PX,
  POKE_UPPER_RATIO,
  POKE_COOLDOWN_MS
} from './config'

const rand = (a, b) => a + Math.random() * (b - a)
const easeInOut = (t) => (t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2)
const clamp = (v, a, b) => Math.min(Math.max(v, a), b)

/** 由位移逻辑自己收尾的状态：不排「播完回 idle」的定时器 */
const MANUAL_STATES = new Set(['idle', 'walk', 'jump', 'throw', 'drag', 'sleep'])

export function useMascotMotion({ timers, position, faceKey, settings, isMini, isSleepy, routeName }) {
  const state = ref('idle')
  const facing = ref('right')
  const lean = ref(0)
  const lift = ref(0)
  const paused = ref(false)
  const sleeping = ref(false) // 打盹（与「深夜待机用 sleep 剪辑」区分）
  const squashX = ref(1)
  const squashY = ref(1)
  const boredom = ref(0)

  let rafId = null
  let roamId = null
  let settleId = null
  let napId = null
  let wakeId = null
  let squashRaf = null
  let hoverHold = false
  let menuHold = false
  let dragging = false
  let disposed = false
  let pendingOneShot = null
  let lastInteractAt = Date.now()
  let pausedAt = 0
  let lastPokeAt = 0

  const cooldowns = createCooldowns()

  // 系统「减少动态效果」：保留呼吸（放慢），但不再自主走动 / 跳跃
  const reduced = ref(false)
  const mq = typeof window !== 'undefined' && window.matchMedia
    ? window.matchMedia('(prefers-reduced-motion: reduce)')
    : null
  const onMq = (e) => { reduced.value = e.matches }
  if (mq) {
    reduced.value = mq.matches
    // Safari 13 只有 addListener，没有 addEventListener
    if (mq.addEventListener) mq.addEventListener('change', onMq)
    else if (mq.addListener) mq.addListener(onMq)
  }

  const eco = computed(() => !!settings.eco)
  const motionOn = computed(() => settings.motion !== false)
  const roamOn = computed(() => settings.roam !== false)
  const napOn = computed(() => settings.nap !== false)

  /**
   * 播放倍速：用户设置（0.5–2x）再乘上系统「减少动态」的 0.55 折。
   * 播放器把倍速折算进帧延迟，状态机的「播完回 idle」与跳跃位移也按同一个倍速缩放。
   */
  const playbackRate = computed(() => {
    const user = Math.min(SPEED_MAX, Math.max(SPEED_MIN, Number(settings.speed) || 1))
    return user * (reduced.value ? 0.55 : 1)
  })

  const clip = computed(() => resolveClip(faceKey.value, state.value, facing.value, {
    motion: motionOn.value,
    sleepy: isSleepy.value,
    eco: eco.value
  }))

  const frozen = computed(() => paused.value || !motionOn.value || eco.value)

  /** 0–1 的无聊度：深夜里阈值更短，用来调自主动作的权重 */
  function boredom01() {
    const span = isSleepy.value ? BOREDOM_NAP_MS_NIGHT : BOREDOM_NAP_MS
    return clamp((Date.now() - lastInteractAt) / Math.max(1000, span), 0, 1)
  }

  function blocked() {
    return disposed || paused.value || dragging || hoverHold || menuHold ||
      isMini.value || eco.value || !motionOn.value
  }

  function clearRaf() {
    timers.clear(rafId)
    rafId = null
  }
  function clearRoam() {
    timers.clear(roamId)
    roamId = null
  }
  function clearSettle() {
    timers.clear(settleId)
    settleId = null
  }
  function clearNap() {
    timers.clear(napId)
    napId = null
  }
  function clearWake() {
    timers.clear(wakeId)
    wakeId = null
  }

  function settleIdle() {
    state.value = 'idle'
    lean.value = 0
    lift.value = 0
    // 高优先级动作被打断时排进队列的一次性动作，回 idle 后再补播
    const next = pendingOneShot
    pendingOneShot = null
    if (next) {
      timers.timeout(() => {
        if (state.value === 'idle' && !blocked()) playOneShot(next, true)
      }, 30)
    }
  }

  /** 排下一次自主动作；被挂起时先不排，等 release() 再来 */
  function schedule(delay) {
    clearRoam()
    if (blocked()) return
    roamId = timers.timeout(behave, delay ?? rand(ROAM_MIN_MS, ROAM_MAX_MS))
  }

  /**
   * 统一的切状态入口：清在途调度、写 state、按需排「播完回 idle」。
   * 与旧版的区别是「谁能切」由 actions.canStart 决定，不再散落在各函数里。
   */
  function enterState(name) {
    const prev = state.value
    clearRaf()
    clearRoam()
    clearSettle()
    if (prev === 'walk') position.save()
    state.value = name
    if (name !== 'drag') {
      lean.value = 0
      lift.value = 0
    }
    if (name === 'sleep') sleeping.value = true
    else if (name !== 'drag') sleeping.value = false

    const def = ACTION_DEFS[name]
    if (def?.oneShot && !MANUAL_STATES.has(name)) {
      // +60ms 余量：播放器最后一帧可能晚一两个 tick；倍速要一起折进来
      settleId = timers.timeout(() => {
        settleId = null
        if (state.value !== name) return
        settleIdle()
        schedule()
      }, clipDuration(name) / Math.max(0.1, playbackRate.value || 1) + 60)
    }
  }

  /**
   * 请求切到某个动作。
   * 被拒绝时，如果当前是「不可打断的一次性动作」而请求方也是一次性动作，
   * 就排进 pending（最多 1 个），等它播完再补——业务反馈不再被直接丢掉。
   */
  function request(name, { force = false } = {}) {
    if (disposed || paused.value) return false
    if (!ACTION_DEFS[name]) return false
    if (name === state.value) return false
    if (dragging && name !== 'land' && name !== 'drag') return false

    if (!canStart(state.value, name, { force })) {
      const cur = ACTION_DEFS[state.value]
      if (!force && ACTION_DEFS[name].oneShot && cur?.oneShot && !pendingOneShot) {
        pendingOneShot = name
      }
      return false
    }
    enterState(name)
    return true
  }

  function playOneShot(name, force = false) {
    return request(name, { force })
  }

  /**
   * 跳跃：动画帧负责蓄力 / 拉伸 / 落地缓冲，位移用一条正弦抛物曲线。
   * 起跳前 16% 的时间是蓄力（帧里人物在蹲），所以 lift 延迟 16% 再开始抬升。
   */
  function playJump(height = JUMP_PX, force = false) {
    if (!request('jump', { force })) return false
    const dur = clipDuration('jump') / Math.max(0.1, playbackRate.value || 1)
    const t0 = performance.now()
    const step = (now) => {
      rafId = null
      if (state.value !== 'jump' || disposed) return
      const t = Math.min(1, (now - t0) / dur)
      const air = Math.max(0, Math.min(1, (t - 0.16) / 0.62))
      lift.value = Math.sin(Math.PI * air) * height
      if (t < 1 && !paused.value) rafId = timers.raf(step)
      else {
        settleIdle()
        schedule()
      }
    }
    rafId = timers.raf(step)
    return true
  }

  // ---- 走动：匀速 + 缓入缓出，位置直接写回 position.pos ----
  function startWalk() {
    if (blocked() || state.value !== 'idle') return false
    const base = position.pos.value || position.defaultPos()
    if (!base) return false
    const { w } = position.dims()
    const minX = MIN_GAP
    const maxX = Math.max(MIN_GAP, window.innerWidth - w - MIN_GAP)

    let dir = Math.random() < 0.5 ? -1 : 1
    if (base.x <= minX + 32) dir = 1
    else if (base.x >= maxX - 32) dir = -1

    const room = dir > 0 ? maxX - base.x : base.x - minX
    const dist = Math.min(room, rand(WALK_MIN_PX, WALK_MAX_PX))
    if (dist < 24) {
      schedule(ROAM_RETRY_MS)
      return false
    }
    if (!request('walk')) return false

    facing.value = dir > 0 ? 'right' : 'left'
    lean.value = dir > 0 ? WALK_LEAN : -WALK_LEAN

    const from = base.x
    const to = base.x + dir * dist
    const dur = Math.min(WALK_MAX_MS, Math.max(WALK_MIN_MS, (dist / WALK_SPEED) * 1000))
    const t0 = performance.now()
    const step = (now) => {
      rafId = null
      if (state.value !== 'walk' || disposed) return
      const t = Math.min(1, (now - t0) / dur)
      position.pos.value = { x: from + (to - from) * easeInOut(t), y: base.y }
      if (t < 1 && !paused.value) rafId = timers.raf(step)
      else finishWalk()
    }
    rafId = timers.raf(step)
    return true
  }

  function finishWalk() {
    clearRaf()
    if (state.value !== 'walk') return
    settleIdle()
    position.save()
    if (Math.random() < JUMP_AFTER_WALK) playJump(JUMP_PX)
    else schedule()
  }

  // ---- 拖拽：最高优先级，按下即接管 ----
  function beginDrag() {
    if (dragging) return
    dragging = true
    clearRaf()
    clearRoam()
    clearSettle()
    clearWake()
    pendingOneShot = null
    state.value = 'drag'
    sleeping.value = false
    lean.value = 0
    lift.value = 0
    lastInteractAt = Date.now()
  }

  /**
   * 松手。带速度时进入甩抛（dsh-pet 的手感），否则播落地缓冲。
   * @param {number} vx px/ms
   * @param {number} vy px/ms
   */
  function endDrag(vx = 0, vy = 0) {
    if (!dragging) return
    dragging = false
    clearRaf()
    if (Math.hypot(vx, vy) >= THROW_MIN_V) throwPet(vx, vy)
    else playOneShot('land', true)
  }

  /**
   * 甩抛：指数摩擦 + 边缘反弹，落定后接 land。
   * 只写 position.pos，不碰 localStorage，结束时统一 save()。
   */
  function throwPet(vx = 0, vy = 0) {
    if (disposed || paused.value) return false
    const base = position.pos.value || position.defaultPos()
    if (!base) return false
    let sx = clamp(vx, -THROW_MAX_V, THROW_MAX_V)
    let sy = clamp(vy, -THROW_MAX_V, THROW_MAX_V)
    if (Math.hypot(sx, sy) < THROW_MIN_V) {
      playOneShot('land', true)
      return false
    }
    if (!request('throw', { force: true })) {
      playOneShot('land', true)
      return false
    }

    facing.value = sx >= 0 ? 'right' : 'left'
    lean.value = clamp(sx * WALK_LEAN * 0.5, -WALK_LEAN, WALK_LEAN)

    const { w, h } = position.dims()
    const minX = MIN_GAP
    const maxX = Math.max(MIN_GAP, window.innerWidth - w - MIN_GAP)
    const maxY = Math.max(56, window.innerHeight - h - MIN_GAP)
    const minY = 56
    let px = clamp(base.x, minX, maxX)
    let py = clamp(base.y, minY, maxY)
    const t0 = performance.now()
    let last = t0

    const step = (now) => {
      rafId = null
      if (state.value !== 'throw' || disposed) return
      const dt = Math.min(32, Math.max(8, now - last))
      last = now
      const k = Math.pow(THROW_FRICTION, dt / 16)
      sx *= k
      sy *= k
      px += sx * dt
      py += sy * dt
      if (px < minX || px > maxX) {
        px = clamp(px, minX, maxX)
        sx = -sx * THROW_BOUNCE
      }
      if (py < minY || py > maxY) {
        py = clamp(py, minY, maxY)
        sy = -sy * THROW_BOUNCE
      }
      position.pos.value = { x: px, y: py }
      const t = Math.min(1, (now - t0) / THROW_MAX_MS)
      lift.value = Math.sin(t * Math.PI) * THROW_LIFT_PX
      if (t < 1 && Math.hypot(sx, sy) > 0.06 && !paused.value) {
        rafId = timers.raf(step)
      } else {
        position.snap()
        position.save()
        playOneShot('land', true)
      }
    }
    rafId = timers.raf(step)
    return true
  }

  // ---- 悬停 / 菜单：暂停自主行为，但正在跳 / 招手的那一下会自己播完 ----
  function hold(kind) {
    if (kind === 'menu') menuHold = true
    else hoverHold = true
    clearRoam()
    if (state.value === 'walk') {
      clearRaf()
      settleIdle()
    }
  }

  function release(kind) {
    if (kind === 'menu') menuHold = false
    else hoverHold = false
    if (!hoverHold && !menuHold) schedule(HOLD_RELEASE_MS)
  }

  /** 悬停时朝鼠标方向轻微偏一下，表达「注意到你了」（只影响 idle 姿态） */
  function hoverTilt(dir) {
    if (state.value !== 'idle' || sleeping.value || eco.value || !motionOn.value) return
    lean.value = HOVER_LEAN * clamp(dir, -1, 1)
  }

  /** 点击 Q 弹：先压扁再回弹（参考 dsh-pet 的挤压手感，叠加在帧动画之上） */
  function pokeSquash() {
    timers.clear(squashRaf)
    squashX.value = SQUASH_X
    squashY.value = SQUASH_Y
    const t0 = performance.now()
    const step = (now) => {
      squashRaf = null
      const t = Math.min(1, (now - t0) / SQUASH_MS)
      // cos 回弹：中途过冲一次再收正，比线性回正更像「Q 弹」
      const k = Math.cos(t * Math.PI * 1.5) * (1 - t)
      squashX.value = 1 + (SQUASH_X - 1) * k
      squashY.value = 1 + (SQUASH_Y - 1) * k
      if (t < 1 && !paused.value) squashRaf = timers.raf(step)
      else {
        squashX.value = 1
        squashY.value = 1
      }
    }
    squashRaf = timers.raf(step)
  }

  function markInteract() {
    lastInteractAt = Date.now()
    boredom.value = 0
    clearNap()
    if (state.value === 'idle') napCheck()
  }

  /**
   * 点一下：按点击部位（上半身 = 摸头）从不同池子里抽反应。
   * @param {number} ratio 点击点在身体高度上的比例（0 = 头顶，1 = 脚底）
   */
  function pokeAt(ratio = 0.5) {
    if (eco.value || !motionOn.value || isMini.value || paused.value) return false
    if (sleeping.value) return false // 睡眠时的点击由上层先唤醒
    const now = Date.now()
    if (now - lastPokeAt < POKE_COOLDOWN_MS) return false
    lastPokeAt = now
    markInteract()

    const pool = ratio < POKE_UPPER_RATIO ? POKE_REACTIONS.head : POKE_REACTIONS.body
    const r = pickWeighted(filterCooled(pool, cooldowns)) || pickWeighted(pool)
    if (!r) return false

    pokeSquash()
    if (r.state === 'jump') return playJump(JUMP_PX_TAP, true)
    const ok = playOneShot(r.state, true)
    if (ok) cooldowns.mark(r.state)
    return ok
  }

  /** 兼容旧调用点：不知道部位时按身体中段处理 */
  function poke() {
    return pokeAt(0.6)
  }

  /**
   * 外部业务事件驱动的情绪动作（对应 dsh-desktop-pet 的 event → semantic state）。
   *   celebrate 发布成功 / 任务完成；failed 保存失败；think 思考摸鱼；wave 打招呼
   * 高优先级动作会打断走动；正在跳跃 / 落地时排进 pending，播完再补。
   */
  function react(kind) {
    if (disposed || paused.value) return false
    if (kind === 'jump') return playJump(JUMP_PX, false)
    if (kind === 'sleep') return sleep()
    if (kind === 'wake') return wake(true)
    return request(kind, { force: false })
  }

  // ---- 待机打盹（参考 dsh-pet 的睡觉状态：入睡 → 循环 → 起床） ----
  function napCheck() {
    clearNap()
    if (disposed || paused.value) return
    boredom.value = boredom01()
    if (!napOn.value || sleeping.value || state.value !== 'idle') return

    const threshold = isSleepy.value ? BOREDOM_NAP_MS_NIGHT : BOREDOM_NAP_MS
    const idleFor = Date.now() - lastInteractAt
    if (idleFor >= threshold) {
      sleep()
      return
    }
    napId = timers.timeout(napCheck, Math.max(3000, Math.min(12000, threshold - idleFor + 200)))
  }

  /** 入睡：状态切到 sleep 剪辑，并排一个「睡饱自己醒」的定时器 */
  function armWakeTimer() {
    clearWake()
    wakeId = timers.timeout(() => {
      wakeId = null
      if (sleeping.value) wake(false)
    }, NAP_MIN_MS + rand(0, 16000))
  }

  function sleep() {
    if (disposed || paused.value || sleeping.value || state.value !== 'idle') return false
    if (!napOn.value) return false
    enterState('sleep')
    clearNap()
    clearRoam()
    armWakeTimer()
    return true
  }

  /** 叫醒：被用户点 / 拖 / 自动睡饱都会走这里；greet=true 时招个手 */
  function wake(greet = true) {
    if (!sleeping.value) return false
    sleeping.value = false
    clearWake()
    lastInteractAt = Date.now()
    boredom.value = 0
    state.value = 'idle'
    lean.value = 0
    lift.value = 0
    if (greet) playOneShot('wave', true)
    schedule()
    napCheck()
    return true
  }

  function behave() {
    roamId = null
    if (blocked()) return
    if (sleeping.value) return
    if (reduced.value) {
      schedule(rand(ROAM_MIN_MS, ROAM_MAX_MS))
      return
    }

    const pool = filterCooled(
      behaviorWeights({
        route: routeName?.value || '',
        hour: new Date().getHours(),
        boredom: boredom01(),
        roam: roamOn.value
      }),
      cooldowns
    )
    // 池子被冷却清空时退回「发呆」，保证不会因为全在冷却就干站着
    const pick = pickWeighted(pool) || (cooldowns.ready('think') ? { state: 'think' } : { state: 'wave' })

    if (pick.state === 'walk') startWalk()
    else if (pick.state === 'jump') playJump(JUMP_PX)
    else if (pick.state === 'sleep') {
      if (!sleep()) schedule()
    } else {
      const ok = playOneShot(pick.state)
      if (ok) cooldowns.mark(pick.state)
      else schedule(ROAM_RETRY_MS)
    }
  }

  /** 外部（收起 / 切后台 / 省电）统一暂停：清掉所有在途的动画与排程 */
  function pause() {
    paused.value = true
    pausedAt = Date.now()
    clearRaf()
    clearRoam()
    clearSettle()
    clearNap()
    clearWake()
    pendingOneShot = null
    timers.clear(squashRaf)
    squashRaf = null
    squashX.value = 1
    squashY.value = 1
    if (sleeping.value) {
      state.value = 'sleep'
      lean.value = 0
      lift.value = 0
    } else {
      settleIdle()
    }
  }

  function resume() {
    if (disposed) return
    paused.value = false
    // 后台待了多久都不算「无聊」，否则一回来就打盹
    if (pausedAt) {
      lastInteractAt += Date.now() - pausedAt
      pausedAt = 0
    }
    if (sleeping.value) {
      state.value = 'sleep'
      armWakeTimer()
      napCheck()
      return
    }
    settleIdle()
    schedule(rand(2600, 6200))
    napCheck()
  }

  function onSettingChange() {
    clearRaf()
    clearRoam()
    clearSettle()
    clearNap()
    clearWake()
    pendingOneShot = null
    if (!napOn.value && sleeping.value) {
      sleeping.value = false
    }
    settleIdle()
    if (!blocked()) schedule(rand(1800, 4200))
    napCheck()
  }
  watch([eco, motionOn, roamOn, napOn], onSettingChange)

  onBeforeUnmount(() => {
    disposed = true
    clearRaf()
    clearRoam()
    clearSettle()
    clearNap()
    clearWake()
    if (mq) {
      if (mq.removeEventListener) mq.removeEventListener('change', onMq)
      else if (mq.removeListener) mq.removeListener(onMq)
    }
  })

  return {
    state,
    facing,
    lean,
    lift,
    paused,
    frozen,
    playbackRate,
    clip,
    sleeping,
    boredom,
    squashX,
    squashY,
    hold,
    release,
    beginDrag,
    endDrag,
    playJump,
    playOneShot,
    poke,
    pokeAt,
    nudge: poke,
    react,
    sleep,
    wake,
    hoverTilt,
    markInteract,
    pause,
    resume,
    startWalk,
    throwPet
  }
}
