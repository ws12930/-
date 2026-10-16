<template>
  <div class="blog-page">
    <div class="auth-bg left"></div>
    <div class="auth-bg right"></div>

    <PageNav :items="navItems" />

    <div class="blog-main pg-shell mid">
      <header class="pg-head">
        <h1 class="grad-text pg-title">FENGLIN BLOG</h1>
        <p class="grad-text pg-sub">{{ isMine ? '我 的 文 章' : '博 客 文 章' }}</p>
      </header>

      <div class="panel toolbar" v-if="!isMine">
        <input
          v-model.trim="keyword"
          class="tool-input"
          type="search"
          placeholder="搜索标题或摘要"
          @keyup.enter="reload"
        />
        <select v-model="categoryId" class="tool-select" @change="reload">
          <option value="">全部分类</option>
          <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
        </select>
        <select v-model="tagId" class="tool-select" @change="reload">
          <option value="">全部标签</option>
          <option v-for="t in tags" :key="t.id" :value="String(t.id)">{{ t.name }}</option>
        </select>
        <button class="g-btn g-btn-sm tool-btn" @click="reload">搜 索</button>
      </div>

      <p v-if="error" class="auth-alert err">{{ error }}</p>
      <p v-if="loading" class="blog-empty">加载中…</p>

      <div v-else class="post-list">
        <article
          v-for="post in posts"
          :key="post.id"
          class="panel post-card"
          @click="openPost(post)"
        >
          <h2 class="post-title">{{ post.title }}</h2>
          <p class="post-summary">{{ post.summary || '（暂无摘要）' }}</p>
          <div class="post-meta">
            <UserAvatar
              :avatar="post.authorAvatar"
              :name="post.authorName"
              :faction="post.authorFaction"
              :size="20"
            />
            <span class="author" @click.stop="openAuthor(post)">{{ post.authorName }}</span>
            <span class="sep">·</span>
            <span>{{ post.categoryName }}</span>
            <span class="sep">·</span>
            <span>{{ formatDate(post.publishedAt || post.createdAt) }}</span>
            <span class="sep">·</span>
            <span>{{ post.viewCount || 0 }} 次阅读</span>
            <span v-if="isMine" class="badge">{{ post.status === 'PUBLISHED' ? '已发布' : '草稿' }}</span>
            <span class="meta-actions" @click.stop>
              <button class="mini-btn" :class="{ on: post.liked }" @click="like(post)">
                ♥ {{ post.likeCount || 0 }}
              </button>
              <button class="mini-btn" @click="openPost(post)">评论 {{ post.commentCount || 0 }}</button>
            </span>
          </div>
          <div class="post-tags" v-if="post.tags && post.tags.length">
            <span v-for="t in post.tags" :key="t.id" class="tag-chip">{{ t.name }}</span>
          </div>
          <div class="post-actions" v-if="isMine" @click.stop>
            <button class="auth-link" @click="editPost(post)">编辑</button>
            <button class="auth-link danger" @click="removePost(post)">删除</button>
          </div>
        </article>

        <p v-if="!posts.length" class="blog-empty">暂无文章</p>
      </div>

      <div class="pager" v-if="totalPages > 1">
        <button class="g-btn g-btn-sm" :disabled="current <= 1" @click="changePage(current - 1)">
          上一页
        </button>
        <span class="page-info">{{ current }} / {{ totalPages }}</span>
        <button class="g-btn g-btn-sm" :disabled="current >= totalPages" @click="changePage(current + 1)">
          下一页
        </button>
      </div>
    </div>

    <p v-if="hint" class="auth-toast">{{ hint }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { listPosts, listMyPosts, listCategories, listTags, deletePost } from '../api/blog'
import { getToken } from '../api/auth'
import { toggleLike } from '../api/social'
import UserAvatar from '../components/UserAvatar.vue'
import PageNav from '../components/PageNav.vue'

const router = useRouter()
const route = useRoute()

const posts = ref([])
const categories = ref([])
const tags = ref([])
const keyword = ref('')
const categoryId = ref('')
const tagId = ref('')
const current = ref(1)
const totalPages = ref(1)
const loading = ref(false)
const error = ref('')
const hint = ref('')

const isMine = computed(() => route.name === 'mine')
const logged = computed(() => !!getToken())

/* 右上角统一导航：未登录时只给公开入口 */
const navItems = computed(() => {
  const items = [{ label: '返回首页', to: 'home' }]
  if (!isMine.value) items.push({ label: '全部文章', to: 'blog' })
  if (logged.value) {
    items.push({ label: '我的文章', to: 'mine' })
    items.push({ label: '写文章', to: 'editor' })
  }
  return items
})

let hintTimer = null
function toast(msg) {
  hint.value = msg
  clearTimeout(hintTimer)
  hintTimer = setTimeout(() => (hint.value = ''), 2200)
}
onBeforeUnmount(() => clearTimeout(hintTimer))

async function loadTaxonomy() {
  try {
    const [c, t] = await Promise.all([listCategories(), listTags()])
    categories.value = c.data || []
    tags.value = t.data || []
  } catch (e) {
    // 分类标签加载失败不阻塞文章列表
  }
}

async function load(page = 1) {
  loading.value = true
  error.value = ''
  current.value = page
  try {
    const params = { current: page, size: 10 }
    if (!isMine.value) {
      if (keyword.value) params.keyword = keyword.value
      if (categoryId.value) params.categoryId = categoryId.value
      if (tagId.value) params.tagId = tagId.value
    }
    const res = isMine.value ? await listMyPosts(params) : await listPosts(params)
    posts.value = res.data?.records || []
    totalPages.value = Number(res.data?.pages || 1)
  } catch (e) {
    error.value = e.message || '文章加载失败'
    posts.value = []
  } finally {
    loading.value = false
  }
}

function reload() {
  load(1)
}

function changePage(page) {
  if (page >= 1 && page <= totalPages.value) {
    load(page)
  }
}

function openPost(post) {
  if (post.status !== 'PUBLISHED') {
    toast('草稿暂不可公开访问，可在“我的文章”中编辑')
    return
  }
  router.push({ name: 'post', params: { slug: post.slug } })
}

async function like(post) {
  if (!logged.value) {
    toast('请先登录')
    return
  }
  try {
    const res = await toggleLike(post.id)
    post.likeCount = res.data.likeCount
    post.liked = res.data.liked
  } catch (e) {
    toast(e.message || '操作失败')
  }
}

function openAuthor(post) {
  if (!post.authorId) return
  router.push({ name: 'user', params: { id: post.authorId } })
}

function editPost(post) {
  router.push({ name: 'editor', query: { id: post.id } })
}

async function removePost(post) {
  if (!window.confirm(`确认删除文章《${post.title}》？`)) return
  try {
    await deletePost(post.id)
    toast('文章已删除')
    load(current.value)
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

function formatDate(v) {
  if (!v) return ''
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v).slice(0, 10)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  loadTaxonomy()
  load(1)
})
</script>

<style scoped>
.blog-page {
  height: 100%;
  position: relative;
  display: flex;
  justify-content: center;
  overflow: hidden;
}

/* 宽度与顶部留白由全局 .pg-shell 提供，这里只保留滚动行为 */
.blog-main {
  max-height: 100%;
  overflow-y: auto;
}

.toolbar {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.tool-input,
.tool-select {
  flex: 1;
  min-width: 140px;
  padding: 10px 12px;
  font-size: 14px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
}
.tool-input:focus,
.tool-select:focus {
  border-color: var(--c-ninja);
  box-shadow: 0 0 0 3px rgba(255, 140, 26, 0.12);
}
.tool-select option { background: #14141f; }
/* 尺寸由 main.css 的 .g-btn-sm 统一提供，此处只保留布局约束 */
.tool-btn { flex: none; }

.post-list { display: flex; flex-direction: column; gap: 14px; }

.post-card {
  padding: 20px 22px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}
.post-card:hover {
  transform: translateY(-3px);
  border-color: rgba(255, 255, 255, 0.22);
  box-shadow: 0 6px 24px rgba(255, 140, 26, 0.16), 0 6px 24px rgba(0, 128, 255, 0.16);
}

.post-title {
  font-family: var(--font-display);
  font-size: 19px;
  letter-spacing: 1px;
  color: var(--c-text);
}
.post-summary {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--c-dim);
}
.post-meta {
  margin-top: 12px;
  font-size: 12px;
  color: var(--c-dim);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.post-meta .sep { opacity: 0.5; }
.author { color: var(--c-text); cursor: pointer; }
.author:hover { text-decoration: underline; }

.meta-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
  flex: none;
}
/* 次级按钮样式收敛到 main.css 的 .mini-btn，此处不再重复定义 */

.badge {
  padding: 2px 8px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: var(--c-text);
}

.post-tags { margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap; }
.tag-chip {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  color: var(--c-shinigami);
  border: 1px solid rgba(0, 229, 255, 0.35);
  background: rgba(0, 229, 255, 0.08);
}

.post-actions {
  margin-top: 14px;
  display: flex;
  gap: 16px;
  font-size: 13px;
}
.post-actions .danger { color: #ff8080; }

.blog-empty {
  text-align: center;
  font-size: 14px;
  color: var(--c-dim);
  padding: 30px 0;
  letter-spacing: 1px;
}

.pager {
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}
/* 分页按钮尺寸由 .g-btn-sm 提供 */
.page-info { font-size: 13px; color: var(--c-dim); }

@media (max-width: 700px) {
  .toolbar { flex-direction: column; }
  .tool-btn { width: 100%; }
}
</style>
