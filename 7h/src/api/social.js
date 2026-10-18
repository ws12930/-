/**
 * 社交接口封装：资料 / 用户主页 / 关注 / 点赞 / 私信
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

/* ===== 资料 ===== */

/** 更新当前用户资料：昵称 / 头像 / 签名 / 头像配色主题 */
export function updateProfile(payload) {
  return request('/auth/profile', { method: 'PUT', body: payload })
}

/* ===== 用户 ===== */

/** 用户公开主页 */
export function getUserProfile(userId) {
  return request(`/users/${userId}`)
}

/** 按昵称或用户名搜索用户 */
export function searchUsers(keyword, limit = 10) {
  return request(`/users/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`)
}

/** 博客社区：全部活跃博主（按粉丝数排序） */
export function listCommunity(limit = 100) {
  return request(`/community/users?limit=${limit}`)
}

/** 某用户已发布的文章 */
export function listUserPosts(userId, current = 1, size = 10) {
  return request(`/users/${userId}/posts?current=${current}&size=${size}`)
}

/** 关注 / 取关切换 */
export function toggleFollow(userId) {
  return request(`/users/${userId}/follow`, { method: 'POST' })
}

export function listFollowing(userId) {
  return request(`/users/${userId}/following`)
}

export function listFollowers(userId) {
  return request(`/users/${userId}/followers`)
}

/* ===== 点赞 ===== */

/** 点赞 / 取消点赞切换 */
export function toggleLike(postId) {
  return request(`/posts/${postId}/like`, { method: 'POST' })
}

/* ===== 私信 ===== */

export function listConversations() {
  return request('/messages')
}

export function getUnreadCount() {
  return request('/messages/unread')
}

export function listMessages(peerId, limit = 100) {
  return request(`/messages/${peerId}?limit=${limit}`)
}

export function sendMessage(peerId, content) {
  return request(`/messages/${peerId}`, { method: 'POST', body: { content } })
}
