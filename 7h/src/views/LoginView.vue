<template>
  <div class="login">
    <div class="login-bg left"></div>
    <div class="login-bg right"></div>

    <!-- 看板娘「青禾」立绘 -->
    <div class="auth-mascot">
      <img :src="mascotImg" alt="看板娘 青禾" />
    </div>

    <div class="card panel">
      <h1 class="grad-text title">7h</h1>
      <p class="subtitle grad-text">登 录</p>

      <form @submit.prevent="handleLogin">
        <div class="field">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            @focus="error = ''"
          />
        </div>

        <div class="field">
          <label for="password">密码</label>
          <div class="pw-wrap">
            <input
              id="password"
              v-model="form.password"
              :type="showPw ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
              @focus="error = ''"
            />
            <button type="button" class="toggle" @click="showPw = !showPw">
              {{ showPw ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <div class="row">
          <label class="remember">
            <input v-model="form.remember" type="checkbox" />
            <span>记住我</span>
          </label>
          <a class="link" href="#" @click.prevent="goForgot">忘记密码？</a>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>

        <button class="g-btn submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <p class="register">还没有账号？<a class="link" href="#" @click.prevent="goRegister">立即注册</a></p>
    </div>

    <p v-if="hint" class="hint-toast">{{ hint }}</p>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '../api/auth'
import mascotImg from '../assets/mascot/mascot-welcome.png'

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: '',
  password: '',
  remember: false
})

const showPw = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')
const hint = ref('')

const REMEMBER_KEY = 'bn-login-remember'

onMounted(() => {
  try {
    const saved = localStorage.getItem(REMEMBER_KEY)
    if (saved) {
      const { username } = JSON.parse(saved)
      form.username = username || ''
      form.remember = true
    }
  } catch (e) { /* 忽略损坏的本地数据 */ }

  // 注册成功 / 重置密码后跳转回来时，自动带入用户名
  const from = route.query.username
  if (typeof from === 'string' && from) {
    form.username = from
  }
})

let hintTimer = null
function toast(msg) {
  hint.value = msg
  clearTimeout(hintTimer)
  hintTimer = setTimeout(() => (hint.value = ''), 2200)
}
onBeforeUnmount(() => clearTimeout(hintTimer))

function validate() {
  if (!form.username.trim()) return '请输入用户名'
  if (form.username.trim().length < 3) return '用户名至少 3 个字符'
  if (!form.password) return '请输入密码'
  if (form.password.length < 6) return '密码至少 6 位'
  return ''
}

async function handleLogin() {
  error.value = validate()
  if (error.value) return

  loading.value = true
  success.value = ''
  try {
    const res = await login({
      username: form.username.trim(),
      password: form.password,
      remember: form.remember
    })
    const user = res.data
    localStorage.setItem('bn-user', JSON.stringify(user))
    success.value = `欢迎回来，${user.nickname || user.username}！`

    if (form.remember) {
      localStorage.setItem(REMEMBER_KEY, JSON.stringify({ username: user.username }))
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }
    form.password = ''
    toast('登录成功，正在进入…')
    // 从文章页等位置跳来登录时，登录后回到原页面
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    setTimeout(() => {
      if (redirect && redirect.startsWith('/')) {
        router.replace(redirect)
      } else {
        router.push({ name: 'home' })
      }
    }, 700)
  } catch (e) {
    error.value = e.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function goForgot() {
  router.push({ name: 'forgot' })
}

function goRegister() {
  router.push({ name: 'register' })
}
</script>

<style scoped>
.login {
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-bg { position: absolute; inset: 0; }
.login-bg.left  { background: radial-gradient(ellipse at 15% 40%, rgba(143, 208, 174, 0.32), transparent 55%); }
.login-bg.right { background: radial-gradient(ellipse at 85% 40%, rgba(22, 68, 48, 0.42), transparent 55%); }

.card {
  position: relative;
  z-index: 2;
  width: min(400px, 92vw);
  padding: 36px 38px 28px;
  text-align: center;
}

.title {
  font-family: var(--font-display);
  font-size: clamp(24px, 4vw, 34px);
  letter-spacing: 3px;
}

.subtitle {
  font-size: 15px;
  letter-spacing: 10px;
  margin: 4px 0 26px;
}

.field { text-align: left; margin-bottom: 16px; }

.field label {
  display: block;
  font-size: 13px;
  color: var(--c-dim);
  margin-bottom: 7px;
  letter-spacing: 1px;
}

.field input {
  width: 100%;
  padding: 12px 14px;
  font-size: 15px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.field input::placeholder { color: rgba(255, 255, 255, 0.28); }
.field input:focus {
  border-color: var(--c-ninja);
  box-shadow: 0 0 0 3px rgba(150, 182, 168, 0.1);
}

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

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin: 4px 0 18px;
}

.remember {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-dim);
  cursor: pointer;
}
.remember input { accent-color: var(--c-ninja); cursor: pointer; }

.link { color: var(--c-shinigami); text-decoration: none; }
.link:hover { text-decoration: underline; }

.error {
  color: #ff5c5c;
  font-size: 13px;
  margin-bottom: 12px;
  letter-spacing: 1px;
}
.success {
  color: #8fd0ae;
  font-size: 13px;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.submit { width: 100%; padding: 13px; letter-spacing: 8px; font-size: 16px; }
.submit:disabled { letter-spacing: 2px; }

.register {
  margin-top: 20px;
  font-size: 13px;
  color: var(--c-dim);
}

.hint-toast {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 22px;
  font-size: 13px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.15);
  z-index: 5;
}

@media (max-width: 900px) {
  .card { padding: 30px 26px 24px; }
}
</style>
