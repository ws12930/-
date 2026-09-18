<template>
  <div class="login">
    <div class="login-bg left"></div>
    <div class="login-bg right"></div>

    <img class="cut naruto" :src="narutoImg" alt="鸣人" />
    <img class="cut ichigo" :src="ichigoImg" alt="一护" />

    <div class="card panel">
      <h1 class="grad-text title">BLEACH × NARUTO</h1>
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
          <a class="link" href="#" @click.prevent="toast('请联系管理员重置密码')">忘记密码？</a>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>

        <button class="g-btn submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <div class="divider"><span>或使用阵营身份登录</span></div>

      <div class="oauth">
        <button class="oauth-btn ninja" @click="oauth('木叶忍者')">
          <span class="dot"></span> 木叶忍者
        </button>
        <button class="oauth-btn shinigami" @click="oauth('代理死神')">
          <span class="dot"></span> 代理死神
        </button>
      </div>

      <p class="register">还没有账号？<a class="link" href="#" @click.prevent="toast('注册功能暂未开放')">立即注册</a></p>
    </div>

    <p v-if="hint" class="hint-toast">{{ hint }}</p>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import narutoImg from '../assets/naruto_cut.png'
import ichigoImg from '../assets/ichigo_cut.png'

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

function handleLogin() {
  error.value = validate()
  if (error.value) return

  loading.value = true
  success.value = ''
  // 纯前端演示：无后端，模拟请求
  setTimeout(() => {
    loading.value = false
    success.value = `欢迎回来，${form.username.trim()}！`

    if (form.remember) {
      localStorage.setItem(REMEMBER_KEY, JSON.stringify({ username: form.username.trim() }))
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }
    form.password = ''
  }, 900)
}

function oauth(faction) {
  toast(`正在跳转 ${faction} 身份授权…（演示）`)
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
.login-bg.left  { background: radial-gradient(ellipse at 15% 40%, rgba(217, 84, 0, 0.32), transparent 55%); }
.login-bg.right { background: radial-gradient(ellipse at 85% 40%, rgba(0, 78, 146, 0.42), transparent 55%); }

.cut {
  position: absolute;
  bottom: 0;
  height: 80%;
  max-height: 660px;
  filter: drop-shadow(0 0 30px rgba(0, 0, 0, 0.8));
  pointer-events: none;
  animation: float 5s ease-in-out infinite;
}
.cut.naruto { left: 3%; }
.cut.ichigo { right: 3%; animation-delay: 2.5s; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

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
  box-shadow: 0 0 0 3px rgba(255, 140, 26, 0.12);
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
  color: #4ade80;
  font-size: 13px;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.submit { width: 100%; padding: 13px; letter-spacing: 8px; font-size: 16px; }
.submit:disabled { letter-spacing: 2px; }

.divider {
  position: relative;
  margin: 24px 0 18px;
  color: var(--c-dim);
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 28%;
  height: 1px;
  background: rgba(255, 255, 255, 0.14);
}
.divider::before { left: 0; }
.divider::after { right: 0; }

.oauth { display: flex; gap: 12px; }

.oauth-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 0;
  font-size: 14px;
  letter-spacing: 1px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  color: var(--c-dim);
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.oauth-btn .dot { width: 8px; height: 8px; border-radius: 50%; }
.oauth-btn.ninja .dot { background: var(--c-ninja); }
.oauth-btn.shinigami .dot { background: var(--c-shinigami); }
.oauth-btn.ninja:hover { color: var(--c-ninja); border-color: var(--c-ninja); background: rgba(255, 140, 26, 0.08); }
.oauth-btn.shinigami:hover { color: var(--c-shinigami); border-color: var(--c-shinigami); background: rgba(0, 229, 255, 0.08); }

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
  .cut { opacity: 0.3; height: 60%; }
}
</style>
