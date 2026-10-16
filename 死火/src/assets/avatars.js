/**
 * 双阵营预设头像库
 * avatar 存储格式："阵营:序号"，例如 "ninja:0"、"shinigami:2"
 * 为空时回退到昵称首字 + 阵营默认配色
 */
export const AVATAR_PRESETS = [
  // 木叶忍者：火橙色系
  { id: 'ninja:0', faction: 'ninja', glyph: '涡', label: '漩涡', bg: 'linear-gradient(135deg, #ffd24a, #ff6a00)' },
  { id: 'ninja:1', faction: 'ninja', glyph: '忍', label: '忍者', bg: 'linear-gradient(135deg, #ffb347, #d95400)' },
  { id: 'ninja:2', faction: 'ninja', glyph: '狐', label: '九尾', bg: 'linear-gradient(135deg, #ff9040, #c0392b)' },
  { id: 'ninja:3', faction: 'ninja', glyph: '雷', label: '雷切', bg: 'linear-gradient(135deg, #ffe27a, #ff8c1a)' },
  // 代理死神：冰蓝色系
  { id: 'shinigami:0', faction: 'shinigami', glyph: '斩', label: '斩月', bg: 'linear-gradient(135deg, #b9f4ff, #0090ff)' },
  { id: 'shinigami:1', faction: 'shinigami', glyph: '死', label: '死神', bg: 'linear-gradient(135deg, #7fd8ff, #004e92)' },
  { id: 'shinigami:2', faction: 'shinigami', glyph: '卍', label: '卍解', bg: 'linear-gradient(135deg, #a5f3ff, #0066cc)' },
  { id: 'shinigami:3', faction: 'shinigami', glyph: '月', label: '月牙', bg: 'linear-gradient(135deg, #d7f9ff, #0077b6)' }
]

export const DEFAULT_PRESET = {
  ninja: AVATAR_PRESETS[0],
  shinigami: AVATAR_PRESETS[4]
}

/**
 * 解析 avatar 字符串 → 预设对象；图片地址返回 { image: url }
 *
 * 三种图片来源都要认：
 *   - 站外绝对地址 http(s)://
 *   - 站内上传路径 /uploads/年/月/xxx.png（后端 UploadController 的返回格式）
 *   - base64 内联 data:image
 * 只认绝对地址的话，自定义上传的头像会静默回退成预设字块。
 */
export function resolveAvatar(avatar, faction = 'ninja') {
  const raw = (avatar || '').trim()
  if (!raw) return DEFAULT_PRESET[faction] || DEFAULT_PRESET.ninja
  if (/^https?:\/\//i.test(raw) || raw.startsWith('data:image') || raw.startsWith('/')) {
    return { image: raw }
  }
  const matched = AVATAR_PRESETS.find((preset) => preset.id === raw)
  if (matched) return matched
  // 兼容历史数据：只存了阵营的情况
  return DEFAULT_PRESET[raw] || DEFAULT_PRESET[faction] || DEFAULT_PRESET.ninja
}

export function normalizeFaction(faction) {
  return faction === 'shinigami' ? 'shinigami' : 'ninja'
}
