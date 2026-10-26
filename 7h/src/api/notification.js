/**
 * 消息通知接口封装：别人评论/回复/点赞/关注我的提醒
 * 开发期由 vite 代理 /api -> 8081
 */
const BASE = '/api'

function getToken() {
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

  if (!res.ok || data.code !== 0) {
    throw new Error(data.message || '请求失败，请稍后重试')
  }
  return data
}

/** 通知列表（最近 100 条） */
export function listNotifications() {
  return request('/notifications')
}

/** 未读数 */
export function getNotificationUnread() {
  return request('/notifications/unread')
}

/** 全部标记已读，返回 { updated } */
export function markAllNotificationsRead() {
  return request('/notifications/read', { method: 'POST' })
}

/** 单条标记已读 */
export function markNotificationRead(id) {
  return request(`/notifications/${id}/read`, { method: 'POST' })
}
