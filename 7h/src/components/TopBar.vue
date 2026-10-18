<template>
  <header class="topbar">
    <BackButton
      v-if="back"
      class="tb-back"
      :to="typeof back === 'string' ? back : 'home'"
      :label="backLabel"
    />

    <div class="tb-logo grad-text" @click="goHome">7h</div>

    <div v-if="showSearch" class="tb-search">
      <div class="search-wrap">
        <input
          v-model.trim="keyword"
          class="search-input"
          type="search"
          :placeholder="placeholder"
          @keyup.enter="emit('search')"
          @focus="searchFocused = true"
          @blur="searchFocused = false"
        />
        <!-- 热搜轮播：输入框为空且未聚焦时显示，点击直接搜索 -->
        <button
          v-if="hotVisible"
          class="hot-tip"
          :title="hotTitle"
          @click="searchHot"
        >
          🔥<span :key="hotTitle" class="hot-text">{{ hotTitle }}</span>
        </button>
      </div>
      <select v-model="categoryId" class="search-select" @change="emit('search')">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
      </select>
      <button class="g-btn g-btn-sm search-btn" @click="emit('search')">搜索</button>
    </div>

    <div class="tb-user">
      <button class="tb-btn" :class="{ on: active === 'editor' }" @click="goEditor">写文章</button>
      <button class="tb-btn" :class="{ on: active === 'mine' }" @click="goMine">我的文章</button>
      <button class="tb-btn" :class="{ on: active === 'notifications' }" @click="goNotifications">
        🔔 消息<span v-if="notifUnread" class="tb-dot">{{ notifUnread > 99 ? '99+' : notifUnread }}</span>
      </button>
      <button class="tb-btn" :class="{ on: active === 'messages' }" @click="goMessages">
        私信<span v-if="unread" class="tb-dot">{{ unread > 99 ? '99+' : unread }}</span>
      </button>
      <button class="tb-btn danger" @click="logout">退出</button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import BackButton from './BackButton.vue'
import { clearToken } from '../api/auth'
import { listCategories, listPosts } from '../api/blog'
import { getUnreadCount } from '../api/social'
import { getNotificationUnread } from '../api/notification'

const props = defineProps({
  /** 当前高亮的导航：home | mine | messages | notifications | editor | profile */
  active: { type: String, default: '' },
  /** 是否显示搜索区（首页/文章列表页用） */
  showSearch: { type: Boolean, default: false },
  /**
   * 顶栏左侧返回入口（全站统一 BackButton）：
   *   不传 / false  → 不显示
   *   路由名字符串   → 点击跳到该路由，如 'home'
   */
  back: { type: [Boolean, String], default: false },
  /** 返回键文字 */
  backLabel: { type: String, default: '返回首页' }
})

const emit = defineEmits(['search'])

const keyword = defineModel('keyword', { type: String, default: '' })
const categoryId = defineModel('categoryId', { type: String, default: '' })

const router = useRouter()
const unread = ref(0)
const notifUnread = ref(0)
const categories = ref([])

/* ===== 搜索框热搜轮播 ===== */
const searchFocused = ref(false)
const hotTitles = ref([])
const hotIdx = ref(0)
let hotTimer = null

const hotTitle = computed(() => hotTitles.value[hotIdx.value]?.title || '')
/** 默认提示文字去掉，搜索框只显示热搜轮播；热搜加载失败时才兜底显示提示 */
const placeholder = computed(() => (hotTitle.value ? '' : '搜索文章标题或摘要…'))
/** 覆盖层只在「空输入 + 未聚焦」时出现，聚焦后让位给光标 */
const hotVisible = computed(() => !keyword.value && !searchFocused.value && !!hotTitle.value)

async function loadHotTitles() {
  try {
    const res = await listPosts({ current: 1, size: 50 })
    const records = res.data?.records || []
    hotTitles.value = [...records]
      .sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0))
      .slice(0, 5)
  } catch (e) {
    // 热搜加载失败不影响搜索功能
  }
}

function searchHot() {
  if (!hotTitle.value) return
  keyword.value = hotTitle.value
  searchFocused.value = false
  emit('search')
}

let pollTimer = null

async function refreshUnread() {
  getUnreadCount()
    .then((res) => (unread.value = res.data?.count || 0))
    .catch(() => {})
  getNotificationUnread()
    .then((res) => (notifUnread.value = res.data?.count || 0))
    .catch(() => {})
}

onMounted(() => {
  listCategories()
    .then((res) => (categories.value = res.data || []))
    .catch(() => {})
  refreshUnread()
  // 后端暂无 WebSocket：轮询刷新私信 + 消息通知未读数
  pollTimer = setInterval(refreshUnread, 15000)
  if (props.showSearch) {
    loadHotTitles()
    // 每 4 秒轮换一条热搜标题
    hotTimer = setInterval(() => {
      if (hotTitles.value.length) {
        hotIdx.value = (hotIdx.value + 1) % hotTitles.value.length
      }
    }, 4000)
  }
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(hotTimer)
})

function goHome() {
  router.push({ name: 'home' })
}
function goMine() {
  router.push({ name: 'mine' })
}
function goMessages() {
  router.push({ name: 'messages' })
}
function goNotifications() {
  router.push({ name: 'notifications' })
}
function goEditor() {
  router.push({ name: 'editor' })
}
function logout() {
  clearToken()
  localStorage.removeItem('bn-user')
  router.replace({ name: 'login' })
}
</script>

<style scoped>
.topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 30;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 24px;
  background: var(--c-panel);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 18px rgba(0, 0, 0, 0.4);
}

/* 顶栏内的返回键：沿用全局 .back-btn，仅微调尺寸贴合 60px 顶栏 */
.tb-back {
  flex: none;
  height: 30px;
  padding: 0 13px 0 10px;
  font-size: 12.5px;
}

.tb-logo {
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 2px;
  white-space: nowrap;
  cursor: pointer;
  flex: none;
}

.tb-search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 560px;
}
.search-wrap {
  flex: 1;
  min-width: 120px;
  position: relative;
  display: flex;
}
.search-input {
  flex: 1;
  min-width: 0;
  width: 100%;
  padding: 9px 14px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  outline: none;
}
.search-input:focus {
  border-color: var(--c-ninja);
  box-shadow: 0 0 0 3px rgba(150, 182, 168, 0.1);
}

/* 热搜轮播覆盖层：盖在空输入框上，看起来就是 placeholder，但可以点 */
.hot-tip {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  max-width: calc(100% - 30px);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--c-dim);
  background: none;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  transition: color 0.2s;
}
.hot-tip:hover { color: var(--c-ninja); }
.hot-text {
  overflow: hidden;
  text-overflow: ellipsis;
  animation: hot-in 0.45s ease;
}
@keyframes hot-in {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.search-select {
  flex: none;
  width: 120px;
  padding: 9px 10px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  outline: none;
}
.search-select option { background: #14171b; }
/* 尺寸由 main.css 的 .g-btn-sm 提供，此处保留胶囊造型 */
.search-btn {
  flex: none;
  letter-spacing: 1px;
  border-radius: 999px;
}

.tb-user {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
  min-width: 0;
}
.tb-btn {
  position: relative;
  height: 30px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  font-size: 12.5px;
  letter-spacing: 1px;
  color: var(--c-dim);
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}
.tb-btn:hover { color: #fff; background: rgba(255, 255, 255, 0.07); border-color: rgba(255, 255, 255, 0.28); }
.tb-btn.on { color: #fff; background: rgba(188, 205, 197, 0.09); border-color: rgba(188, 205, 197, 0.3); }
.tb-btn.danger { color: #ff8a8a; }
.tb-btn.danger:hover { color: #ff6b6b; background: rgba(255, 107, 107, 0.1); border-color: rgba(255, 107, 107, 0.45); }
.tb-dot {
  position: absolute;
  top: -7px;
  right: -2px;
  min-width: 15px;
  padding: 0 4px;
  height: 15px;
  line-height: 15px;
  border-radius: 8px;
  background: #ff5c5c;
  color: #fff;
  font-size: 10px;
  letter-spacing: 0;
  text-align: center;
}

@media (max-width: 860px) {
  .tb-logo { display: none; }
  .tb-search { max-width: none; }
}
@media (max-width: 700px) {
  .topbar {
    height: auto;
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px 14px;
  }
  .tb-search { order: 3; flex-basis: 100%; }
  .tb-btn { height: 26px; padding: 0 10px; font-size: 12px; }
}
</style>
