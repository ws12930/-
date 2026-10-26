/**
 * 后端接口封装（Spring Boot: http://localhost:8081/api/auth）
 * 开发期由 vite 代理 /api -> 8081，生产部署时改为同源即可。
 */
const BASE = '/api/auth'

const TOKEN_KEY = 'bn-token'

async function request(path, { method = 'GET', body } = {}) {
  let res
  try {
    res = await fetch(BASE + path, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined
    })
  } catch (e) {
    throw new Error('无法连接后端服务，请确认 springBootTest 已启动（端口 8081）')
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

/** 注册 */
export function register(payload) {
  return request('/register', { method: 'POST', body: payload })
}

/** 登录：成功后会把访问令牌写入本地，供博客写接口使用 */
export function login(payload) {
  return request('/login', { method: 'POST', body: payload }).then((data) => {
    if (data?.data?.token) {
      localStorage.setItem(TOKEN_KEY, data.data.token)
    }
    return data
  })
}

/** 当前登录用户资料（令牌还在本地、但用户信息丢了时用它补全） */
export function getMe() {
  return request('/me')
}

/** 当前登录用户的访问令牌 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 清除本地登录态（不含后端令牌吊销，服务端令牌到期后自动失效） */
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/** 用户名是否可用 */
export function checkUsername(username) {
  return request(`/check-username?username=${encodeURIComponent(username)}`)
}

/** 找回密码第一步：校验用户名 + 邮箱 */
export function verifyIdentity(payload) {
  return request('/forgot/verify', { method: 'POST', body: payload })
}

/** 找回密码第二步：凭令牌重置密码 */
export function resetPassword(payload) {
  return request('/forgot/reset', { method: 'POST', body: payload })
}

/** 健康检查 */
export function ping() {
  return request('/ping')
}
