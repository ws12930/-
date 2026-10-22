/**
 * 博客接口封装（Spring Boot: http://localhost:8081/api）
 * 开发期由 vite 代理 /api -> 8081，生产部署时改为同源即可。
 */
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

  // 后端统一返回体：{ code:0 成功 | 非0 业务失败, message, data }
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

/** 已发布文章分页 */
export function listPosts(params) {
  return request('/posts' + toQuery(params))
}

/** 当前用户的文章（含草稿） */
export function listMyPosts(params) {
  return request('/posts/mine' + toQuery(params))
}

/** 文章详情 */
export function getPostBySlug(slug) {
  return request(`/posts/slug/${encodeURIComponent(slug)}`)
}

/** 分类列表 */
export function listCategories() {
  return request('/categories')
}

/** 标签列表 */
export function listTags() {
  return request('/tags')
}

/** 评论列表 */
export function listComments(postId) {
  return request(`/posts/${postId}/comments`)
}

/** 发布评论 */
export function createComment(postId, payload) {
  return request(`/posts/${postId}/comments`, { method: 'POST', body: payload })
}

/** 删除评论 */
export function deleteComment(commentId) {
  return request(`/comments/${commentId}`, { method: 'DELETE' })
}

/** 新建文章 */
export function createPost(payload) {
  return request('/posts', { method: 'POST', body: payload })
}

/** 编辑文章 */
export function updatePost(postId, payload) {
  return request(`/posts/${postId}`, { method: 'PUT', body: payload })
}

/** 删除文章 */
export function deletePost(postId) {
  return request(`/posts/${postId}`, { method: 'DELETE' })
}

/** 置顶 / 取消置顶（作者本人或管理员） */
export function setPinned(postId, pinned) {
  return request(`/posts/${postId}/pin`, { method: 'POST', body: { pinned: !!pinned } })
}
