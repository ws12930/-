<template>
  <div class="home">
    <TopBar active="home" show-search v-model:keyword="keyword" @search="doSearch" />

    <div class="pg-shell wide home-shell">
      <header class="pg-head">
        <h1 class="pg-title grad-text">首 页</h1>
        <p class="pg-sub grad-text">看看大家最近写了什么</p>
      </header>

      <!-- 首屏轮播栏：挂载期间背景定格，避免两个轮播同时动 -->
      <BannerCarousel class="home-banner" />

      <div v-if="categories.length" class="chip-row">
        <button
          class="seg-btn"
          :class="{ on: !activeCategory }"
          type="button"
          @click="pickCategory(null)"
        >
          全部
        </button>
        <button
          v-for="c in categories"
          :key="c.id"
          class="seg-btn"
          :class="{ on: activeCategory === c.id }"
          type="button"
          @click="pickCategory(c.id)"
        >
          {{ c.name }}
        </button>
      </div>

      <p v-if="error" class="home-error">{{ error }}</p>

      <div v-if="loading" class="empty-state">
        <span class="es-ico">🌾</span>
        <span>正在加载…</span>
      </div>

      <div v-else-if="!posts.length" class="empty-state">
        <span class="es-ico">📝</span>
        <span>还没有文章，去写一篇吧</span>
      </div>

      <ul v-else class="post-list">
        <li v-for="p in posts" :key="p.id" class="post-card panel hoverable" @click="openPost(p)">
          <div class="pc-head">
            <UserAvatar :user="p.author || {}" :size="30" />
            <div class="pc-meta">
              <span class="pc-author">{{ p.author?.nickname || p.author?.username || '匿名' }}</span>
              <span class="pc-time">{{ formatTime(p.createdAt) }}</span>
            </div>
            <span v-if="p.pinned" class="pc-pin">置顶</span>
          </div>

          <h2 class="pc-title">{{ p.title }}</h2>
          <p class="pc-summary">{{ summary(p) }}</p>

          <div class="pc-foot">
            <span class="pc-tag" v-for="t in (p.tags || []).slice(0, 3)" :key="t">{{ t }}</span>
            <span class="pc-spacer"></span>
            <button class="mini-btn" :class="{ on: p.liked }" type="button" @click.stop="like(p)">
              {{ p.liked ? '♥' : '♡' }} {{ p.likeCount || 0 }}
            </button>
            <button
              v-if="isMine(p)"
              class="mini-btn danger"
              type="button"
              @click.stop="remove(p)"
            >
              删除
            </button>
          </div>
        </li>
      </ul>

      <div v-if="total > pageSize" class="pager">
        <button class="g-btn g-btn-sm" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="pager-info">{{ page }} / {{ totalPages }}</span>
        <button class="g-btn g-btn-sm" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { listPosts, listCategories, deletePost, setPinned } from '../api/blog'
import { toggleLike } from '../api/social'
import TopBar from '../components/TopBar.vue'
import BannerCarousel from '../components/BannerCarousel.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { pauseBg, resumeBg } from '../utils/bgState'
import { toPlainSummary } from '../utils/richText'
import { withPostAuthors } from '../utils/author'

const router = useRouter()

const posts = ref([])
const categories = ref([])
const activeCategory = ref(null)
const keyword = ref('')
const appliedKeyword = ref('')
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const me = ref(null)
try {
  me.value = JSON.parse(localStorage.getItem('bn-user') || 'null')
} catch {
  me.value = null
}

function isMine(p) {
  return !!me.value && !!p.author && p.author.id === me.value.id
}

function summary(p) {
  return toPlainSummary(p.summary || p.content || '', 90)
}

function formatTime(v) {
  if (!v) return ''
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v).slice(0, 10)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await listPosts({
      current: page.value,
      size: pageSize,
      categoryId: activeCategory.value || undefined,
      keyword: appliedKeyword.value || undefined
    })
    const data = res.data || {}
    posts.value = withPostAuthors(data.records || data.list || [])
    total.value = data.total ?? posts.value.length
  } catch (e) {
    error.value = e.message || '加载失败'
    posts.value = []
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await listCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

function pickCategory(id) {
  activeCategory.value = id
  page.value = 1
  load()
}

function doSearch() {
  appliedKeyword.value = keyword.value.trim()
  page.value = 1
  load()
}

function goPage(n) {
  if (n < 1 || n > totalPages.value) return
  page.value = n
  load()
}

function openPost(p) {
  if (p.slug) router.push({ name: 'post', params: { slug: p.slug } })
}

async function like(p) {
  try {
    await toggleLike(p.id)
    p.liked = !p.liked
    p.likeCount = (p.likeCount || 0) + (p.liked ? 1 : -1)
  } catch (e) {
    error.value = e.message || '操作失败'
  }
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

onMounted(() => {
  pauseBg()
  load()
  loadCategories()
})

onBeforeUnmount(() => {
  resumeBg()
})
</script>

<style scoped>
.home { height: 100%; overflow-y: auto; }
.home-shell { padding-bottom: 48px; }

.home-banner { margin-bottom: 18px; }

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 18px;
}

.home-error {
  text-align: center;
  color: #ff8a8a;
  font-size: 13px;
  margin-bottom: 12px;
}

.post-list { list-style: none; display: flex; flex-direction: column; gap: 14px; }

.post-card { padding: 16px 18px; }

.pc-head { display: flex; align-items: center; gap: 10px; }
.pc-meta { display: flex; flex-direction: column; line-height: 1.3; }
.pc-author { font-size: 13px; color: var(--c-text); }
.pc-time { font-size: 11px; color: var(--c-dim); }
.pc-pin {
  margin-left: auto;
  font-size: 11px;
  color: var(--c-acid);
  border: 1px solid rgba(217, 255, 77, 0.35);
  border-radius: 10px;
  padding: 2px 8px;
}

.pc-title {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 12px 0 6px;
  letter-spacing: 1px;
}

.pc-summary {
  font-size: 13px;
  color: var(--c-dim);
  line-height: 1.7;
  margin-bottom: 12px;
}

.pc-foot { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pc-tag {
  font-size: 11px;
  color: var(--c-mint);
  border: 1px solid rgba(194, 214, 204, 0.28);
  border-radius: 10px;
  padding: 2px 8px;
}
.pc-spacer { flex: 1; }

.pager { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 22px; }
.pager-info { font-size: 12px; color: var(--c-dim); }
</style>
