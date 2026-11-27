/**
 * 帧素材的加载与解码缓存（预载 / 播放器共用）。
 *
 * 改造前有两个独立缓存：`preloadMotion()` 用 `new Image()` 逐张预热，
 * `MascotSprite` 内部又自己维护一份 `imgCache` —— 同一张图会被解码两次，
 * 而且预热只发生在 `requestIdleCallback` 里，首屏切换动作仍有空窗。
 *
 * 现在统一到这里，参考 dsh-pet 的素材管线与 Web 平台能力：
 * - `createImageBitmap()` 优先：解码可以离开主线程，播放器只剩 drawImage；
 *   不支持时回退 `<img>`（Safari 14 及以下 / 老内核）。
 * - 同一 URL 只请求 / 解码一次，预载与播放器共用同一个 Promise 缓存。
 * - 分两批预热：核心动作（待机 / 走动 / 跳跃 / 拖拽 / 落地）优先，
 *   情绪动作（招手 / 庆祝 / 发呆 / 沮丧 / 睡觉）等页面真正空闲再补；
 *   `navigator.connection.saveData` 或 2G 网络下只预热 idle。
 * - 失败重试一次（800ms 后），仍失败就记住 URL，避免反复请求拖慢首屏。
 * - GIF 不在这里解码：交给 MascotSprite 的 gifuct-js 分支（保持原有能力）。
 */
import { sheetUrl } from './animations'

/** url -> Promise<ImageBitmap | HTMLImageElement | null> */
const cache = new Map()
/** 确认加载失败的 URL（不再重试） */
const failed = new Set()

const CORE_SHEETS = ['idle', 'walk-r', 'walk-l', 'jump', 'drag', 'land']
const EXTRA_SHEETS = ['sleep', 'wave', 'celebrate', 'think', 'failed']

function isGif(url) {
  return /\.gif($|\?)/i.test(url)
}

function loadImageEl(url) {
  return new Promise((resolve) => {
    if (typeof Image === 'undefined') return resolve(null)
    const im = new Image()
    im.decoding = 'async'
    im.onload = () => resolve(im)
    im.onerror = () => resolve(null)
    im.src = url
  })
}

/** 读一次，失败后隔一会儿重试一次；两次都失败才记入 failed */
async function decodeOnce(url) {
  if (typeof fetch === 'function' && typeof createImageBitmap === 'function') {
    try {
      const resp = await fetch(url, { cache: 'force-cache' })
      if (resp.ok) {
        const blob = await resp.blob()
        return await createImageBitmap(blob)
      }
    } catch (e) {
      /* 跨域 / file:// / 解码失败：走 <img> 兜底 */
    }
  }
  return loadImageEl(url)
}

function decode(url) {
  if (failed.has(url)) return Promise.resolve(null)
  if (isGif(url)) return Promise.resolve(null)
  return decodeOnce(url).then(async (first) => {
    if (first) return first
    await new Promise((r) => setTimeout(r, 800))
    const second = await decodeOnce(url)
    if (!second) failed.add(url)
    return second
  })
}

/**
 * 取一张帧条。返回值可以直接 drawImage（ImageBitmap / HTMLImageElement）。
 * 失败返回 null，播放器保持上一帧画面即可，不抛异常。
 */
export function loadSheet(url) {
  if (!url) return Promise.resolve(null)
  if (!cache.has(url)) {
    cache.set(url, decode(url).catch(() => null))
  }
  return cache.get(url)
}

/** 是否已经进过缓存（用于判断「预热是否真的完成」） */
export function sheetLoaded(url) {
  return cache.has(url)
}

function lowDataMode() {
  try {
    const c = navigator.connection || navigator.mozConnection || navigator.webkitConnection
    if (!c) return false
    if (c.saveData) return true
    return /(^|-)2g$/.test(String(c.effectiveType || ''))
  } catch (e) {
    return false
  }
}

function onIdle(fn) {
  const idle = window.requestIdleCallback || ((cb) => window.setTimeout(cb, 2500))
  try {
    idle(fn, { timeout: 6000 })
  } catch (e) {
    fn()
  }
}

let preloadedFace = ''
let preloadToken = 0

/**
 * 预热某个表情的动作帧条。
 * @param {string} faceKey welcome / nofight / writing
 * @param {{ force?: boolean, coreOnly?: boolean }} opts
 *   force    同一个表情也重新跑一次（换素材调试时用）
 *   coreOnly 只预热核心动作（首屏想更快时用）
 */
export function preloadMotion(faceKey, { force = false, coreOnly = false } = {}) {
  if (!faceKey) return
  if (!force && preloadedFace === faceKey) return
  preloadedFace = faceKey
  const token = ++preloadToken

  const sheets = coreOnly || lowDataMode()
    ? ['idle', ...CORE_SHEETS.filter((s) => s !== 'idle').slice(0, 2)]
    : [...CORE_SHEETS, ...EXTRA_SHEETS]

  onIdle(async () => {
    for (const sheet of sheets) {
      if (token !== preloadToken) return // 已经换表情了，别再为旧表情烧流量
      const url = sheetUrl(faceKey, sheet)
      if (!url) continue
      await loadSheet(url)
    }
  })
}

/** 立即加载当前表情的 idle（首屏只等这一张），首屏不需要 requestIdleCallback */
export function preloadPrimary(faceKey) {
  const url = sheetUrl(faceKey, 'idle')
  return loadSheet(url)
}

/** 测试 / 调试用：清空缓存 */
export function clearSheetCache() {
  cache.clear()
  failed.clear()
  preloadedFace = ''
  preloadToken += 1
}
