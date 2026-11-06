<template>
  <div class="rp-mask" @click.self="$emit('close')">
    <div class="panel rp-dialog" role="dialog" aria-modal="true">
      <h3 class="rp-title grad-text">举 报</h3>
      <p class="rp-target">{{ target?.label || '这条内容' }}</p>

      <div class="rp-reasons">
        <button
          v-for="r in REASONS"
          :key="r"
          class="mini-btn rp-reason"
          type="button"
          :class="{ active: reason === r }"
          @click="reason = r"
        >
          {{ r }}
        </button>
      </div>

      <textarea
        v-model.trim="detail"
        class="rp-input"
        rows="3"
        maxlength="200"
        placeholder="补充说明（选填），比如具体哪句话有问题"
      ></textarea>

      <p class="rp-note">同一内容只能举报一次，管理员处理后会在通知里回复你。</p>
      <p v-if="error" class="rp-error">{{ error }}</p>

      <div class="rp-actions">
        <button class="g-btn g-btn-sm" type="button" :disabled="busy" @click="submit">
          {{ busy ? '提交中…' : '提 交' }}
        </button>
        <button class="g-btn g-btn-sm rp-cancel" type="button" :disabled="busy" @click="$emit('close')">
          取 消
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 举报弹窗：文章 / 评论 / 用户共用。
 * 理由与后台下拉、blog_report.reason 的取值保持一致。
 */
import { ref } from 'vue'
import { submitReport } from '../api/community'

const props = defineProps({
  /** { type: 'POST' | 'COMMENT' | 'USER', id: Number, label: String } */
  target: { type: Object, required: true }
})

const emit = defineEmits(['close', 'done'])

const REASONS = ['广告垃圾', '辱骂攻击', '违法违规', '其他']

const reason = ref(REASONS[0])
const detail = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  if (busy.value) return
  error.value = ''
  busy.value = true
  try {
    await submitReport({
      targetType: props.target.type,
      targetId: props.target.id,
      reason: reason.value,
      detail: detail.value
    })
    emit('done')
  } catch (e) {
    error.value = e.message || '提交失败，请稍后重试'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.rp-mask {
  position: fixed;
  inset: 0;
  z-index: 88;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(5, 7, 10, 0.72);
  backdrop-filter: blur(4px);
}

.rp-dialog {
  width: min(440px, 100%);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rp-title {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
}

.rp-target {
  font-size: 12px;
  color: var(--c-dim);
  word-break: break-word;
}

.rp-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rp-reason.active {
  color: var(--c-acid);
  border-color: rgba(217, 255, 77, 0.45);
  background: rgba(217, 255, 77, 0.08);
}

.rp-input {
  width: 100%;
  padding: 9px 11px;
  font-size: 13px;
  font-family: inherit;
  line-height: 1.6;
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 4px;
  outline: none;
  resize: vertical;
}

.rp-input:focus { border-color: rgba(143, 208, 174, 0.55); }

.rp-note {
  font-size: 11px;
  color: var(--c-dim);
  line-height: 1.6;
}

.rp-error {
  font-size: 12px;
  color: #ffb4a8;
}

.rp-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.rp-cancel { color: var(--c-dim); }
</style>
