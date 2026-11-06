/**
 * 自定义表情接口封装（Spring Boot: http://192.168.68.77:8081/api/stickers）
 *
 * 所有接口都会返回「最新完整列表」，调用方拿到 data 直接覆盖本地缓存即可。
 * 具体同步策略见 utils/stickers.js。
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

/** 我的表情列表 */
export function listStickers() {
  return request('/stickers')
}

/** 新增一个表情 */
export function addSticker(name, url) {
  return request('/stickers', { method: 'POST', body: { name, url } })
}

/** 批量导入本机缓存的表情，list = [{ name, url }] */
export function importStickers(list) {
  return request('/stickers/import', { method: 'POST', body: { stickers: list } })
}

/** 删除一个表情 */
export function deleteSticker(id) {
  return request(`/stickers/${id}`, { method: 'DELETE' })
}
