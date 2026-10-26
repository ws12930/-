<template>
  <div class="community-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell mid">
      <header class="pg-head">
        <h1 class="pg-title grad-text">社 区</h1>
        <p class="pg-sub grad-text">发现有意思的博主</p>
      </header>

      <div class="cm-search">
        <input v-model.trim="keyword" type="text" placeholder="按昵称搜索用户" @keydown.enter="doSearch" />
        <button class="g-btn g-btn-sm" :disabled="searching" @click="doSearch">
          {{ searching ? '搜索中…' : '搜索' }}
        </button>
      </div>

      <p v-if="error" class="ct-error">{{ error }}</p>

      <div v-if="loading" class="empty-state"><span class="es-ico">🌾</span><span>正在加载…</span></div>
      <div v-else-if="!users.length" class="empty-state">
        <span class="es-ico">🫧</span>
        <span>没有找到匹配的用户</span>
      </div>

      <ul v-else class="ct-list">
        <li v-for="u in users" :key="u.id" class="panel hoverable ct-item">
          <UserAvatar :user="u" :size="44" />
          <div class="ct-main" @click="openUser(u)">
            <span class="ct-name">{{ u.nickname || u.username }}</span>
            <span class="ct-sign">{{ u.signature || '这个人很神秘，什么都没写' }}</span>
          </div>
          <span class="ct-fans">{{ u.followerCount ?? 0 }} 粉丝</span>
          <button
            v-if="!isSelf(u)"
            class="mini-btn"
            :class="{ on: u.followed, mutual: u.followed && u.mutual }"
            type="button"
            :disabled="busyId === u.id"
            @click="follow(u)"
          >
            {{ followLabel(u) }}
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listCommunity, searchUsers, toggleFollow } from '../api/social'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import UserAvatar from '../components/UserAvatar.vue'

const router = useRouter()

const users = ref([])
const keyword = ref('')
const loading = ref(false)
const searching = ref(false)
const busyId = ref(null)
const error = ref('')

const me = ref(null)
try {
  me.value = JSON.parse(localStorage.getItem('bn-user') || 'null')
} catch {
  me.value = null
}

const navItems = [{ label: '首页', to: 'home' }]

function isSelf(u) {
  return !!me.value && u.id === me.value.id
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await listCommunity(100)
    users.value = res.data || []
  } catch (e) {
    error.value = e.message || '加载失败'
    users.value = []
  } finally {
    loading.value = false
  }
}

async function doSearch() {
  const kw = keyword.value
  if (!kw) {
    await load()
    return
  }
  searching.value = true
  error.value = ''
  try {
    const res = await searchUsers(kw, 20)
    users.value = res.data || []
  } catch (e) {
    error.value = e.message || '搜索失败'
    users.value = []
  } finally {
    searching.value = false
  }
}

/** 关注按钮文案：互相关注 > 已关注 > 未关注 */
function followLabel(u) {
  if (!u.followed) return '+ 关注'
  return u.mutual ? '互相关注' : '已关注'
}

async function follow(u) {
  busyId.value = u.id
  try {
    const res = await toggleFollow(u.id)
    const d = res.data || {}
    u.followed = d.followed ?? !u.followed
    u.mutual = !!d.mutual
    if (typeof d.followerCount === 'number') u.followerCount = d.followerCount
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    busyId.value = null
  }
}

function openUser(u) {
  router.push({ name: 'user', params: { id: u.id } })
}

onMounted(load)
</script>

<style scoped>
.community-page { height: 100%; overflow-y: auto; }
.ct-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }

.cm-search { display: flex; gap: 10px; margin-bottom: 16px; }
.cm-search input {
  flex: 1;
  padding: 10px 13px;
  font-size: 14px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  font-family: inherit;
}
.cm-search input:focus { border-color: var(--c-ninja); }

.ct-list { list-style: none; display: flex; flex-direction: column; gap: 10px; }
.ct-item { display: flex; align-items: center; gap: 12px; padding: 13px 16px; }
.ct-main { flex: 1; min-width: 0; display: flex; flex-direction: column; cursor: pointer; }
.ct-name { font-size: 14px; }
.ct-sign {
  font-size: 12px;
  color: var(--c-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ct-fans { font-size: 11px; color: var(--c-mint); flex: none; }
</style>
