/**
 * 自定义表情包。
 *
 * 存储策略：**账号优先，浏览器缓存兜底**。
 *   1. 登录状态下，列表以服务端（blog_sticker 表）为准，换设备 / 清缓存都不会丢；
 *   2. 本机 localStorage 只作为离线缓存和「迁移源」——首次登录时把本地已有表情
 *      批量导入账号，之后缓存就是服务端列表的镜像；
 *   3. 未登录（或后端不可用）时退化为纯本地，行为与旧版一致。
 *
 * 图片本体由后端 /uploads/** 提供，这里只存引用。
 */
import {
  listStickers,
  addSticker as apiAdd,
  importStickers as apiImport,
  deleteSticker as apiDelete,
  getToken
} from '../api/stickers'

const STORAGE_KEY = '7h-stickers'
export const MAX_COUNT = 30

/** 读本地缓存（同步，供渲染直接使用） */
export function loadStickers() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const list = JSON.parse(raw)
    if (!Array.isArray(list)) return []
    return list.filter((s) => s && typeof s.url === 'string' && s.url).slice(0, MAX_COUNT)
  } catch {
    return []
  }
}

function save(list) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch {
    /* 隐私模式下 localStorage 可能不可写，忽略即可 */
  }
  return list
}

function isLoggedIn() {
  return !!getToken()
}

/**
 * 与账号同步：把本地独有的表情导入服务端，再用服务端列表刷新缓存。
 * 失败时静默返回本地缓存（离线可用）。
 */
export async function syncStickers() {
  const local = loadStickers()
  if (!isLoggedIn()) return local
  try {
    let server = (await listStickers()).data || []
    const localOnly = local.filter((s) => s.url && !server.some((x) => x.url === s.url))
    if (localOnly.length) {
      const merged = await apiImport(
        localOnly.slice(0, MAX_COUNT).map((s) => ({ name: s.name || '表情', url: s.url }))
      )
      server = merged.data || server
    }
    return save(server)
  } catch (e) {
    return local
  }
}

/**
 * 新增一个表情。
 * @returns {Promise<{ok:boolean, list:Array, message:string}>}
 */
export async function addSticker(name, url) {
  const local = loadStickers()
  const cleanedUrl = (url || '').trim()
  if (!cleanedUrl) {
    return { ok: false, list: local, message: '缺少表情图片地址' }
  }
  if (local.some((s) => s.url === cleanedUrl)) {
    return { ok: false, list: local, message: '这个表情已经添加过了' }
  }
  if (local.length >= MAX_COUNT) {
    return { ok: false, list: local, message: `最多只能存 ${MAX_COUNT} 个表情` }
  }

  const label = (name || '表情').trim() || '表情'

  if (isLoggedIn()) {
    try {
      const res = await apiAdd(label, cleanedUrl)
      return { ok: true, list: save(res.data || local), message: '' }
    } catch (e) {
      // 服务端失败时不再写本地，避免出现「本机有、账号没有」的脏数据
      return { ok: false, list: local, message: e.message || '表情保存失败' }
    }
  }

  const next = save([...local, { id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, name: label, url: cleanedUrl }])
  return { ok: true, list: next, message: '' }
}

/** 删除一个表情，返回删除后的列表 */
export async function removeSticker(id) {
  const local = loadStickers()
  const target = local.find((s) => String(s.id) === String(id))
  const rest = local.filter((s) => String(s.id) !== String(id))

  // 服务端记录（id 为数字）需要同步删除；本地临时 id 只清缓存即可
  if (target && isLoggedIn() && /^\d+$/.test(String(target.id))) {
    try {
      const res = await apiDelete(target.id)
      return save(res.data || rest)
    } catch (e) {
      return local
    }
  }
  return save(rest)
}
