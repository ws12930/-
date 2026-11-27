/**
 * 桌宠动作注册表：谁有资格打断谁、自主动作按什么权重抽、同一个动作的冷却。
 *
 * 参考并改造自两个开源实现：
 * 1. **Shimeji-ee**（BSD-2-Clause）的 Behavior / Action 双层结构：
 *    Behavior 是高层状态（待机、沿地板走、被拖拽……），Action 是「动画 + 位移」的原子单位；
 *    一个 Behavior 结束后从带权重的 NextBehaviorList 里抽下一个，而不是每帧随机。
 *    这里对应成 `behaviorWeights()` + `ACTION_DEFS`：权重按页面 / 时段 / 无聊度调整，
 *    优先级决定抢占，一次性动作播完再回 idle。
 * 2. **dsh-pet**（MIT）的动作链与物理手感：一百余段动画按权重首尾相接，
 *    点击有 Q 弹挤压、拖拽可以甩抛、空闲会自己找事做。这里落地成
 *    `POKE_REACTIONS`（按点击部位分池）、`createCooldowns()`（同动作冷却）
 *    以及 useMascotMotion 里的甩抛 / 挤压。
 *
 * 与 animations.js 的分工：那里管「素材怎么播」（帧数 / 帧率 / sheet 解析），
 * 这里管「什么时候播、能不能打断、播完接什么」。两边都不碰 UI 与业务事件。
 */
import { ACTION_COOLDOWN_MS } from './config'

/**
 * 动作元信息。
 *   priority      数值越大越强势，高优先级可以打断低优先级
 *   oneShot       播完自动回 idle（等价 Shimeji 的 Action 结束 → NextBehavior）
 *   interruptible 能否被同优先级 / 更高优先级的动作中途接管
 *
 * 顺序只作可读性，真正比较的是 priority：
 *   drag(100) > land(60) > celebrate/failed(50) > throw(45) > jump(40)
 *   > think/wave(30) > walk(20) > sleep(5) > idle(0)
 */
export const ACTION_DEFS = {
  idle: { priority: 0, oneShot: false, interruptible: true },
  sleep: { priority: 5, oneShot: false, interruptible: true },
  walk: { priority: 20, oneShot: false, interruptible: true },
  think: { priority: 30, oneShot: true, interruptible: true },
  wave: { priority: 30, oneShot: true, interruptible: true },
  jump: { priority: 40, oneShot: true, interruptible: false },
  throw: { priority: 45, oneShot: true, interruptible: false },
  celebrate: { priority: 50, oneShot: true, interruptible: false },
  failed: { priority: 50, oneShot: true, interruptible: false },
  land: { priority: 60, oneShot: true, interruptible: false },
  drag: { priority: 100, oneShot: false, interruptible: false }
}

export function actionDef(name) {
  return ACTION_DEFS[name] || null
}

/**
 * 能否从 current 切到 next。
 * force = true 是「用户明确要求」（点菜单、点击反应），绕过优先级但仍不允许打断拖拽。
 */
/**
 * 「自然收尾」：这些状态结束时会自己接下一个动作，即使当前动作声明了不可打断。
 * 漏掉这条规则会让甩抛 / 跳跃卡在自己的收尾上（曾经真的发生过：
 * throw 不可打断 → 落定时请求 land 被拒 → 永远停在半空）。
 */
const NATURAL_EXITS = {
  jump: ['land', 'idle'],
  throw: ['land'],
  drag: ['land']
}

/**
 * 能否从 current 切到 next。
 * force = true 是「用户明确要求」（点菜单、点击反应），绕过优先级但仍不允许截断
 * 半空中的跳跃 / 落地缓冲；自然收尾（NATURAL_EXITS）永远放行。
 */
export function canStart(current, next, { force = false } = {}) {
  const n = ACTION_DEFS[next]
  if (!n) return false
  if (next === 'drag') return true
  const c = ACTION_DEFS[current]
  if (!c) return true
  if (next === current) return false
  if (NATURAL_EXITS[current]?.includes(next)) return true
  // 正在播的一次性动作如果声明了「不可打断」，就让它播完；
  // force（点击 / 菜单）也不能把半空中的跳跃、落地缓冲截断。
  if (!c.interruptible && c.oneShot) return false
  if (force) return true
  if (n.priority > c.priority) return true
  if (n.priority < c.priority) return false
  // 同优先级：只有当前动作可被打断，且不是「两个一次性动作互顶」
  if (!c.interruptible) return false
  return !(n.oneShot && c.oneShot)
}

/** 待机小动作池（原在 animations.js，挪到这里统一管理权重） */
export const IDLE_VARIANTS = [
  { state: 'think', weight: 55 },
  { state: 'wave', weight: 45 }
]

/**
 * 点击反应池：按点击部位分开（参考 Shimeji 的交互热点）。
 * 摸头更容易得到招手 / 发呆，戳身体更容易蹦一下。
 */
export const POKE_REACTIONS = {
  head: [
    { state: 'wave', weight: 52 },
    { state: 'think', weight: 30 },
    { state: 'jump', weight: 18 }
  ],
  body: [
    { state: 'jump', weight: 50 },
    { state: 'wave', weight: 28 },
    { state: 'think', weight: 22 }
  ]
}

/** 按权重抽一项；权重为 0 / 空表时返回 null */
export function pickWeighted(list) {
  if (!list || !list.length) return null
  const total = list.reduce((s, it) => s + Math.max(0, it.weight || 0), 0)
  if (total <= 0) return null
  let r = Math.random() * total
  for (const it of list) {
    r -= Math.max(0, it.weight || 0)
    if (r <= 0) return it
  }
  return list[list.length - 1]
}

/**
 * 自主动作的权重表（Shimeji 的 NextBehaviorList）。
 * ctx: { route, hour, boredom(0–1), roam(boolean) }
 * 返回 [{ state, weight }]，调用方过滤掉冷却中的项再抽。
 */
export function behaviorWeights(ctx = {}) {
  const { route = '', hour = 12, boredom = 0, roam = true } = ctx
  const night = hour >= 23 || hour < 6
  const w = { walk: 54, jump: 22, think: 14, wave: 10 }

  if (!roam) w.walk = 0
  if (night) {
    // 深夜不折腾：发呆 + 打盹，避免半夜满屏乱走
    w.walk = 0
    w.jump = 0
    w.think = 58
    w.wave = 10
    w.sleep = 32
  }
  if (route === 'editor' || route === 'mine') {
    w.think += 20
    w.walk -= 8
  }
  if (route === 'community' || route === 'messages' || route === 'notifications') {
    w.wave += 14
    w.walk -= 6
  }
  if (route === 'post') {
    w.walk += 6
    w.think += 4
  }
  // 无聊度越高越倾向自己找点事（发呆 / 招手），但不再增加走动频率
  if (boredom > 0.45) {
    w.think += 14
    w.walk -= 6
  }

  return Object.entries(w)
    .map(([state, weight]) => ({ state, weight }))
    .filter((it) => it.weight > 0)
    .map((it) => ({ ...it, weight: Math.round(it.weight) }))
}

/** 从候选里剔除还在冷却中的动作 */
export function filterCooled(list, cooldowns, now = Date.now()) {
  return (list || []).filter((it) => cooldowns.ready(it.state, now))
}

/**
 * 极简冷却表：同一个待机小动作 / 点击反应在 ACTION_COOLDOWN_MS 内不重复。
 * 只存字符串 → 时间戳，不做淘汰；桌宠的动作名是有限集合，不会无限增长。
 */
export function createCooldowns(ms = ACTION_COOLDOWN_MS) {
  const last = new Map()
  return {
    ready(name, now = Date.now()) {
      if (!last.has(name)) return true
      return now - last.get(name) >= ms
    },
    mark(name, now = Date.now()) {
      last.set(name, now)
    },
    reset() {
      last.clear()
    }
  }
}
