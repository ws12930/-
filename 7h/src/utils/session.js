/**
 * 登录会话状态（全局单例）。
 *
 * 只负责「账号被封禁」这一件事：
 *   1. 清掉本地登录态（bn-token / bn-user）；
 *   2. 置起全局弹窗状态，由 components/BanNotice.vue 渲染；
 *   3. 定时巡检 /api/auth/me，做到管理员封禁后最多十几秒就自动登出，
 *      不用等用户去点页面、等下一个接口返回。
 *
 * 后端对应错误码：40301（见 com.fenglin.blog.common.BizException#banned）。
 */
import { reactive } from 'vue'

/** 与后端 BizException.BANNED_MESSAGE 保持一致 */
export const BANNED_MESSAGE = '该账号已被封禁。请联系管理员。'

/** 账号被封禁的错误码，普通 403（权限不足）不走登出逻辑 */
export const BANNED_CODE = 40301

/** 全局弹窗状态，App.vue 里挂 <BanNotice /> 读它 */
export const banNotice = reactive({
  visible: false,
  message: BANNED_MESSAGE
})

/** 判断后端返回体是不是「账号被封禁」 */
export function isBanned(data) {
  return !!data && data.code === BANNED_CODE
}

/**
 * 收到封禁响应时调用：清登录态 + 弹窗。
 * 并发请求可能同时命中，这里做成幂等，重复调用只是重复赋值。
 */
export function handleBanned(message) {
  localStorage.removeItem('bn-token')
  localStorage.removeItem('bn-user')
  banNotice.message = message || BANNED_MESSAGE
  banNotice.visible = true
}

/** 关闭弹窗（用户点「我知道了」） */
export function dismissBan() {
  banNotice.visible = false
}

/** 巡检间隔：后台标签页会被浏览器节流到 1 分钟一次，前台 15 秒 */
const HEARTBEAT_MS = 15000

let timer = null

async function probe() {
  const token = localStorage.getItem('bn-token')
  if (!token) return
  try {
    const res = await fetch('/api/auth/me', {
      headers: { Authorization: 'Bearer ' + token }
    })
    if (res.ok) return
    const data = await res.json().catch(() => null)
    if (isBanned(data)) handleBanned(data.message)
  } catch (e) {
    // 后端不可达时什么都不做：断网、重启服务不该把人踢下线
  }
}

/** 在 App.vue 挂载时调用一次，重复调用不会叠加多个定时器 */
export function startBanWatch() {
  if (timer) return
  probe()
  timer = setInterval(probe, HEARTBEAT_MS)
}
