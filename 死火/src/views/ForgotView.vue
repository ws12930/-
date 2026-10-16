<template>
  <div class="auth-page">
    <div class="auth-bg left"></div>
    <div class="auth-bg right"></div>

    <!-- 阵营立绘：选中/悬停时放大并外发光 -->
    <div
      class="auth-cut naruto"
      :class="{ active: activeFaction === 'ninja', dimmed: activeFaction === 'shinigami' }"
      @mouseenter="hoverFaction = 'ninja'"
      @mouseleave="hoverFaction = null"
    >
      <img :src="narutoImg" alt="鸣人" />
    </div>
    <div
      class="auth-cut ichigo"
      :class="{ active: activeFaction === 'shinigami', dimmed: activeFaction === 'ninja' }"
      @mouseenter="hoverFaction = 'shinigami'"
      @mouseleave="hoverFaction = null"
    >
      <img :src="ichigoImg" alt="一护" />
    </div>

    <div class="auth-card panel">
      <h1 class="grad-text auth-title">BLEACH × NARUTO</h1>
      <p class="grad-text auth-subtitle">找回密码</p>

      <div class="auth-steps">
        <div class="auth-step" :class="{ done: step > 1, current: step === 1 }">
          <span class="num">1</span> 验证身份
        </div>
        <span class="auth-step-line"></span>
        <div class="auth-step" :class="{ done: step > 2, current: step === 2 }">
          <span class="num">2</span> 设置新密码
        </div>
        <span class="auth-step-line"></span>
        <div class="auth-step" :class="{ done: step === 3, current: step === 3 }">
          <span class="num">3</span> 完成
        </div>
      </div>

      <p v-if="error" class="auth-alert err">{{ error }}</p>
      <p v-if="success" class="auth-alert ok">{{ success }}</p>

      <!-- 第一步：验证身份 -->
      <form v-if="step === 1" @submit.prevent="handleVerify">
        <div class="auth-field">
          <label for="fg-username">用户名</label>
          <input
            id="fg-username"
            v-model.trim="form.username"
            type="text"
            placeholder="请输入注册时的用户名"
            autocomplete="username"
          />
        </div>

        <div class="auth-field">
          <label for="fg-email">注册邮箱</label>
          <input
            id="fg-email"
            v-model.trim="form.email"
            type="email"
            placeholder="请输入注册时填写的邮箱"
            autocomplete="email"
          />
          <p class="auth-hint tip">邮箱与注册信息一致即可重置密码</p>
        </div>

        <button class="g-btn submit" type="submit" :disabled="loading">
          {{ loading ? '验证中…' : '验 证 身 份' }}
        </button>
      </form>

      <!-- 第二步：设置新密码 -->
      <form v-else-if="step === 2" @submit.prevent="handleReset">
        <p class="auth-hint ok verified">身份已验证：{{ form.username }}</p>

        <div class="auth-field">
          <label for="fg-new">新密码（6-32 位）</label>
          <div class="pw-wrap">
            <input
              id="fg-new"
              v-model="form.newPassword"
              :type="showPw ? 'text' : 'password'"
              placeholder="请输入新密码"
              autocomplete="new-password"
            />
            <button type="button" class="toggle" @click="showPw = !showPw">
              {{ showPw ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <div class="auth-field">
          <label for="fg-confirm">确认新密码</label>
          <input
            id="fg-confirm"
            v-model="form.confirmPassword"
            :type="showPw ? 'text' : 'password'"
            placeholder="请再次输入新密码"
            autocomplete="new-password"
          />
        </div>

        <button class="g-btn submit" type="submit" :disabled="loading">
          {{ loading ? '提交中…' : '重 置 密 码' }}
        </button>
        <button type="button" class="auth-link back" @click="restart">重新验证身份</button>
      </form>

      <!-- 第三步：完成 -->
      <div v-else class="done-box">
        <p class="done-text grad-text">密码已重置</p>
        <p class="auth-hint tip">请使用新密码重新登录</p>
        <button class="g-btn submit" type="button" @click="goLogin">前 往 登 录</button>
      </div>

      <div class="auth-foot">
        <a class="auth-link" @click="goLogin">返回登录</a>
      </div>
    </div>

    <p v-if="hint" class="auth-toast">{{ hint }}</p>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { verifyIdentity, resetPassword } from '../api/auth'
import narutoImg from '../assets/naruto_cut.png'
import ichigoImg from '../assets/ichigo_cut.png'

const router = useRouter()

/** 1 验证身份 · 2 设置新密码 · 3 完成 */
const step = ref(1)

const form = reactive({
  username: '',
  email: '',
  newPassword: '',
  confirmPassword: ''
})

const showPw = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')
const hint = ref('')
const resetToken = ref('')

const hoverFaction = ref(null)
// 未明确选择阵营时，两侧立绘都不高亮；悬停时临时高亮
const activeFaction = computed(() => hoverFaction.value)

let hintTimer = null
function toast(msg) {
  hint.value = msg
  clearTimeout(hintTimer)
  hintTimer = setTimeout(() => (hint.value = ''), 2200)
}
onBeforeUnmount(() => clearTimeout(hintTimer))

async function handleVerify() {
  error.value = ''
  success.value = ''

  if (!form.username) {
    error.value = '请输入用户名'
    return
  }
  if (!form.email) {
    error.value = '请输入注册时填写的邮箱'
    return
  }
  if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    error.value = '邮箱格式不正确'
    return
  }

  loading.value = true
  try {
    const res = await verifyIdentity({
      username: form.username,
      email: form.email
    })
    resetToken.value = res.data.token
    success.value = '身份验证通过，请设置新密码'
    step.value = 2
  } catch (e) {
    error.value = e.message || '身份验证失败'
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  error.value = ''

  if (!form.newPassword) {
    error.value = '请输入新密码'
    return
  }
  if (form.newPassword.length < 6 || form.newPassword.length > 32) {
    error.value = '密码需 6-32 位'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    await resetPassword({
      token: resetToken.value,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })
    success.value = ''
    step.value = 3
  } catch (e) {
    error.value = e.message || '重置失败，请重新验证身份'
    if (e.message && e.message.includes('凭证')) {
      step.value = 1
      resetToken.value = ''
    }
  } finally {
    loading.value = false
  }
}

function restart() {
  step.value = 1
  resetToken.value = ''
  form.newPassword = ''
  form.confirmPassword = ''
  error.value = ''
  success.value = ''
}

function goLogin() {
  router.push({ name: 'login', query: { username: form.username } })
}
</script>

<style scoped>
.pw-wrap { position: relative; }
.pw-wrap input { padding-right: 58px; }
.toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: var(--c-dim);
  letter-spacing: 1px;
}
.toggle:hover { color: var(--c-ninja); }

.verified { margin-bottom: 14px; text-align: left; }

.submit { width: 100%; padding: 13px; letter-spacing: 6px; font-size: 16px; margin-top: 4px; }
.submit:disabled { letter-spacing: 2px; }

.back {
  display: block;
  margin: 14px auto 0;
  font-size: 13px;
}

.done-box { padding: 6px 0 10px; }
.done-text {
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 4px;
  margin-bottom: 10px;
}
</style>
