<template>
  <div v-if="state" class="cd-mask" @click.self="$emit('close')" @keydown.esc="$emit('close')">
    <div class="panel cd-dialog" role="dialog" aria-modal="true">
      <h3 class="cd-title grad-text">{{ state.title }}</h3>
      <p v-if="state.message" class="cd-message">{{ state.message }}</p>
      <p v-if="state.detail" class="cd-detail">{{ state.detail }}</p>
      <div class="cd-actions">
        <button class="g-btn g-btn-sm" type="button" :class="{ danger: state.danger }" @click="$emit('confirm')">
          {{ state.confirmText }}
        </button>
        <button class="g-btn g-btn-sm cd-cancel" type="button" @click="$emit('close')">
          {{ state.cancelText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 全站统一的二次确认弹窗。
 * 状态由 composables/useConfirm 提供，本组件只负责渲染与抛事件，
 * 这样父组件不用自己维护「弹窗开着没 / 点了哪个按钮」。
 */
import { onMounted, onBeforeUnmount } from 'vue'

defineProps({
  /** { title, message, detail, confirmText, cancelText, danger } */
  state: { type: Object, default: null }
})

const emit = defineEmits(['confirm', 'close'])

function onKey(e) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.cd-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(5, 7, 10, 0.72);
  backdrop-filter: blur(4px);
}

.cd-dialog {
  width: min(400px, 100%);
  padding: 22px 22px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cd-title {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
  margin-bottom: 2px;
}

.cd-message {
  font-size: 13px;
  line-height: 1.7;
  color: var(--c-text);
  word-break: break-word;
}

.cd-detail {
  font-size: 11px;
  line-height: 1.6;
  color: var(--c-dim);
  padding: 8px 10px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.04);
  border-left: 2px solid rgba(143, 208, 174, 0.4);
  word-break: break-word;
}

.cd-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 4px;
}

.cd-cancel { color: var(--c-dim); }

.cd-actions .danger {
  color: #ffb4a8;
  background:
    linear-gradient(var(--c-bg), var(--c-bg)) padding-box,
    linear-gradient(90deg, #ff8a7a, #ffb4a8) border-box;
}
</style>
