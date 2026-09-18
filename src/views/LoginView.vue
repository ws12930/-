<template>
  <div class="login">
    <div class="login-bg left"></div>
    <div class="login-bg right"></div>

    <img class="cut naruto" :src="narutoImg" alt="鸣人" />
    <img class="cut ichigo" :src="ichigoImg" alt="一护" />

    <div class="login-center">
      <h1 class="grad-text title">BLEACH × NARUTO</h1>
      <p class="subtitle grad-text">死 神 对 火 影</p>
      <p class="tagline">当灵压遇上查克拉，两个世界只需一个选择。</p>

      <div class="menu">
        <button class="g-btn" @click="showNew = true">开始新游戏</button>
        <button class="g-btn" :disabled="!hasSave()" @click="continueGame">继续游戏</button>
        <button class="g-btn" @click="$router.push('/cg')">CG 回放</button>
        <button class="g-btn" @click="$router.push('/endings')">结局回顾</button>
      </div>

      <p v-if="saveInfo" class="save-hint">
        已有存档：{{ saveInfo.name }} ·
        {{ saveInfo.faction === 'ninja' ? '忍者阵营' : '死神阵营' }} ·
        CG {{ saveInfo.cgCount }}/2 · 结局 {{ saveInfo.endingCount }}/2
      </p>
    </div>

    <!-- 新游戏输入 -->
    <div v-if="showNew" class="modal" @click.self="showNew = false">
      <div class="panel modal-box">
        <h2 class="grad-text">输入你的名字</h2>
        <input v-model="name" class="name-input" maxlength="12" placeholder="无名者" @keyup.enter="startGame" />
        <p class="modal-hint">之后你将选择阵营：木叶忍者 或 代理死神。</p>
        <button class="g-btn" @click="startGame">决 定</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSave } from '../composables/useSave'
import narutoImg from '../assets/naruto_cut.png'
import ichigoImg from '../assets/ichigo_cut.png'

const router = useRouter()
const { newGame, hasSave, summary, state } = useSave()

const showNew = ref(false)
const name = ref('')
const saveInfo = computed(() => summary())

function startGame() {
  newGame(name.value.trim(), null)
  showNew.value = false
  router.push('/home')
}

function continueGame() {
  router.push('/story')
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
.login-bg.left  { background: radial-gradient(ellipse at 15% 40%, rgba(217, 84, 0, 0.35), transparent 55%); }
.login-bg.right { background: radial-gradient(ellipse at 85% 40%, rgba(0, 78, 146, 0.45), transparent 55%); }

.cut {
  position: absolute;
  bottom: 0;
  height: 78%;
  max-height: 640px;
  filter: drop-shadow(0 0 30px rgba(0, 0, 0, 0.8));
  pointer-events: none;
  animation: float 5s ease-in-out infinite;
}
.cut.naruto { left: 4%; }
.cut.ichigo { right: 4%; animation-delay: 2.5s; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

.login-center {
  position: relative;
  z-index: 2;
  text-align: center;
}

.title {
  font-family: var(--font-display);
  font-size: clamp(38px, 7vw, 76px);
  letter-spacing: 6px;
}

.subtitle {
  font-size: clamp(16px, 2.4vw, 26px);
  letter-spacing: 14px;
  margin-top: 6px;
}

.tagline {
  color: var(--c-dim);
  margin: 18px 0 40px;
  letter-spacing: 2px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: center;
}

.save-hint {
  margin-top: 26px;
  font-size: 13px;
  color: var(--c-dim);
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.modal-box {
  padding: 36px 44px;
  text-align: center;
  width: min(420px, 90vw);
}

.modal-box h2 { margin-bottom: 20px; }

.name-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 16px;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  outline: none;
  text-align: center;
  letter-spacing: 2px;
  margin-bottom: 14px;
}
.name-input:focus { border-color: var(--c-ninja); }

.modal-hint { color: var(--c-dim); font-size: 13px; margin-bottom: 22px; }

@media (max-width: 860px) {
  .cut { opacity: 0.35; height: 60%; }
}
</style>
