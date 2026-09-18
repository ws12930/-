<template>
  <div class="story" :class="factionClass">
    <!-- 顶部状态栏 -->
    <header class="topbar">
      <span class="chapter grad-text">{{ chapter?.title }}</span>
      <div class="actions">
        <button class="mini-btn" @click="autoPlay = !autoPlay">{{ autoPlay ? '自动：开' : '自动：关' }}</button>
        <button class="mini-btn" @click="skipAll">跳过章节</button>
        <button class="mini-btn" @click="saveNow">保存进度</button>
        <button class="mini-btn" @click="$router.push('/')">标题</button>
      </div>
    </header>

    <!-- 对话区 -->
    <div class="dialog-area" @click="advance">
      <template v-if="current?.speaker === 'choice'">
        <div class="choice-box panel" @click.stop>
          <p class="choice-prompt grad-text">{{ current.prompt }}</p>
          <button
            v-for="(opt, i) in current.options"
            :key="i"
            class="g-btn choice-btn"
            @click="pickBranch(opt.goto)"
          >{{ opt.label }}</button>
        </div>
      </template>

      <template v-else>
        <div class="speaker-name" :class="current?.speaker">
          {{ speakerLabel }}
        </div>
        <div class="dialog-box panel">
          <p class="dialog-text">{{ displayedText }}<span class="caret" v-if="typing">▌</span></p>
          <p class="click-hint">{{ typing ? '点击加速' : '点击继续 ▾' }}</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { CHAPTERS, findChapter } from '../data/story'
import { useSave } from '../composables/useSave'

const router = useRouter()
const { state, unlockEnding, unlockCG } = useSave()

const chapter = ref(null)
const lineIdx = ref(0)
const displayedText = ref('')
const typing = ref(false)
const autoPlay = ref(false)

let typeTimer = null
let autoTimer = null

const current = computed(() => chapter.value?.lines[lineIdx.value])

const speakerLabel = computed(() => {
  switch (current.value?.speaker) {
    case 'narration': return '—— 旁白 ——'
    case 'naruto': return '漩涡鸣人'
    case 'ichigo': return '黑崎一护'
    default: return ''
  }
})

const factionClass = computed(() =>
  state.faction === 'ninja' ? 'theme-ninja' : state.faction === 'shinigami' ? 'theme-shinigami' : ''
)

function showLine() {
  const line = current.value
  if (!line || line.speaker === 'choice') return
  // 对应角色登场时解锁 CG
  if (line.speaker === 'naruto') unlockCG('cg-naruto')
  if (line.speaker === 'ichigo') unlockCG('cg-ichigo')
  const text = line.text
  displayedText.value = ''
  typing.value = true
  clearInterval(typeTimer)
  let i = 0
  typeTimer = setInterval(() => {
    i++
    displayedText.value = text.slice(0, i)
    if (i >= text.length) {
      clearInterval(typeTimer)
      typing.value = false
      if (autoPlay.value) scheduleAuto()
    }
  }, 34)
}

function scheduleAuto() {
  clearTimeout(autoTimer)
  autoTimer = setTimeout(advance, 1400)
}

function advance() {
  if (!current.value || current.value.speaker === 'choice') return
  if (typing.value) {
    // 点击加速：直接显示整句
    clearInterval(typeTimer)
    displayedText.value = current.value.text
    typing.value = false
    if (autoPlay.value) scheduleAuto()
    return
  }
  next()
}

function next() {
  if (lineIdx.value < chapter.value.lines.length - 1) {
    lineIdx.value++
    state.chapterIndex = CHAPTERS.indexOf(chapter.value)
    state.lineIndex = lineIdx.value
    showLine()
  } else {
    // 章节结束
    const last = current.value?.text || ''
    if (last.includes('【结局')) {
      const m = last.match(/【结局[:：](.+?)】/)
      if (m) unlockEndingFromText(m[1])
    }
    router.push('/endings')
  }
}

function pickBranch(goto) {
  const target = findChapter(goto)
  if (target) {
    chapter.value = target
    lineIdx.value = 0
    state.chapterIndex = CHAPTERS.indexOf(target)
    state.lineIndex = 0
    showLine()
  }
}

function skipAll() {
  // 跳过：直接解锁本章结局并跳转
  const texts = chapter.value.lines.map((l) => l.text || '').join(' ')
  const m = texts.match(/【结局[:：](.+?)】/)
  if (m) unlockEndingFromText(m[1])
  router.push('/endings')
}

function saveNow() {
  state.chapterIndex = CHAPTERS.indexOf(chapter.value)
  state.lineIndex = lineIdx.value
  state.updatedAt = new Date().toISOString()
}

function unlockEndingFromText(title) {
  const map = { '羁绊初成': 'ending-1', '不打不相识': 'ending-2' }
  const id = map[title]
  if (id) unlockEnding(id)
}

onMounted(() => {
  // 从所选阵营对应的存档位置恢复，或从第一章开始
  if (!state.faction) {
    router.replace('/home')
    return
  }
  chapter.value = CHAPTERS[state.chapterIndex] || CHAPTERS[0]
  lineIdx.value = 0
  showLine()
})

onBeforeUnmount(() => {
  clearInterval(typeTimer)
  clearTimeout(autoTimer)
})

watch(current, (v) => {
  if (v && v.speaker === 'choice') {
    clearInterval(typeTimer)
    typing.value = false
  }
})
</script>

<style scoped>
.story {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 18px 26px;
}

.story.theme-ninja .dialog-box { border-left: 3px solid var(--c-ninja); }
.story.theme-shinigami .dialog-box { border-left: 3px solid var(--c-shinigami); }

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chapter { font-size: 20px; letter-spacing: 3px; }

.actions { display: flex; gap: 10px; }

.mini-btn {
  padding: 7px 14px;
  font-size: 13px;
  color: var(--c-dim);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  transition: color 0.2s, border-color 0.2s;
}
.mini-btn:hover { color: var(--c-text); border-color: var(--c-ninja); }

.dialog-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding-bottom: 30px;
}

.speaker-name {
  align-self: flex-start;
  margin-left: 12px;
  margin-bottom: -10px;
  padding: 6px 22px;
  font-size: 16px;
  letter-spacing: 3px;
  z-index: 2;
  border-radius: 6px 6px 0 0;
  background: var(--c-panel);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-bottom: none;
}
.speaker-name.naruto { color: var(--c-ninja); }
.speaker-name.ichigo { color: var(--c-shinigami); }
.speaker-name.narration { color: var(--c-dim); }

.dialog-box {
  padding: 26px 32px 40px;
  min-height: 150px;
}

.dialog-text {
  font-size: 19px;
  line-height: 1.9;
  letter-spacing: 1px;
  min-height: 70px;
}

.caret { animation: blink 0.8s steps(1) infinite; color: var(--c-ninja); }
@keyframes blink { 50% { opacity: 0; } }

.click-hint {
  position: absolute;
  right: 40px;
  bottom: 14px;
  font-size: 12px;
  color: var(--c-dim);
}

.dialog-box { position: relative; }

.choice-box {
  padding: 34px 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.choice-prompt { font-size: 20px; margin-bottom: 8px; }

.choice-btn { min-width: 320px; }
</style>
