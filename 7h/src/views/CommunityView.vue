<template>
  <div class="community-page">
    <TopBar back="home" back-label="返回首页" />

    <div class="pg-shell mid">
      <header class="pg-head">
        <h1 class="pg-title grad-text">社 区 广 场</h1>
        <p class="pg-sub grad-text">像逛贴吧一样，逛大家的文章</p>
      </header>

      <!-- 社区状态条：签到 / 等级称号 / 创建吧 -->
      <div class="cp-status">
        <div v-if="checkin" class="cp-checkin">
          <span class="cp-lv">Lv{{ checkin.level }} · {{ checkin.title }}</span>
          <span class="cp-exp">{{ checkin.exp }} 经验</span>
          <span v-if="checkin.streak" class="cp-streak">连续 {{ checkin.streak }} 天</span>
          <button
            class="g-btn g-btn-sm cp-checkin-btn"
            type="button"
            :disabled="checkin.checked || checking"
            @click="handleCheckin"
          >
            {{ checkin.checked ? '今日已签到' : (checking ? '签到中…' : '签到 +5') }}
          </button>
        </div>
        <button class="g-btn g-btn-sm cp-create-btn" type="button" @click="openCreate">
          ＋ 创建吧
        </button>
      </div>

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
          :class="{ on: categoryId === String(ba.id), mine: isMyBa(ba) }"
          type="button"
          :title="ba.ownerName ? `吧主：${ba.ownerName}` : '站点分类'"
          @click="switchBa(String(ba.id))"
        >
          <span v-if="ba.ownerName" class="ba-crown">👑</span>{{ ba.name }}<span class="ba-cnt">{{ ba.count }}</span>
        </button>
      </nav>

      <!-- 当前吧管理条：吧主可见 -->
      <div v-if="currentBa && isMyBa(currentBa)" class="cp-manage">
        <span class="cp-manage-hint">你是『{{ currentBa.name }}』的吧主，可置顶或删除本吧帖子</span>
        <button class="mini-btn" type="button" @click="openEdit">编辑吧简介</button>
      </div>

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

          <!-- 吧主管理：置顶 / 删除本吧帖子 -->
          <div v-if="currentBa && isMyBa(currentBa)" class="tb-manage" @click.stop>
            <button class="mini-btn" type="button" :disabled="managing" @click="togglePin(p)">
              {{ p.pinned ? '取消置顶' : '置顶' }}
            </button>
            <button class="mini-btn danger" type="button" :disabled="managing" @click="removePost(p)">
              删除
            </button>
          </div>
        </li>
      </ul>

      <Pager
        :current="page.current" :pages="page.pages" :total="page.total"
        :disabled="loading"
        @go="load"
      />
    </div>

    <!-- 创建 / 编辑吧 -->
    <div v-if="dialog" class="cp-mask" @click.self="closeDialog">
      <div class="panel cp-dialog">
        <h3 class="cp-dialog-title grad-text">
          {{ dialog === 'create' ? '创 建 一 个 吧' : '编 辑 吧 简 介' }}
        </h3>
        <input
          v-model.trim="form.name"
          class="cp-input"
          type="text"
          maxlength="20"
          :disabled="dialog === 'edit'"
          placeholder="吧名，2-20 个字"
        />
        <textarea
          v-model.trim="form.description"
          class="cp-input"
          rows="3"
          maxlength="200"
          placeholder="一句话介绍这个吧（选填）"
        ></textarea>
        <p class="cp-dialog-note">
          {{ dialog === 'create' ? '创建后你就是第一任吧主，每人最多创建 3 个吧' : '吧名创建后不可修改' }}
        </p>
        <p v-if="dialogError" class="cp-error">{{ dialogError }}</p>
        <div class="cp-dialog-actions">
          <button class="g-btn g-btn-sm" type="button" :disabled="saving" @click="submitDialog">
            {{ saving ? '提交中…' : '确 定' }}
          </button>
          <button class="g-btn g-btn-sm cp-cancel" type="button" @click="closeDialog">取 消</button>
        </div>
      </div>
    </div>

    <div v-if="toast" class="cp-toast">{{ toast }}</div>

    <ConfirmDialog :state="confirmState" @confirm="confirm" @close="close" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listPosts, listCategoryStats, deletePost, setPinned } from '../api/blog'
import { createBa, updateBa, checkinToday, doCheckin } from '../api/community'
import TopBar from '../components/TopBar.vue'
import UserAvatar from '../components/UserAvatar.vue'
import Pager from '../components/Pager.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { useConfirm } from '../composables/useConfirm'
import { toPlainSummary } from '../utils/richText'

const router = useRouter()

const me = ref(null)
try {
  me.value = JSON.parse(localStorage.getItem('bn-user') || 'null')
} catch {
  me.value = null
}
const logged = computed(() => !!localStorage.getItem('bn-token'))

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

/* ================= 签到 / 经验 ================= */
const checkin = ref(null)
const checking = ref(false)
const toast = ref('')
let toastTimer = null

function showToast(message) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

async function loadCheckin() {
  if (!logged.value) return
  try {
    const res = await checkinToday()
    checkin.value = res.data
  } catch {
    checkin.value = null
  }
}

async function handleCheckin() {
  if (!logged.value) {
    showToast('登录后才能签到')
    return
  }
  checking.value = true
  try {
    const res = await doCheckin()
    checkin.value = res.data
    showToast(`签到成功 +${res.data.gained} 经验，连续 ${res.data.streak} 天`)
  } catch (e) {
    showToast(e.message || '签到失败')
    loadCheckin()
  } finally {
    checking.value = false
  }
}

/* ================= 吧：创建 / 管理 ================= */
const dialog = ref('')        // '' | 'create' | 'edit'
const form = ref({ name: '', description: '' })
const saving = ref(false)
const dialogError = ref('')

const currentBa = computed(() =>
  bars.value.find((b) => String(b.id) === categoryId.value) || null
)

function isMyBa(ba) {
  return !!(me.value && ba && ba.ownerId && ba.ownerId === me.value.id)
}

function openCreate() {
  if (!logged.value) {
    showToast('登录后才能创建吧')
    return
  }
  dialogError.value = ''
  form.value = { name: '', description: '' }
  dialog.value = 'create'
}

function openEdit() {
  if (!currentBa.value) return
  dialogError.value = ''
  form.value = {
    name: currentBa.value.name,
    description: currentBa.value.description || ''
  }
  dialog.value = 'edit'
}

function closeDialog() {
  if (saving.value) return
  dialog.value = ''
  dialogError.value = ''
}

async function submitDialog() {
  if (saving.value) return
  const name = form.value.name.trim()
  if (dialog.value === 'create' && !name) {
    dialogError.value = '请输入吧名'
    return
  }
  saving.value = true
  dialogError.value = ''
  try {
    if (dialog.value === 'create') {
      const res = await createBa(name, form.value.description)
      dialog.value = ''
      await loadBars()
      showToast(res.message || '吧创建成功')
      if (res.data?.id) switchBa(String(res.data.id))
    } else {
      const res = await updateBa(currentBa.value.id, currentBa.value.name, form.value.description)
      dialog.value = ''
      await loadBars()
      showToast(res.message || '吧信息已更新')
    }
  } catch (e) {
    dialogError.value = e.message || '提交失败'
  } finally {
    saving.value = false
  }
}

/* ================= 吧主：置顶 / 删除本吧帖子 ================= */
const managing = ref(false)
const { state: confirmState, ask: askConfirm, confirm, close } = useConfirm()

async function togglePin(p) {
  if (managing.value) return
  managing.value = true
  try {
    await setPinned(p.id, !p.pinned)
    await load(page.value.current)
    showToast(p.pinned ? '已取消置顶' : '已置顶')
  } catch (e) {
    showToast(e.message || '操作失败')
  } finally {
    managing.value = false
  }
}

function removePost(p) {
  if (managing.value) return
  askConfirm({
    title: '删 除 帖 子',
    message: `确定删除「${p.title || '（无标题）'}」吗？`,
    detail: '该操作不可恢复，帖子下的回复会一并消失。',
    confirmText: '删 除',
    danger: true,
    onConfirm: async () => {
      managing.value = true
      try {
        await deletePost(p.id)
        showToast('帖子已删除')
        await loadBars()
        await load(page.value.current)
      } catch (e) {
        showToast(e.message || '删除失败')
      } finally {
        managing.value = false
      }
    }
  })
}

onMounted(() => {
  loadBars()
  loadCheckin()
  load(1)
})
</script>

<style scoped>
.community-page {
  /* 与全站一致：自己当滚动容器，否则 html/body 的 overflow:hidden 会把超长内容裁掉 */
  height: 100%;
  overflow-y: auto;
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

/* 社区状态条：签到 + 创建吧 */
.cp-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin: 14px 0 2px;
}

.cp-checkin {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 7px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
}

.cp-lv { font-size: 12px; color: var(--c-ninja); letter-spacing: 0.5px; }
.cp-exp { font-size: 12px; color: var(--c-dim); }
.cp-streak { font-size: 12px; color: var(--c-acid); }
.cp-checkin-btn { padding: 4px 12px; font-size: 12px; }
.cp-create-btn { padding: 6px 14px; font-size: 12px; }

/* 吧主标识 */
.ba-chip.mine { border-color: rgba(217, 255, 77, 0.34); }
.ba-crown { font-size: 11px; margin-right: 2px; }

.cp-manage {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin: 0 0 12px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--c-dim);
  border-left: 3px solid rgba(217, 255, 77, 0.4);
  border-radius: 0 8px 8px 0;
  background: rgba(217, 255, 77, 0.06);
}

/* 帖子上的吧主操作 */
.tb-manage {
  display: flex;
  flex: none;
  gap: 6px;
  align-self: center;
  margin-left: 8px;
}

.tb-manage .mini-btn {
  padding: 4px 9px;
  font-size: 11px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  cursor: pointer;
}

.tb-manage .mini-btn:hover { border-color: rgba(150, 182, 168, 0.42); }
.tb-manage .mini-btn.danger { color: #ff9d9d; border-color: rgba(255, 157, 157, 0.3); }
.tb-manage .mini-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 创建 / 编辑吧对话框 */
.cp-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(5, 7, 10, 0.72);
  backdrop-filter: blur(4px);
}

.cp-dialog {
  width: min(420px, 100%);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cp-dialog-title {
  font-size: 16px;
  letter-spacing: 2px;
  margin-bottom: 2px;
}

.cp-input {
  width: 100%;
  padding: 9px 11px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
}

.cp-input:focus { border-color: var(--c-ninja); }
.cp-input:disabled { opacity: 0.6; }

.cp-dialog-note { font-size: 11px; color: var(--c-dim); margin: 0; }
.cp-dialog-actions { display: flex; gap: 8px; justify-content: flex-end; }
.cp-dialog-actions .cp-cancel { color: var(--c-dim); }

.cp-toast {
  position: fixed;
  left: 50%;
  bottom: 56px;
  transform: translateX(-50%);
  z-index: 95;
  padding: 9px 20px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(20, 24, 30, 0.94);
  border: 1px solid rgba(150, 182, 168, 0.4);
  border-radius: 999px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.5);
  pointer-events: none;
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
