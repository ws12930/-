<template>
  <div class="home">
    <h1 class="grad-text title">选择你的阵营</h1>
    <p class="hint">世界正在裂缝两端注视着你。</p>

    <div class="factions">
      <button class="faction ninja" @click="choose('ninja')">
        <img :src="narutoImg" alt="鸣人" />
        <div class="info">
          <h2>木叶忍者</h2>
          <p>查克拉 · 意志之火 · 永不言弃</p>
        </div>
      </button>

      <button class="faction shinigami" @click="choose('shinigami')">
        <img :src="ichigoImg" alt="一护" />
        <div class="info">
          <h2>代理死神</h2>
          <p>灵压 · 斩魄刀 · 守护之心</p>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useSave } from '../composables/useSave'
import narutoImg from '../assets/naruto_cut.png'
import ichigoImg from '../assets/ichigo_cut.png'

const router = useRouter()
const { state } = useSave()

function choose(faction) {
  state.faction = faction
  router.push('/story')
}
</script>

<style scoped>
.home {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.title { font-size: clamp(30px, 5vw, 52px); letter-spacing: 8px; }
.hint { color: var(--c-dim); margin-bottom: 34px; letter-spacing: 2px; }

.factions {
  display: flex;
  gap: 46px;
  flex-wrap: wrap;
  justify-content: center;
  padding: 0 24px;
}

.faction {
  position: relative;
  width: 300px;
  border-radius: 14px;
  overflow: hidden;
  background: var(--c-panel);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: transform 0.25s, box-shadow 0.25s;
  padding-bottom: 18px;
}

.faction img {
  width: 100%;
  height: 340px;
  object-fit: cover;
  object-position: top;
}

.faction.ninja:hover { transform: translateY(-6px); box-shadow: 0 12px 40px rgba(255, 106, 0, 0.35); border-color: var(--c-ninja); }
.faction.shinigami:hover { transform: translateY(-6px); box-shadow: 0 12px 40px rgba(0, 128, 255, 0.35); border-color: var(--c-shinigami); }

.info { padding: 16px 18px 0; text-align: center; }
.info h2 { font-size: 22px; letter-spacing: 4px; margin-bottom: 8px; }
.ninja .info h2 { color: var(--c-ninja); }
.shinigami .info h2 { color: var(--c-shinigami); }
.info p { color: var(--c-dim); font-size: 13px; letter-spacing: 1px; }
</style>
