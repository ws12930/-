/**
 * 桌宠动作素材表：帧数、帧率、循环方式，以及「状态 -> 素材」的解析规则。
 *
 * 素材是 scripts/gen_mascot_frames.py 烘焙出来的横向帧条（sprite sheet），
 * 每个单元格固定 CELL.w x CELL.h，和生成脚本里的常量必须一致：
 *   第 i 帧 = (i * CELL.w, 0, (i + 1) * CELL.w, CELL.h)
 *
 * 想换成手绘帧 / Shimeji 的 PNG 序列 / 网上的 GIF，只要保持单元格尺寸和帧数一致，
 * 直接覆盖同名文件即可；GIF 还把下面 CUSTOM_SHEETS 的注释看一遍。
 *
 * ===== 帧率（本轮优化）=====
 * 帧数和 fps 同比例提高，动作总时长（frames / fps）与旧版完全一致：
 *   idle  8f/5fps(1.60s)  -> 10f/6fps(1.67s)
 *   walk  6f/9fps(0.67s)  ->  8f/12fps(0.67s)
 *   jump  8f/12fps(0.67s) -> 10f/15fps(0.67s)
 *   land  4f/16fps(0.25s) ->  5f/20fps(0.25s)
 * 观感更顺，但播放器改成按帧延迟调度后，唤醒次数 == fps，
 * 待机时仍是每秒 6 次，而不是跟着屏幕刷新率空转 60 次。
 */

/** 帧条单元格尺寸（px）。改这里必须同步改 scripts/gen_mascot_frames.py */
export const CELL = { w: 128, h: 134 }

/**
 * 状态 -> 剪辑定义
 *   sheet  文件名里「人物-状态」的后半段
 *   frames 帧数（必须和实际帧条一致，否则会画到空白）
 *   fps    播放帧率（配合 MascotSprite 的定时调度，不再被屏幕刷新率放大）
 *   loop   是否循环；false 表示一次性动作，播完由状态机接回 idle
 */
export const CLIPS = {
  idle: { sheet: 'idle', frames: 10, fps: 6, loop: true },
  sleep: { sheet: 'sleep', frames: 6, fps: 3, loop: true },
  walkR: { sheet: 'walk-r', frames: 8, fps: 12, loop: true },
  walkL: { sheet: 'walk-l', frames: 8, fps: 12, loop: true },
  jump: { sheet: 'jump', frames: 10, fps: 15, loop: false },
  drag: { sheet: 'drag', frames: 6, fps: 10, loop: true },
  land: { sheet: 'land', frames: 5, fps: 20, loop: false },
  // 甩飞：沿用「被拎住」的姿态循环，位移与离地由状态机算；
  // 落点到达后接 land，视觉上像被丢出去再站稳。
  throw: { sheet: 'drag', frames: 6, fps: 10, loop: true },

  // ↓ 参考 DSH（DeepSeek Harness）桌宠插件补齐的状态
  //   wave     对应 Kun Like Pet 的 wave（点击回应）
  //   celebrate 对应 celebrating / SUCCESS（发布成功）
  //   think     对应 review / THINKING（发呆、摸鱼）
  //   failed    对应 failed / ERROR（出错、沮丧）
  wave: { sheet: 'wave', frames: 4, fps: 10, loop: false },
  celebrate: { sheet: 'celebrate', frames: 6, fps: 12, loop: false },
  think: { sheet: 'think', frames: 5, fps: 6, loop: false },
  failed: { sheet: 'failed', frames: 5, fps: 6, loop: false }
}

/**
 * 待机小动作池的兼容出口。
 * 权重表已迁到 actions.js（那里同时管优先级与冷却），这里保留导出，
 * 避免外部或旧文档里的 `IDLE_VARIANTS` 引用断掉。
 */
export { IDLE_VARIANTS } from './actions'

/** 由 Vite 在构建时收集所有帧条，返回 文件名 -> 产物 URL */
const SHEETS = import.meta.glob('../assets/mascot/anim/*.webp', {
  eager: true,
  query: '?url',
  import: 'default'
})

/**
 * 用 GIF 覆盖某个状态（可选）。GIF 会走 MascotSprite 里的 gifuct-js 解码分支，
 * 帧数 / 帧率以 GIF 自身为准，CLIPS 里的 frames/fps 会被忽略。
 *
 * 例：
 *   import idleGif from '../assets/mascot/anim/gif/welcome-idle.gif'
 *   CUSTOM_SHEETS['welcome-idle'] = idleGif
 */
export const CUSTOM_SHEETS = {}

/** 某个表情 + 状态对应的帧条 URL（未命中返回 ''，调用方会兜底到 idle） */
export function sheetUrl(faceKey, sheet) {
  return SHEETS[`../assets/mascot/anim/${faceKey}-${sheet}.webp`] || ''
}

/** 剪辑时长（ms），状态机用它对齐位移 / 跳跃的物理时间 */
export function clipDuration(name) {
  const c = CLIPS[name]
  if (!c) return 0
  return (c.frames / c.fps) * 1000
}

/**
 * 解析当前应当播放的剪辑。
 * @param {string} faceKey welcome / nofight / writing
 * @param {string} state   idle / walk / jump / drag / land / wave / celebrate / think / failed
 * @param {'left'|'right'} facing 走动朝向
 * @param {{ motion: boolean, sleepy: boolean, eco: boolean }} opts
 */
export function resolveClip(faceKey, state, facing, opts = {}) {
  const { motion = true, sleepy = false, eco = false } = opts
  let name = 'idle'
  if (state === 'walk') name = facing === 'left' ? 'walkL' : 'walkR'
  else if (CLIPS[state] && state !== 'idle') name = state

  // 深夜待机换成「睡觉」的慢呼吸；省电模式 / 关掉动态动作时退回单帧静态图
  if (name === 'idle' && sleepy && !eco) name = 'sleep'

  const def = CLIPS[name] || CLIPS.idle
  const custom = CUSTOM_SHEETS[`${faceKey}-${def.sheet}`]
  const src = custom || sheetUrl(faceKey, def.sheet) || sheetUrl(faceKey, 'idle')

  if (!motion || eco) {
    return { key: `${faceKey}-static`, src: sheetUrl(faceKey, 'idle'), frames: 1, fps: 1, loop: true, still: true }
  }
  return {
    key: `${faceKey}-${name}`,
    src,
    frames: def.frames,
    fps: def.fps,
    loop: def.loop,
    still: false
  }
}

/**
 * 素材预热统一由 `mascot/assets.js` 负责（预解码 + 分层排队 + 失败重试），
 * 这里不再保留第二套 `new Image()` 预热，避免同一张图被解码两次。
 * 旧调用点：`import { preloadMotion } from '../mascot/assets'`
 */
