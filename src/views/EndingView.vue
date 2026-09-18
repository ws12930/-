<template>
  <div class="ending-view">
    <header class="topbar">
      <h1 class="grad-text">结局回顾</h1>
      <button class="mini-btn" @click="$router.push('/')">返回标题</button>
    </header>

    <div class="list">
      <div
        v-for="e in ENDINGS"
        :key="e.id"
        class="ending panel"
        :class="{ locked: !unlocked(e.id), 'grad-text': unlocked(e.id) }"
      >
        <template v-if="unlocked(e.id)">
          <h2>◆ {{ e.title }}</h2>
          <p>{{ e.desc }}</p>
        </template>
        <template v-else>
          <h2>？？？</h2>
          <p>尚未达成的结局……</p>
        </template>
      </div>
    </div>

    <div class="stats">
      已解锁结局 {{ state.unlockedEndings.length }} / {{ ENDINGS.length }}
      · 已解锁 CG {{ state.unlockedCG.length }} / {{ CG_LIST.length }}
    </div>
  </div>
</template>

<script setup>
import { ENDINGS, CG_LIST } from '../data/story'
import { useSave } from '../composables/useSave'

const { state } = useSave()

function unlocked(id) {
  return state.unlockedEndings.includes(id)
}
</script>

<style scoped>
.ending-view {
  height: 100%;
  overflow-y: auto;
  padding: 24px 32px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 26px;
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

.list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 720px;
  margin: 0 auto;
}

.ending { padding: 24px 30px; }
.ending h2 { letter-spacing: 3px; margin-bottom: 10px; font-size: 21px; }
.ending p { color: var(--c-dim); line-height: 1.7; }
.ending.locked { opacity: 0.5; }

.stats {
  margin-top: 30px;
  text-align: center;
  color: var(--c-dim);
  font-size: 14px;
  letter-spacing: 1px;
}
</style>
