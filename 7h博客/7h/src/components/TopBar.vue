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

    <div ref="userWrap" class="tb-user">
      <button
        class="tb-me"
        :class="{ on: open }"
        type="button"
        :aria-expanded="open"
        :title="`${displayName} · 打开菜单`"
        @click="toggleMenu"
      >
        <UserAvatar :user="me" :size="26" />
        <span class="tb-name">{{ displayName }}</span>
        <span class="tb-caret" :class="{ up: open }"></span>
        <span v-if="totalUnread" class="tb-me-dot"></span>
      </button>

      <transition name="tb-menu">
        <div v-if="open" class="tb-menu">
          <div class="tb-menu-head">
            <UserAvatar :user="me" :size="44" />
            <div class="tb-menu-id">
              <span class="tb-menu-name">{{ displayName }}</span>
              <span class="tb-menu-at">@{{ me?.username || 'user' }}</span>
            </div>
          </div>

          <button
            v-for="item in menuItems"
            :key="item.key"
            class="tb-menu-item"
            :class="{ on: active === item.key }"
            type="button"
            @click="go(item)"
          >
            <span>{{ item.label }}</span>
            <span v-if="item.badge" class="tb-mi-badge">
              {{ item.badge > 99 ? '99+' : item.badge }}
            </span>
          </button>

          <span class="tb-menu-sep"></span>

          <button class="tb-menu-item danger" type="button" @click="logout">退出登录</button>
        </div>
      </transition>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import BackButton from './BackButton.vue'
import UserAvatar from './UserAvatar.vue'
import { clearToken } from '../api/auth'
import { listCategories, listPosts } from '../api/blog'
import { getUnreadCount, getUserProfile } from '../api/social'
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
const route = useRoute()
const unread = ref(0)
const notifUnread = ref(0)
const categories = ref([])

/* ===== 右上角头像 + 下拉菜单 ===== */
const me = ref(null)
const open = ref(false)
const userWrap = ref(null)

const displayName = computed(
  () => me.value?.nickname || me.value?.username || '未登录'
)
/** 收起状态下靠这个红点提示「有未读」，具体数字展开后才看得到 */
const totalUnread = computed(() => (unread.value || 0) + (notifUnread.value || 0))

const menuItems = computed(() => [
  { key: 'profile', label: '个人资料' },
  { key: 'mine', label: '我的文章' },
  { key: 'editor', label: '写文章' },
  { key: 'notifications', label: '消息', badge: notifUnread.value },
  { key: 'messages', label: '私信', badge: unread.value },
  { key: 'community', label: '社区广场' }
])

function toggleMenu() {
  open.value = !open.value
}

function closeMenu() {
  open.value = false
}

function go(item) {
  closeMenu()
  router.push({ name: item.key })
}

/** 点面板外关闭；userWrap 内的点击由各按钮自行处理 */
function onDocumentClick(e) {
  if (!open.value) return
  if (userWrap.value && !userWrap.value.contains(e.target)) closeMenu()
}

function onKeydown(e) {
  if (e.key === 'Escape') closeMenu()
}

/** 拉最新资料：在资料页改完头像/昵称，回到其他页顶栏会同步 */
async function loadMe() {
  let cached = null
  try {
    cached = JSON.parse(localStorage.getItem('bn-user') || 'null')
  } catch {
    cached = null
  }
  if (!cached?.id) {
    me.value = cached
    return
  }
  try {
    const res = await getUserProfile(cached.id)
    const data = res.data?.user || res.data || {}
    me.value = { ...cached, ...data }
  } catch {
    me.value = cached
  }
}

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
  loadMe()
  refreshUnread()
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
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
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})

// 菜单里的项跳走后自动收起；从资料页返回时顺带刷新一次头像昵称
watch(
  () => route.fullPath,
  () => {
    closeMenu()
    loadMe()
  }
)

function goHome() {
  router.push({ name: 'home' })
}
function logout() {
  closeMenu()
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
  position: relative;
  margin-left: auto;
  display: flex;
  align-items: center;
  flex: none;
}

/* 头像胶囊：展开态高亮，有未读时右上角挂聚合红点 */
.tb-me {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 11px 0 5px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.tb-me:hover { background: rgba(255, 255, 255, 0.07); border-color: rgba(255, 255, 255, 0.28); }
.tb-me.on { background: rgba(188, 205, 197, 0.09); border-color: rgba(188, 205, 197, 0.3); }

.tb-name {
  max-width: 96px;
  font-size: 12.5px;
  letter-spacing: 0.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tb-caret {
  width: 6px;
  height: 6px;
  border-right: 1.5px solid var(--c-dim);
  border-bottom: 1.5px solid var(--c-dim);
  transform: rotate(45deg) translate(-1px, -1px);
  transition: transform 0.2s ease;
}
.tb-caret.up { transform: rotate(-135deg) translate(-1px, -1px); }

.tb-me-dot {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff5c5c;
  box-shadow: 0 0 0 2px var(--c-panel);
}

/* ===== 下拉面板 ===== */
.tb-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 40;
  width: 220px;
  padding: 6px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 12px;
  background: rgba(20, 20, 32, 0.98);
  box-shadow: 0 12px 34px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
}

.tb-menu-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px 12px;
}
.tb-menu-id { display: flex; flex-direction: column; min-width: 0; }
.tb-menu-name {
  font-size: 13px;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tb-menu-at { font-size: 11px; color: var(--c-dim); }

.tb-menu-sep {
  display: block;
  height: 1px;
  margin: 6px 4px;
  background: rgba(255, 255, 255, 0.1);
}

.tb-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 9px 10px;
  font-size: 13px;
  letter-spacing: 0.5px;
  color: var(--c-text);
  background: none;
  border: none;
  border-radius: 8px;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s, color 0.18s;
}
.tb-menu-item:hover { background: rgba(255, 255, 255, 0.08); }
.tb-menu-item.on { color: #fff; background: rgba(150, 182, 168, 0.16); }
.tb-menu-item.danger { color: #ff8a8a; }
.tb-menu-item.danger:hover { color: #ff6b6b; background: rgba(255, 107, 107, 0.12); }

.tb-mi-badge {
  flex: none;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  line-height: 18px;
  font-size: 11px;
  text-align: center;
  color: #fff;
  border-radius: 9px;
  background: #ff5c5c;
}

.tb-menu-enter-active,
.tb-menu-leave-active { transition: opacity 0.16s ease, transform 0.16s ease; }
.tb-menu-enter-from,
.tb-menu-leave-to { opacity: 0; transform: translateY(-6px); }

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
  /* 窄屏顶栏会换行，头像只留图标避免挤爆 */
  .tb-name { display: none; }
  .tb-me { height: 32px; padding: 0 8px 0 5px; }
}
</style>
