/**
 * 管理后台接口封装（仅 ADMIN 可用，后端由 @RequireAdmin 把关）
 */
import { SESSION_TOKEN_KEY } from '../utils/constants'
import { handleBanned, isBanned } from '../utils/session'

const BASE = '/api'

export function getToken() {
  return localStorage.getItem(SESSION_TOKEN_KEY) || ''
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

function toQuery(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  })
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

/** 全站文章（含草稿） */
export function adminPosts(params) {
  return request('/admin/posts' + toQuery(params))
}

/** 用户列表 */
export function adminUsers(params) {
  return request('/admin/users' + toQuery(params))
}

/** 启用 / 禁用账号：status 1 正常 / 0 禁用 */
export function setUserStatus(userId, status) {
  return request(`/admin/users/${userId}/status?status=${status}`, { method: 'PUT' })
}

/** 升级 / 取消管理员权限：role 取 ADMIN / USER */
export function setUserRole(userId, role) {
  return request(`/admin/users/${userId}/role?role=${role}`, { method: 'PUT' })
}

/** 举报列表：status 取 PENDING / HANDLED / IGNORED / 空（全部） */
export function adminReports(params) {
  return request('/admin/reports' + toQuery(params))
}

/** 处理举报：status 取 HANDLED / IGNORED */
export function handleReport(reportId, status) {
  return request(`/admin/reports/${reportId}/status?status=${status}`, { method: 'PUT' })
}

/* ==================== 评论审核 ==================== */

/** 评论列表：status 取 ALL / NORMAL / HIDDEN，keyword 匹配内容 */
export function adminComments(params) {
  return request('/admin/comments' + toQuery(params))
}

/** 审核评论：HIDDEN 隐藏下架 / NORMAL 恢复展示 */
export function setCommentStatus(commentId, status) {
  return request(`/admin/comments/${commentId}/status?status=${status}`, { method: 'PUT' })
}

/** 删除评论 */
export function deleteComment(commentId) {
  return request(`/admin/comments/${commentId}`, { method: 'DELETE' })
}

/* ==================== 签到 ==================== */

/** 签到记录：keyword 匹配昵称 / 用户名 */
export function adminCheckins(params) {
  return request('/admin/checkins' + toQuery(params))
}

/** 签到概览 */
export function adminCheckinSummary() {
  return request('/admin/checkins/summary')
}

/* ==================== 私信管理 ==================== */

/** 全站私信：keyword 匹配正文或双方昵称 / 用户名 */
export function adminMessages(params) {
  return request('/admin/messages' + toQuery(params))
}

/** 私信概览：{ total, today, unread } */
export function adminMessageSummary() {
  return request('/admin/messages/summary')
}

/** 删除一条私信（违规内容处理） */
export function deleteAdminMessage(messageId) {
  return request(`/admin/messages/${messageId}`, { method: 'DELETE' })
}

/* ==================== 社区（吧）管理 ==================== */

/** 全部吧：含吧主、帖子数与启用状态 */
export function adminCategories(params) {
  return request('/admin/categories' + toQuery(params))
}

/** 启用 / 停用吧：status 1 启用 / 0 停用 */
export function setCategoryStatus(categoryId, status) {
  return request(`/admin/categories/${categoryId}/status?status=${status}`, { method: 'PUT' })
}

/** 修改吧信息：管理员可以改名 */
export function updateAdminCategory(categoryId, name, description) {
  return request(`/admin/categories/${categoryId}`, {
    method: 'PUT',
    body: { name, description }
  })
}

/** 删除吧：吧下还有帖子时后端会拒绝 */
export function deleteAdminCategory(categoryId) {
  return request(`/admin/categories/${categoryId}`, { method: 'DELETE' })
}
