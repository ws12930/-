<template>
  <div class="admin-page">
    <TopBar back="home" back-label="返回首页" />

    <div class="pg-shell mid">
      <header class="pg-head">
        <h1 class="pg-title grad-text">管 理 后 台</h1>
        <p class="pg-sub grad-text">文章 · 评论审核 · 用户 · 签到 · 举报</p>
      </header>

      <div class="ad-tabs">
        <button
          v-for="item in tabs"
          :key="item.key"
          class="ad-tab"
          :class="{ on: tab === item.key }"
          type="button"
          @click="switchTab(item.key)"
        >
          {{ item.label }}
          <span v-if="item.key === 'reports' && pendingCount" class="ad-badge">{{ pendingCount }}</span>
        </button>
      </div>

      <!-- ================= 文章 ================= -->
      <section v-if="tab === 'posts'">
        <div class="ad-toolbar">
          <input
            v-model.trim="postQuery.keyword"
            class="ad-input"
            type="search"
            placeholder="按标题搜索"
            @keydown.enter="loadPosts(1)"
          />
          <select v-model="postQuery.status" class="ad-select" @change="loadPosts(1)">
            <option value="">全部状态</option>
            <option value="PUBLISHED">已发布</option>
            <option value="DRAFT">草稿</option>
          </select>
          <button class="g-btn g-btn-sm" :disabled="loading.posts" @click="loadPosts(1)">查询</button>
        </div>

        <p v-if="error.posts" class="ad-error">{{ error.posts }}</p>
        <div v-if="loading.posts && !posts.length" class="empty-state">
          <span class="es-ico">🌾</span><span>正在加载…</span>
        </div>
        <div v-else-if="!posts.length" class="empty-state">
          <span class="es-ico">🗂️</span><span>没有符合条件的文章</span>
        </div>

        <ul v-else class="ad-list">
          <li v-for="p in posts" :key="p.id" class="panel ad-row">
            <div class="ad-main">
              <div class="ad-line">
                <span v-if="p.pinned" class="ad-tag pin">顶</span>
                <span class="ad-tag" :class="isDraft(p) ? 'draft' : 'pub'">{{ isDraft(p) ? '草稿' : '已发布' }}</span>
                <a class="ad-title" :href="'#/posts/' + p.slug">{{ p.title || '（无标题）' }}</a>
              </div>
              <span class="ad-meta">
                #{{ p.id }} · 浏览 {{ p.viewCount ?? 0 }} · 赞 {{ p.likeCount ?? 0 }} · 评论 {{ p.commentCount ?? 0 }} ·
                {{ formatTime(p.createdAt) }}
              </span>
            </div>
            <div class="ad-actions">
              <button class="mini-btn" type="button" :disabled="busy" @click="togglePin(p)">
                {{ p.pinned ? '取消置顶' : '置顶' }}
              </button>
              <button class="mini-btn danger" type="button" :disabled="busy" @click="removePost(p)">删除</button>
            </div>
          </li>
        </ul>

        <Pager
          :current="postPage.current" :pages="postPage.pages" :total="postPage.total"
          :disabled="loading.posts" @go="loadPosts"
        />
      </section>

      <!-- ================= 评论审核 ================= -->
      <section v-else-if="tab === 'comments'">
        <div class="ad-toolbar">
          <input
            v-model.trim="commentQuery.keyword"
            class="ad-input"
            type="search"
            placeholder="按评论内容搜索"
            @keydown.enter="loadComments(1)"
          />
          <select v-model="commentQuery.status" class="ad-select" @change="loadComments(1)">
            <option value="ALL">全部评论</option>
            <option value="NORMAL">正常展示</option>
            <option value="HIDDEN">已隐藏</option>
          </select>
          <button class="g-btn g-btn-sm" :disabled="loading.comments" @click="loadComments(1)">查询</button>
        </div>

        <p v-if="error.comments" class="ad-error">{{ error.comments }}</p>
        <div v-if="loading.comments && !comments.length" class="empty-state">
          <span class="es-ico">💬</span><span>正在加载…</span>
        </div>
        <div v-else-if="!comments.length" class="empty-state">
          <span class="es-ico">💬</span><span>没有符合条件的评论</span>
        </div>

        <ul v-else class="ad-list">
          <li v-for="c in comments" :key="c.id" class="panel ad-row ad-row-top">
            <div class="ad-main">
              <div class="ad-line">
                <span class="ad-tag" :class="c.hidden ? 'draft' : 'pub'">{{ c.hidden ? '已隐藏' : '正常' }}</span>
                <span class="ad-title plain">{{ c.authorName || '匿名用户' }}</span>
                <span class="ad-meta">
                  评论于
                  <a v-if="c.postSlug" class="ad-link" :href="'#/posts/' + c.postSlug">{{ c.postTitle || '（无标题）' }}</a>
                  <template v-else>{{ c.postTitle || '（文章已删除）' }}</template>
                </span>
              </div>
              <p class="ad-content">{{ c.content }}</p>
              <span class="ad-meta">#{{ c.id }} · 用户 {{ c.userId }} · {{ formatTime(c.createdAt) }}</span>
            </div>
            <div class="ad-actions">
              <button class="mini-btn" type="button" :disabled="busy" @click="toggleComment(c)">
                {{ c.hidden ? '恢复展示' : '隐藏下架' }}
              </button>
              <button class="mini-btn danger" type="button" :disabled="busy" @click="removeComment(c)">删除</button>
            </div>
          </li>
        </ul>

        <Pager
          :current="commentPage.current" :pages="commentPage.pages" :total="commentPage.total"
          :disabled="loading.comments" @go="loadComments"
        />
      </section>

      <!-- ================= 用户 ================= -->
      <section v-else-if="tab === 'users'">
        <div class="ad-toolbar">
          <input
            v-model.trim="userQuery.keyword"
            class="ad-input"
            type="search"
            placeholder="按昵称 / 用户名搜索"
            @keydown.enter="loadUsers(1)"
          />
          <button class="g-btn g-btn-sm" :disabled="loading.users" @click="loadUsers(1)">查询</button>
        </div>

        <p v-if="error.users" class="ad-error">{{ error.users }}</p>
        <div v-if="!users.length" class="empty-state">
          <span class="es-ico">👥</span><span>{{ loading.users ? '正在加载…' : '没有匹配的用户' }}</span>
        </div>

        <ul v-else class="ad-list">
          <li v-for="u in users" :key="u.id" class="panel ad-row">
            <div class="ad-main">
              <div class="ad-line">
                <span class="ad-tag" :class="u.role === 'ADMIN' ? 'admin' : 'pub'">{{ u.role }}</span>
                <span class="ad-title plain">{{ u.nickname || u.username }}</span>
                <span class="ad-meta">@{{ u.username }}</span>
              </div>
              <span class="ad-meta">
                Lv{{ u.level }} · {{ u.title }} · {{ u.exp }} 经验 · {{ u.email || '未填邮箱' }} · {{ formatTime(u.createdAt) }}
              </span>
            </div>
            <div class="ad-actions">
              <span class="ad-tag" :class="u.status === 1 ? 'pub' : 'draft'">{{ u.status === 1 ? '正常' : '已禁用' }}</span>
              <button class="mini-btn" type="button" :disabled="busy" @click="toggleUser(u)">
                {{ u.status === 1 ? '禁用' : '启用' }}
              </button>
            </div>
          </li>
        </ul>

        <Pager
          :current="userPage.current" :pages="userPage.pages" :total="userPage.total"
          :disabled="loading.users" @go="loadUsers"
        />
      </section>

      <!-- ================= 签到 ================= -->
      <section v-else-if="tab === 'checkins'">
        <div class="ad-stats">
          <div class="panel ad-stat">
            <span class="ad-stat-num">{{ summary.todayUsers }}</span>
            <span class="ad-stat-label">今日签到人数</span>
          </div>
          <div class="panel ad-stat">
            <span class="ad-stat-num">{{ summary.todayCount }}</span>
            <span class="ad-stat-label">今日签到次数</span>
          </div>
          <div class="panel ad-stat">
            <span class="ad-stat-num">{{ summary.totalCount }}</span>
            <span class="ad-stat-label">累计签到次数</span>
          </div>
          <div class="panel ad-stat">
            <span class="ad-stat-num">{{ summary.maxStreak }}</span>
            <span class="ad-stat-label">最长连续天数</span>
          </div>
        </div>

        <div class="ad-toolbar">
          <input
            v-model.trim="checkinQuery.keyword"
            class="ad-input"
            type="search"
            placeholder="按昵称 / 用户名搜索"
            @keydown.enter="loadCheckins(1)"
          />
          <button class="g-btn g-btn-sm" :disabled="loading.checkins" @click="loadCheckins(1)">查询</button>
        </div>

        <p v-if="error.checkins" class="ad-error">{{ error.checkins }}</p>
        <div v-if="loading.checkins && !checkins.length" class="empty-state">
          <span class="es-ico">📅</span><span>正在加载…</span>
        </div>
        <div v-else-if="!checkins.length" class="empty-state">
          <span class="es-ico">📅</span><span>还没有签到记录</span>
        </div>

        <ul v-else class="ad-list">
          <li v-for="k in checkins" :key="k.id" class="panel ad-row">
            <div class="ad-main">
              <div class="ad-line">
                <span class="ad-title plain">{{ k.nickname || k.userName || '未知用户' }}</span>
                <span class="ad-meta">@{{ k.userName }}</span>
                <span class="ad-chip">+{{ k.expGained ?? 0 }} 经验</span>
              </div>
              <span class="ad-meta">
                {{ k.checkinDate }} 签到 · 连续 {{ k.streak ?? 1 }} 天 · {{ formatTime(k.createdAt) }}
              </span>
            </div>
          </li>
        </ul>

        <Pager
          :current="checkinPage.current" :pages="checkinPage.pages" :total="checkinPage.total"
          :disabled="loading.checkins" @go="loadCheckins"
        />
      </section>

      <!-- ================= 举报 ================= -->
      <section v-else>
        <div class="ad-toolbar">
          <select v-model="reportQuery.status" class="ad-select" @change="loadReports(1)">
            <option value="PENDING">待处理</option>
            <option value="HANDLED">已处理</option>
            <option value="IGNORED">已驳回</option>
            <option value="">全部</option>
          </select>
          <button class="g-btn g-btn-sm" :disabled="loading.reports" @click="loadReports(1)">刷新</button>
        </div>

        <p v-if="error.reports" class="ad-error">{{ error.reports }}</p>
        <div v-if="!reports.length" class="empty-state">
          <span class="es-ico">🫧</span><span>{{ loading.reports ? '正在加载…' : '暂无举报，风平浪静' }}</span>
        </div>

        <ul v-else class="ad-list">
          <li v-for="r in reports" :key="r.id" class="panel ad-row">
            <div class="ad-main">
              <div class="ad-line">
                <span class="ad-tag" :class="r.status === 0 ? 'draft' : 'pub'">{{ r.statusText }}</span>
                <span class="ad-title plain">{{ typeLabel(r.targetType) }} #{{ r.targetId }}</span>
                <span class="ad-chip">{{ r.reason }}</span>
              </div>
              <span v-if="r.targetSnapshot" class="ad-meta">内容：{{ r.targetSnapshot }}</span>
              <span v-if="r.detail" class="ad-meta">说明：{{ r.detail }}</span>
              <span class="ad-meta">举报人：{{ r.reporterName || '匿名' }} · {{ formatTime(r.createdAt) }}</span>
            </div>
            <div v-if="r.status === 0" class="ad-actions">
              <button class="mini-btn" type="button" :disabled="busy" @click="resolveReport(r, 'HANDLED')">已处理</button>
              <button class="mini-btn danger" type="button" :disabled="busy" @click="resolveReport(r, 'IGNORED')">驳回</button>
            </div>
          </li>
        </ul>

        <Pager
          :current="reportPage.current" :pages="reportPage.pages" :total="reportPage.total"
          :disabled="loading.reports" @go="loadReports"
        />
      </section>
    </div>

    <div v-if="toast" class="ad-toast">{{ toast }}</div>

    <ConfirmDialog :state="confirmState" @confirm="confirm" @close="close" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import TopBar from '../components/TopBar.vue'
import Pager from '../components/Pager.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { useConfirm } from '../composables/useConfirm'
import { deletePost, setPinned } from '../api/blog'
import {
  adminPosts,
  adminUsers,
  adminReports,
  adminComments,
  adminCheckins,
  adminCheckinSummary,
  setUserStatus,
  setCommentStatus,
  deleteComment,
  handleReport
} from '../api/admin'

const tabs = [
  { key: 'posts', label: '文章' },
  { key: 'comments', label: '评论' },
  { key: 'users', label: '用户' },
  { key: 'checkins', label: '签到' },
  { key: 'reports', label: '举报' }
]
const tab = ref('posts')

const posts = ref([])
const comments = ref([])
const users = ref([])
const checkins = ref([])
const reports = ref([])

const postPage = reactive({ current: 1, size: 10, total: 0, pages: 1 })
const commentPage = reactive({ current: 1, size: 10, total: 0, pages: 1 })
const userPage = reactive({ current: 1, size: 10, total: 0, pages: 1 })
const checkinPage = reactive({ current: 1, size: 10, total: 0, pages: 1 })
const reportPage = reactive({ current: 1, size: 10, total: 0, pages: 1 })

const postQuery = reactive({ keyword: '', status: '' })
const commentQuery = reactive({ keyword: '', status: 'ALL' })
const userQuery = reactive({ keyword: '' })
const checkinQuery = reactive({ keyword: '' })
const reportQuery = reactive({ status: 'PENDING' })

const loading = reactive({ posts: false, comments: false, users: false, checkins: false, reports: false })
const error = reactive({ posts: '', comments: '', users: '', checkins: '', reports: '' })
const summary = reactive({ todayUsers: 0, todayCount: 0, totalCount: 0, maxStreak: 0 })
const pendingCount = ref(0)
const busy = ref(false)
const toast = ref('')
let toastTimer = null

const { state: confirmState, ask: askConfirm, confirm, close } = useConfirm()

const TYPE_LABEL = { POST: '文章', COMMENT: '评论', USER: '用户' }
const typeLabel = (t) => TYPE_LABEL[t] || t || '内容'

function showToast(message) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2400)
}

function formatTime(v) {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}

function isDraft(p) {
  return String(p.status || '').toUpperCase() === 'DRAFT'
}

function pickPage(target, data) {
  target.current = data?.current || 1
  target.total = data?.total || 0
  target.pages = data?.pages || 1
}

async function loadPosts(next = 1) {
  loading.posts = true
  error.posts = ''
  try {
    const res = await adminPosts({
      current: next,
      size: postPage.size,
      keyword: postQuery.keyword,
      status: postQuery.status
    })
    posts.value = res.data?.records || []
    pickPage(postPage, res.data)
  } catch (e) {
    error.posts = e.message || '加载失败'
    posts.value = []
  } finally {
    loading.posts = false
  }
}

async function loadComments(next = 1) {
  loading.comments = true
  error.comments = ''
  try {
    const res = await adminComments({
      current: next,
      size: commentPage.size,
      keyword: commentQuery.keyword,
      status: commentQuery.status
    })
    comments.value = res.data?.records || []
    pickPage(commentPage, res.data)
  } catch (e) {
    error.comments = e.message || '加载失败'
    comments.value = []
  } finally {
    loading.comments = false
  }
}

async function loadUsers(next = 1) {
  loading.users = true
  error.users = ''
  try {
    const res = await adminUsers({ current: next, size: userPage.size, keyword: userQuery.keyword })
    users.value = res.data?.records || []
    pickPage(userPage, res.data)
  } catch (e) {
    error.users = e.message || '加载失败'
    users.value = []
  } finally {
    loading.users = false
  }
}

async function loadCheckins(next = 1) {
  loading.checkins = true
  error.checkins = ''
  try {
    const res = await adminCheckins({
      current: next,
      size: checkinPage.size,
      keyword: checkinQuery.keyword
    })
    checkins.value = res.data?.records || []
    pickPage(checkinPage, res.data)
  } catch (e) {
    error.checkins = e.message || '加载失败'
    checkins.value = []
  } finally {
    loading.checkins = false
  }
  loadSummary()
}

async function loadSummary() {
  try {
    const res = await adminCheckinSummary()
    Object.assign(summary, res.data || {})
  } catch (e) {
    /* 概览失败不影响列表 */
  }
}

async function loadReports(next = 1) {
  loading.reports = true
  error.reports = ''
  try {
    const res = await adminReports({ current: next, size: reportPage.size, status: reportQuery.status })
    reports.value = res.data?.records || []
    pickPage(reportPage, res.data)
  } catch (e) {
    error.reports = e.message || '加载失败'
    reports.value = []
  } finally {
    loading.reports = false
  }
}

async function refreshPending() {
  try {
    const res = await adminReports({ current: 1, size: 1, status: 'PENDING' })
    pendingCount.value = res.data?.total || 0
  } catch {
    pendingCount.value = 0
  }
}

function switchTab(key) {
  if (tab.value === key) return
  tab.value = key
  if (key === 'posts' && !posts.value.length) loadPosts(1)
  else if (key === 'comments') loadComments(commentPage.current)
  else if (key === 'users' && !users.value.length) loadUsers(1)
  else if (key === 'checkins') loadCheckins(checkinPage.current)
  else if (key === 'reports' && !reports.value.length) loadReports(1)
}

async function togglePin(post) {
  busy.value = true
  try {
    const res = await setPinned(post.id, !post.pinned)
    post.pinned = res.data?.pinned ?? !post.pinned
    showToast(post.pinned ? '已置顶' : '已取消置顶')
  } catch (e) {
    showToast(e.message || '操作失败')
  } finally {
    busy.value = false
  }
}

function removePost(post) {
  askConfirm({
    title: '删 除 文 章',
    message: `确定删除《${post.title || '无标题'}》吗？`,
    detail: '删除后无法恢复，文章下的评论也会一并消失。',
    confirmText: '删 除',
    danger: true,
    onConfirm: async () => {
      busy.value = true
      try {
        await deletePost(post.id)
        showToast('文章已删除')
        loadPosts(postPage.current)
      } catch (e) {
        showToast(e.message || '删除失败')
      } finally {
        busy.value = false
      }
    }
  })
}

async function toggleComment(comment) {
  const next = comment.hidden ? 'NORMAL' : 'HIDDEN'
  busy.value = true
  try {
    await setCommentStatus(comment.id, next)
    comment.hidden = next === 'HIDDEN'
    comment.status = next
    showToast(comment.hidden ? '评论已隐藏，前台不再展示' : '评论已恢复展示')
    if (commentQuery.status !== 'ALL') loadComments(commentPage.current)
  } catch (e) {
    showToast(e.message || '操作失败')
  } finally {
    busy.value = false
  }
}

function removeComment(comment) {
  askConfirm({
    title: '删 除 评 论',
    message: '确定删除这条评论吗？',
    detail: `作者：${comment.authorName || '匿名'} · 内容：${(comment.content || '').slice(0, 40)}`,
    confirmText: '删 除',
    danger: true,
    onConfirm: async () => {
      busy.value = true
      try {
        await deleteComment(comment.id)
        showToast('评论已删除')
        loadComments(commentPage.current)
      } catch (e) {
        showToast(e.message || '删除失败')
      } finally {
        busy.value = false
      }
    }
  })
}

async function toggleUser(user) {
  const next = user.status === 1 ? 0 : 1
  if (next === 0) {
    askConfirm({
      title: '禁 用 账 号',
      message: `确定禁用 ${user.nickname || user.username} 吗？`,
      detail: '禁用后该账号无法登录、不能发帖评论，可随时重新启用。',
      confirmText: '禁 用',
      danger: true,
      onConfirm: () => applyUserStatus(user, next)
    })
    return
  }
  applyUserStatus(user, next)
}

async function applyUserStatus(user, next) {
  busy.value = true
  try {
    await setUserStatus(user.id, next)
    user.status = next
    showToast(next === 1 ? '账号已启用' : '账号已禁用')
  } catch (e) {
    showToast(e.message || '操作失败')
  } finally {
    busy.value = false
  }
}

async function resolveReport(report, status) {
  busy.value = true
  try {
    await handleReport(report.id, status)
    showToast(status === 'HANDLED' ? '举报已处理' : '举报已驳回')
    loadReports(reportPage.current)
    refreshPending()
  } catch (e) {
    showToast(e.message || '操作失败')
  } finally {
    busy.value = false
  }
}

onMounted(() => {
  loadPosts(1)
  refreshPending()
  loadSummary()
})
</script>

<style scoped>
/* height:100% + overflow-y:auto：全站 html/body/#app 都是 overflow:hidden，
   页面必须自己成为滚动容器，否则内容一长就被裁掉（以前这里写 min-height:100vh 就没有滚动条）。 */
.admin-page {
  height: 100%;
  overflow-y: auto;
  padding-bottom: 60px;
}

/* 细滚动条，和暗色主题贴合 */
.admin-page::-webkit-scrollbar {
  width: 8px;
}

.admin-page::-webkit-scrollbar-thumb {
  background: rgba(143, 208, 174, 0.22);
  border-radius: 999px;
}

.admin-page::-webkit-scrollbar-thumb:hover {
  background: rgba(143, 208, 174, 0.38);
}

.pg-shell {
  max-width: 880px;
  margin: 0 auto;
  padding: 0 18px;
}

.pg-head {
  margin: 18px 0 14px;
}

.pg-title {
  margin: 0;
  font-size: 22px;
  letter-spacing: 6px;
}

.pg-sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--c-dim);
  letter-spacing: 1px;
}

.ad-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.ad-tab {
  position: relative;
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid rgba(143, 208, 174, 0.28);
  background: rgba(143, 208, 174, 0.06);
  color: var(--c-text);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.ad-tab:hover {
  background: rgba(143, 208, 174, 0.16);
}

.ad-tab.on {
  background: var(--c-qinghe);
  border-color: var(--c-qinghe);
  color: #0b0e11;
  font-weight: 600;
}

.ad-badge {
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ff8f8f;
  color: #2a0d0d;
  font-size: 11px;
  line-height: 16px;
  display: inline-block;
}

.ad-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.ad-input,
.ad-select {
  padding: 7px 11px;
  border-radius: 9px;
  border: 1px solid rgba(143, 208, 174, 0.22);
  background: rgba(11, 14, 17, 0.6);
  color: var(--c-text);
  font-size: 13px;
  font-family: inherit;
}

.ad-input {
  flex: 1;
  min-width: 180px;
}

.ad-input:focus,
.ad-select:focus {
  outline: none;
  border-color: rgba(143, 208, 174, 0.55);
}

.ad-error {
  margin: 0 0 10px;
  font-size: 12px;
  color: #ff8f8f;
}

/* 签到概览 */
.ad-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.ad-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 12px 8px;
}

.ad-stat-num {
  font-size: 20px;
  font-weight: 600;
  color: var(--c-qinghe);
  line-height: 1.2;
}

.ad-stat-label {
  font-size: 11px;
  color: var(--c-dim);
  letter-spacing: 0.5px;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .ad-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.ad-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ad-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
}

/* 评论行内容较高，顶部对齐更好看 */
.ad-row-top {
  align-items: flex-start;
}

.ad-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ad-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ad-title {
  font-size: 14px;
  color: var(--c-text);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.ad-title:hover {
  color: var(--c-qinghe);
}

.ad-title.plain {
  cursor: default;
}

.ad-link {
  color: var(--c-qinghe);
  text-decoration: none;
}

.ad-link:hover {
  text-decoration: underline;
}

/* 评论正文：最多三行，避免长评论把列表撑爆 */
.ad-content {
  margin: 2px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--c-text);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  white-space: pre-wrap;
}

.ad-meta {
  font-size: 12px;
  color: var(--c-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ad-tag {
  padding: 1px 7px;
  border-radius: 5px;
  font-size: 11px;
  border: 1px solid rgba(143, 208, 174, 0.3);
  color: var(--c-dim);
  white-space: nowrap;
}

.ad-tag.pub {
  color: var(--c-qinghe);
  border-color: rgba(143, 208, 174, 0.45);
}

.ad-tag.draft {
  color: #ffd479;
  border-color: rgba(255, 212, 121, 0.4);
}

.ad-tag.pin {
  color: #ff8f8f;
  border-color: rgba(255, 143, 143, 0.45);
}

.ad-tag.admin {
  color: var(--c-acid);
  border-color: rgba(217, 255, 77, 0.4);
}

.ad-chip {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(143, 208, 174, 0.12);
  color: var(--c-mint);
  font-size: 11px;
}

.ad-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.ad-toast {
  position: fixed;
  left: 50%;
  bottom: 42px;
  transform: translateX(-50%);
  padding: 8px 18px;
  border-radius: 999px;
  background: rgba(11, 14, 17, 0.92);
  border: 1px solid rgba(143, 208, 174, 0.35);
  color: var(--c-text);
  font-size: 13px;
  z-index: 80;
}
</style>
