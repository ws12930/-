<template>
  <nav class="pg-nav">
    <button
      v-for="item in items"
      :key="item.label"
      class="auth-link"
      :class="{ danger: item.danger }"
      @click="go(item)"
    >
      {{ item.label }}
    </button>
  </nav>
</template>

<script setup>
import { useRouter } from 'vue-router'

/**
 * 二级页统一导航条。
 * 样式由 main.css 的 .pg-nav 提供（固定右上角），本组件只负责渲染条目与跳转。
 *
 * items 元素：
 *   { label: '返回首页', to: 'home' }        —— to 为路由名，走 router.push
 *   { label: '退出登录', onClick: fn }        —— 自定义行为优先于 to
 *   { label: '删除', danger: true }           —— 红色强调
 */
const props = defineProps({
  items: { type: Array, default: () => [] }
})

const router = useRouter()

function go(item) {
  if (typeof item.onClick === 'function') {
    item.onClick()
    return
  }
  if (item.to) router.push({ name: item.to })
}
</script>
