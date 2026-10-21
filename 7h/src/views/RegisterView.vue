<template>
  <div class="auth-page">
    <div class="auth-bg left"></div>
    <div class="auth-bg right"></div>

    <div class="auth-mascot">
      <img :src="mascotImg" alt="看板娘 青禾" />
    </div>

    <div class="auth-card panel">
      <h1 class="auth-title grad-text">注 册</h1>
      <p class="auth-subtitle grad-text">加入 7h</p>

      <p v-if="alert.text" class="auth-alert" :class="alert.type">{{ alert.text }}</p>

      <form @submit.prevent="handleRegister">
        <div class="auth-field" :class="fieldState('username')">
          <label for="reg-username">用户名</label>
          <input
            id="reg-username"
            v-model.trim="form.username"
            type="text"
            placeholder="3-20 位字母、数字或下划线"
            autocomplete="username"
            @blur="checkName"
          />
          <p v-if="hints.username" class="auth-hint" :class="hints.username.type">{{ hints.username.text }}</p>
        </div>

        <div class="auth-field" :class="fieldState('nickname')">
          <label for="reg-nickname">昵称</label>
          <input id="reg-nickname" v-model.trim="form.nickname" type="text" placeholder="展示在文章上的名字" />
        </div>

        <div class="auth-field" :class="fieldState('email')">
          <label for="reg-email">邮箱</label>
          <input id="reg-email" v-model.trim="form.email" type="email" placeholder="用于找回密码" autocomplete="email" />
        </div>

        <div class="auth-field" :class="fieldState('password')">
          <label for="reg-password">密码</label>
          <div class="pw-wrap">
            <input
              id="reg-password"
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

        <div class="auth-field" :class="fieldState('confirm')">
          <label for="reg-confirm">确认密码</label>
          <input
            id="reg-confirm"
            v-model="form.confirm"
            :type="showPw ? 'text' : 'password'"
            placeholder="再输入一次"
            autocomplete="new-password"
          />
        </div>

        <button class="g-btn submit" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注 册' }}
        </button>
      </form>

      <div class="auth-foot">
        <span>已有账号？</span>
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
import { register, checkUsername } from '../api/auth'
import BackButton from '../components/BackButton.vue'
import mascotImg from '../assets/mascot/mascot-welcome.png'

const router = useRouter()

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirm: ''
})

const hints = reactive({})
const alert = reactive({ text: '', type: 'err' })
const showPw = ref(false)
const loading = ref(false)
const toast = ref('')

const NAME_RE = /^[A-Za-z0-9_]{3,20}$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2200)
}
onBeforeUnmount(() => clearTimeout(toastTimer))

function fieldState(key) {
  const h = hints[key]
  if (!h) return ''
  return h.type === 'err' ? 'invalid' : 'valid'
}

async function checkName() {
  if (!form.username) {
    delete hints.username
    return
  }
  if (!NAME_RE.test(form.username)) {
    hints.username = { type: 'err', text: '3-20 位字母、数字或下划线' }
    return
  }
  try {
    const res = await checkUsername(form.username)
    const available = res.data === true || res.data?.available === true
    hints.username = available
      ? { type: 'ok', text: '用户名可用' }
      : { type: 'err', text: '该用户名已被占用' }
  } catch (e) {
    // 后端不可用时只做格式提示，不阻塞注册流程
    hints.username = { type: 'tip', text: '暂时无法校验，提交时会再检查' }
  }
}

function validate() {
  if (!NAME_RE.test(form.username)) return '用户名需为 3-20 位字母、数字或下划线'
  if (!EMAIL_RE.test(form.email)) return '邮箱格式不正确'
  if (form.password.length < 6) return '密码至少 6 位'
  if (form.password !== form.confirm) return '两次输入的密码不一致'
  return ''
}

async function handleRegister() {
  const msg = validate()
  alert.text = msg
  alert.type = 'err'
  if (msg) return

  loading.value = true
  alert.text = ''
  try {
    await register({
      username: form.username,
      nickname: form.nickname || form.username,
      email: form.email,
      password: form.password
    })
    alert.type = 'ok'
    alert.text = '注册成功，正在跳转到登录页…'
    showToast('注册成功')
    setTimeout(() => {
      router.push({ name: 'login', query: { username: form.username } })
    }, 900)
  } catch (e) {
    alert.type = 'err'
    alert.text = e.message || '注册失败，请稍后重试'
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
.auth-title { margin-bottom: 6px; }
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

.submit { width: 100%; padding: 13px; letter-spacing: 8px; font-size: 16px; margin-top: 6px; }
.submit:disabled { letter-spacing: 2px; }
</style>
