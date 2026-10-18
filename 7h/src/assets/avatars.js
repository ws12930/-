/**
 * 预设头像库（暖色 / 冷色两套配色）
 *
 * avatar 存储格式："主题:序号"，例如 "ninja:0"、"shinigami:2"。
 * 这里的 ninja / shinigami 只作为两套配色主题的键名存在（暖橙 / 冰蓝），
 * 界面上不再有阵营概念，也不展示任何阵营文案。
 * 为空时回退到昵称首字 + 暖色默认配色。
 */
export const AVATAR_PRESETS = [
  // 暖色系：橙金
  { id: 'ninja:0', faction: 'ninja', glyph: '枫', label: '枫叶', bg: 'linear-gradient(135deg, #dce6e1, #57836d)' },
  { id: 'ninja:1', faction: 'ninja', glyph: '焰', label: '焰火', bg: 'linear-gradient(135deg, #b3cabc, #41584c)' },
  { id: 'ninja:2', faction: 'ninja', glyph: '曦', label: '晨曦', bg: 'linear-gradient(135deg, #a8c1b4, #4a7f66)' },
  { id: 'ninja:3', faction: 'ninja', glyph: '阳', label: '暖阳', bg: 'linear-gradient(135deg, #e4ebe7, #8fd0ae)' },
  // 冷色系：冰蓝
  { id: 'shinigami:0', faction: 'shinigami', glyph: '汐', label: '潮汐', bg: 'linear-gradient(135deg, #d5e3dc, #0090ff)' },
  { id: 'shinigami:1', faction: 'shinigami', glyph: '澜', label: '微澜', bg: 'linear-gradient(135deg, #a9c9ba, #25332c)' },
  { id: 'shinigami:2', faction: 'shinigami', glyph: '渊', label: '深澜', bg: 'linear-gradient(135deg, #c6d8cf, #4a5f54)' },
  { id: 'shinigami:3', faction: 'shinigami', glyph: '霁', label: '霁月', bg: 'linear-gradient(135deg, #e2eae6, #52695e)' }
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
  // 兼容历史数据：只存了主题键的情况
  return DEFAULT_PRESET[raw] || DEFAULT_PRESET[faction] || DEFAULT_PRESET.ninja
}

/** 归一化配色主题键：非冷色一律按暖色处理 */
export function normalizeFaction(faction) {
  return faction === 'shinigami' ? 'shinigami' : 'ninja'
}
