<template>
  <div class="user-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell mid">
      <p v-if="error" class="uv-error">{{ error }}</p>

      <section v-if="profile" class="panel uv-card" :class="{ banned: profile.banned }">
        <div class="uv-head">
          <UserAvatar :user="profile.user || profile" :size="64" />
          <div class="uv-info">
            <div class="uv-name-row">
              <h1 class="uv-name grad-text">{{ profile.user?.nickname || profile.nickname || 'TA' }}</h1>
              <span v-if="profile.banned" class="uv-banned">已封禁</span>
            </div>
            <p class="uv-sign">{{ profile.user?.signature || profile.signature || '这个人很神秘，什么都没写' }}</p>
            <div class="uv-stats">
              <span>文章 {{ profile.postCount ?? posts.length }}</span>
              <span>粉丝 {{ profile.followerCount ?? 0 }}</span>
              <span>关注 {{ profile.followingCount ?? 0 }}</span>
            </div>
          </div>
          <div v-if="!isSelf && !profile.banned" class="uv-actions">
            <button
              class="mini-btn"
              :class="{ on: profile.followed, mutual: profile.followed && profile.mutual }"
              type="button"
              :disabled="busy"
              @click="follow"
            >
              {{ followLabel(profile) }}
            </button>
            <button class="uv-report" type="button" title="举报这个用户" @click="openReport">
              举报
            </button>
          </div>
          <p v-else-if="profile.banned" class="uv-banned-note">该账号因违规已被封禁</p>
        </div>
      </section>

      <section v-if="posts.length" class="uv-posts">
        <h2 class="uv-sub">
          <span class="ph-icon">📄</span>
          TA 的文章
        </h2>
        <ul class="uv-list">
          <li v-for="p in posts" :key="p.id" class="panel hoverable uv-item" @click="open(p)">
            <span class="uv-item-title">{{ p.title }}</span>
            <span class="uv-item-time">{{ formatTime(p.createdAt) }}</span>
          </li>
        </ul>
      </section>

      <div v-else-if="profile && !loading" class="empty-state">
        <span class="es-ico">🫧</span>
        <span>TA 还没有发布过文章</span>
      </div>
    </div>

    <ReportDialog
      v-if="reportTarget"
      :target="reportTarget"
      @close="reportTarget = null"
      @done="onReportDone"
    />
    <p v-if="toast" class="uv-toast">{{ toast }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUserProfile, toggleFollow, listUserPosts } from '../api/social'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import UserAvatar from '../components/UserAvatar.vue'
import ReportDialog from '../components/ReportDialog.vue'

const router = useRouter()
const route = useRoute()

const profile = ref(null)
const posts = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')

const me = ref(null)
try {
  me.value = JSON.parse(localStorage.getItem('bn-user') || 'null')
} catch {
  me.value = null
}

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => !!me.value && me.value.id === userId.value)

const navItems = [{ label: '首页', to: 'home' }]

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
  try {
    const res = await getUserProfile(userId.value)
    profile.value = res.data || null
    try {
      const p = await listUserPosts(userId.value, 1, 20)
      const d = p.data || {}
      posts.value = d.records || d.list || []
    } catch {
      posts.value = []
    }
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

/** 关注按钮文案：互相关注 > 已关注 > 未关注 */
function followLabel(p) {
  if (!p || !p.followed) return '+ 关注'
  return p.mutual ? '互相关注' : '已关注'
}

async function follow() {
  if (!profile.value) return
  busy.value = true
  try {
    const res = await toggleFollow(userId.value)
    const d = res.data || {}
    profile.value.followed = d.followed ?? !profile.value.followed
    profile.value.mutual = !!d.mutual
    if (typeof d.followerCount === 'number') profile.value.followerCount = d.followerCount
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    busy.value = false
  }
}

function open(p) {
  if (p.slug) router.push({ name: 'post', params: { slug: p.slug } })
}

/* ================= 举报用户 ================= */

const reportTarget = ref(null)   // { type: 'USER', id, label }，非空即弹窗打开
const toast = ref('')
let toastTimer = null

function showToast(message) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

function openReport() {
  if (!me.value) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const u = profile.value || {}
  const name = u.nickname || u.username || 'TA'
  reportTarget.value = { type: 'USER', id: userId.value, label: `用户：${name}${u.username ? `（@${u.username}）` : ''}` }
}

function onReportDone() {
  reportTarget.value = null
  showToast('举报已提交，管理员会尽快处理')
}

onBeforeUnmount(() => clearTimeout(toastTimer))

onMounted(load)
</script>

<style scoped>
.user-page { height: 100%; overflow-y: auto; }
.uv-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }

.uv-card { padding: 22px 24px; margin-bottom: 18px; }
.uv-head { display: flex; align-items: center; gap: 16px; }
.uv-info { flex: 1; min-width: 0; }
.uv-name { font-family: var(--font-display); font-size: 20px; letter-spacing: 2px; }
.uv-name-row { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.uv-sign { font-size: 13px; color: var(--c-dim); margin: 6px 0 10px; line-height: 1.6; }
.uv-stats { display: flex; gap: 16px; font-size: 12px; color: var(--c-mint); }
.uv-actions { display: flex; align-items: center; gap: 8px; }

/* 封禁灰态：名字与统计压暗，避免被误认为正常活跃账号 */
.uv-card.banned .uv-name,
.uv-card.banned .uv-stats,
.uv-card.banned .uv-sign { opacity: 0.55; }
.uv-banned {
  flex: none;
  padding: 2px 9px;
  font-size: 11px;
  letter-spacing: 1px;
  color: #ff8a8a;
  -webkit-text-fill-color: currentColor;
  border: 1px solid rgba(255, 138, 138, 0.42);
  border-radius: 999px;
}
.uv-banned-note { max-width: 180px; font-size: 11.5px; line-height: 1.6; color: #ff8a8a; text-align: right; }

/* 举报：低饱和描边，鼠标移上去才变暖，避免抢关注的视觉权重 */
.uv-report {
  padding: 5px 11px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--c-dim);
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.uv-report:hover { color: #ff8a8a; border-color: rgba(255, 138, 138, 0.4); }

.uv-toast {
  position: fixed;
  left: 50%;
  bottom: 42px;
  transform: translateX(-50%);
  padding: 8px 18px;
  font-size: 12.5px;
  border-radius: 999px;
  background: rgba(20, 24, 22, 0.92);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
  z-index: 90;
}

.uv-sub { display: flex; align-items: center; gap: 10px; font-size: 15px; letter-spacing: 2px; margin-bottom: 12px; }
.uv-list { list-style: none; display: flex; flex-direction: column; gap: 10px; }
.uv-item { display: flex; align-items: center; gap: 12px; padding: 13px 16px; }
.uv-item-title { font-size: 14px; }
.uv-item-time { margin-left: auto; font-size: 11px; color: var(--c-dim); }
</style>
