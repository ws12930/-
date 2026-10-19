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
          <h2 class="ms-side-title">会话</h2>
          <div v-if="!conversations.length" class="empty-state">
            <span class="es-ico">💬</span>
            <span>还没有会话</span>
          </div>
          <ul v-else class="ms-conv-list">
            <li
              v-for="c in conversations"
              :key="c.peerId"
              class="ms-conv"
              :class="{ on: currentPeer === c.peerId }"
              @click="open(c.peerId)"
            >
              <UserAvatar :user="c.peer || {}" :size="30" />
              <div class="ms-conv-main">
                <span class="ms-conv-name">{{ c.peer?.nickname || c.peer?.username || '用户' }}</span>
                <span class="ms-conv-last">{{ c.lastMessage || '' }}</span>
              </div>
              <span v-if="c.unread" class="ms-badge">{{ c.unread }}</span>
            </li>
          </ul>
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
                :class="{ mine: isMine(m) }"
              >
                <RichText :content="m.content" variant="bubble" class="ms-bubble-text" />
                <span class="ms-bubble-time">{{ formatTime(m.createdAt) }}</span>
              </div>
            </div>

            <div class="ms-composer">
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { listConversations, listMessages, sendMessage } from '../api/social'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import UserAvatar from '../components/UserAvatar.vue'
import RichText from '../components/RichText.vue'
import ComposerBar from '../components/ComposerBar.vue'

const conversations = ref([])
const messages = ref([])
const currentPeer = ref(null)
const draft = ref('')
const inputEl = ref(null)
const streamEl = ref(null)
const sending = ref(false)
const error = ref('')

const me = ref(null)
try {
  me.value = JSON.parse(localStorage.getItem('bn-user') || 'null')
} catch {
  me.value = null
}

const navItems = [{ label: '首页', to: 'home' }]

function isMine(m) {
  const sid = m.senderId ?? m.fromUserId ?? m.sender?.id
  return !!me.value && sid === me.value.id
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
    await sendMessage(currentPeer.value, content)
    draft.value = ''
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
.ms-bubble-time { display: block; margin-top: 4px; font-size: 10px; color: var(--c-dim); }

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
.ms-actions { display: flex; justify-content: flex-end; margin-top: 8px; }

@media (max-width: 760px) {
  .ms-layout { flex-direction: column; }
  .ms-side { width: 100%; max-height: 180px; }
}
</style>
