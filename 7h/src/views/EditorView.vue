<template>
  <div class="editor-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell wide write-shell">
      <header class="write-head">
        <span class="ph-icon write-head-icon" aria-hidden="true">✎</span>
        <div class="write-head-text">
          <h1 class="pg-title grad-text">{{ isEdit ? '编辑文章' : '写文章' }}</h1>
          <p class="pg-sub grad-text">支持 Markdown 图片、MP4 视频、链接、粗体与行内代码</p>
        </div>
      </header>

      <p v-if="error" class="ed-error">{{ error }}</p>

      <div class="panel write-paper">
        <!-- 标题：像文档标题一样直接写在纸上 -->
        <div class="write-title-row">
          <input
            id="ed-title"
            v-model.trim="form.title"
            class="write-title"
            type="text"
            placeholder="请输入文章标题"
          />
          <span class="write-title-count">{{ titleChars }} 字</span>
        </div>

        <!-- 元信息：一行轻量设置，摘要按需展开 -->
        <div class="write-meta">
          <div class="wm-item">
            <span class="wm-key">分类</span>
            <div class="ed-select-wrap">
              <select id="ed-category" v-model="form.categoryId" class="wm-control wm-select">
                <option :value="null">未分类</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
          </div>

          <div class="wm-item">
            <span class="wm-key">标签</span>
            <input
              id="ed-tags"
              v-model.trim="tagText"
              class="wm-control wm-tags"
              type="text"
              placeholder="逗号分隔，如：日常,随笔"
            />
          </div>

          <button type="button" class="wm-summary-toggle" @click="showSummary = !showSummary">
            {{ showSummary ? '收起摘要' : '+ 添加摘要' }}
          </button>
        </div>

        <div v-if="showSummary" class="write-summary">
          <input
            id="ed-summary"
            v-model.trim="form.summary"
            class="wm-control"
            type="text"
            placeholder="一句话概括这篇文章（可留空，自动截取正文）"
          />
        </div>

        <div class="write-divider"></div>

        <div class="write-content">
          <!-- 左侧：文章正文 -->
          <div class="write-main">
            <div class="write-toolbar">
              <div class="wt-tools">
                <button type="button" class="wt-btn" title="粗体" @click="wrapSelection('**', '**', '粗体文字')"><b>B</b></button>
                <button type="button" class="wt-btn" title="斜体" @click="wrapSelection('*', '*', '斜体文字')"><i>I</i></button>
                <button type="button" class="wt-btn" title="行内代码" @click="wrapSelection('`', '`', 'code')"><code>&lt;/&gt;</code></button>
                <button type="button" class="wt-btn" title="二级标题" @click="prefixLine('## ', '小标题')">H2</button>
                <span class="wt-sep"></span>
                <ComposerBar
                  v-model="form.content"
                  :target="contentInput"
                  :upload="true"
                  :video="true"
                  :stickers="false"
                  @uploaded="onUploaded"
                  @error="error = $event"
                />
              </div>

              <div class="wt-modes">
                <button type="button" :class="{ on: !previewMode }" @click="previewMode = false">编辑</button>
                <button type="button" :class="{ on: previewMode }" @click="previewMode = true">预览</button>
              </div>
            </div>

            <div class="write-body">
              <textarea
                id="ed-content"
                ref="contentInput"
                v-show="!previewMode"
                v-model="form.content"
                class="write-textarea"
                rows="18"
                placeholder="从这里开始写作…视频可从右侧素材栏插入，或点工具栏「视频」上传 MP4"
              ></textarea>
              <div v-show="previewMode" class="write-preview">
                <RichText v-if="form.content" :content="form.content" class="write-preview-body" />
                <p v-else class="write-preview-empty">还没有正文，切回「编辑」开始写吧</p>
              </div>
            </div>
          </div>

          <!-- 右侧：视频素材栏 -->
          <aside class="write-media">
            <div class="media-head">
              <span class="media-title">视频素材</span>
              <button type="button" class="media-add" :disabled="uploadingVideo" @click="openVideoPicker">
                {{ uploadingVideo ? '上传中…' : '+ 上传' }}
              </button>
              <input
                ref="videoInput"
                class="cb-file"
                type="file"
                accept="video/mp4,.mp4"
                @change="onVideoPicked"
              />
            </div>
            <p class="media-tip">MP4 ≤ 200MB，上传后点「插入」放进正文</p>

            <div v-if="videoList.length" class="media-list">
              <div v-for="(v, i) in videoList" :key="v.url + i" class="media-card">
                <div class="mc-thumb">
                  <video :src="v.url" preload="metadata" muted></video>
                  <span class="mc-play" aria-hidden="true">▶</span>
                </div>
                <div class="mc-body">
                  <p class="mc-name" :title="v.name">{{ v.name }}</p>
                  <p class="mc-size">{{ formatSize(v.size) }}</p>
                  <button type="button" class="mc-insert" @click="insertVideo(v.url)">插入到正文</button>
                </div>
              </div>
            </div>
            <p v-else class="media-empty">还没有视频素材<br />上传 MP4 后会显示在这里</p>
          </aside>
        </div>

        <p class="write-hint">⌘ / Ctrl + S 快速存草稿 · 自定义状态 ≤16 字符，除 DRAFT 外均正常展示</p>

        <!-- 底部常驻操作栏 -->
        <div class="write-footer">
          <div class="wf-left">
            <span class="wf-count">{{ contentChars }} 字符 · 约 {{ wordCount }} 字</span>
            <span class="wf-dot">·</span>
            <span class="wf-status-label">状态</span>
            <div class="wf-seg">
              <button type="button" :class="{ on: statusText === 'PUBLISHED' }" @click="statusText = 'PUBLISHED'">发布</button>
              <button type="button" :class="{ on: statusText === 'DRAFT' }" @click="statusText = 'DRAFT'">草稿</button>
            </div>
            <input
              v-model.trim="statusText"
              class="wf-status-input"
              list="ed-status-options"
              maxlength="16"
              placeholder="自定义状态"
            />
            <datalist id="ed-status-options">
              <option value="PUBLISHED"></option>
              <option value="DRAFT"></option>
            </datalist>
          </div>

          <div class="wf-actions">
            <button class="g-btn g-btn-sm" :disabled="saving" @click="save('DRAFT')">
              {{ saving ? '保存中…' : '存为草稿' }}
            </button>
            <button class="g-btn write-publish" :disabled="saving" @click="save('PUBLISHED')">
              {{ saving ? '发布中…' : '发 布' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { EVT_BLOG_SAVED } from '../utils/constants'
import { reactive, ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createPost, updatePost, listCategories, listPosts } from '../api/blog'
import { uploadFile } from '../api/upload'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import ComposerBar from '../components/ComposerBar.vue'
import RichText from '../components/RichText.vue'

const router = useRouter()
const route = useRoute()

const editingId = computed(() => Number(route.query.id) || null)
const isEdit = computed(() => !!editingId.value)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  categoryId: null
})
const tagText = ref('')
const statusText = ref('PUBLISHED')
const categories = ref([])
const contentInput = ref(null)
const saving = ref(false)
const error = ref('')
const previewMode = ref(false)
const showSummary = ref(false)
const videoInput = ref(null)
const videoList = ref([])
const uploadingVideo = ref(false)

const VIDEO_MARKER_RE = /@\[视频\]\(([^)\s]+)\)/g

const titleChars = computed(() => form.title.length)
const contentChars = computed(() => form.content.length)
const wordCount = computed(() => form.content.replace(/\s+/g, '').length)

const navItems = [{ label: '我的文章', to: 'mine' }]

async function loadCategories() {
  try {
    const res = await listCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

/** 编辑态回填：从列表里按 id 找，拿不到就提示 */
async function loadEditing() {
  if (!editingId.value) return
  try {
    const res = await listPosts({ current: 1, size: 100 })
    const data = res.data || {}
    const rows = data.records || data.list || []
    const hit = rows.find((r) => r.id === editingId.value)
    if (!hit) {
      error.value = '没有找到这篇文章'
      return
    }
    form.title = hit.title || ''
    form.summary = hit.summary || ''
    form.content = hit.content || ''
    form.categoryId = hit.categoryId ?? null
    statusText.value = hit.status || 'PUBLISHED'
    tagText.value = (hit.tags || []).join(',')
    showSummary.value = !!form.summary
    collectVideosFromContent()
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

/** 在光标处包一层 Markdown 标记，选中文字则包裹选区，否则插入占位文字 */
function wrapSelection(prefix, suffix, placeholder) {
  const el = contentInput.value
  const current = form.content || ''
  const hasCaret = el && typeof el.selectionStart === 'number'
  const start = hasCaret ? el.selectionStart : current.length
  const end = hasCaret ? el.selectionEnd : current.length
  const picked = current.slice(start, end) || placeholder
  form.content = current.slice(0, start) + prefix + picked + suffix + current.slice(end)
  nextTick(() => {
    if (!el || typeof el.setSelectionRange !== 'function') return
    el.focus()
    const caret = start + prefix.length + picked.length
    el.setSelectionRange(caret, caret)
  })
}

/** 给当前行加行首标记（如 ## 标题） */
function prefixLine(prefix, placeholder) {
  const el = contentInput.value
  const current = form.content || ''
  const hasCaret = el && typeof el.selectionStart === 'number'
  const pos = hasCaret ? el.selectionStart : current.length
  const lineStart = current.lastIndexOf('\n', pos - 1) + 1
  const lineEnd = current.indexOf('\n', pos)
  const end = lineEnd === -1 ? current.length : lineEnd
  const line = current.slice(lineStart, end) || placeholder
  form.content = current.slice(0, lineStart) + prefix + line + current.slice(end)
  nextTick(() => {
    if (!el || typeof el.setSelectionRange !== 'function') return
    el.focus()
    const caret = lineStart + prefix.length + line.length
    el.setSelectionRange(caret, caret)
  })
}

/** 从已有正文里收集视频标记，编辑旧文章时右侧素材栏也能列出视频 */
function collectVideosFromContent() {
  const seen = new Set()
  const list = []
  for (const match of form.content.matchAll(VIDEO_MARKER_RE)) {
    const url = match[1]
    if (seen.has(url)) continue
    seen.add(url)
    const name = url.split('/').pop() || '视频'
    list.push({ url, name, size: 0 })
  }
  videoList.value = list
}

function addVideo(result) {
  if (!result?.url) return
  if (videoList.value.some((v) => v.url === result.url)) return
  videoList.value.push({
    url: result.url,
    name: result.name || result.url.split('/').pop() || '视频',
    size: result.size || 0
  })
}

/** 工具栏「视频」按钮上传成功后也同步到右侧素材栏 */
function onUploaded(result) {
  addVideo(result)
}

function openVideoPicker() {
  if (uploadingVideo.value) return
  videoInput.value?.click()
}

/** 侧栏独立上传 MP4，上传完直接插入正文 */
async function onVideoPicked(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return

  const isMp4 = /\.mp4$/i.test(file.name) || file.type === 'video/mp4'
  if (!isMp4) {
    error.value = '仅支持 MP4 视频'
    return
  }
  if (file.size > 200 * 1024 * 1024) {
    error.value = '视频不能超过 200MB'
    return
  }

  error.value = ''
  uploadingVideo.value = true
  try {
    const result = await uploadFile(file)
    addVideo(result)
    insertVideo(result.url)
  } catch (e) {
    const raw = e.message || '视频上传失败'
    const legacy = /不支持的文件类型|仅支持常见图片与文档/.test(raw)
    error.value = legacy
      ? '后端未更新，请重启 Spring Boot 服务后重试（原提示：' + raw + '）'
      : raw
  } finally {
    uploadingVideo.value = false
  }
}

/** 把视频标记插入正文光标处，自动补换行让它独占一段 */
function insertVideo(url) {
  const el = contentInput.value
  const current = form.content || ''
  const hasCaret = el && typeof el.selectionStart === 'number'
  const pos = hasCaret ? el.selectionStart : current.length
  const before = current.slice(0, pos)
  const after = current.slice(pos)
  const lead = before && !before.endsWith('\n') ? '\n' : ''
  const tail = after && !after.startsWith('\n') ? '\n' : ''
  form.content = before + lead + `@[视频](${url})` + tail + after
  previewMode.value = false
  nextTick(() => {
    if (!el || typeof el.setSelectionRange !== 'function') return
    el.focus()
    const caret = (before + lead + `@[视频](${url})`).length
    el.setSelectionRange(caret, caret)
  })
}

function formatSize(bytes) {
  if (!bytes) return '大小未知'
  if (bytes < 1024 * 1024) return Math.max(1, Math.round(bytes / 1024)) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function buildPayload(status) {
  const tags = tagText.value
    .split(/[,，]/)
    .map((t) => t.trim())
    .filter(Boolean)
  return {
    title: form.title,
    summary: form.summary,
    content: form.content,
    categoryId: form.categoryId || null,
    tags,
    status
  }
}

function save(preset) {
  const v = String(preset || '').trim()
  statusText.value = v ? v.toUpperCase() : (statusText.value.trim() || 'PUBLISHED')
  doSave()
}

async function doSave() {
  error.value = ''
  if (!form.title) {
    error.value = '请先填写标题'
    return
  }
  if (!form.content) {
    error.value = '正文不能为空'
    return
  }
  const status = (statusText.value || '').trim() || 'PUBLISHED'
  saving.value = true
  try {
    const payload = buildPayload(status)
    if (isEdit.value) {
      await updatePost(editingId.value, payload)
    } else {
      await createPost(payload)
    }
    window.dispatchEvent(new CustomEvent(EVT_BLOG_SAVED, { detail: { status } }))
    router.push({ name: status.toUpperCase() === 'DRAFT' ? 'mine' : 'blog' })
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

/** Ctrl / ⌘ + S 快速存草稿，避免和浏览器「保存网页」撞车 */
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    if (!saving.value) save('DRAFT')
  }
}

onMounted(() => {
  loadCategories()
  loadEditing()
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.editor-page { height: 100%; overflow-y: auto; }
.write-shell { padding-top: 50px; padding-bottom: 36px; }

/* ===== 轻量页头 ===== */
.write-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}
.write-head-icon { font-size: 19px; }
.write-head-text { text-align: left; }
.write-head .pg-title { font-size: 21px; letter-spacing: 4px; }
.write-head .pg-sub { font-size: 12px; letter-spacing: 2px; margin-top: 3px; }

.ed-error {
  max-width: 760px;
  margin: -6px auto 14px;
  padding: 10px 16px;
  font-size: 13px;
  color: #ff9c9c;
  text-align: center;
  border: 1px solid rgba(255, 128, 128, 0.28);
  border-radius: 8px;
  background: rgba(255, 90, 90, 0.08);
}

/* ===== 写作纸张 ===== */
.write-paper { padding: 42px 52px 0; }

.write-title-row { display: flex; align-items: baseline; gap: 14px; }
.write-title {
  flex: 1;
  min-width: 0;
  padding: 0 0 10px;
  font-family: var(--font-display);
  font-size: clamp(26px, 3vw, 34px);
  line-height: 1.4;
  letter-spacing: 2px;
  color: var(--c-text);
  background: transparent;
  border: none;
  border-bottom: 1px solid transparent;
  outline: none;
  transition: border-color 0.2s;
}
.write-title::placeholder { color: rgba(233, 237, 240, 0.26); }
.write-title:focus { border-bottom-color: rgba(143, 208, 174, 0.38); }
.write-title-count { flex: none; font-size: 11px; color: var(--c-dim); font-variant-numeric: tabular-nums; }

/* ===== 元信息 ===== */
.write-meta { display: flex; align-items: center; gap: 18px; flex-wrap: wrap; margin-top: 10px; }
.wm-item { display: flex; align-items: center; gap: 8px; }
.wm-key { font-size: 12px; letter-spacing: 1px; color: var(--c-dim); white-space: nowrap; }
.wm-control {
  padding: 6px 10px;
  font-size: 13px;
  font-family: inherit;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s, background 0.2s;
}
.wm-control::placeholder { color: rgba(150, 161, 169, 0.55); }
.wm-control:hover { border-color: rgba(255, 255, 255, 0.2); }
.wm-control:focus { border-color: rgba(143, 208, 174, 0.55); background: rgba(255, 255, 255, 0.06); }
.wm-select { appearance: none; -webkit-appearance: none; padding-right: 28px; cursor: pointer; }
.wm-select option { color: #111; }
.wm-tags { width: 250px; }
.ed-select-wrap { position: relative; }
.ed-select-wrap::after {
  content: '▾';
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  font-size: 11px;
  color: var(--c-mint);
  pointer-events: none;
}
.wm-summary-toggle {
  font-size: 12px;
  letter-spacing: 0.5px;
  color: var(--c-ninja);
  padding: 5px 2px;
  transition: color 0.2s;
}
.wm-summary-toggle:hover { color: #fff; }
.write-summary { margin-top: 12px; }
.write-summary .wm-control { width: 100%; }

.write-divider {
  height: 1px;
  margin-top: 22px;
  background: linear-gradient(90deg, rgba(143, 208, 174, 0.3), rgba(255, 255, 255, 0.05) 62%, transparent);
}

/* ===== 正文 + 视频素材 双栏 ===== */
.write-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 232px;
  gap: 22px;
  align-items: start;
}
.write-main { min-width: 0; }

.write-media {
  position: sticky;
  top: 20px;
  padding-left: 20px;
  border-left: 1px solid rgba(255, 255, 255, 0.08);
}
.media-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.media-title { font-size: 13px; letter-spacing: 1px; color: #c3cec9; }
.media-add {
  padding: 4px 10px;
  font-size: 11px;
  color: var(--c-ninja);
  border: 1px solid rgba(143, 208, 174, 0.32);
  border-radius: 6px;
  background: rgba(143, 208, 174, 0.08);
  transition: color 0.2s, background 0.2s;
}
.media-add:hover:not(:disabled) { color: #fff; background: rgba(143, 208, 174, 0.2); }
.media-add:disabled { opacity: 0.55; cursor: not-allowed; }
.cb-file { display: none; }
.media-tip { margin-top: 6px; font-size: 11px; line-height: 1.6; color: var(--c-dim); }
.media-empty {
  margin-top: 18px;
  padding: 30px 8px;
  text-align: center;
  font-size: 11px;
  line-height: 1.9;
  color: var(--c-dim);
  border: 1px dashed rgba(255, 255, 255, 0.12);
  border-radius: 8px;
}
.media-list { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; }
.media-card {
  display: flex;
  gap: 10px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  transition: border-color 0.2s, background 0.2s;
}
.media-card:hover { border-color: rgba(143, 208, 174, 0.38); background: rgba(143, 208, 174, 0.05); }
.mc-thumb {
  position: relative;
  flex: none;
  width: 76px;
  height: 48px;
  overflow: hidden;
  border-radius: 5px;
  background: #000;
}
.mc-thumb video { width: 100%; height: 100%; object-fit: cover; pointer-events: none; }
.mc-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  background: rgba(0, 0, 0, 0.25);
}
.mc-body { min-width: 0; flex: 1; }
.mc-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--c-text);
}
.mc-size { margin-top: 2px; font-size: 10px; color: var(--c-dim); }
.mc-insert {
  margin-top: 6px;
  padding: 3px 8px;
  font-size: 10px;
  letter-spacing: 0.5px;
  color: var(--c-ninja);
  border: 1px solid rgba(143, 208, 174, 0.3);
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}
.mc-insert:hover { color: #fff; background: rgba(143, 208, 174, 0.18); }

/* ===== 工具栏 ===== */
.write-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 0;
}
.wt-tools { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
/* 双栏后主栏变窄，工具按钮只留图标，悬停仍有 title 说明 */
.wt-tools :deep(.cb-label) { display: none; }
.wt-tools :deep(.cb-btn) { padding: 6px 9px; font-size: 13px; }
/* 上传提示单独占一行，避免把编辑/预览切换挤到下一行 */
.wt-tools :deep(.cb-tip) { flex-basis: 100%; margin-left: 0; }
.wt-btn {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--c-dim);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.03);
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.wt-btn:hover {
  color: var(--c-text);
  border-color: rgba(143, 208, 174, 0.42);
  background: rgba(143, 208, 174, 0.1);
}
.wt-btn code { font-size: 11px; font-family: Consolas, Monaco, monospace; }
.wt-sep { width: 1px; height: 18px; margin: 0 4px; background: rgba(255, 255, 255, 0.12); }
.wt-modes {
  display: flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
}
.wt-modes button {
  padding: 4px 14px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--c-dim);
  border-radius: 999px;
  transition: color 0.2s, background 0.2s;
}
.wt-modes button:hover { color: var(--c-text); }
.wt-modes button.on { color: var(--c-text); background: rgba(143, 208, 174, 0.16); }

/* ===== 正文 ===== */
.write-body { border-top: 1px solid rgba(255, 255, 255, 0.07); }
.write-textarea {
  display: block;
  width: 100%;
  min-height: 430px;
  padding: 22px 0 20px;
  font-size: 15px;
  font-family: inherit;
  line-height: 2;
  color: var(--c-text);
  background: transparent;
  border: none;
  outline: none;
  resize: vertical;
}
.write-textarea::placeholder { color: rgba(150, 161, 169, 0.5); }
.write-preview { min-height: 260px; padding: 22px 0; }
.write-preview-body { font-size: 15px; line-height: 1.95; word-break: break-word; }
.write-preview-empty { padding: 100px 0; text-align: center; font-size: 13px; color: var(--c-dim); }
.write-hint { margin: 0 0 14px; font-size: 11px; color: var(--c-dim); text-align: right; }

/* ===== 底部操作栏 ===== */
.write-footer {
  position: sticky;
  bottom: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin: 0 -52px;
  padding: 13px 52px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0 0 14px 14px;
  background: linear-gradient(0deg, rgba(16, 20, 24, 0.97), rgba(16, 20, 24, 0.9));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}
.wf-left { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.wf-count { font-size: 12px; color: var(--c-dim); font-variant-numeric: tabular-nums; }
.wf-dot { font-size: 12px; color: rgba(150, 161, 169, 0.5); }
.wf-status-label { font-size: 12px; color: var(--c-dim); }
.wf-seg {
  display: inline-flex;
  gap: 2px;
  padding: 2px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.03);
}
.wf-seg button {
  padding: 4px 10px;
  font-size: 11px;
  color: var(--c-dim);
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}
.wf-seg button:hover { color: var(--c-text); }
.wf-seg button.on { color: #eafff4; background: rgba(143, 208, 174, 0.18); }
.wf-status-input {
  width: 122px;
  padding: 5px 8px;
  font-size: 11px;
  font-family: inherit;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s;
}
.wf-status-input::placeholder { color: rgba(150, 161, 169, 0.5); }
.wf-status-input:focus { border-color: rgba(143, 208, 174, 0.55); }
.wf-actions { display: flex; align-items: center; gap: 10px; margin-left: auto; }
.write-publish {
  color: #f2fff8;
  letter-spacing: 3px;
  border-color: transparent;
  background:
    linear-gradient(135deg, rgba(143, 208, 174, 0.28), rgba(217, 255, 77, 0.12)) padding-box,
    linear-gradient(90deg, #8fd0ae, #d9ff4d) border-box;
}
.write-publish:hover {
  box-shadow: 0 6px 26px rgba(143, 208, 174, 0.24), 0 0 0 1px rgba(217, 255, 77, 0.18);
}

@media (max-width: 700px) {
  .write-shell { padding-top: 56px; }
  .write-paper { padding: 26px 20px 0; }
  .write-footer { margin: 0 -20px; padding: 12px 20px; }
  .wm-tags { width: 100%; }
  .wm-item { width: 100%; }
  .wf-left { width: 100%; }
  .wf-actions { width: 100%; justify-content: flex-end; }
  .write-content { grid-template-columns: 1fr; gap: 20px; }
  .write-media {
    position: static;
    padding: 18px 0 0;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
  }
  .write-textarea, .write-preview { min-height: 320px; }
}
</style>
