<template>
  <div class="blog-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell wide">
      <header class="pg-head">
        <h1 class="pg-title grad-text">{{ isMine ? '我 的' : '广 场' }}</h1>
        <p class="pg-sub grad-text">{{ isMine ? '草稿与已发布' : '全部公开文章' }}</p>
      </header>

      <div class="seg-row">
        <button class="seg-btn" :class="{ on: scope === 'all' }" type="button" @click="scope = 'all'; reload()">全部</button>
        <button class="seg-btn" :class="{ on: scope === 'published' }" type="button" @click="scope = 'published'; reload()">已发布</button>
        <button v-if="isMine" class="seg-btn" :class="{ on: scope === 'draft' }" type="button" @click="scope = 'draft'; reload()">草稿</button>
        <button class="g-btn g-btn-sm write-btn" type="button" @click="goEditor">写文章</button>
      </div>

      <p v-if="error" class="blog-error">{{ error }}</p>

      <div v-if="loading" class="empty-state"><span class="es-ico">🌾</span><span>正在加载…</span></div>
      <div v-else-if="!rows.length" class="empty-state"><span class="es-ico">📝</span><span>这里还没有内容</span></div>

      <ul v-else class="post-list">
        <li v-for="p in rows" :key="p.id" class="post-card panel hoverable" @click="open(p)">
          <h2 class="pc-title">{{ p.title }}</h2>
          <p class="pc-summary">{{ summary(p) }}</p>
          <div class="pc-foot">
            <span v-if="isDraft(p)" class="pc-draft">草稿</span>
            <span v-else-if="p.status && p.status !== 'PUBLISHED'" class="pc-status">{{ p.status }}</span>
            <span class="pc-time">{{ formatTime(p.createdAt) }}</span>
            <span class="pc-spacer"></span>
            <button
              v-if="isMine"
              class="mini-btn"
              type="button"
              @click.stop="togglePin(p)"
            >
              {{ p.pinned ? '取消置顶' : '置顶' }}
            </button>
            <button v-if="isMine" class="mini-btn" type="button" @click.stop="edit(p)">编辑</button>
            <button v-if="isMine" class="mini-btn danger" type="button" @click.stop="remove(p)">删除</button>
          </div>
        </li>
      </ul>

      <div v-if="totalPages > 1" class="pager">
        <button class="g-btn g-btn-sm" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="pager-info">{{ page }} / {{ totalPages }}</span>
        <button class="g-btn g-btn-sm" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { listPosts, listMyPosts, deletePost, setPinned } from '../api/blog'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import { toPlainSummary } from '../utils/richText'

const router = useRouter()
const route = useRoute()

const isMine = computed(() => route.name === 'mine')
const rows = ref([])
const scope = ref('all')
const loading = ref(false)
const error = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

const navItems = [
  { label: '首页', to: 'home' },
  { label: '写文章', to: 'editor' }
]

function summary(p) {
  return toPlainSummary(p.summary || p.content || '', 90)
}

/** 草稿判定：状态是字符串，忽略大小写；历史数据里的数字 0 也视为草稿 */
function isDraft(p) {
  const s = String(p?.status ?? '').trim().toUpperCase()
  return s === 'DRAFT' || s === '0'
}

function formatTime(v) {
  if (!v) return ''
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v).slice(0, 10)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

async function load() {
  loading.value = true
  error.value = ''
  const params = { current: page.value, size }
  // 状态是字符串：DRAFT 才是草稿，其余（含自定义值）都算已公开
  if (scope.value === 'published') params.status = 'PUBLISHED'
  if (scope.value === 'draft') params.status = 'DRAFT'
  try {
    const res = isMine.value ? await listMyPosts(params) : await listPosts(params)
    const data = res.data || {}
    rows.value = data.records || data.list || []
    total.value = data.total ?? rows.value.length
  } catch (e) {
    error.value = e.message || '加载失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function goPage(n) {
  if (n < 1 || n > totalPages.value) return
  page.value = n
  load()
}

function open(p) {
  if (!p.slug) return
  router.push({ name: 'post', params: { slug: p.slug } })
}

function edit(p) {
  router.push({ name: 'editor', query: { id: p.id } })
}

function goEditor() {
  router.push({ name: 'editor' })
}

async function remove(p) {
  if (!window.confirm(`确定删除《${p.title}》？`)) return
  try {
    await deletePost(p.id)
    await load()
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

async function togglePin(p) {
  try {
    await setPinned(p.id, !p.pinned)
    await load()
  } catch (e) {
    error.value = e.message || '操作失败'
  }
}

onMounted(load)
</script>

<style scoped>
.blog-page { height: 100%; overflow-y: auto; }

.seg-row { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.write-btn { margin-left: auto; }

.blog-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }

.post-list { list-style: none; display: flex; flex-direction: column; gap: 14px; }
.post-card { padding: 16px 18px; }
.pc-title { font-family: var(--font-display); font-size: 18px; margin-bottom: 6px; letter-spacing: 1px; }
.pc-summary { font-size: 13px; color: var(--c-dim); line-height: 1.7; margin-bottom: 12px; }
.pc-foot { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pc-draft {
  font-size: 11px;
  color: #ffd479;
  border: 1px solid rgba(255, 212, 121, 0.35);
  border-radius: 10px;
  padding: 2px 8px;
}
/* 自定义状态：中性色，和草稿的黄色区分开 */
.pc-status {
  font-size: 11px;
  color: var(--c-mint);
  border: 1px solid rgba(143, 208, 174, 0.32);
  border-radius: 10px;
  padding: 2px 8px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pc-time { font-size: 11px; color: var(--c-dim); }
.pc-spacer { flex: 1; }

.pager { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 22px; }
.pager-info { font-size: 12px; color: var(--c-dim); }
</style>
