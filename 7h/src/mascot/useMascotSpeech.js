/**
 * 桌宠的「说话」：台词选取、气泡状态、场景 / 时段 / 保存反馈、随机冒话、保存提醒。
 *
 * 从 Mascot.vue 里抽出来后，所有台词出口都汇聚到 speak()，于是防打扰门禁可以只写一次：
 * 静音 / 收起 / 标签页在后台时一律不弹气泡，也不起 bubbleTimer。
 * 原实现里拖拽反馈、blog:saved、关游戏窗口各自直接改气泡，收起状态下仍会冒话。
 */
import { ref } from 'vue'
import {
  SCENE_LINES,
  SCENE_FACE,
  POSTED,
  DRAFTED,
  SAVE_FAILED,
  daypartLine,
  BUBBLE_MS,
  SAVE_NUDGE_MS,
  CHATTER_MIN_MS,
  CHATTER_JITTER_MS
} from './config'

/** 归一化保存结果：EditorView 派发 PUBLISHED / DRAFT / ERROR，其余一律按发布成功处理 */
export function normalizeSaveStatus(status) {
  const s = String(status ?? '').trim().toUpperCase()
  if (s === 'ERROR') return 'ERROR'
  if (s === 'DRAFT') return 'DRAFT'
  return 'PUBLISHED'
}

export function useMascotSpeech({ timers, getFace, isMuted, isMini, route }) {
  const line = ref('')
  const lastLine = ref('')
  const showBubble = ref(false)
  let bubbleTimer = null
  let chatterTimer = null
  let saveNudgeTimer = null

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

  /** 场景对应的表情下标（soft 表情包只有两张，由调用方对池长取模） */
  function faceForScene(name) {
    return SCENE_FACE[name] ?? 0
  }

  /**
   * 唯一的台词出口。text 为空时按当前表情随机说一句（保持原 scheduleChatter 的行为）。
   * 门禁集中在这里：静音、收起、后台一律不说话，也不注册 bubbleTimer。
   */
  function speak(text) {
    if (isMuted.value || isMini.value || document.hidden) return
    const t = text || pickFresh(getFace().lines)
    if (!t) return
    lastLine.value = t
    line.value = t
    showBubble.value = true
    timers.clear(bubbleTimer)
    bubbleTimer = timers.timeout(() => (showBubble.value = false), BUBBLE_MS)
  }

  function dismissBubble() {
    timers.clear(bubbleTimer)
    showBubble.value = false
  }

  /** 说当前场景的话；没有场景台词就退回当前表情的台词 */
  function speakScene() {
    const scene = SCENE_LINES[route.name] || []
    speak(scene.length ? pickFresh(scene) : pickFresh(getFace().lines))
  }

  /**
   * 编辑器保存后：detail.status 为 PUBLISHED / DRAFT / ERROR。
   * 旧版只认 DRAFT，其余全部按「发布成功」播报，保存失败时会说出「发布成功！」——
   * 现在三态分开，并把归一化结果返回给调用方，由它决定播哪个情绪动作。
   * 返回 kind 不受 speak() 门禁影响：静音时不说话，动作照样可以给。
   */
  function speakSaved(status) {
    const kind = normalizeSaveStatus(status)
    const pool = kind === 'ERROR' ? SAVE_FAILED : kind === 'DRAFT' ? DRAFTED : POSTED
    speak(pickFresh(pool))
    return kind
  }

  /** 随机冒话：25-50s 一次，优先说当前场景的话 / 分时段问候 */
  function scheduleChatter() {
    timers.clear(chatterTimer)
    chatterTimer = timers.timeout(() => {
      if (!isMini.value && !document.hidden) {
        const r = Math.random()
        if (r < 0.3) speak(daypartLine())
        else if (r < 0.62) {
          const scene = SCENE_LINES[route.name] || []
          speak(scene.length ? pickFresh(scene) : '')
        } else speak('')
      }
      scheduleChatter()
    }, CHATTER_MIN_MS + Math.random() * CHATTER_JITTER_MS)
  }

  /**
   * 编辑器页待久了提醒保存。
   * 原实现在路由 watch 里无条件挂 180s 定时器（收起 / 后台也挂），
   * 这里直接在这些状态返回，连定时器都不注册。
   */
  function scheduleSaveNudge() {
    timers.clear(saveNudgeTimer)
    if (route.name !== 'editor' || isMini.value || document.hidden) return
    saveNudgeTimer = timers.timeout(() => {
      if (route.name === 'editor') speak('写这么多啦，记得点一下保存哦')
    }, SAVE_NUDGE_MS)
  }

  return {
    line,
    lastLine,
    showBubble,
    pick,
    pickFresh,
    faceForScene,
    speak,
    dismissBubble,
    speakScene,
    speakSaved,
    scheduleChatter,
    scheduleSaveNudge
  }
}
