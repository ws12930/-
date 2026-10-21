<template>
  <div class="auth-page">
    <div class="auth-bg left"></div>
    <div class="auth-bg right"></div>

    <div class="auth-mascot">
      <img :src="mascotImg" alt="看板娘 青禾" />
    </div>

    <div class="auth-card panel">
      <h1 class="auth-title grad-text">找回密码</h1>

      <div class="auth-steps">
        <div class="auth-step" :class="{ done: step > 1, current: step === 1 }">
          <span class="num">1</span><span>验证身份</span>
        </div>
        <span class="auth-step-line"></span>
        <div class="auth-step" :class="{ done: step > 2, current: step === 2 }">
          <span class="num">2</span><span>重置密码</span>
        </div>
      </div>

      <p v-if="alert.text" class="auth-alert" :class="alert.type">{{ alert.text }}</p>

      <!-- 第一步：用户名 + 邮箱 -->
      <form v-if="step === 1" @submit.prevent="handleVerify">
        <div class="auth-field">
          <label for="fp-username">用户名</label>
          <input id="fp-username" v-model.trim="form.username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="auth-field">
          <label for="fp-email">注册邮箱</label>
          <input id="fp-email" v-model.trim="form.email" type="email" placeholder="注册时填写的邮箱" autocomplete="email" />
        </div>
        <button class="g-btn submit" type="submit" :disabled="loading">
          {{ loading ? '验证中…' : '下一步' }}
        </button>
      </form>

      <!-- 第二步：新密码 -->
      <form v-else @submit.prevent="handleReset">
        <div class="auth-field">
          <label for="fp-password">新密码</label>
          <div class="pw-wrap">
            <input
              id="fp-password"
              v-model="form.password"
              :type="showPw ? 'text' : 'password'"
              placeholder="至少 6 位"
              autocomplete="new-password"
            />
            <button type="button" class="toggle" @click="showPw = !showPw">
              {{ showPw ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>
        <div class="auth-field">
          <label for="fp-confirm">确认新密码</label>
          <input
            id="fp-confirm"
            v-model="form.confirm"
            :type="showPw ? 'text' : 'password'"
            placeholder="再输入一次"
            autocomplete="new-password"
          />
        </div>
        <button class="g-btn submit" type="submit" :disabled="loading">
          {{ loading ? '提交中…' : '确认重置' }}
        </button>
      </form>

      <div class="auth-foot">
        <span>想起来了？</span>
        <a class="auth-link" @click="goLogin">去登录</a>
      </div>
      <div class="auth-foot">
        <BackButton label="返回" />
      </div>
    </div>

    <p v-if="toast" class="auth-toast">{{ toast }}</p>
  </div>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { verifyIdentity, resetPassword } from '../api/auth'
import BackButton from '../components/BackButton.vue'
import mascotImg from '../assets/mascot/mascot-welcome.png'

const router = useRouter()

const step = ref(1)
const token = ref('')
const form = reactive({ username: '', email: '', password: '', confirm: '' })
const alert = reactive({ text: '', type: 'err' })
const showPw = ref(false)
const loading = ref(false)
const toast = ref('')

let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2200)
}
onBeforeUnmount(() => clearTimeout(toastTimer))

async function handleVerify() {
  if (!form.username || !form.email) {
    alert.type = 'err'
    alert.text = '请填写用户名和注册邮箱'
    return
  }
  loading.value = true
  alert.text = ''
  try {
    const res = await verifyIdentity({ username: form.username, email: form.email })
    token.value = res.data?.token || ''
    alert.type = 'ok'
    alert.text = '身份验证通过，请设置新密码'
    showToast('验证成功')
    step.value = 2
  } catch (e) {
    alert.type = 'err'
    alert.text = e.message || '验证失败，请检查用户名和邮箱'
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  if (form.password.length < 6) {
    alert.type = 'err'
    alert.text = '新密码至少 6 位'
    return
  }
  if (form.password !== form.confirm) {
    alert.type = 'err'
    alert.text = '两次输入的密码不一致'
    return
  }
  loading.value = true
  alert.text = ''
  try {
    await resetPassword({ token: token.value, password: form.password })
    alert.type = 'ok'
    alert.text = '密码已重置，正在跳转到登录页…'
    showToast('重置成功')
    setTimeout(() => {
      router.push({ name: 'login', query: { username: form.username } })
    }, 900)
  } catch (e) {
    alert.type = 'err'
    alert.text = e.message || '重置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push({ name: 'login' })
}
</script>

<style scoped>
.auth-card { text-align: center; }
.auth-field { text-align: left; }

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

.submit { width: 100%; padding: 13px; letter-spacing: 6px; font-size: 16px; margin-top: 6px; }
.submit:disabled { letter-spacing: 2px; }
</style>
