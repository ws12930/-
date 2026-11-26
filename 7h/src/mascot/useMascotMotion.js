/**
 * 桌宠动作状态机。
 *
 * 之前的「动作」只有几段 CSS keyframes（is-peek / is-hop / is-sway）随机播放，
 * 没有状态、没有优先级、也没有位移，表现上就是「原地抖一下」。这里换成一个
 * 显式的有限状态机，把「姿态动画」和「位置 / 高度」分开：
 *
 *   state   idle | walk | jump | drag | land | wave | celebrate | think | failed
 *   lean    整体倾斜（deg）——走动时朝行进方向压一点，交给 CSS transform
 *   lift    离地高度（px）——跳跃时按抛物线抬升，交给 CSS translateY
 *
 * 帧动画只负责挤压 / 拉伸 / 呼吸，位移由这里算，所以：
 *   - 换素材不用改位移逻辑，换位移也不用重烘焙帧；
 *   - 拖拽时用户的手永远优先，任何自主动作都会被 beginDrag() 立刻掐断；
 *   - 所有定时器都走 useMascotTimers，收起 / 切后台 / 卸载时一起清干净。
 *
 * ===== 动作集参考 DSH（DeepSeek Harness）桌宠插件 =====
 * Practice019/dsh-kun-like-pet（MIT）用 9 个状态随工作状态切换；
 * sereinmono/dsh-desktop-pet（MIT）把状态收敛成语义状态、按优先级折叠、
 * 瞬时状态 2s 后自动回到 IDLE；PC2005-cloud/dsh-pet（MIT）把上百段动画
 * 按权重首尾衔接，点击还会随机给不同反应。这里对应落地成：
 *   - 一次性动作（wave / celebrate / think / failed / jump / land）播完自动回 idle，
 *     等价于 dsh-desktop-pet 的「瞬时状态回归」；
 *   - 拖拽 > 落地 > 一次性动作 > 走动 > 待机 的抢占顺序，等价于它的优先级折叠；
 *   - 待机久了按权重抽一个小动作，点击也按权重给不同反应，等价于 dsh-pet 的权重衔接。
 *
 * hover / 菜单打开视为「暂时挂起」：不再起新的自主动作，但不打断正在播的一次性动作
 * （跳跃会自然落地、招手会挥完），避免鼠标一放上去就「僵住」。
 */
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { resolveClip, clipDuration, IDLE_VARIANTS } from './animations'
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
  SPEED_MAX
} from './config'

const rand = (a, b) => a + Math.random() * (b - a)
const easeInOut = (t) => (t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2)

/** 点击时的随机反应（参考 dsh-pet：同一个点击动作有好几种表现） */
const POKE_REACTIONS = [
  { state: 'wave', weight: 46 },
  { state: 'jump', weight: 34 },
  { state: 'think', weight: 20 }
]

/** 按权重抽一项，权重为 0 或空表时退回第一项 */
function pickWeighted(list) {
  if (!list || !list.length) return null
  const total = list.reduce((s, it) => s + (it.weight || 0), 0)
  if (total <= 0) return list[0]
  let r = Math.random() * total
  for (const it of list) {
    r -= it.weight || 0
    if (r <= 0) return it
  }
  return list[list.length - 1]
}

export function useMascotMotion({ timers, position, faceKey, settings, isMini, isSleepy }) {
  const state = ref('idle')
  const facing = ref('right')
  const lean = ref(0)
  const lift = ref(0)
  const paused = ref(false)

  let rafId = null
  let roamId = null
  let settleId = null // 一次性动作「播完回 idle」的定时器（land / wave / celebrate / ...）
  let hoverHold = false
  let menuHold = false
  let dragging = false
  let disposed = false

  // 系统「减少动态效果」：保留呼吸（放慢），但不再自主走动 / 跳跃
  const reduced = ref(false)
  const mq = typeof window !== 'undefined' && window.matchMedia
    ? window.matchMedia('(prefers-reduced-motion: reduce)')
    : null
  const onMq = (e) => { reduced.value = e.matches }
  if (mq) {
    reduced.value = mq.matches
    mq.addEventListener?.('change', onMq)
  }

  const eco = computed(() => !!settings.eco)
  const motionOn = computed(() => settings.motion !== false)
  const roamOn = computed(() => settings.roam !== false)
  /**
   * 播放倍速：用户设置（0.5–2x，参考 dsh-desktop-pet 的 animationSpeed 0.25–4）
   * 再乘上系统「减少动态」的 0.55 折。播放器直接把倍速折算进帧延迟，不叠加定时器。
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

  /** 播放器是否应该停住（省电 / 关掉动态动作 / 外部暂停） */
  const frozen = computed(() => paused.value || !motionOn.value || eco.value)

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

  function settleIdle(extraLift = 0) {
    state.value = 'idle'
    lean.value = 0
    lift.value = extraLift
  }

  /** 排下一次自主动作；被挂起时先不排，等 release() 再来 */
  function schedule(delay) {
    clearRoam()
    if (blocked()) return
    roamId = timers.timeout(behave, delay ?? rand(ROAM_MIN_MS, ROAM_MAX_MS))
  }

  /**
   * 一次性动作的统一入口：切状态 + 按剪辑时长排「回 idle」。
   * 等价于 dsh-desktop-pet 的瞬时状态（SUCCESS / ERROR / STARTING）2s 后回归 IDLE，
   * 只是这里的时长跟着素材走，换帧率不会让动作被切断。
   */
  function playOneShot(name, force = false) {
    if (disposed || paused.value || dragging) return
    if (!clipDuration(name)) return
    if (!force) {
      if (blocked()) return
      if (state.value !== 'idle') return
    }
    clearRaf()
    clearRoam()
    clearSettle()
    state.value = name
    lean.value = 0
    lift.value = 0
    // +60ms 余量：播放器最后一帧的定时器可能比这里晚一两个 tick，
    // 提前切回 idle 会让最后那帧看不到。
    settleId = timers.timeout(() => {
      settleId = null
      if (state.value === name) {
        settleIdle()
        schedule()
      }
    }, clipDuration(name) + 60)
  }

  function behave() {
    roamId = null
    if (blocked()) return
    // 深夜 / 系统要求减少动态：只呼吸，不折腾
    if (isSleepy.value || reduced.value) {
      schedule(rand(ROAM_MIN_MS, ROAM_MAX_MS))
      return
    }
    const roll = Math.random()
    if (roamOn.value && roll < 0.54) startWalk()
    else if (roll < 0.76) playJump(JUMP_PX)
    // 其余情况抽一个待机小动作（发呆 / 招手），不是每次都动，留点安静的空隙
    else playOneShot(pickWeighted(IDLE_VARIANTS)?.state)
  }

  // ---- 走动：匀速 + 缓入缓出，位置直接写回 position.pos，和拖拽共用同一套 clamp ----
  function startWalk() {
    if (blocked() || state.value !== 'idle') return
    const base = position.pos.value || position.defaultPos()
    if (!base) return
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
      return
    }

    clearRaf()
    clearRoam()
    facing.value = dir > 0 ? 'right' : 'left'
    lean.value = dir > 0 ? WALK_LEAN : -WALK_LEAN
    state.value = 'walk'

    const from = base.x
    const to = base.x + dir * dist
    const dur = Math.min(WALK_MAX_MS, Math.max(WALK_MIN_MS, (dist / WALK_SPEED) * 1000))
    const t0 = performance.now()

    const step = (now) => {
      rafId = null
      if (state.value !== 'walk') return
      const t = Math.min(1, (now - t0) / dur)
      // 目标点在 startWalk 里已经按视口和 MIN_GAP 夹过，这里不再逐帧读 offsetWidth，
      // 免得每帧都强制同步布局（拖拽路径有 clamp，是因为那里要跟鼠标瞬时坐标）
      position.pos.value = { x: from + (to - from) * easeInOut(t), y: base.y }
      if (t < 1 && !paused.value) rafId = timers.raf(step)
      else finishWalk()
    }
    rafId = timers.raf(step)
  }

  function finishWalk() {
    clearRaf()
    if (state.value !== 'walk') return
    settleIdle()
    position.save()
    if (Math.random() < JUMP_AFTER_WALK) playJump(JUMP_PX)
    else schedule()
  }

  /**
   * 跳跃：动画帧负责蓄力 / 拉伸 / 落地缓冲，位移用一条正弦抛物曲线。
   * 起跳前 16% 的时间是蓄力（帧里人物在蹲），所以 lift 延迟 16% 再开始抬升。
   */
  function playJump(height = JUMP_PX, force = false) {
    if (disposed || paused.value || dragging) return
    if (!force) {
      if (blocked()) return
      // 只在 idle 时起跳：不会把招手 / 庆祝 / 发呆从中途打断
      if (state.value !== 'idle') return
    }
    clearRaf()
    clearRoam()
    clearSettle()
    state.value = 'jump'
    lean.value = 0
    const dur = clipDuration('jump')
    const t0 = performance.now()

    const step = (now) => {
      rafId = null
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
  }

  // ---- 拖拽：最高优先级，按下即接管 ----
  function beginDrag() {
    if (dragging) return
    dragging = true
    clearRaf()
    clearRoam()
    clearSettle()
    state.value = 'drag'
    lean.value = 0
    lift.value = 0
  }

  function endDrag() {
    if (!dragging) return
    dragging = false
    clearRaf()
    // 落地缓冲也走一次性动作的统一回归，时长跟着素材走
    playOneShot('land', true)
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

  /**
   * 被点一下：按权重随机给一个反应（挥手 / 轻跳 / 歪头发呆）。
   * 参考 dsh-pet 的「点击有开心、害羞、傲娇等多种表现」——固定一个动作太机械。
   * force = true 是必须的：能点到它，鼠标必然悬停，hoverHold 会挡住普通入口。
   */
  function poke() {
    if (eco.value || !motionOn.value || isMini.value || paused.value || dragging) return
    if (state.value !== 'idle') return
    const r = pickWeighted(POKE_REACTIONS)
    if (!r) return
    if (r.state === 'jump') playJump(JUMP_PX_TAP, true)
    else playOneShot(r.state, true)
  }

  /**
   * 外部业务事件驱动的情绪动作（对应 dsh-desktop-pet 的 event -> semantic state）。
   *   'celebrate' 发布成功 / 任务完成
   *   'failed'    保存失败 / 出错
   *   'think'     思考、摸鱼
   *   'wave'      打招呼
   * 只打断 idle / walk，不打断跳跃与拖拽（正在半空中的事让它们自己收尾）。
   */
  function react(kind) {
    if (disposed || paused.value || dragging) return
    if (state.value !== 'idle' && state.value !== 'walk') return
    if (state.value === 'walk') {
      clearRaf()
      position.save()
    }
    if (kind === 'jump') playJump(JUMP_PX, true)
    else playOneShot(kind, true)
  }

  /** 外部（收起 / 切后台 / 省电）统一暂停：清掉所有在途的动画与排程 */
  function pause() {
    paused.value = true
    clearRaf()
    clearRoam()
    clearSettle()
    settleIdle()
  }

  function resume() {
    if (disposed) return
    paused.value = false
    settleIdle()
    schedule(rand(2600, 6200))
  }

  function onSettingChange() {
    clearRaf()
    clearRoam()
    clearSettle()
    settleIdle()
    if (!blocked()) schedule(rand(1800, 4200))
  }
  watch([eco, motionOn, roamOn], onSettingChange)

  onBeforeUnmount(() => {
    disposed = true
    clearRaf()
    clearRoam()
    clearSettle()
    mq?.removeEventListener?.('change', onMq)
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
    hold,
    release,
    beginDrag,
    endDrag,
    playJump,
    playOneShot,
    poke,
    nudge: poke, // 兼容旧调用点：语义已经是「被戳一下的随机反应」
    react,
    pause,
    resume,
    startWalk
  }
}
