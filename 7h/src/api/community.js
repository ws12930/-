/**
 * 社区接口封装（Spring Boot: http://192.168.68.77:8081/api）
 * 覆盖签到、建吧 / 改吧、举报提交。
 * 开发期由 vite 代理 /api -> 8081，生产由 start.py 反代，页面只用相对路径。
 */
import { handleBanned, isBanned } from '../utils/session'

const BASE = '/api'

export function getToken() {
  return localStorage.getItem('bn-token') || ''
}

function authHeaders() {
  const token = getToken()
  return token ? { Authorization: 'Bearer ' + token } : {}
}

async function request(path, { method = 'GET', body } = {}) {
  let res
  try {
    res = await fetch(BASE + path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders()
      },
      body: body ? JSON.stringify(body) : undefined
    })
  } catch (e) {
    throw new Error('无法连接后端服务，请确认博客后端已启动（端口 8081）')
  }

  let data = null
  try {
    data = await res.json()
  } catch (e) {
    throw new Error('后端返回内容无法解析')
  }

  // 账号被封禁（40301）：清登录态并弹窗
  if (isBanned(data)) handleBanned(data.message)

  if (!res.ok || data.code !== 0) {
    throw new Error(data.message || '请求失败，请稍后重试')
  }
  return data
}

/* ==================== 签到 ==================== */

/** 今日签到状态 + 等级 / 经验进度：{ level, title, exp, levelStartExp, nextLevelExp, streak, checked, gained } */
export function checkinToday() {
  return request('/checkin/today')
}

/** 签到（+5 经验）；重复签到后端会返回业务错误 */
export function doCheckin() {
  return request('/checkin', { method: 'POST' })
}

/* ==================== 吧 ==================== */

/** 建吧：创建者自动成为吧主 */
export function createBa(name, description) {
  return request('/categories', { method: 'POST', body: { name, description } })
}

/** 改吧：仅吧主或管理员 */
export function updateBa(id, name, description) {
  return request(`/categories/${id}`, { method: 'PUT', body: { name, description } })
}

/* ==================== 举报 ==================== */

/** 举报内容：payload = { targetType: POST|COMMENT|USER, targetId, reason, detail } */
export function submitReport(payload) {
  return request('/reports', { method: 'POST', body: payload })
}
