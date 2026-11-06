<template>
  <div v-if="pages > 1" class="pg">
    <button
      class="mini-btn"
      type="button"
      :disabled="disabled || current <= 1"
      @click="go(current - 1)"
    >
      ‹ 上一页
    </button>

    <div class="pg-nums">
      <template v-for="(item, i) in pageList" :key="item + '-' + i">
        <span v-if="item === '...'" class="pg-gap">…</span>
        <button
          v-else
          class="mini-btn pg-num"
          type="button"
          :class="{ active: item === current }"
          :disabled="disabled || item === current"
          @click="go(item)"
        >
          {{ item }}
        </button>
      </template>
    </div>

    <button
      class="mini-btn"
      type="button"
      :disabled="disabled || current >= pages"
      @click="go(current + 1)"
    >
      下一页 ›
    </button>

    <span v-if="total > 0" class="pg-total">共 {{ total }} 条</span>
  </div>
</template>

<script setup>
/**
 * 通用分页条。
 * 页码只渲染「当前页附近」的窗口，避免几十页时把工具栏撑爆。
 */
import { computed } from 'vue'

const props = defineProps({
  current: { type: Number, default: 1 },
  pages: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['go'])

const WINDOW = 2

const pageList = computed(() => {
  const pages = Math.max(1, props.pages || 1)
  const cur = Math.min(Math.max(1, props.current || 1), pages)
  if (pages <= 7) {
    return Array.from({ length: pages }, (_, i) => i + 1)
  }
  const list = [1]
  const from = Math.max(2, cur - WINDOW)
  const to = Math.min(pages - 1, cur + WINDOW)
  if (from > 2) list.push('...')
  for (let p = from; p <= to; p++) list.push(p)
  if (to < pages - 1) list.push('...')
  list.push(pages)
  return list
})

function go(page) {
  const target = Math.min(Math.max(1, page), Math.max(1, props.pages))
  if (props.disabled || target === props.current) return
  emit('go', target)
}
</script>

<style scoped>
.pg {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 14px 0 4px;
}

.pg-nums {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pg-num {
  min-width: 28px;
  padding: 5px 8px;
}

.pg-num.active {
  color: var(--c-acid);
  border-color: rgba(217, 255, 77, 0.45);
  background: rgba(217, 255, 77, 0.08);
}

.pg-gap {
  padding: 0 2px;
  font-size: 12px;
  color: var(--c-dim);
}

.pg-total {
  margin-left: 4px;
  font-size: 11px;
  color: var(--c-dim);
}
</style>
