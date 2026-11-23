/**
 * 桌宠的位置：视口约束、默认位置、靠边吸附、localStorage 记忆与版本迁移。
 *
 * P0 修复重点在 load()：原实现恢复存档坐标后不做任何校验，只有窗口 resize 时才 clamp。
 * 用户换显示器 / 从大屏挪到小屏 / 分屏后首次打开不会触发 resize，桌宠会整个跑到
 * 屏幕外，连「回到右下角」菜单都点不到（菜单也被裁在屏外）。
 * 现在恢复坐标后立刻 clamp 一次，onResize / 拖拽 / snap 共用同一个 clampPos。
 *
 * 另一个坑：收起成小圆点时 rootEl 只有 42px，若按这个尺寸 clamp，展开成 112px 后
 * 右下角就会溢出，所以 clamp 统一按「展开态尺寸」估算（见 dims()）。
 */
import { ref, computed } from 'vue'
import { MASCOT_STORE_KEY, MASCOT_STORE_KEY_V2 } from '../utils/constants'
import {
  DEFAULT_SETTINGS, EDGE, TOP_LIMIT, MIN_GAP, SNAP_RANGE, DEFAULT_SIZE
} from './config'

export function useMascotPosition({ rootEl, getSnapshot, applySnapshot, onStorageError }) {
  const pos = ref(null) // { x, y } px（元素左上角）；null = 走 CSS 默认右下角
  const side = ref('right')
  const dragging = ref(false)

  /** 展开态尺寸：收起时也要按展开算，否则展开后会溢出右下角 */
  function dims() {
    const el = rootEl.value
    return {
      w: Math.max(el ? el.offsetWidth : 0, DEFAULT_SIZE + 20),
      h: Math.max(el ? el.offsetHeight : 0, 190)
    }
  }

  /** 唯一的视口约束函数：拖拽、resize、loadState、snap 全部走这里 */
  function clampPos(p) {
    if (!p) return null
    const { w, h } = dims()
    const maxX = Math.max(MIN_GAP, window.innerWidth - w - MIN_GAP)
    const maxY = Math.max(TOP_LIMIT, window.innerHeight - h - MIN_GAP)
    return {
      x: Math.min(Math.max(p.x, MIN_GAP), maxX),
      y: Math.min(Math.max(p.y, TOP_LIMIT), maxY)
    }
  }

  function defaultPos() {
    const { w, h } = dims()
    return { x: window.innerWidth - w - EDGE, y: window.innerHeight - h - 14 }
  }

  /** 靠近左右边缘 SNAP_RANGE 内吸附；并始终保证不出屏 */
  function snap() {
    const p = pos.value
    if (!p) return
    const { w } = dims()
    const mid = p.x + w / 2
    side.value = mid < window.innerWidth / 2 ? 'left' : 'right'
    const edgeL = EDGE
    const edgeR = window.innerWidth - w - EDGE
    let next = p
    if (Math.abs(p.x - edgeL) < SNAP_RANGE) next = { ...p, x: edgeL }
    else if (Math.abs(p.x - edgeR) < SNAP_RANGE) next = { ...p, x: edgeR }
    pos.value = clampPos(next)
  }

  const posStyle = computed(() => {
    if (!pos.value) return {}
    return {
      left: `${pos.value.x}px`,
      top: `${pos.value.y}px`,
      right: 'auto',
      bottom: 'auto'
    }
  })

  function save() {
    try {
      localStorage.setItem(
        MASCOT_STORE_KEY,
        JSON.stringify({ version: 3, pos: pos.value, side: side.value, ...getSnapshot() })
      )
    } catch (e) {
      // 隐私模式 / 配额满：不再静默吞掉，交给调用方给一句可见提示
      onStorageError?.()
    }
  }

  /**
   * 恢复状态。v2 存档没有 version / settings 字段，读出来后补齐默认值并回写成 v3，
   * 避免以后每次都走迁移分支，也避免新增设置项时拿到 undefined。
   */
  function load() {
    let raw = null
    let legacy = false
    try {
      raw = localStorage.getItem(MASCOT_STORE_KEY)
      if (!raw) {
        raw = localStorage.getItem(MASCOT_STORE_KEY_V2)
        legacy = !!raw
      }
    } catch (e) {
      onStorageError?.()
      return
    }
    if (!raw) return
    try {
      const s = JSON.parse(raw)
      if (!s || typeof s !== 'object') return
      // P0：恢复坐标后立刻按当前视口 clamp，换屏 / 小窗口也不会跑到屏外
      if (s.pos && typeof s.pos.x === 'number' && typeof s.pos.y === 'number') {
        pos.value = clampPos(s.pos)
      }
      side.value = s.side === 'left' ? 'left' : 'right'
      applySnapshot?.({
        mini: !!s.mini,
        muted: !!s.muted,
        settings: { ...DEFAULT_SETTINGS, ...(s.settings || {}) }
      })
      if (legacy) {
        save()
        try {
          localStorage.removeItem(MASCOT_STORE_KEY_V2)
        } catch (e) { /* 旧键删不掉不影响功能 */ }
      }
    } catch (e) {
      /* 存档损坏：忽略它，按默认状态启动，不阻断页面 */
    }
  }

  function reset() {
    pos.value = null
    side.value = 'right'
    save()
  }

  return { pos, side, dragging, dims, clampPos, defaultPos, snap, posStyle, save, load, reset }
}
