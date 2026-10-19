<template>
  <button
    class="back-btn"
    :class="[`is-${variant}`, { 'is-fixed': fixed }]"
    type="button"
    :aria-label="label || '返回'"
    @click="go"
  >
    <svg class="bb-arrow" viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M14.5 5.5 L8 12 L14.5 18.5"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <span v-if="label" class="bb-text">{{ label }}</span>
  </button>
</template>

<script setup>
import { useRouter } from 'vue-router'

/**
 * 全站统一返回键。
 *
 * 视觉由 main.css 的 .back-btn 系列提供：胶囊 + 左箭头 + 玻璃底，
 * hover 时箭头左移、描边点亮。任何页面都不要再自己写返回按钮。
 *
 * props:
 *   label    按钮文字，默认「返回」；传 '' 则只显示箭头圆形按钮
 *   to       指定目标路由名（走 router.push），优先级最高
 *   fallback 没有历史记录时的兜底路由，默认 'home'
 *   variant  'solid'（默认胶囊） | 'text'（表单内的轻量文字链）
 *   fixed    是否固定在视口左上角（二级页统一放置点）
 */
const props = defineProps({
  label: { type: String, default: '返回' },
  to: { type: String, default: '' },
  fallback: { type: String, default: 'home' },
  variant: { type: String, default: 'solid' },
  fixed: { type: Boolean, default: false }
})

const router = useRouter()

function go() {
  if (props.to) {
    router.push({ name: props.to })
    return
  }
  // vue-router 把上一页信息存在 history.state.back，为空表示没有站内历史
  if (window.history.state && window.history.state.back) {
    router.back()
    return
  }
  router.push({ name: props.fallback })
}
</script>
