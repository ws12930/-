/**
 * 轻量富文本渲染：支持 markdown 子集（图片 / 链接 / 粗体 / 行内代码 / 换行）。
 *
 * 安全策略（重要）：
 * 1. 处理顺序是「先提取链接 → 再整体转义」。
 *    如果反过来（先转义再解析），就没法区分用户真实输入的 `&` 和伪造的 `&quot;`，
 *    会导致像 https://x.com?a=1&b=2 这种正常 URL 被误判。
 * 2. 所有 URL 过白名单：仅允许 http(s)、mailto 和站内绝对路径（如 /uploads/...）。
 *    javascript:、data:、vbscript:、"//evil.com" 一律拒绝。
 * 3. 非链接部分整体 HTML 转义，所以用户写 <script> 只会显示成文本。
 * 4. 链接 / 图片先替换成占位符，等其余语法处理完再还原，
 *    避免后续正则改坏已经生成好的 href。
 */

const ESCAPE_MAP = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
}

const ESCAPE_MAP_RE = /[&<>"']/g

/** 控制字符一并剔除，防止用户伪造内部占位符 */
const CONTROL_CHARS = /[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g

function escapeHtml(input) {
  return String(input).replace(ESCAPE_MAP_RE, (c) => ESCAPE_MAP[c])
}

/**
 * URL 白名单校验，不通过返回 null。
 * 入参是「原始文本」里取出的 URL（尚未转义），因此可以放心拒绝字面实体。
 */
function safeUrl(raw) {
  const url = String(raw).trim()
  if (!url) return null
  // 合法 URL 不含空白与引号类字符
  if (/[\s"'<>`\\]/.test(url)) return null
  // 字面实体是属性闭合攻击的常见手法，直接拒绝
  if (/&(quot|#39|#x27|apos|lt|gt);/i.test(url)) return null
  if (/^(https?:\/\/|mailto:)/i.test(url)) return url
  // 站内绝对路径；"//host" 这种协议相对写法要挡掉
  if (/^\/[^/]/.test(url)) return url
  return null
}

/** 图片：![alt](/uploads/x.png) */
const IMAGE_RE = /!\[([^\]]*)\]\(([^)\s]+)\)/g
/** 链接：[文字](https://...) */
const LINK_RE = /\[([^\]]+)\]\(([^)\s]+)\)/g
/** 裸链接：https://... */
const BARE_LINK_RE = /(^|[\s(（【])(https?:\/\/[^\s]+)/g
/** 行内代码：`code` */
const CODE_RE = /`([^`]+)`/g
/** 粗体：**text** */
const BOLD_RE = /\*\*([^*]+)\*\*/g

const PLACEHOLDER_RE = /\u0000(\d+)\u0000/g

function anchor(url, label) {
  return `<a href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${label}</a>`
}

export function renderRichText(text) {
  if (!text) return ''

  const tokens = []
  const stash = (html) => {
    tokens.push(html)
    return `\u0000${tokens.length - 1}\u0000`
  }

  // 先去掉控制字符，避免用户伪造占位符
  let out = String(text).replace(CONTROL_CHARS, '')

  // ---- 第 1 步：在原始文本上提取图片 / 链接（此时 URL 还是原样，判断最准确）----

  out = out.replace(IMAGE_RE, (whole, alt, url) => {
    const safe = safeUrl(url)
    if (!safe) return whole
    return stash(`<img src="${escapeHtml(safe)}" alt="${escapeHtml(alt)}" loading="lazy">`)
  })

  out = out.replace(LINK_RE, (whole, label, url) => {
    const safe = safeUrl(url)
    if (!safe) return whole
    return stash(anchor(safe, escapeHtml(label)))
  })

  out = out.replace(BARE_LINK_RE, (whole, prefix, url) => {
    const safe = safeUrl(url)
    if (!safe) return whole
    return prefix + stash(anchor(safe, escapeHtml(safe)))
  })

  // ---- 第 2 步：剩下的普通文本整体转义 ----
  // 占位符是 \u0000N\u0000，不在转义表里，会原样保留
  out = escapeHtml(out)

  // ---- 第 3 步：处理行内语法（此时内容已转义，注入是安全的）----

  out = out.replace(CODE_RE, (whole, code) => stash(`<code>${code}</code>`))
  out = out.replace(BOLD_RE, '<strong>$1</strong>')
  out = out.replace(/\n/g, '<br>')

  // ---- 第 4 步：还原占位符 ----
  return out.replace(PLACEHOLDER_RE, (whole, index) => tokens[Number(index)] ?? '')
}

/**
 * 纯文本摘要：给消息气泡、文章卡片等只用一行摘要的地方用。
 * 把 markdown 标记和 URL 折叠成可读短文本。
 */
export function toPlainSummary(text, maxLength = 80) {
  if (!text) return ''
  const out = String(text)
    .replace(IMAGE_RE, '[图片]')
    .replace(LINK_RE, '$1')
    .replace(CODE_RE, '$1')
    .replace(BOLD_RE, '$1')
    .replace(BARE_LINK_RE, (whole, prefix, url) => prefix + url)
    .replace(/\s+/g, ' ')
    .trim()
  return out.length > maxLength ? out.slice(0, maxLength) + '…' : out
}
