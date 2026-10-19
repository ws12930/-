<template>
  <span
    class="uavatar"
    :class="[factionClass, { ringed: ring }]"
    :style="avatarStyle"
    :title="displayName"
  >
    <img v-if="resolved.image" :src="resolved.image" :alt="name || '头像'" />
    <span v-else class="glyph">{{ glyph }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { resolveAvatar, normalizeFaction } from '../assets/avatars'

const props = defineProps({
  /** 直接传用户对象（推荐）：{ avatar, nickname, username, faction } */
  user: { type: Object, default: null },
  avatar: { type: String, default: '' },
  name: { type: String, default: '' },
  faction: { type: String, default: '' },
  size: { type: [Number, String], default: 36 },
  ring: { type: Boolean, default: false }
})

/* user 对象优先，同时兼容单独传 avatar / name / faction 的旧写法 */
const avatarSrc = computed(() => props.avatar || props.user?.avatar || '')
const displayName = computed(
  () => props.name || props.user?.nickname || props.user?.username || ''
)
const userFaction = computed(() => props.faction || props.user?.faction || 'ninja')

const factionClass = computed(() => normalizeFaction(userFaction.value))
const resolved = computed(() => resolveAvatar(avatarSrc.value, userFaction.value))

const glyph = computed(() => {
  if (resolved.value.glyph) return resolved.value.glyph
  const text = (displayName.value || '?').trim()
  return text.slice(0, 1).toUpperCase()
})

const avatarStyle = computed(() => {
  const px = typeof props.size === 'number' ? `${props.size}px` : props.size
  const base = { width: px, height: px }
  if (resolved.value.bg) {
    base.background = resolved.value.bg
  }
  base.fontSize = `calc(${px} * 0.46)`
  return base
})
</script>

<style scoped>
.uavatar {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  color: #0b0e11;
  font-family: var(--font-display);
  font-weight: 700;
  line-height: 1;
  user-select: none;
}

.uavatar.ninja { box-shadow: 0 0 12px rgba(150, 182, 168, 0.3); }
.uavatar.shinigami { box-shadow: 0 0 12px rgba(188, 205, 197, 0.3); }
.uavatar.ringed { border: 2px solid rgba(255, 255, 255, 0.55); }

.uavatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.glyph {
  display: block;
  transform: translateY(1px);
}
</style>
