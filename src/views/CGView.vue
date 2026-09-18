<template>
  <div class="cg-view">
    <header class="topbar">
      <h1 class="grad-text">CG 回放</h1>
      <button class="mini-btn" @click="$router.push('/')">返回标题</button>
    </header>

    <div class="grid">
      <div
        v-for="cg in CG_LIST"
        :key="cg.id"
        class="card panel"
        :class="{ locked: !unlocked(cg.id) }"
        @click="openCG(cg)"
      >
        <img v-if="unlocked(cg.id)" :src="cg.img" :alt="cg.title" />
        <div v-else class="locked-face">
          <span class="lock">🔒</span>
          <p>尚未解锁</p>
        </div>
        <div class="meta" v-if="unlocked(cg.id)">
          <h3>{{ cg.title }}</h3>
          <p>{{ cg.desc }}</p>
        </div>
      </div>
    </div>

    <div v-if="viewer" class="viewer" @click.self="viewer = null">
      <img :src="viewer.img" :alt="viewer.title" />
      <p class="viewer-title grad-text">{{ viewer.title }}</p>
      <p class="viewer-desc">{{ viewer.desc }}</p>
      <button class="mini-btn" @click="viewer = null">关闭</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { CG_LIST } from '../data/story'
import { useSave } from '../composables/useSave'

const { state, unlockCG } = useSave()
const viewer = ref(null)

function unlocked(id) {
  return state.unlockedCG.includes(id)
}

function openCG(cg) {
  if (!unlocked(cg.id)) return
  viewer.value = cg
}
</script>

<style scoped>
.cg-view {
  height: 100%;
  overflow-y: auto;
  padding: 24px 32px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.topbar h1 { font-size: 30px; letter-spacing: 6px; }

.mini-btn {
  padding: 8px 18px;
  font-size: 13px;
  color: var(--c-dim);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}
.mini-btn:hover { color: var(--c-text); border-color: var(--c-ninja); }

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 22px;
}

.card {
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.card:not(.locked):hover { transform: translateY(-4px); box-shadow: 0 10px 30px rgba(0, 128, 255, 0.25); }

.card img { width: 100%; height: 300px; object-fit: cover; object-position: top; }

.card.locked { cursor: default; opacity: 0.6; }

.locked-face {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--c-dim);
  gap: 8px;
}
.lock { font-size: 34px; }

.meta { padding: 14px 16px; }
.meta h3 { letter-spacing: 2px; margin-bottom: 6px; }
.meta p { color: var(--c-dim); font-size: 13px; line-height: 1.6; }

.viewer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 10;
}
.viewer img { max-height: 70vh; max-width: 90vw; border-radius: 8px; }
.viewer-title { font-size: 24px; letter-spacing: 4px; }
.viewer-desc { color: var(--c-dim); margin-bottom: 8px; }
</style>
