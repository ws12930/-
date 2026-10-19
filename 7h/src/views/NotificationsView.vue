<template>
  <div class="noti-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell mid">
      <header class="pg-head">
        <h1 class="pg-title grad-text">通 知</h1>
        <p class="pg-sub grad-text">评论、点赞与关注提醒</p>
      </header>

      <p v-if="error" class="nt-error">{{ error }}</p>

      <div class="nt-bar">
        <span class="nt-unread" v-if="unread">未读 {{ unread }}</span>
        <span class="nt-spacer"></span>
        <button class="g-btn g-btn-sm" :disabled="!unread || busy" @click="readAll">全部标记已读</button>
      </div>

      <div v-if="loading" class="empty-state"><span class="es-ico">🌾</span><span>正在加载…</span></div>
      <div v-else-if="!items.length" class="empty-state">
        <span class="es-ico">🔔</span>
        <span>暂时没有新通知</span>
      </div>

      <ul v-else class="nt-list">
        <li
          v-for="n in items"
          :key="n.id"
          class="panel nt-item"
          :class="{ unread: !n.read, link: !!targetOf(n) }"
          @click="openItem(n)"
        >
          <span class="nt-icon">{{ iconOf(n.type) }}</span>
          <div class="nt-main">
            <p class="nt-text">
              <b>{{ actorName(n) }}</b>
              {{ textOf(n.type) }}
            </p>
            <p v-if="n.postTitle" class="nt-target">《{{ n.postTitle }}》</p>
            <span class="nt-time">{{ formatTime(n.createdAt) }}</span>
          </div>
          <span v-if="!n.read" class="nt-dot"></span>
          <span v-if="targetOf(n)" class="nt-arrow" aria-hidden="true">›</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  listNotifications,
  getNotificationUnread,
  markAllNotificationsRead,
  markNotificationRead
} from '../api/notification'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'

const router = useRouter()

const items = ref([])
const unread = ref(0)
const loading = ref(false)
const busy = ref(false)
const error = ref('')

const navItems = [{ label: '首页', to: 'home' }]

const TYPE_TEXT = {
  comment: '评论了你的文章',
  reply: '回复了你的评论',
  like: '赞了你的文章',
  follow: '关注了你',
  message: '给你发了私信'
}

const TYPE_ICON = {
  comment: '💬',
  reply: '↩️',
  like: '♥',
  follow: '＋',
  message: '✉️'
}

/** 后端返回的是大写类型（COMMENT / REPLY / LIKE / FOLLOW），这里统一成小写再查表 */
function typeOf(t) {
  return String(t || '').toLowerCase()
}
function textOf(t) {
  return TYPE_TEXT[typeOf(t)] || '与你互动'
}
function iconOf(t) {
  return TYPE_ICON[typeOf(t)] || '🔔'
}

/** 通知里的操作者同样是扁平字段（actorName / actorAvatar） */
function actorName(n) {
  return n.actorName || n.actorUsername || n.actor?.nickname || n.actor?.username || '有人'
}

/**
 * 通知跳转目标：
 * 评论 / 回复 / 点赞 -> 对应文章，并带上 comment 参数定位到那条评论
 * 关注 -> 对方个人主页
 * 其余（无文章 slug 等）-> 不跳转
 */
function targetOf(n) {
  const t = typeOf(n.type)
  if (t === 'comment' || t === 'reply' || t === 'like') {
    if (!n.postSlug) return null
    return { name: 'post', params: { slug: n.postSlug }, query: n.commentId ? { comment: String(n.commentId) } : {} }
  }
  if (t === 'follow' && n.actorId) {
    return { name: 'user', params: { id: n.actorId } }
  }
  if (t === 'message') {
    return { name: 'messages' }
  }
  return null
}

async function openItem(n) {
  const target = targetOf(n)
  if (!n.read) {
    try {
      await markNotificationRead(n.id)
      n.read = true
      unread.value = Math.max(0, unread.value - 1)
    } catch {
      // 标记失败不影响跳转
    }
  }
  if (target) router.push(target)
}

function formatTime(v) {
  if (!v) return ''
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await listNotifications()
    // 后端字段是 readFlag(0/1)，模板按 read 判断，这里归一一次
    items.value = (res.data || []).map((n) => ({ ...n, read: Number(n.readFlag) === 1 || !!n.read }))
    try {
      const r = await getNotificationUnread()
      // 后端返回 { count: N }
      unread.value = Number(r.data?.count ?? r.data?.unread ?? 0) || 0
    } catch {
      unread.value = items.value.filter((n) => !n.read).length
    }
  } catch (e) {
    error.value = e.message || '加载失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

async function readAll() {
  busy.value = true
  try {
    await markAllNotificationsRead()
    items.value.forEach((n) => (n.read = true))
    unread.value = 0
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.noti-page { height: 100%; overflow-y: auto; }
.nt-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }

.nt-bar { display: flex; align-items: center; margin-bottom: 14px; }
.nt-unread { font-size: 12px; color: var(--c-acid); }
.nt-spacer { flex: 1; }

.nt-list { list-style: none; display: flex; flex-direction: column; gap: 10px; }
.nt-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
}
.nt-item.unread { border-color: rgba(150, 182, 168, 0.34); background: rgba(150, 182, 168, 0.06); }
.nt-item.link:hover { border-color: rgba(150, 182, 168, 0.42); background: rgba(150, 182, 168, 0.1); }
.nt-arrow {
  flex: none;
  align-self: center;
  font-size: 18px;
  line-height: 1;
  color: var(--c-dim);
  transition: color 0.2s, transform 0.2s;
}
.nt-item.link:hover .nt-arrow { color: var(--c-mint); transform: translateX(3px); }

.nt-icon {
  flex: none;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}
.nt-main { flex: 1; min-width: 0; }
.nt-text { font-size: 13px; line-height: 1.6; }
.nt-text b { color: var(--c-mint); }
.nt-target { font-size: 12px; color: var(--c-dim); margin-top: 3px; }
.nt-time { display: block; margin-top: 5px; font-size: 11px; color: var(--c-dim); }
.nt-dot {
  flex: none;
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--c-acid);
}
</style>
