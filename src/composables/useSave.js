import { reactive, watch } from 'vue'

const STORAGE_KEY = 'bn-galgame-save'

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch (e) { /* corrupted save, start fresh */ }
  return {
    playerName: '',
    faction: null,          // 'shinigami' | 'ninja'
    chapterIndex: 0,
    lineIndex: 0,
    unlockedCG: [],
    unlockedEndings: [],
    updatedAt: null
  }
}

const state = reactive(load())

watch(
  state,
  () => {
    state.updatedAt = new Date().toISOString()
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  },
  { deep: true }
)

export function useSave() {
  function newGame(name, faction) {
    state.playerName = name || '无名者'
    state.faction = faction
    state.chapterIndex = 0
    state.lineIndex = 0
    state.unlockedCG = []
    state.unlockedEndings = []
  }

  function unlockCG(id) {
    if (!state.unlockedCG.includes(id)) state.unlockedCG.push(id)
  }

  function unlockEnding(id) {
    if (!state.unlockedEndings.includes(id)) state.unlockedEndings.push(id)
  }

  function hasSave() {
    return !!state.playerName && state.faction !== null
  }

  function summary() {
    if (!hasSave()) return null
    return {
      name: state.playerName,
      faction: state.faction,
      chapterIndex: state.chapterIndex,
      lineIndex: state.lineIndex,
      updatedAt: state.updatedAt,
      cgCount: state.unlockedCG.length,
      endingCount: state.unlockedEndings.length
    }
  }

  return { state, newGame, unlockCG, unlockEnding, hasSave, summary }
}
