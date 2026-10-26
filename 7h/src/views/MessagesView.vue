<template>
  <div class="msg-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell wide msg-shell">
      <header class="pg-head">
        <h1 class="pg-title grad-text">私 信</h1>
        <p class="pg-sub grad-text">和关注的人聊两句</p>
      </header>

      <p v-if="error" class="ms-error">{{ error }}</p>

      <div class="ms-layout">
        <aside class="panel ms-side">
          <div class="ms-search">
            <input
              v-model.trim="keyword"
              type="text"
              class="ms-search-input"
              placeholder="搜索会话 / 找人"
              @keydown.enter="doSearch"
            />
            <button
              v-if="keyword"
              class="ms-search-clear"
              type="button"
              title="清空"
              @click="clearSearch"
            >
              ✕
            </button>
          </div>

          <h2 class="ms-side-title">会话</h2>
          <div v-if="!conversations.length" class="empty-state">
            <span class="es-ico">💬</span>
            <span>还没有会话</span>
          </div>
          <div v-else-if="!matchedConversations.length" class="ms-tip">没有匹配的会话</div>
          <ul v-else class="ms-conv-list">
            <li
              v-for="c in matchedConversations"
              :key="c.peerId"
              class="ms-conv"
              :class="{ on: currentPeer === c.peerId }"
              @click="open(c.peerId)"
            >
              <UserAvatar :user="peerOf(c)" :size="30" />
              <div class="ms-conv-main">
                <span class="ms-conv-name">{{ peerName(c) }}</span>
                <span class="ms-conv-last">{{ c.lastMessage || '' }}</span>
              </div>
              <span v-if="c.unread" class="ms-badge">{{ c.unread }}</span>
            </li>
          </ul>

          <template v-if="keyword">
            <h2 class="ms-side-title ms-side-title-sub">找人</h2>
            <div v-if="searching" class="ms-tip">搜索中…</div>
            <div v-else-if="!people.length" class="ms-tip">没有找到相关用户</div>
            <ul v-else class="ms-conv-list">
              <li
                v-for="p in people"
                :key="p.id"
                class="ms-conv"
                :class="{ on: currentPeer === p.id }"
                @click="startChat(p)"
              >
                <UserAvatar :user="p" :size="30" />
                <div class="ms-conv-main">
                  <span class="ms-conv-name">{{ p.nickname || p.username }}</span>
                  <span class="ms-conv-last">@{{ p.username }}</span>
                </div>
                <span class="ms-new">聊天</span>
              </li>
            </ul>
          </template>
        </aside>

        <section class="panel ms-main">
          <div v-if="!currentPeer" class="empty-state">
            <span class="es-ico">✉️</span>
            <span>从左边选一个人开始聊天</span>
          </div>

          <template v-else>
            <div class="ms-stream" ref="streamEl">
              <div v-if="!messages.length" class="empty-state">
                <span class="es-ico">🫧</span>
                <span>还没有消息，打个招呼吧</span>
              </div>
              <div
                v-for="(m, i) in messages"
                :key="m.id || i"
                class="ms-bubble"
                :class="{ mine: isMine(m), recalled: m.recalled === 1 }"
              >
                <template v-if="m.recalled === 1">
                  <span class="ms-recalled">
                    {{ isMine(m) ? '你撤回了一条消息' : '对方撤回了一条消息' }}
                  </span>
                </template>
                <template v-else>
                  <div v-if="m.quoteId" class="ms-quote">
                    <span class="ms-quote-name">{{ m.quoteSenderName || '消息' }}</span>
                    <span class="ms-quote-text">{{ m.quoteContent || '消息已撤回' }}</span>
                  </div>
                  <RichText :content="m.content" variant="bubble" class="ms-bubble-text" />
                </template>
                <div class="ms-bubble-foot">
                  <span class="ms-bubble-time">{{ formatTime(m.createdAt) }}</span>
                  <template v-if="m.recalled !== 1">
                    <button class="ms-quote-btn" type="button" @click="startQuote(m)">引用</button>
                    <button
                      v-if="isMine(m)"
                      class="ms-quote-btn"
                      type="button"
                      @click="askConfirm(m, 'recall')"
                    >
                      撤回
                    </button>
                    <button
                      class="ms-quote-btn danger"
                      type="button"
                      @click="askConfirm(m, 'delete')"
                    >
                      删除
                    </button>
                  </template>
                </div>
                <div v-if="confirming && confirming.id === m.id" class="ms-confirm">
                  <span class="ms-confirm-text">{{ confirmText }}</span>
                  <button class="ms-confirm-yes" type="button" @click="doConfirm">确定</button>
                  <button class="ms-confirm-no" type="button" @click="cancelConfirm">取消</button>
                </div>
              </div>
            </div>

            <div class="ms-composer">
              <div v-if="quoting" class="ms-quote-to">
                <span class="ms-quote-to-name">引用 {{ quoting.senderName }}</span>
                <span class="ms-quote-to-text">{{ quoting.preview }}</span>
                <button class="ms-quote-cancel" type="button" title="取消引用" @click="cancelQuote">
                  ✕
                </button>
              </div>
              <ComposerBar
                v-model="draft"
                :target="inputEl"
                :upload="true"
                :stickers="true"
                placeholder="输入消息…"
              />
              <textarea
                ref="inputEl"
                v-model="draft"
                class="ms-input"
                rows="2"
                placeholder="按 Ctrl + Enter 发送"
                @keydown.ctrl.enter="send"
              ></textarea>
              <div class="ms-actions">
                <button
                  v-if="quoting"
                  class="ms-cancel"
                  type="button"
                  @click="cancelQuote"
                >
                  取消引用
                </button>
                <button class="g-btn g-btn-sm" :disabled="sending" @click="send">
                  {{ sending ? '发送中…' : '发送' }}
                </button>
              </div>
            </div>
          </template>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import {
  listConversations,
  listMessages,
  sendMessage,
  searchUsers,
  deleteMessage,
  recallMessage
} from '../api/social'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import UserAvatar from '../components/UserAvatar.vue'
import RichText from '../components/RichText.vue'
import ComposerBar from '../components/ComposerBar.vue'
import { toPlainSummary } from '../utils/richText'

const conversations = ref([])
const messages = ref([])
const currentPeer = ref(null)
const draft = ref('')
const inputEl = ref(null)
const streamEl = ref(null)
const sending = ref(false)
const error = ref('')

/** 正在引用的消息：{ id, senderName, preview }，null 表示不引用 */
const quoting = ref(null)

/**
 * 待确认的危险操作：{ id, kind }，kind 为 'delete' | 'recall'。
 * 不用 window.confirm —— 原生弹窗在部分内嵌预览环境里会被静默拦截，点了没反应。
 */
const confirming = ref(null)
const confirmText = computed(() =>
  confirming.value?.kind === 'recall'
    ? '撤回后双方都会看到「消息已撤回」，确定？'
    : '删除后仅你自己看不到，对方不受影响'
)

function askConfirm(m, kind) {
  confirming.value = confirming.value && confirming.value.id === m.id && confirming.value.kind === kind
    ? null
    : { id: m.id, kind }
}

function cancelConfirm() {
  confirming.value = null
}

async function doConfirm() {
  const target = confirming.value
  if (!target || !currentPeer.value) return
  const peerId = currentPeer.value
  confirming.value = null
  try {
    if (target.kind === 'recall') {
      await recallMessage(target.id)
    } else {
      await deleteMessage(target.id)
    }
    await open(peerId)
    await loadConversations()
  } catch (e) {
    error.value = e.message || '操作失败'
  }
}

/* ===== 会话搜索 ===== */
const keyword = ref('')
const people = ref([])
const searching = ref(false)
let searchTimer = null

const me = ref(null)
try {
  me.value = JSON.parse(localStorage.getItem('bn-user') || 'null')
} catch {
  me.value = null
}

const navItems = [{ label: '首页', to: 'home' }]

/** 会话列表按关键字过滤（先本地过滤已有会话） */
const matchedConversations = computed(() => {
  const kw = keyword.value.toLowerCase()
  if (!kw) return conversations.value
  return conversations.value.filter((c) => peerName(c).toLowerCase().includes(kw))
})

function peerName(c) {
  return c.peerName || c.peer?.nickname || c.peer?.username || '用户'
}

function peerOf(c) {
  return c.peer || { nickname: c.peerName, avatar: c.peerAvatar, faction: c.peerFaction }
}

watch(keyword, (v) => {
  clearTimeout(searchTimer)
  if (!v) {
    people.value = []
    return
  }
  searchTimer = setTimeout(doSearch, 300)
})

/** 搜人：用于开启一个还没有历史消息的会话 */
async function doSearch() {
  const kw = keyword.value
  if (!kw) {
    people.value = []
    return
  }
  searching.value = true
  try {
    const res = await searchUsers(kw, 20)
    people.value = (res.data || []).filter((u) => !me.value || u.id !== me.value.id)
  } catch {
    people.value = []
  } finally {
    searching.value = false
  }
}

function clearSearch() {
  keyword.value = ''
  people.value = []
}

async function startChat(user) {
  clearSearch()
  await open(user.id)
  await loadConversations()
}

/** 当前会话对方的昵称，用于引用条上显示「引用 谁」 */
function currentPeerName() {
  const conv = conversations.value.find((c) => c.peerId === currentPeer.value)
  return conv ? peerName(conv) : ''
}

function isMine(m) {
  const sid = m.senderId ?? m.fromUserId ?? m.sender?.id
  return !!me.value && sid === me.value.id
}

/** 点气泡上的「引用」：把这条消息设为引用目标，输入框聚焦 */
function startQuote(m) {
  const name = isMine(m) ? me.value?.nickname || '我' : currentPeerName() || '对方'
  quoting.value = {
    id: m.id,
    senderName: name,
    preview: toPlainSummary(m.content, 40)
  }
  nextTick(() => inputEl.value?.focus())
}

function cancelQuote() {
  quoting.value = null
}

function formatTime(v) {
  if (!v) return ''
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadConversations() {
  try {
    const res = await listConversations()
    conversations.value = res.data || []
  } catch (e) {
    error.value = e.message || '加载失败'
    conversations.value = []
  }
}

async function open(peerId) {
  currentPeer.value = peerId
  messages.value = []
  quoting.value = null
  confirming.value = null
  try {
    const res = await listMessages(peerId)
    messages.value = res.data || []
    await nextTick()
    if (streamEl.value) streamEl.value.scrollTop = streamEl.value.scrollHeight
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content || !currentPeer.value) return
  sending.value = true
  error.value = ''
  try {
    await sendMessage(currentPeer.value, content, quoting.value ? quoting.value.id : null)
    draft.value = ''
    quoting.value = null
    await open(currentPeer.value)
    await loadConversations()
  } catch (e) {
    error.value = e.message || '发送失败'
  } finally {
    sending.value = false
  }
}

onMounted(loadConversations)
</script>

<style scoped>
.msg-page { height: 100%; }
.msg-shell { display: flex; flex-direction: column; height: 100%; padding-bottom: 24px; }

.ms-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }

.ms-layout { display: flex; gap: 14px; flex: 1; min-height: 0; }

.ms-side { width: 240px; flex: none; padding: 16px; overflow-y: auto; }
.ms-side-title { font-size: 13px; letter-spacing: 2px; color: var(--c-dim); margin-bottom: 12px; }
.ms-side-title-sub { margin-top: 16px; }

.ms-search { position: relative; margin-bottom: 14px; }
.ms-search-input {
  width: 100%;
  padding: 8px 28px 8px 11px;
  font-size: 13px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  font-family: inherit;
}
.ms-search-input:focus { border-color: var(--c-ninja); }
.ms-search-clear {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  padding: 2px 6px;
  font-size: 12px;
  color: var(--c-dim);
  background: transparent;
  border: none;
  cursor: pointer;
}
.ms-search-clear:hover { color: var(--c-text); }

.ms-tip { font-size: 12px; color: var(--c-dim); padding: 6px 2px 10px; }
.ms-new { flex: none; font-size: 11px; color: var(--c-mint); }
.ms-conv-list { list-style: none; display: flex; flex-direction: column; gap: 6px; }
.ms-conv {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.ms-conv:hover { background: rgba(255, 255, 255, 0.05); }
.ms-conv.on { background: rgba(150, 182, 168, 0.14); }
.ms-conv-main { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.ms-conv-name { font-size: 13px; }
.ms-conv-last {
  font-size: 11px;
  color: var(--c-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ms-badge {
  flex: none;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  font-size: 11px;
  border-radius: 9px;
  background: #ff6b8a;
  color: #fff;
  padding: 0 5px;
}

.ms-main { flex: 1; min-width: 0; padding: 16px; display: flex; flex-direction: column; }
.ms-stream { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding-right: 4px; }

.ms-bubble {
  align-self: flex-start;
  max-width: 76%;
  padding: 9px 13px;
  border-radius: 12px 12px 12px 4px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.ms-bubble.mine {
  align-self: flex-end;
  border-radius: 12px 12px 4px 12px;
  background: rgba(150, 182, 168, 0.18);
  border-color: rgba(150, 182, 168, 0.32);
}
.ms-bubble-text { font-size: 13px; line-height: 1.7; word-break: break-word; }
.ms-bubble-foot { display: flex; align-items: center; justify-content: flex-end; gap: 8px; margin-top: 4px; }
.ms-bubble-time { font-size: 10px; color: var(--c-dim); }
.ms-quote-btn {
  padding: 0 4px;
  font-size: 10px;
  color: var(--c-dim);
  background: transparent;
  border: none;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.18s, color 0.18s;
}
.ms-bubble:hover .ms-quote-btn { opacity: 1; }
.ms-quote-btn:hover { color: var(--c-mint); }
.ms-quote-btn.danger:hover { color: #ff8a8a; }

/* 撤回后的气泡：双方都看得到，只是内容换成提示 */
.ms-bubble.recalled {
  align-self: center;
  max-width: 90%;
  background: rgba(255, 255, 255, 0.04);
  border-style: dashed;
  border-radius: 10px;
}
.ms-recalled { font-size: 12px; color: var(--c-dim); font-style: italic; }

/* 删除 / 撤回的二次确认：页面内确认，不依赖原生弹窗 */
.ms-confirm {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed rgba(255, 255, 255, 0.14);
}
.ms-confirm-text { flex: 1; font-size: 11px; color: var(--c-dim); line-height: 1.4; }
.ms-confirm-yes,
.ms-confirm-no {
  flex: none;
  padding: 2px 10px;
  font-size: 11px;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  transition: opacity 0.2s;
}
.ms-confirm-yes {
  color: #fff;
  background: rgba(255, 107, 138, 0.72);
  border: 1px solid rgba(255, 107, 138, 0.9);
}
.ms-confirm-no {
  color: var(--c-dim);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.18);
}
.ms-confirm-yes:hover,
.ms-confirm-no:hover { opacity: 0.82; }

/* 气泡内的引用块 */
.ms-quote {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 5px;
  padding: 5px 9px;
  border-left: 3px solid rgba(150, 182, 168, 0.38);
  border-radius: 0 6px 6px 0;
  background: rgba(255, 255, 255, 0.06);
}
.ms-quote-name { font-size: 11px; color: var(--c-mint); }
.ms-quote-text {
  font-size: 11px;
  line-height: 1.5;
  color: var(--c-dim);
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 输入框上方的引用目标提示条 */
.ms-quote-to {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 6px 10px;
  font-size: 12px;
  border-left: 3px solid rgba(150, 182, 168, 0.42);
  border-radius: 0 6px 6px 0;
  background: rgba(255, 255, 255, 0.06);
}
.ms-quote-to-name { flex: none; color: var(--c-mint); }
.ms-quote-to-text {
  flex: 1;
  min-width: 0;
  color: var(--c-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ms-quote-cancel {
  flex: none;
  padding: 0 4px;
  font-size: 12px;
  color: var(--c-dim);
  background: transparent;
  border: none;
  cursor: pointer;
}
.ms-quote-cancel:hover { color: var(--c-text); }

.ms-composer { margin-top: 12px; }
.ms-input {
  width: 100%;
  margin-top: 10px;
  padding: 10px 12px;
  font-size: 14px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  resize: none;
  font-family: inherit;
}
.ms-input:focus { border-color: var(--c-ninja); }
.ms-actions { display: flex; justify-content: flex-end; align-items: center; gap: 10px; margin-top: 8px; }
.ms-cancel {
  padding: 5px 14px;
  font-size: 12px;
  color: var(--c-dim);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.ms-cancel:hover { color: var(--c-text); border-color: rgba(255, 255, 255, 0.32); }

@media (max-width: 760px) {
  .ms-layout { flex-direction: column; }
  .ms-side { width: 100%; max-height: 180px; }
}
</style>
