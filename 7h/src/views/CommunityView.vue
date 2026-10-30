<template>
  <div class="community-page">
    <TopBar back="home" back-label="返回首页" />

    <div class="pg-shell mid">
      <header class="pg-head">
        <h1 class="pg-title grad-text">社 区 广 场</h1>
        <p class="pg-sub grad-text">像逛贴吧一样，逛大家的文章</p>
      </header>

      <!-- 吧导航：分类即吧 -->
      <nav class="ba-nav">
        <button
          class="ba-chip"
          :class="{ on: categoryId === '' }"
          type="button"
          @click="switchBa('')"
        >
          全部
        </button>
        <button
          v-for="ba in bars"
          :key="ba.id"
          class="ba-chip"
          :class="{ on: categoryId === String(ba.id) }"
          type="button"
          @click="switchBa(String(ba.id))"
        >
          {{ ba.name }}<span class="ba-cnt">{{ ba.count }}</span>
        </button>
      </nav>

      <!-- 排序 + 搜索 -->
      <div class="cp-toolbar">
        <div class="cp-tabs">
          <button
            class="cp-tab"
            :class="{ on: sort === 'hot' }"
            type="button"
            @click="switchSort('hot')"
          >
            🔥 热门
          </button>
          <button
            class="cp-tab"
            :class="{ on: sort === 'latest' }"
            type="button"
            @click="switchSort('latest')"
          >
            🕘 最新
          </button>
        </div>
        <input
          v-model.trim="keyword"
          class="cp-search"
          type="search"
          placeholder="搜索标题 / 摘要"
          @keydown.enter="load(1)"
        />
        <button class="g-btn g-btn-sm" :disabled="loading" @click="load(1)">搜索</button>
      </div>

      <p v-if="error" class="cp-error">{{ error }}</p>

      <div v-if="loading" class="empty-state"><span class="es-ico">🌾</span><span>正在加载…</span></div>
      <div v-else-if="!posts.length" class="empty-state">
        <span class="es-ico">🫧</span>
        <span>{{ keyword ? '没有匹配的帖子' : '这个吧还没有帖子，去写第一篇吧' }}</span>
      </div>

      <!-- 帖子流 -->
      <ul v-else class="tb-list">
        <li
          v-for="p in posts"
          :key="p.id"
          class="panel hoverable tb-item"
          @click="openPost(p)"
        >
          <UserAvatar :user="authorOf(p)" :size="44" />
          <div class="tb-main">
            <div class="tb-title-line">
              <span v-if="p.pinned" class="tb-badge pin">顶</span>
              <span class="tb-title">{{ p.title || '（无标题）' }}</span>
            </div>
            <p class="tb-summary">{{ plainSummary(p.summary) || '作者没有写摘要' }}</p>
            <div class="tb-meta">
              <span class="tb-author">{{ p.authorName || '匿名' }}</span>
              <span class="tb-ba">『{{ p.categoryName || '无吧' }}』</span>
              <span class="tb-stat reply">{{ p.commentCount ?? 0 }} 回复</span>
              <span class="tb-stat">{{ p.viewCount ?? 0 }} 浏览</span>
              <span class="tb-stat">赞 {{ p.likeCount ?? 0 }}</span>
              <span class="tb-time">{{ formatTime(p.publishedAt || p.createdAt) }}</span>
            </div>
          </div>
        </li>
      </ul>

      <Pager
        :current="page.current" :pages="page.pages" :total="page.total"
        :disabled="loading"
        @go="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listPosts, listCategoryStats } from '../api/blog'
import TopBar from '../components/TopBar.vue'
import UserAvatar from '../components/UserAvatar.vue'
import Pager from '../components/Pager.vue'
import { toPlainSummary } from '../utils/richText'

const router = useRouter()

const bars = ref([])          // 吧（分类 + 帖子数）
const posts = ref([])
const keyword = ref('')
const categoryId = ref('')    // 当前选中的吧；'' = 全部
const sort = ref('hot')       // hot | latest
const loading = ref(false)
const error = ref('')
const page = ref({ current: 1, size: 10, total: 0, pages: 1 })

const plainSummary = (s) => toPlainSummary(s, 60)

function authorOf(p) {
  return { avatar: p.authorAvatar, faction: p.authorFaction, username: p.authorName, nickname: p.authorName }
}

function formatTime(v) {
  if (!v) return ''
  const d = new Date(String(v).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(v).slice(0, 10)
  const diff = Date.now() - d.getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return min + ' 分钟前'
  const hour = Math.floor(min / 60)
  if (hour < 24) return hour + ' 小时前'
  const day = Math.floor(hour / 24)
  if (day < 30) return day + ' 天前'
  return String(v).slice(0, 10)
}

async function loadBars() {
  try {
    const res = await listCategoryStats()
    bars.value = res.data || []
  } catch {
    bars.value = []
  }
}

function pickPage(data, prev) {
  return {
    current: data?.current || 1,
    size: data?.size || prev.size,
    total: data?.total || 0,
    pages: data?.pages || 1
  }
}

async function load(next = 1) {
  loading.value = true
  error.value = ''
  try {
    const res = await listPosts({
      current: next,
      size: page.value.size,
      keyword: keyword.value,
      categoryId: categoryId.value || undefined,
      sort: sort.value
    })
    posts.value = res.data?.records || []
    page.value = pickPage(res.data, page.value)
  } catch (e) {
    error.value = e.message || '加载失败'
    posts.value = []
  } finally {
    loading.value = false
  }
}

function switchBa(id) {
  if (categoryId.value === id) return
  categoryId.value = id
  load(1)
}

function switchSort(s) {
  if (sort.value === s) return
  sort.value = s
  load(1)
}

function openPost(p) {
  router.push({ name: 'post', params: { slug: p.slug } })
}

onMounted(() => {
  loadBars()
  load(1)
})
</script>

<style scoped>
.community-page {
  min-height: 100vh;
  padding: 96px 16px 48px;
  box-sizing: border-box;
}
.community-page .pg-shell {
  max-width: 860px;
}

/* 吧导航 */
.ba-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0 12px;
}
.ba-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 15px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--c-dim);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}
.ba-chip:hover { color: var(--c-text); border-color: rgba(255, 255, 255, 0.3); }
.ba-chip.on {
  color: #fff;
  background: rgba(150, 182, 168, 0.16);
  border-color: var(--c-ninja);
}
.ba-cnt {
  font-size: 11px;
  color: var(--c-dim);
  background: rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  padding: 1px 7px;
}
.ba-chip.on .ba-cnt { color: #fff; background: rgba(255, 255, 255, 0.16); }

/* 排序 + 搜索 */
.cp-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.cp-tabs { display: flex; gap: 6px; }
.cp-tab {
  padding: 8px 16px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--c-dim);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}
.cp-tab:hover { color: var(--c-text); }
.cp-tab.on {
  color: #fff;
  border-bottom-color: var(--c-ninja);
}
.cp-search {
  flex: 1;
  max-width: 280px;
  margin-left: auto;
  padding: 9px 14px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  outline: none;
}
.cp-search:focus { border-color: var(--c-ninja); }

.cp-error {
  padding: 10px 14px;
  font-size: 13px;
  color: #ff8a8a;
  background: rgba(255, 107, 107, 0.08);
  border: 1px solid rgba(255, 107, 107, 0.25);
  border-radius: 10px;
}

/* 帖子流 */
.tb-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  list-style: none;
  margin: 0;
  padding: 0;
}
.tb-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
}
.tb-main {
  flex: 1;
  min-width: 0;
}
.tb-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.tb-badge {
  flex: none;
  padding: 1px 7px;
  font-size: 11px;
  border-radius: 4px;
  letter-spacing: 0;
}
.tb-badge.pin {
  color: #f0c674;
  background: rgba(240, 198, 116, 0.14);
}
.tb-title {
  font-size: 15px;
  color: var(--c-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tb-item:hover .tb-title { color: var(--c-ninja); }
.tb-summary {
  margin: 6px 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--c-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tb-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 11.5px;
  color: var(--c-dim);
}
.tb-author { color: var(--c-text); }
.tb-ba { color: var(--c-ninja); }
.tb-stat.reply { color: #9fb8d8; }
.tb-time { margin-left: auto; }

@media (max-width: 700px) {
  .cp-toolbar { flex-wrap: wrap; }
  .cp-search { max-width: none; flex-basis: 100%; order: 3; }
  .tb-time { display: none; }
}
</style>
