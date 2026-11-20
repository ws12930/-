/**
 * 站内游戏的地址解析 + 探活。
 *
 * 为什么单独抽一个文件：游戏产物不打进仓库（几个 MB 的静态包），
 * 地址随时可能换 —— 本地调试、WorkBuddy 分享链接、自己扔到 public/ 下同源托管，
 * 三种情况都要能跑，所以地址按「三层优先级」解析，改地址不用改组件代码。
 *
 *   1. 运行时注入（最高优先级）：index.html 里的
 *        <script>window.__7H_GAME_URL__ = './game/'</script>
 *      留空字符串 = 当作没配，继续往下找。线上要换链接直接改 dist/index.html，
 *      不用重新打包。
 *   2. 构建时注入：.env.local / .env.production 里的 VITE_GAME_URL、VITE_RIFT_URL。
 *   3. 代码默认值：同源静态托管目录（public/game/、public/rift-clash/）。
 *      这是最稳的做法 —— 同源就没有 CORS、没有链接过期、探活也读得到状态码。
 *
 * 两个游戏各有一套配置：
 *   「七日回响」Galgame  → resolveGameUrl()
 *   「裂锋对决」格斗      → resolveRiftUrl()
 */

/** 同源托管目录（public/ 下的静态目录，打包后原样出现在 dist 根） */
const DEFAULT_GAME_URL = './game/'
const DEFAULT_RIFT_URL = './rift-clash/'

/** 探活最多等多久：超过就按「判断不了」放行，不能让用户对着面板干等 */
const PROBE_TIMEOUT_MS = 4000

/** 安全读 window 上的注入值：SSR / 测试环境里没有 window 也不能炸 */
function readGlobal(key) {
  try {
    if (typeof window === 'undefined') return ''
    const value = window[key]
    return typeof value === 'string' ? value.trim() : ''
  } catch {
    return ''
  }
}

/** 读构建时注入的环境变量；变量不存在时 import.meta.env 上是 undefined，不是 '' */
function readEnv(key) {
  try {
    const value = import.meta.env?.[key]
    return typeof value === 'string' ? value.trim() : ''
  } catch {
    return ''
  }
}

/** 三层优先级：运行时注入 → 构建时注入 → 代码默认值 */
function pick(globalKey, envKey, fallback) {
  return readGlobal(globalKey) || readEnv(envKey) || fallback
}

/** 「七日回响」地址 */
export function resolveGameUrl() {
  return pick('__7H_GAME_URL__', 'VITE_GAME_URL', DEFAULT_GAME_URL)
}

/** 「裂锋对决」地址 */
export function resolveRiftUrl() {
  return pick('__7H_RIFT_URL__', 'VITE_RIFT_URL', DEFAULT_RIFT_URL)
}

/**
 * 把相对地址补成绝对地址。
 * 必须用 document.baseURI 解析：站点的 base 是 './'，
 * 用户当前所在的 hash 路由不会影响解析结果，但拼错一个斜杠就会 404。
 */
export function absolutize(url) {
  const raw = (url || '').trim()
  if (!raw) return ''
  try {
    return new URL(raw, typeof document === 'undefined' ? undefined : document.baseURI).href
  } catch {
    return raw
  }
}

/**
 * 是否同源。同源才敢读 iframe 内部（GameStage 靠这个把「链接失效」翻成站内提示），
 * 跨源时浏览器一律抛 SecurityError。
 */
export function isSameOrigin(url) {
  const abs = absolutize(url)
  if (!abs || typeof location === 'undefined') return false
  try {
    return new URL(abs).origin === location.origin
  } catch {
    return false
  }
}

/**
 * 探一下游戏链接还活着没有。
 *
 * 返回值约定（GameStage 依赖它区分三种情况）：
 *   { ok: true }        链接正常
 *   { ok: false, status } 明确挂了（403/404/410 = 链接失效，其余 = 服务端报错）
 *   { ok: null }        判断不了 —— 跨源读不到状态、超时、断网
 *
 * 为什么需要 ok: null 这一档：探活失败不等于游戏挂了。
 * 很多站点的 HEAD 不实现 / 被 WAF 拦 / 跨源没给 CORS 头，
 * 这些情况下把入口整个判死，用户就永远打不开游戏了，所以一律放行。
 */
export async function probeGameUrl(url) {
  const abs = absolutize(url)
  if (!abs) return { ok: false, status: 404 }

  const sameOrigin = isSameOrigin(abs)
  const controller = typeof AbortController === 'function' ? new AbortController() : null
  const timer = setTimeout(() => controller?.abort(), PROBE_TIMEOUT_MS)
  // 跨源时不要带 credentials，避免对端没开 ACAO-credentials 直接判失败
  const base = {
    method: 'HEAD',
    cache: 'no-store',
    redirect: 'follow',
    signal: controller?.signal,
    ...(sameOrigin ? { credentials: 'same-origin' } : { credentials: 'omit' })
  }

  try {
    let resp = await fetch(abs, base)
    // 有些静态服务器（含部分 Java 静态资源映射）不认 HEAD，会回 405/501，
    // 这时候用 GET 再试一次，别把「不支持 HEAD」当成「链接失效」
    if (resp.status === 405 || resp.status === 501) {
      resp = await fetch(abs, { ...base, method: 'GET', headers: { Range: 'bytes=0-0' } })
    }
    if (resp.ok || resp.type === 'opaque') return { ok: true, status: resp.status || 200 }
    return { ok: false, status: resp.status }
  } catch (err) {
    // AbortError = 超时，TypeError = 跨源被 CORS 挡 / 断网。
    // 跨源的 403、404 在浏览器里同样表现为 TypeError，读不到状态码，
    // 所以这里只能给 null（放行），不能武断判死。
    if (err?.name === 'AbortError') return { ok: null, status: 0 }
    return { ok: null, status: 0 }
  } finally {
    clearTimeout(timer)
  }
}
