<template>
  <header class="topbar">
    <div class="tb-logo grad-text" @click="goHome">BLEACH×NARUTO</div>

    <div v-if="showSearch" class="tb-search">
      <input
        v-model.trim="keyword"
        class="search-input"
        type="search"
        placeholder="搜索文章标题或摘要…"
        @keyup.enter="emit('search')"
      />
      <select v-model="categoryId" class="search-select" @change="emit('search')">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
      </select>
      <button class="g-btn g-btn-sm search-btn" @click="emit('search')">搜索</button>
    </div>

    <div class="tb-user">
      <span class="tb-avatar" :class="faction">
        {{ (user.nickname || user.username || '?').slice(0, 1) }}
      </span>
      <span class="tb-name">{{ user.nickname || user.username }}</span>
      <button class="tb-btn" :class="{ on: active === 'editor' }" @click="goEditor">写文章</button>
      <button class="tb-btn" :class="{ on: active === 'mine' }" @click="goMine">我的文章</button>
      <button class="tb-btn" :class="{ on: active === 'messages' }" @click="goMessages">
        私信<span v-if="unread" class="tb-dot">{{ unread > 99 ? '99+' : unread }}</span>
      </button>
      <button class="tb-btn danger" @click="logout">退出</button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { clearToken } from '../api/auth'
import { listCategories } from '../api/blog'
import { getUnreadCount } from '../api/social'

const props = defineProps({
  /** 当前高亮的导航：home | mine | messages | editor | profile */
  active: { type: String, default: '' },
  /** 是否显示搜索区（首页/文章列表页用） */
  showSearch: { type: Boolean, default: false }
})

const emit = defineEmits(['search'])

const keyword = defineModel('keyword', { type: String, default: '' })
const categoryId = defineModel('categoryId', { type: String, default: '' })

const router = useRouter()
const user = ref({})
const unread = ref(0)
const categories = ref([])

const faction = computed(() => (user.value.faction === 'shinigami' ? 'shinigami' : 'ninja'))

onMounted(() => {
  try {
    user.value = JSON.parse(localStorage.getItem('bn-user') || '{}')
  } catch (e) {
    user.value = {}
  }
  listCategories()
    .then((res) => (categories.value = res.data || []))
    .catch(() => {})
  getUnreadCount()
    .then((res) => (unread.value = res.data?.count || 0))
    .catch(() => {})
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
.search-input {
  flex: 1;
  min-width: 120px;
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
  box-shadow: 0 0 0 3px rgba(255, 140, 26, 0.12);
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
.search-select option { background: #14141f; }
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
.tb-avatar {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 15px;
  color: #0a0a12;
}
.tb-avatar.ninja { background: linear-gradient(135deg, #ffd24a, #ff6a00); box-shadow: 0 0 14px rgba(255, 140, 26, 0.5); }
.tb-avatar.shinigami { background: linear-gradient(135deg, #b9f4ff, #0090ff); box-shadow: 0 0 14px rgba(0, 229, 255, 0.5); }
.tb-name {
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 110px;
}
.tb-btn {
  position: relative;
  padding: 6px 10px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--c-dim);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
  white-space: nowrap;
}
.tb-btn:hover { color: var(--c-text); background: rgba(255, 255, 255, 0.06); }
.tb-btn.on { color: var(--c-text); background: rgba(255, 255, 255, 0.08); }
.tb-btn.danger:hover { color: #ff8080; }
.tb-dot {
  position: absolute;
  top: -2px;
  right: 0;
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
  .tb-name { display: none; }
  .tb-btn { padding: 6px 6px; font-size: 12px; }
}
</style>
