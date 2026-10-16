/**
 * 私信自定义表情包（存浏览器本地，仅本机可见）。
 *
 * 存的是上传后的图片地址而不是 base64：
 * blog_message.content 只有 varchar(1000)，base64 一张图就能撑爆，
 * 而 /uploads/年/月/xxx.png 这种地址只有几十个字符。
 */

const STORAGE_KEY = 'bn-stickers'
const MAX_COUNT = 24

function read() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const list = JSON.parse(raw)
    return Array.isArray(list) ? list.filter((s) => s && s.url) : []
  } catch (e) {
    return []
  }
}

function write(list) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch (e) {
    // 存储写满或隐私模式下失败时不影响聊天功能
  }
}

export function loadStickers() {
  return read()
}

/**
 * 新增一个表情。
 * @returns {{ok: boolean, list: Array, message?: string}}
 */
export function addSticker(name, url) {
  const list = read()
  if (list.length >= MAX_COUNT) {
    return { ok: false, list, message: `表情包最多 ${MAX_COUNT} 个` }
  }
  if (list.some((s) => s.url === url)) {
    return { ok: false, list, message: '这个表情已经添加过了' }
  }
  const next = [...list, { id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`, name, url }]
  write(next)
  return { ok: true, list: next }
}

export function removeSticker(id) {
  const next = read().filter((s) => s.id !== id)
  write(next)
  return next
}
