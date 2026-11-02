<template>
  <div class="profile-page">
    <PageNav :items="navItems" />
    <BackButton is-fixed label="返回" />

    <div class="pg-shell narrow">
      <header class="pg-head">
        <h1 class="pg-title grad-text">个人资料</h1>
        <p class="pg-sub grad-text">修改昵称、头像与签名</p>
      </header>

      <p v-if="error" class="pf-error">{{ error }}</p>
      <p v-if="ok" class="pf-ok">{{ ok }}</p>

      <!-- 等级 / 称号 / 经验与签到 -->
      <div v-if="checkin" class="panel pf-level">
        <div class="pf-level-head">
          <span class="pf-lv-badge">Lv{{ checkin.level }}</span>
          <span class="pf-lv-title grad-text">{{ checkin.title }}</span>
          <span class="pf-lv-exp">{{ checkin.exp }} 经验</span>
          <button
            class="g-btn g-btn-sm pf-checkin"
            type="button"
            :disabled="checkin.checked || checking"
            @click="handleCheckin"
          >
            {{ checkin.checked ? '今日已签到' : (checking ? '签到中…' : '签到 +5') }}
          </button>
        </div>
        <div class="pf-bar">
          <div class="pf-bar-fill" :style="{ width: levelProgress + '%' }"></div>
        </div>
        <p class="pf-level-note">
          {{ checkin.nextLevelExp > 0
            ? `距离 Lv${checkin.level + 1} 还需 ${checkin.nextLevelExp - checkin.exp} 经验`
            : '已达最高等级' }}
          · 连续签到 {{ checkin.streak }} 天
        </p>
      </div>

      <div class="panel pf-panel">
        <div class="pf-avatar-row">
          <UserAvatar :user="previewUser" :size="72" />
          <div class="pf-avatar-actions">
            <button class="g-btn g-btn-sm" :disabled="uploading" @click="pickAvatar">
              {{ uploading ? '上传中…' : '更换头像' }}
            </button>
            <span class="pf-note">支持 JPG / PNG / GIF / WEBP，不超过 5MB</span>
            <input
              ref="fileInput"
              class="pf-file"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              @change="onFile"
            />
          </div>
        </div>

        <div class="pf-field">
          <label for="pf-nickname">昵称</label>
          <input id="pf-nickname" v-model.trim="form.nickname" type="text" placeholder="展示给别人的名字" />
        </div>

        <div class="pf-field">
          <label for="pf-avatar">头像地址</label>
          <input id="pf-avatar" v-model.trim="form.avatar" type="text" placeholder="上传后自动填入，也可手填图片链接" />
        </div>

        <div class="pf-field">
          <label for="pf-signature">个性签名</label>
          <textarea id="pf-signature" v-model.trim="form.signature" rows="3" placeholder="一句话介绍自己"></textarea>
        </div>

        <div class="pf-actions">
          <button class="g-btn" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保 存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { updateProfile } from '../api/social'
import { checkinToday, doCheckin } from '../api/community'
import { uploadFile } from '../api/upload'
import PageNav from '../components/PageNav.vue'
import BackButton from '../components/BackButton.vue'
import UserAvatar from '../components/UserAvatar.vue'

const form = reactive({ nickname: '', avatar: '', signature: '' })
const me = ref(null)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const ok = ref('')
const fileInput = ref(null)

/* 等级 / 经验 / 签到 */
const checkin = ref(null)
const checking = ref(false)

const levelProgress = computed(() => {
  const c = checkin.value
  if (!c) return 0
  if (!c.nextLevelExp || c.nextLevelExp <= 0) return 100
  const span = c.nextLevelExp - c.levelStartExp
  if (span <= 0) return 100
  return Math.max(0, Math.min(100, ((c.exp - c.levelStartExp) / span) * 100))
})

async function loadCheckin() {
  try {
    const res = await checkinToday()
    checkin.value = res.data
  } catch {
    checkin.value = null
  }
}

async function handleCheckin() {
  checking.value = true
  try {
    const res = await doCheckin()
    checkin.value = res.data
    ok.value = `签到成功 +${res.data.gained} 经验，连续 ${res.data.streak} 天`
  } catch (e) {
    error.value = e.message || '签到失败'
  } finally {
    checking.value = false
  }
}

const navItems = [
  { label: '首页', to: 'home' },
  { label: '我的文章', to: 'mine' }
]

const previewUser = computed(() => ({
  ...(me.value || {}),
  nickname: form.nickname,
  avatar: form.avatar
}))

function saveLocal(user) {
  localStorage.setItem('bn-user', JSON.stringify(user))
}

async function save() {
  error.value = ''
  ok.value = ''
  if (!form.nickname) {
    error.value = '昵称不能为空'
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname,
      avatar: form.avatar,
      signature: form.signature
    })
    const next = { ...(me.value || {}), ...form }
    me.value = next
    saveLocal(next)
    ok.value = '资料已更新'
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function pickAvatar() {
  fileInput.value?.click()
}

async function onFile(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    error.value = '图片不能超过 5MB'
    return
  }
  uploading.value = true
  error.value = ''
  try {
    const result = await uploadFile(file)
    if (!result.image) throw new Error('该文件不是图片')
    form.avatar = result.url
    ok.value = '头像已上传，记得点保存'
  } catch (err) {
    error.value = err.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  try {
    me.value = JSON.parse(localStorage.getItem('bn-user') || 'null')
  } catch {
    me.value = null
  }
  if (me.value) {
    form.nickname = me.value.nickname || me.value.username || ''
    form.avatar = me.value.avatar || ''
    form.signature = me.value.signature || ''
  }
  loadCheckin()
})
</script>

<style scoped>
.profile-page { height: 100%; overflow-y: auto; }
.pf-error { text-align: center; color: #ff8a8a; font-size: 13px; margin-bottom: 12px; }
.pf-ok { text-align: center; color: #8fd0ae; font-size: 13px; margin-bottom: 12px; }

.pf-panel { padding: 24px 26px; }

/* 等级 / 经验卡 */
.pf-level { padding: 16px 20px; margin-bottom: 14px; }
.pf-level-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pf-lv-badge {
  padding: 2px 9px;
  font-size: 12px;
  font-weight: 700;
  color: var(--c-bg);
  background: var(--c-ninja);
  border-radius: 999px;
}
.pf-lv-title { font-size: 15px; letter-spacing: 1px; }
.pf-lv-exp { font-size: 12px; color: var(--c-dim); }
.pf-checkin { margin-left: auto; padding: 4px 12px; font-size: 12px; }
.pf-bar {
  height: 6px;
  margin: 12px 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.pf-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--c-ninja), var(--c-acid));
  transition: width 0.4s ease;
}
.pf-level-note { font-size: 11px; color: var(--c-dim); margin: 0; }

.pf-avatar-row { display: flex; align-items: center; gap: 18px; margin-bottom: 22px; }
.pf-avatar-actions { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; }
.pf-note { font-size: 11px; color: var(--c-dim); }
.pf-file { display: none; }

.pf-field { text-align: left; margin-bottom: 14px; }
.pf-field label {
  display: block;
  font-size: 13px;
  color: var(--c-dim);
  margin-bottom: 7px;
  letter-spacing: 1px;
}
.pf-field input,
.pf-field textarea {
  width: 100%;
  padding: 11px 13px;
  font-size: 14px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  resize: vertical;
  font-family: inherit;
}
.pf-field input:focus,
.pf-field textarea:focus { border-color: var(--c-ninja); }

.pf-actions { display: flex; justify-content: flex-end; margin-top: 18px; }
</style>
