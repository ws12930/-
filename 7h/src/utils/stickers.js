/**
 * 自定义表情包：存在本机 localStorage，不占后端资源。
 * 上传后的图片地址由后端 /uploads/** 提供，这里只存引用。
 */

const STORAGE_KEY = '7h-stickers'
const MAX_COUNT = 30

/** @returns {{id:string,name:string,url:string}[]} */
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
}

/**
 * 新增一个表情
 * @returns {{ok:boolean, list:Array, message:string}}
 */
export function addSticker(name, url) {
  const list = loadStickers()
  if (list.some((s) => s.url === url)) {
    return { ok: false, list, message: '这个表情已经添加过了' }
  }
  if (list.length >= MAX_COUNT) {
    return { ok: false, list, message: `最多只能存 ${MAX_COUNT} 个表情` }
  }
  const next = [...list, { id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, name: name || '表情', url }]
  save(next)
  return { ok: true, list: next, message: '' }
}

/** 删除一个表情，返回删除后的列表 */
export function removeSticker(id) {
  const next = loadStickers().filter((s) => s.id !== id)
  save(next)
  return next
}
