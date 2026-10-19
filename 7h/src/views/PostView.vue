<template>
  <div class="post-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell mid">
      <p v-if="error" class="post-error">{{ error }}</p>

      <article v-if="post" class="panel post-body">
        <header class="pb-head">
          <h1 class="pb-title grad-text">{{ post.title }}</h1>
          <div class="pb-meta">
            <UserAvatar :user="post.author || {}" :size="28" />
            <a class="pb-author" @click="openAuthor">{{ post.author?.nickname || post.author?.username || '匿名' }}</a>
            <span class="pb-time">{{ formatTime(post.createdAt) }}</span>
            <span class="pb-spacer"></span>
            <button class="mini-btn" :class="{ on: post.liked }" type="button" @click="like">
              {{ post.liked ? '♥' : '♡' }} {{ post.likeCount || 0 }}
            </button>
          </div>
        </header>

        <RichText :content="post.content" class="pb-content" />

        <div v-if="(post.tags || []).length" class="pb-tags">
          <span v-for="t in post.tags" :key="t" class="pc-tag">{{ t }}</span>
        </div>
      </article>

      <section v-if="post" class="panel comments">
        <h2 class="cm-title">
          <span class="ph-icon">💬</span>
          评论 {{ comments.length }}
        </h2>

        <div v-if="logged" class="cm-editor">
          <ComposerBar
            v-model="draft"
            :target="commentInput"
            :upload="true"
            :stickers="true"
            placeholder="说点什么…"
          />
          <textarea
            ref="commentInput"
            v-model="draft"
            class="cm-input"
            rows="3"
            placeholder="友善交流，理性讨论"
          ></textarea>
          <div class="cm-actions">
            <button class="g-btn g-btn-sm" :disabled="sending" @click="submitComment">
              {{ sending ? '发送中…' : '发表评论' }}
            </button>
          </div>
        </div>
        <p v-else class="cm-tip">
          登录后即可参与评论
          <button class="cm-login" type="button" @click="goLogin">去登录</button>
        </p>

        <ul class="cm-list">
          <li
            v-for="c in comments"
            :id="'comment-' + c.id"
            :key="c.id"
            class="cm-item"
            :class="{ flash: highlightId === c.id }"
          >
            <UserAvatar :user="c.author || {}" :size="26" />
            <div class="cm-main">
              <div class="cm-line">
                <span class="cm-name">{{ c.author?.nickname || c.author?.username || '匿名' }}</span>
                <span class="cm-time">{{ formatTime(c.createdAt) }}</span>
              </div>
              <RichText :content="c.content" variant="comment" class="cm-text" />
            </div>
            <button
              v-if="canDelete(c)"
              class="mini-btn danger"
              type="button"
              @click="removeComment(c)"
            >
              删除
            </button>
          </li>
        </ul>

        <div v-if="!comments.length" class="empty-state">
          <span class="es-ico">🫧</span>
          <span>还没有评论，来抢沙发</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPostBySlug, listComments, createComment, deleteComment } from '../api/blog'
import { getMe } from '../api/auth'
import { toggleLike } from '../api/social'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import UserAvatar from '../components/UserAvatar.vue'
import RichText from '../components/RichText.vue'
import ComposerBar from '../components/ComposerBar.vue'
import { withPostAuthor, withCommentAuthors, readMe, isLogged } from '../utils/author'

const router = useRouter()
const route = useRoute()

const post = ref(null)
const comments = ref([])
const draft = ref('')
const commentInput = ref(null)
const sending = ref(false)
const error = ref('')

/** 从通知跳转过来时带 ?comment=id，需要滚动定位并短暂高亮 */
const highlightId = ref(null)
let highlightTimer = null

// 登录态每次实时读取：组件 setup 时缓存的话，登录后再次进入会拿到过期快照
const me = ref(readMe())
const logged = computed(() => isLogged())

/**
 * 读登录态。令牌还在、但本地用户信息丢了（换端口/清缓存/旧数据）时，
 * 用 /auth/me 补全一次，避免明明已登录却显示「登录后即可参与评论」。
 */
async function refreshMe() {
  me.value = readMe()
  if (me.value?.id || !isLogged()) return
  try {
    const res = await getMe()
    if (res.data?.id) {
      localStorage.setItem('bn-user', JSON.stringify(res.data))
      me.value = res.data
    }
  } catch {
    // 拿不到就保持游客态
  }
}

const navItems = [{ label: '首页', to: 'home' }]

function formatTime(v) {
  if (!v) return ''
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v).slice(0, 10)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function canDelete(c) {
  return !!me.value && c.author && c.author.id === me.value.id
}

async function loadPost() {
  error.value = ''
  try {
    const res = await getPostBySlug(route.params.slug)
    post.value = withPostAuthor(res.data || null)
  } catch (e) {
    error.value = e.message || '文章加载失败'
  }
}

async function loadComments() {
  if (!post.value) return
  try {
    const res = await listComments(post.value.id)
    comments.value = withCommentAuthors(res.data || [])
  } catch {
    comments.value = []
  }
}

/** 定位到 ?comment=id 指定的评论并高亮 2.4 秒 */
async function focusComment() {
  const raw = route.query.comment
  const id = Number(raw)
  if (!raw || !Number.isFinite(id)) return
  await nextTick()
  const el = document.getElementById('comment-' + id)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  highlightId.value = id
  clearTimeout(highlightTimer)
  highlightTimer = setTimeout(() => (highlightId.value = null), 2400)
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function like() {
  if (!post.value) return
  try {
    await toggleLike(post.value.id)
    post.value.liked = !post.value.liked
    post.value.likeCount = (post.value.likeCount || 0) + (post.value.liked ? 1 : -1)
  } catch (e) {
    error.value = e.message || '操作失败'
  }
}

async function submitComment() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    await createComment(post.value.id, { content })
    draft.value = ''
    await loadComments()
  } catch (e) {
    error.value = e.message || '评论失败'
  } finally {
    sending.value = false
  }
}

async function removeComment(c) {
  if (!window.confirm('确定删除这条评论？')) return
  try {
    await deleteComment(c.id)
    await loadComments()
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

function openAuthor() {
  if (post.value?.author?.id) router.push({ name: 'user', params: { id: post.value.author.id } })
}

onMounted(async () => {
  refreshMe()
  await loadPost()
  await loadComments()
  await focusComment()
})

onBeforeUnmount(() => clearTimeout(highlightTimer))
</script>

<style scoped>
.post-page { height: 100%; overflow-y: auto; }
.post-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }

.post-body { padding: 24px 26px; margin-bottom: 16px; }
.pb-title { font-family: var(--font-display); font-size: clamp(20px, 3.4vw, 28px); letter-spacing: 2px; }
.pb-meta { display: flex; align-items: center; gap: 10px; margin: 14px 0 18px; }
.pb-author { font-size: 13px; color: var(--c-mint); cursor: pointer; }
.pb-author:hover { text-decoration: underline; }
.pb-time { font-size: 11px; color: var(--c-dim); }
.pb-spacer { flex: 1; }

.pb-content { font-size: 14px; line-height: 1.9; color: var(--c-text); word-break: break-word; }
.pb-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 18px; }
.pc-tag {
  font-size: 11px;
  color: var(--c-mint);
  border: 1px solid rgba(194, 214, 204, 0.28);
  border-radius: 10px;
  padding: 2px 8px;
}

.comments { padding: 20px 22px; }
.cm-title { display: flex; align-items: center; gap: 10px; font-size: 15px; letter-spacing: 2px; margin-bottom: 14px; }

.cm-editor { margin-bottom: 18px; }
.cm-input {
  width: 100%;
  margin-top: 10px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  resize: vertical;
  font-family: inherit;
}
.cm-input:focus { border-color: var(--c-ninja); }
.cm-actions { display: flex; justify-content: flex-end; margin-top: 10px; }
.cm-tip { font-size: 12px; color: var(--c-dim); margin-bottom: 16px; }
.cm-login {
  margin-left: 8px;
  padding: 3px 12px;
  font-size: 12px;
  color: var(--c-mint);
  border: 1px solid rgba(143, 208, 174, 0.4);
  border-radius: 999px;
  background: rgba(143, 208, 174, 0.1);
  transition: background 0.2s, color 0.2s;
}
.cm-login:hover { color: #fff; background: rgba(143, 208, 174, 0.24); }

.cm-list { list-style: none; display: flex; flex-direction: column; gap: 14px; }
.cm-item { display: flex; gap: 10px; align-items: flex-start; padding: 6px 8px; border-radius: 8px; }
.cm-item.flash {
  animation: cm-flash 2.4s ease-out;
  background: rgba(150, 182, 168, 0.12);
  box-shadow: inset 0 0 0 1px rgba(150, 182, 168, 0.32);
}
@keyframes cm-flash {
  0%, 60% { background: rgba(150, 182, 168, 0.2); }
  100% { background: rgba(150, 182, 168, 0); }
}
.cm-main { flex: 1; min-width: 0; }
.cm-line { display: flex; align-items: baseline; gap: 8px; }
.cm-name { font-size: 13px; color: var(--c-text); }
.cm-time { font-size: 11px; color: var(--c-dim); }
.cm-text { font-size: 13px; line-height: 1.75; color: var(--c-text); opacity: 0.92; word-break: break-word; }
</style>
