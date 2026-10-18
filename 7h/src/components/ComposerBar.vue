<template>
  <div class="composer-bar">
    <div class="cb-buttons">
      <button type="button" class="cb-btn" title="插入表情" @click.stop="togglePanel('emoji')">
        😊<span class="cb-label">表情</span>
      </button>
      <button type="button" class="cb-btn" title="插入链接" @click.stop="togglePanel('link')">
        🔗<span class="cb-label">链接</span>
      </button>
      <button
        v-if="upload"
        type="button"
        class="cb-btn"
        title="上传图片或附件"
        :disabled="uploading"
        @click.stop="openFilePicker"
      >
        📎<span class="cb-label">{{ uploading ? '上传中' : '文件' }}</span>
      </button>
      <span v-if="tip" class="cb-tip" :class="{ err: tipError }">{{ tip }}</span>
    </div>

    <!-- 表情 / 表情包面板 -->
    <div v-if="panel === 'emoji'" class="cb-pop" @click.stop>
      <div class="cb-pop-head">
        <div class="cb-tabs">
          <button
            type="button"
            class="cb-tab"
            :class="{ on: emojiTab === 'emoji' }"
            @click="emojiTab = 'emoji'"
          >
            表情
          </button>
          <button
            v-if="stickers"
            type="button"
            class="cb-tab"
            :class="{ on: emojiTab === 'sticker' }"
            @click="emojiTab = 'sticker'"
          >
            表情包
          </button>
        </div>
        <button type="button" class="cb-close" @click="panel = ''">✕</button>
      </div>

      <!-- 内置 emoji -->
      <div v-if="emojiTab === 'emoji'" class="cb-emoji-body">
        <div v-for="group in EMOJI_GROUPS" :key="group.name" class="cb-emoji-group">
          <p class="cb-pop-title">{{ group.name }}</p>
          <div class="cb-emoji-grid">
            <button
              v-for="item in group.items"
              :key="item"
              type="button"
              class="cb-emoji"
              @click="insertEmoji(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>
      </div>

      <!-- 自定义表情包 -->
      <div v-else class="cb-sticker-body">
        <div v-if="stickerList.length" class="cb-sticker-grid">
          <div v-for="s in stickerList" :key="s.id" class="cb-sticker">
            <button type="button" class="cb-sticker-btn" :title="s.name" @click="insertSticker(s)">
              <img :src="s.url" :alt="s.name" />
            </button>
            <button type="button" class="cb-sticker-del" title="删除表情" @click="removeSticker(s.id)">
              ✕
            </button>
          </div>
        </div>
        <p v-else class="cb-sticker-empty">
          还没有表情包，点下面按钮上传一张图片
        </p>

        <div class="cb-sticker-add">
          <button type="button" class="cb-ok" :disabled="stickerUploading" @click="openStickerPicker">
            {{ stickerUploading ? '上传中…' : '+ 添加表情' }}
          </button>
          <span class="cb-pop-note">图片不超过 5MB，存本机</span>
        </div>
        <p v-if="stickerError" class="cb-pop-note err">{{ stickerError }}</p>
      </div>
    </div>

    <!-- 链接面板 -->
    <div v-if="panel === 'link'" class="cb-pop" @click.stop>
      <div class="cb-pop-head">
        <span>插入链接</span>
        <button type="button" class="cb-close" @click="panel = ''">✕</button>
      </div>
      <div class="cb-link-body">
        <input
          v-model.trim="linkUrl"
          class="cb-input"
          type="text"
          placeholder="https://example.com"
          @keyup.enter="confirmLink"
        />
        <input
          v-if="linkMode === 'markdown'"
          v-model.trim="linkText"
          class="cb-input"
          type="text"
          placeholder="显示文字（留空则显示链接本身）"
          @keyup.enter="confirmLink"
        />
        <p class="cb-pop-note">
          {{ linkMode === 'markdown' ? '将以「文字」(链接) 的形式插入' : '将直接插入链接文本' }}
        </p>
        <div class="cb-pop-actions">
          <button type="button" class="cb-ok" @click="confirmLink">插 入</button>
          <button type="button" class="cb-cancel" @click="panel = ''">取 消</button>
        </div>
      </div>
    </div>

    <input
      ref="fileInput"
      class="cb-file"
      type="file"
      :accept="accept"
      @change="onFileChange"
    />
    <input
      ref="stickerInput"
      class="cb-file"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
      @change="onStickerFile"
    />
  </div>
</template>

<script setup>
import { ref, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { uploadFile } from '../api/upload'
import { loadStickers, addSticker, removeSticker as dropSticker } from '../utils/stickers'

const props = defineProps({
  /** 目标输入框的 ref 对象（input / textarea） */
  target: { type: Object, default: null },
  /** 当前文本，配合 update:modelValue 使用 */
  modelValue: { type: String, default: '' },
  /** markdown：插入 [文字](url)；plain：直接插入 url */
  linkMode: { type: String, default: 'markdown' },
  /** 是否显示文件上传按钮 */
  upload: { type: Boolean, default: true },
  /** 是否开启自定义表情包面板（目前只有私信页用） */
  stickers: { type: Boolean, default: false },
  /** 原生 accept 属性 */
  accept: { type: String, default: 'image/*,.pdf,.txt,.md,.csv,.zip,.doc,.docx,.xls,.xlsx,.ppt,.pptx' }
})

const emit = defineEmits(['update:modelValue', 'uploaded', 'error'])

const EMOJI_GROUPS = [
  {
    name: '常用',
    items: ['😀', '😁', '😂', '🤣', '😊', '😍', '😎', '🤔', '😅', '😭', '😡', '🥺', '😴', '🤯', '😱', '🙄', '👍', '👎', '👏', '🙏', '💪', '🤝']
  },
  {
    name: '死火主题',
    items: ['🔥', '⚔️', '🗡️', '💀', '👻', '🍥', '🌀', '👊', '🥷', '🦊', '⚡', '❄️', '🩸', '🌊', '🕶️', '🛡️', '🏮', '⛩️', '🌙', '🌪️']
  },
  {
    name: '符号',
    items: ['❤️', '💔', '✨', '⭐', '🎉', '🎊', '✅', '❌', '❓', '❗', '💯', '🔰', '⚡', '💧', '🌈', '🔱']
  }
]

const panel = ref('')
const linkUrl = ref('')
const linkText = ref('')
const uploading = ref(false)
const tip = ref('')
const tipError = ref(false)
const fileInput = ref(null)

/* ===== 自定义表情包 ===== */
const emojiTab = ref('emoji')
const stickerList = ref(props.stickers ? loadStickers() : [])
const stickerUploading = ref(false)
const stickerError = ref('')
const stickerInput = ref(null)

let tipTimer = null
let closeTimer = null

function showTip(message, isError = false) {
  tip.value = message
  tipError.value = isError
  clearTimeout(tipTimer)
  if (message) {
    tipTimer = setTimeout(() => (tip.value = ''), 2600)
  }
}

function togglePanel(name) {
  panel.value = panel.value === name ? '' : name
  if (panel.value === 'link') {
    linkUrl.value = ''
    linkText.value = ''
  }
}

/**
 * 取到真实 DOM 元素。
 * 注意：模板里写 :target="someRef" 时，Vue 会把 ref 自动解包成 DOM 元素本身。
 * 不能简单用 `t.value || t` —— textarea/input 自身就有 value 属性（字符串），
 * 那样会拿到字符串而不是元素，导致读不到光标位置。
 */
function targetElement() {
  const t = props.target
  if (!t) return null
  if (t.nodeType === 1) return t
  const inner = t.value
  return inner && inner.nodeType === 1 ? inner : null
}

/** 目标是多行输入框（文章正文 / 评论），图片这类块级内容应当独立成行 */
function isMultilineTarget() {
  const el = targetElement()
  return !!el && el.tagName === 'TEXTAREA'
}

/** 多行场景下给块级内容补上换行，避免和前后文字挤在一行 */
function blockSnippet(snippet) {
  if (!isMultilineTarget()) return snippet
  const current = props.modelValue ?? ''
  return current.trim().length ? `\n${snippet}\n` : snippet
}

/** 在光标处插入文本，插入后把光标移到新内容末尾 */
function insertText(text) {
  const el = targetElement()
  const current = props.modelValue ?? ''

  const supportsSelection =
    el && typeof el.selectionStart === 'number' && typeof el.setSelectionRange === 'function'

  if (!supportsSelection) {
    // 拿不到光标位置时退化为追加
    emit('update:modelValue', current + text)
    return
  }

  const start = el.selectionStart
  const end = el.selectionEnd
  emit('update:modelValue', current.slice(0, start) + text + current.slice(end))

  nextTick(() => {
    el.focus()
    const caret = start + text.length
    el.setSelectionRange(caret, caret)
  })
}

function insertEmoji(emoji) {
  insertText(emoji)
  showTip(`已插入 ${emoji}`)
}

/** 表情包以 Markdown 图片插入，RichText 会渲染成小图 */
function insertSticker(sticker) {
  insertText(`![${sticker.name || '表情'}](${sticker.url})`)
  showTip('表情已插入')
}

function openStickerPicker() {
  if (stickerUploading.value) return
  stickerError.value = ''
  stickerInput.value?.click()
}

async function onStickerFile(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return

  if (!file.type.startsWith('image/')) {
    stickerError.value = '请选择图片文件'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    stickerError.value = '图片不能超过 5MB'
    return
  }

  stickerUploading.value = true
  stickerError.value = ''
  try {
    const result = await uploadFile(file)
    if (!result.image) throw new Error('该文件不是图片')
    const outcome = addSticker(result.name || '表情', result.url)
    stickerList.value = outcome.list
    if (!outcome.ok) stickerError.value = outcome.message
  } catch (e) {
    stickerError.value = e.message || '上传失败'
  } finally {
    stickerUploading.value = false
  }
}

function removeSticker(id) {
  stickerList.value = dropSticker(id)
  showTip('表情已删除')
}

function confirmLink() {
  const url = linkUrl.value.trim()
  if (!url) {
    showTip('请输入链接地址', true)
    return
  }
  if (!/^(https?:\/\/|mailto:|\/)/i.test(url)) {
    showTip('链接需以 http:// 、https:// 或 / 开头', true)
    return
  }

  if (props.linkMode === 'markdown') {
    const label = linkText.value.trim() || url
    insertText(`[${label}](${url})`)
  } else {
    insertText(url)
  }
  panel.value = ''
  showTip('链接已插入')
}

function openFilePicker() {
  if (uploading.value) return
  fileInput.value?.click()
}

async function onFileChange(event) {
  const file = event.target.files && event.target.files[0]
  // 立刻复位，保证连续选择同一个文件也能触发 change
  event.target.value = ''
  if (!file) return

  uploading.value = true
  showTip('正在上传…')
  try {
    const result = await uploadFile(file)
    if (result.image) {
      insertText(blockSnippet(`![${result.name}](${result.url})`))
    } else {
      insertText(`[📎 ${result.name}](${result.url})`)
    }
    showTip(`已上传 ${result.name}`)
    emit('uploaded', result)
  } catch (e) {
    const message = e.message || '上传失败'
    showTip(message, true)
    emit('error', message)
  } finally {
    uploading.value = false
  }
}

/** 点击组件外部关闭面板 */
function onDocumentClick() {
  panel.value = ''
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})

onBeforeUnmount(() => {
  clearTimeout(tipTimer)
  clearTimeout(closeTimer)
  document.removeEventListener('click', onDocumentClick)
})
</script>

<style scoped>
.composer-bar {
  position: relative;
}

.cb-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.cb-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  font-size: 13px;
  line-height: 1;
  color: var(--c-dim);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.cb-btn:hover:not(:disabled) {
  color: var(--c-text);
  border-color: rgba(255, 255, 255, 0.34);
  background: rgba(255, 255, 255, 0.08);
}

.cb-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.cb-label {
  font-size: 12px;
  letter-spacing: 1px;
}

.cb-tip {
  font-size: 12px;
  color: var(--c-dim);
  margin-left: 2px;
}

.cb-tip.err {
  color: #ff8080;
}

.cb-file {
  display: none;
}

/* ===== 弹出面板 ===== */
.cb-pop {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  z-index: 40;
  width: 340px;
  max-width: min(340px, 86vw);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 10px;
  background: rgba(20, 20, 32, 0.98);
  box-shadow: 0 12px 34px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
}

.cb-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--c-dim);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.cb-close {
  font-size: 12px;
  color: var(--c-dim);
  padding: 2px 4px;
}

.cb-close:hover {
  color: var(--c-text);
}

.cb-emoji-body {
  max-height: 240px;
  overflow-y: auto;
  padding: 8px 12px 12px;
}

.cb-pop-title {
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--c-dim);
  margin: 8px 0 6px;
}

.cb-emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
}

.cb-emoji {
  font-size: 18px;
  line-height: 1;
  padding: 5px 0;
  border-radius: 6px;
  transition: background 0.15s, transform 0.15s;
}

.cb-emoji:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: scale(1.18);
}

/* ===== 表情 / 表情包 切换 ===== */
.cb-tabs { display: flex; gap: 4px; }
.cb-tab {
  padding: 3px 10px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--c-dim);
  border: 1px solid transparent;
  border-radius: 12px;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.cb-tab:hover { color: var(--c-text); }
.cb-tab.on {
  color: var(--c-text);
  border-color: rgba(150, 182, 168, 0.34);
  background: rgba(150, 182, 168, 0.12);
}

.cb-sticker-body {
  padding: 10px 12px 12px;
}
.cb-sticker-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}
.cb-sticker { position: relative; }
.cb-sticker-btn {
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  transition: border-color 0.2s, transform 0.15s;
}
.cb-sticker-btn:hover {
  border-color: rgba(150, 182, 168, 0.34);
  transform: translateY(-2px);
}
.cb-sticker-btn img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.cb-sticker-del {
  position: absolute;
  top: -5px;
  right: -5px;
  width: 18px;
  height: 18px;
  font-size: 11px;
  line-height: 1;
  color: var(--c-dim);
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(20, 20, 32, 0.95);
  opacity: 0;
  transition: opacity 0.2s, color 0.2s;
}
.cb-sticker:hover .cb-sticker-del { opacity: 1; }
.cb-sticker-del:hover { color: #ff8080; }

.cb-sticker-empty {
  padding: 18px 0;
  text-align: center;
  font-size: 12px;
  color: var(--c-dim);
}
.cb-sticker-add {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.cb-sticker-add .cb-ok { flex: none; padding: 7px 14px; font-size: 12px; letter-spacing: 0; }
.cb-pop-note.err { color: #ff8080; }

.cb-link-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cb-input {
  width: 100%;
  padding: 8px 10px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
}

.cb-input:focus {
  border-color: var(--c-ninja);
}

.cb-pop-note {
  font-size: 11px;
  color: var(--c-dim);
}

.cb-pop-actions {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.cb-ok,
.cb-cancel {
  flex: 1;
  padding: 8px 0;
  font-size: 13px;
  letter-spacing: 1px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.cb-ok {
  color: var(--c-text);
  border-color: rgba(150, 182, 168, 0.34);
  background: rgba(150, 182, 168, 0.14);
}

.cb-ok:hover {
  background: rgba(143, 208, 174, 0.26);
}

.cb-cancel {
  color: var(--c-dim);
}

.cb-cancel:hover {
  color: var(--c-text);
}

@media (max-width: 560px) {
  .cb-label {
    display: none;
  }
  .cb-pop {
    width: 300px;
  }
}
</style>
