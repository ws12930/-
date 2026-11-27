/**
 * 让桌宠偶尔「说点真事」：接站内已有的未读通知 / 未读私信 / 签到状态接口。
 *
 * 台词原先全是静态随机池，跟博客里真实发生的事没有任何关系。这里只做轻量的
 * 只读轮询（不改任何后端契约、不发写请求），并且刻意做得很克制：
 * - 只在登录态下请求；未登录 / 接口报错一律静默，绝不打扰页面；
 * - 只在「数值增加」或「等级变化」时说话，避免每 60s 复读同一句；
 * - 每个路由最多 LIVE_MAX_PER_ROUTE 句、同类提醒有冷却、静音 / 收起 / 后台不说；
 * - 轮询挂在统一 timer registry 上，切后台跟着一起暂停。
 *
 * 防打扰是这一项的前提：桌宠是 fixed 浮层，弹得太勤就会盖住按钮（admin 页
 * 当初就是因为这个才整体不渲染），宁可少说，也不能让人想把它关掉。
 */
import { SESSION_TOKEN_KEY } from '../utils/constants'
import { ref } from 'vue'
import { getNotificationUnread } from '../api/notification'
import { getUnreadCount } from '../api/social'
import { isLogged } from '../utils/author'
import {
  LIVE_POLL_MS,
  LIVE_COOLDOWN_MS,
  LIVE_MAX_PER_ROUTE
} from './config'

const CHECKIN_TODAY_URL = '/api/checkin/today'

/** 后端 R<Map<String,Long>> 的 data 是 { count: n }；兼容直接给数字的情况 */
function readCount(res) {
  const d = res?.data
  if (typeof d === 'number') return d
  const n = Number(d?.count ?? d?.unread ?? 0)
  return Number.isFinite(n) ? n : 0
}

export function useMascotLive({ timers, speak, react, isMuted, isMini }) {
  const unread = ref({ notifications: 0, messages: 0 })
  let lastLevel = null
  let lastCheckinHintAt = 0
  let lastNotifHintAt = 0
  let lastMsgHintAt = 0
  let spokenThisRoute = 0
  let seen = false

  function canSpeak(lastAt) {
    if (isMuted.value || isMini.value || document.hidden) return false
    if (spokenThisRoute >= LIVE_MAX_PER_ROUTE) return false
    return Date.now() - lastAt >= LIVE_COOLDOWN_MS
  }

  /**
   * mood 是可选的情绪动作（wave / celebrate）。只在真的说出口时顺带播，
   * 被防打扰门禁挡掉时不播，避免「静音了还在手舞足蹈」。
   */
  function say(text, key, mood) {
    if (!canSpeak(key === 'checkin' ? lastCheckinHintAt : key === 'msg' ? lastMsgHintAt : lastNotifHintAt)) return
    const now = Date.now()
    if (key === 'checkin') lastCheckinHintAt = now
    else if (key === 'msg') lastMsgHintAt = now
    else lastNotifHintAt = now
    spokenThisRoute += 1
    speak(text)
    if (mood && typeof react === 'function') react(mood)
  }

  async function fetchCheckin() {
    const token = localStorage.getItem(SESSION_TOKEN_KEY) || ''
    if (!token) return null
    const res = await fetch(CHECKIN_TODAY_URL, {
      headers: { Authorization: 'Bearer ' + token },
      cache: 'no-store'
    })
    if (!res.ok) return null
    const body = await res.json().catch(() => null)
    return body?.code === 0 ? body.data : null
  }

  async function poll() {
    if (!isLogged()) return
    // 单个接口挂掉不影响其它两个，也不该把桌宠本身搞崩
    const [n, m, c] = await Promise.allSettled([
      getNotificationUnread(),
      getUnreadCount(),
      fetchCheckin()
    ])
    const notif = n.status === 'fulfilled' ? readCount(n.value) : unread.value.notifications
    const msg = m.status === 'fulfilled' ? readCount(m.value) : unread.value.messages
    const checkin = c.status === 'fulfilled' ? c.value : null

    // 首次快照只建立基线：用户刚进来时不因「本来就有 3 条未读」连环弹三句
    if (!seen) {
      seen = true
      unread.value = { notifications: notif, messages: msg }
      lastLevel = checkin?.level ?? null
      return
    }

    if (notif > unread.value.notifications) say('有人找你啦，去看看通知吧', 'notif', 'wave')
    else if (msg > unread.value.messages) say('收到新消息啦，要不要回一句？', 'msg', 'wave')
    unread.value = { notifications: notif, messages: msg }

    if (checkin) {
      const level = Number(checkin.level) || 0
      if (lastLevel != null && level > lastLevel) {
        say(`升级啦！现在是${checkin.title || `Lv.${level}`}，再写点什么？`, 'checkin', 'celebrate')
      } else if (checkin.checked === false && canSpeak(lastCheckinHintAt)) {
        const streak = Number(checkin.streak) || 0
        say(streak > 1 ? `已经连着签到 ${streak} 天啦，今天也顺手点一下吧` : '今天还没签到呢，顺手点一下吧', 'checkin')
      }
      lastLevel = level
    }
  }

  /** 由 scheduleAll() 调用：挂轮询并立刻取一次基线 */
  function start() {
    timers.interval(poll, LIVE_POLL_MS)
    poll()
  }

  /** 路由切换时重置「本页最多说几句」的计数 */
  function resetRouteCounter() {
    spokenThisRoute = 0
  }

  /** 登出后清掉快照，重新登录时按新用户重新建立基线 */
  function resetSession() {
    seen = false
    unread.value = { notifications: 0, messages: 0 }
    lastLevel = null
  }

  return { unread, start, resetRouteCounter, resetSession }
}
