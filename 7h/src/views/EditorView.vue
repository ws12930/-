<template>
  <div class="editor-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell wide">
      <header class="pg-head">
        <h1 class="pg-title grad-text">{{ isEdit ? '编辑文章' : '写文章' }}</h1>
        <p class="pg-sub grad-text">支持 Markdown 图片、链接、粗体与行内代码</p>
      </header>

      <p v-if="error" class="ed-error">{{ error }}</p>

      <div class="panel ed-panel">
        <div class="ed-field">
          <label for="ed-title">标题</label>
          <input id="ed-title" v-model.trim="form.title" type="text" placeholder="给文章起个名字" />
        </div>

        <div class="ed-row">
          <div class="ed-field ed-grow">
            <label for="ed-category">分类</label>
            <select id="ed-category" v-model="form.categoryId">
              <option :value="null">未分类</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="ed-field ed-grow">
            <label for="ed-tags">标签</label>
            <input id="ed-tags" v-model.trim="tagText" type="text" placeholder="用逗号分隔，如：日常,随笔" />
          </div>
        </div>

        <div class="ed-field">
          <label for="ed-summary">摘要（可留空，自动截取正文）</label>
          <input id="ed-summary" v-model.trim="form.summary" type="text" placeholder="一句话概括" />
        </div>

        <div class="ed-field">
          <label for="ed-content">正文</label>
          <ComposerBar v-model="form.content" :target="contentInput" :upload="true" :stickers="true" />
          <textarea
            id="ed-content"
            ref="contentInput"
            v-model="form.content"
            class="ed-textarea"
            rows="16"
            placeholder="写点什么…支持 ![图片](url)、[链接](url)、**粗体**、`代码`"
          ></textarea>
        </div>

        <div class="ed-preview" v-if="form.content">
          <span class="ed-preview-label">预览</span>
          <RichText :content="form.content" class="ed-preview-body" />
        </div>

        <div class="ed-actions">
          <label class="ed-status">
            <span class="ed-status-label">状态</span>
            <input
              v-model="statusText"
              class="ed-status-input"
              list="ed-status-options"
              maxlength="16"
              placeholder="自定义，如 PUBLISHED"
            />
            <datalist id="ed-status-options">
              <option value="PUBLISHED"></option>
              <option value="DRAFT"></option>
            </datalist>
          </label>
          <button class="g-btn g-btn-sm" :disabled="saving" @click="save('DRAFT')">
            {{ saving ? '保存中…' : '存为草稿' }}
          </button>
          <button class="g-btn" :disabled="saving" @click="save('PUBLISHED')">
            {{ saving ? '发布中…' : '发 布' }}
          </button>
        </div>
        <p class="ed-hint">
          状态可以随便填（≤16 字符）。除了 DRAFT 会当草稿不外显，其他值都会正常展示。
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createPost, updatePost, listCategories, listPosts } from '../api/blog'
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
  } catch (e) {
    error.value = e.message || '加载失败'
  }
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
    window.dispatchEvent(new CustomEvent('blog:saved', { detail: { status } }))
    router.push({ name: status.toUpperCase() === 'DRAFT' ? 'mine' : 'blog' })
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadEditing()
})
</script>

<style scoped>
.editor-page { height: 100%; overflow-y: auto; }
.ed-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }

.ed-panel { padding: 22px 24px; }
.ed-field { text-align: left; margin-bottom: 14px; }
.ed-field label {
  display: block;
  font-size: 13px;
  color: var(--c-dim);
  margin-bottom: 7px;
  letter-spacing: 1px;
}
.ed-field input,
.ed-field select {
  width: 100%;
  padding: 11px 13px;
  font-size: 14px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  font-family: inherit;
}
.ed-field input:focus,
.ed-field select:focus { border-color: var(--c-ninja); }
.ed-field select option { color: #111; }

.ed-row { display: flex; gap: 14px; }
.ed-grow { flex: 1; }

.ed-textarea {
  width: 100%;
  margin-top: 10px;
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  resize: vertical;
  font-family: inherit;
}
.ed-textarea:focus { border-color: var(--c-ninja); }

.ed-preview { margin-top: 16px; text-align: left; }
.ed-preview-label { display: block; font-size: 12px; color: var(--c-dim); margin-bottom: 8px; letter-spacing: 2px; }
.ed-preview-body {
  padding: 14px 16px;
  border: 1px dashed rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.85;
  word-break: break-word;
}

.ed-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.ed-status { display: flex; align-items: center; gap: 8px; margin-right: auto; }
.ed-status-label { font-size: 13px; color: var(--c-dim); letter-spacing: 1px; }
.ed-status-input {
  width: 180px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.18s ease;
}
.ed-status-input:focus { border-color: var(--c-ninja); }

.ed-hint { margin-top: 8px; font-size: 12px; color: var(--c-dim); text-align: right; }
</style>
