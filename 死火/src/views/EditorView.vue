<template>
  <div class="editor-page">
    <div class="auth-bg left"></div>
    <div class="auth-bg right"></div>

    <PageNav :items="navItems" />

    <div class="editor-main pg-shell mid">
      <header class="pg-head">
        <h1 class="grad-text pg-title">FENGLIN BLOG</h1>
        <p class="grad-text pg-sub">{{ isEdit ? '编 辑 文 章' : '写 文 章' }}</p>
      </header>

      <form class="panel editor-form" @submit.prevent="submit">
        <p v-if="error" class="auth-alert err">{{ error }}</p>

        <!-- ===== 常驻区：写作真正需要的三样 ===== -->
        <div class="auth-field">
          <label for="ed-title">标题</label>
          <input id="ed-title" v-model.trim="form.title" type="text" maxlength="150" placeholder="请输入文章标题" />
        </div>

        <div class="auth-field">
          <label for="ed-category">分类</label>
          <select id="ed-category" v-model="form.categoryId" class="ed-select">
            <option value="">请选择分类</option>
            <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
          </select>
        </div>

        <div class="auth-field">
          <label for="ed-content">正文</label>
          <div class="ed-toolbar">
            <ComposerBar :target="contentRef" v-model="form.content" @error="onUploadError" />
          </div>
          <textarea
            id="ed-content"
            ref="contentRef"
            v-model="form.content"
            rows="14"
            class="ed-textarea"
            placeholder="开始写作…"
          ></textarea>
          <p class="auth-hint tip">
            支持插入表情、链接与图片/附件；正文按 Markdown 子集渲染（链接可点、图片直接显示）。
          </p>
        </div>

        <!-- ===== 选填区：默认收起，编辑已有文章且这些字段有值时自动展开 ===== -->
        <button type="button" class="more-toggle" :class="{ on: showMore }" @click="showMore = !showMore">
          <span class="more-title">更多选项</span>
          <span class="more-summary">{{ moreSummary }}</span>
          <span class="more-arrow">{{ showMore ? '收起 ▲' : '展开 ▼' }}</span>
        </button>

        <div v-show="showMore" class="more-body">
          <div class="auth-field">
            <label for="ed-slug">Slug（小写字母、数字和中划线，留空自动生成）</label>
            <input id="ed-slug" v-model.trim="form.slug" type="text" maxlength="120" placeholder="first-post" />
          </div>

          <div class="auth-field">
            <label for="ed-summary">摘要（留空自动截取正文）</label>
            <input id="ed-summary" v-model.trim="form.summary" type="text" maxlength="500" placeholder="一句话概括这篇文章" />
          </div>

          <div class="auth-field">
            <label for="ed-cover">封面地址（选填）</label>
            <input id="ed-cover" v-model.trim="form.coverUrl" type="text" maxlength="500" placeholder="https://..." />
          </div>

          <div class="auth-field">
            <label>标签</label>
            <div class="tag-picker">
              <button
                v-for="t in tags"
                :key="t.id"
                type="button"
                class="tag-toggle"
                :class="{ picked: form.tagIds.includes(t.id) }"
                @click="toggleTag(t.id)"
              >
                {{ t.name }}
              </button>
              <span v-if="!tags.length" class="auth-hint tip">暂无标签</span>
            </div>
          </div>

          <div class="auth-field">
            <label>发布状态</label>
            <div class="status-picker">
              <button
                type="button"
                class="seg-btn"
                :class="{ on: form.status === 'PUBLISHED' }"
                @click="form.status = 'PUBLISHED'"
              >
                立即发布
              </button>
              <button
                type="button"
                class="seg-btn"
                :class="{ on: form.status === 'DRAFT' }"
                @click="form.status = 'DRAFT'"
              >
                存为草稿
              </button>
            </div>
          </div>
        </div>

        <div class="editor-actions">
          <button class="g-btn submit" type="submit" :disabled="saving">
            {{ saving ? '保存中…' : '保 存 文 章' }}
          </button>
          <button class="auth-link" type="button" @click="goMine">返回我的文章</button>
          <button v-if="isEdit" class="auth-link danger" type="button" @click="remove">删除文章</button>
        </div>
      </form>
    </div>

    <p v-if="hint" class="auth-toast">{{ hint }}</p>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import ComposerBar from '../components/ComposerBar.vue'
import PageNav from '../components/PageNav.vue'
import {
  listCategories,
  listTags,
  createPost,
  updatePost,
  deletePost,
  listMyPosts
} from '../api/blog'

const router = useRouter()
const route = useRoute()

const form = reactive({
  title: '',
  slug: '',
  categoryId: '',
  summary: '',
  coverUrl: '',
  content: '',
  status: 'PUBLISHED',
  tagIds: []
})

const categories = ref([])
const tags = ref([])
const saving = ref(false)
const error = ref('')
const hint = ref('')
const contentRef = ref(null)

/* 右上角统一导航 */
const navItems = [
  { label: '返回首页', to: 'home' },
  { label: '我的文章', to: 'mine' }
]

/** 选填区是否展开 */
const showMore = ref(false)

/** 折叠时把里面填了什么告诉用户，避免"内容藏起来了不知道" */
const moreSummary = computed(() => {
  const parts = []
  if (form.tagIds.length) parts.push(`${form.tagIds.length} 个标签`)
  if (form.slug) parts.push('自定义 Slug')
  if (form.summary) parts.push('摘要')
  if (form.coverUrl) parts.push('封面')
  if (form.status === 'DRAFT') parts.push('存为草稿')
  return parts.length ? `已设置 ${parts.join(' · ')}` : 'Slug / 摘要 / 封面 / 标签 / 状态'
})

/** 上传失败时，把错误显示在表单顶部的提示条里 */
function onUploadError(message) {
  error.value = message
}

const postId = computed(() => (route.query.id ? Number(route.query.id) : null))
const isEdit = computed(() => !!postId.value)

let hintTimer = null
function toast(msg) {
  hint.value = msg
  clearTimeout(hintTimer)
  hintTimer = setTimeout(() => (hint.value = ''), 2200)
}
onBeforeUnmount(() => clearTimeout(hintTimer))

function toggleTag(id) {
  const index = form.tagIds.indexOf(id)
  if (index >= 0) {
    form.tagIds.splice(index, 1)
  } else {
    form.tagIds.push(id)
  }
}

function validate() {
  if (!form.title) return '请输入文章标题'
  if (!form.categoryId) return '请选择文章分类'
  if (!form.content) return '请输入文章正文'
  return ''
}

async function submit() {
  error.value = validate()
  if (error.value) return

  saving.value = true
  try {
    const payload = {
      categoryId: Number(form.categoryId),
      title: form.title,
      slug: form.slug || undefined,
      summary: form.summary || undefined,
      content: form.content,
      coverUrl: form.coverUrl || undefined,
      status: form.status,
      tagIds: form.tagIds
    }
    if (isEdit.value) {
      await updatePost(postId.value, payload)
      toast('文章已更新')
    } else {
      await createPost(payload)
      toast('文章已保存')
    }
    router.push({ name: 'mine' })
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!window.confirm('确认删除这篇文章？')) return
  try {
    await deletePost(postId.value)
    toast('文章已删除')
    router.push({ name: 'mine' })
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

function goMine() {
  router.push({ name: 'mine' })
}

async function loadExisting() {
  const res = await listMyPosts({ current: 1, size: 50 })
  const found = (res.data?.records || []).find((p) => p.id === postId.value)
  if (!found) {
    error.value = '未找到该文章，或你无权编辑'
    return
  }
  form.title = found.title || ''
  form.slug = found.slug || ''
  form.categoryId = found.categoryId ? String(found.categoryId) : ''
  form.summary = found.summary || ''
  form.coverUrl = found.coverUrl || ''
  form.content = found.content || ''
  form.status = found.status === 'PUBLISHED' ? 'PUBLISHED' : 'DRAFT'
  form.tagIds = (found.tags || []).map((t) => t.id)
  // 选填区里已经有内容时默认展开，否则用户会以为这些内容丢了
  showMore.value = !!(
    form.slug ||
    form.summary ||
    form.coverUrl ||
    form.tagIds.length ||
    form.status === 'DRAFT'
  )
}

onMounted(async () => {
  try {
    const [c, t] = await Promise.all([listCategories(), listTags()])
    categories.value = c.data || []
    tags.value = t.data || []
  } catch (e) {
    error.value = '分类或标签加载失败'
  }
  if (isEdit.value) {
    try {
      await loadExisting()
    } catch (e) {
      error.value = e.message || '文章加载失败'
    }
  }
})
</script>

<style scoped>
.editor-page {
  height: 100%;
  position: relative;
  display: flex;
  justify-content: center;
  overflow: hidden;
}

/* 宽度与顶部留白由全局 .pg-shell 提供，这里只保留滚动行为 */
.editor-main {
  max-height: 100%;
  overflow-y: auto;
}

.editor-form { padding: 26px 28px; }

/* ===== 更多选项折叠条 ===== */
.more-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 4px 0 14px;
  padding: 11px 14px;
  font-size: 13px;
  text-align: left;
  color: var(--c-dim);
  border: 1px dashed rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.more-toggle:hover {
  color: var(--c-text);
  border-color: rgba(255, 255, 255, 0.34);
  background: rgba(255, 255, 255, 0.05);
}
.more-toggle.on {
  border-style: solid;
  border-color: rgba(255, 140, 26, 0.45);
  background: rgba(255, 140, 26, 0.08);
}
.more-title { flex: none; letter-spacing: 2px; color: var(--c-text); }
.more-summary {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.more-arrow { flex: none; font-size: 12px; letter-spacing: 1px; }

.more-body {
  padding: 14px 16px 2px;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
}
.more-body .auth-field:last-child { margin-bottom: 14px; }

/* 正文上方的表情/链接/文件工具栏 */
.ed-toolbar { margin-bottom: 8px; }

.ed-select,
.ed-textarea {
  width: 100%;
  padding: 12px 14px;
  font-size: 15px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  font-family: inherit;
}
.ed-select { font-size: 14px; }
.ed-select option { background: #14141f; }
.ed-textarea { line-height: 1.8; resize: vertical; }
.ed-select:focus,
.ed-textarea:focus {
  border-color: var(--c-ninja);
  box-shadow: 0 0 0 3px rgba(255, 140, 26, 0.12);
}

.tag-picker { display: flex; gap: 8px; flex-wrap: wrap; }
.tag-toggle {
  padding: 6px 14px;
  font-size: 12px;
  border-radius: 14px;
  color: var(--c-dim);
  border: 1px solid rgba(255, 255, 255, 0.16);
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.tag-toggle:hover { color: var(--c-shinigami); border-color: var(--c-shinigami); }
.tag-toggle.picked {
  color: var(--c-text);
  border-color: var(--c-shinigami);
  background: rgba(0, 229, 255, 0.14);
}

/* 二选一选择器复用 main.css 的 .seg-btn，此处只保留布局与选中态高亮 */
.status-picker { display: flex; gap: 12px; }
.status-picker .seg-btn { flex: 1; padding: 10px 0; }
.status-picker .seg-btn.on {
  border-color: var(--c-ninja);
  box-shadow: 0 0 16px rgba(255, 140, 26, 0.3);
}

.editor-actions {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}
.submit { padding: 12px 32px; font-size: 15px; letter-spacing: 3px; }
.danger { color: #ff8080; margin-left: auto; }

@media (max-width: 700px) {
  .editor-form { padding: 20px 18px; }
}
</style>
