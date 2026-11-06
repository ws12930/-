<template>
  <div class="app-shell">
    <BgCarousel />
    <div class="site-grid" aria-hidden="true"></div>
    <CursorGlow />
    <Leaves />
    <router-view />
    <!-- 后台管理是密集列表页，看板娘浮层（fixed 右下角 + 气泡）正好压在行内
         「置顶 / 删除」按钮上，因此后台路由不渲染；其余页面保持原样。 -->
    <Mascot v-if="!isConsole" />
    <!-- 账号被封禁的全局提示：任何页面、任何接口命中 40301 都会弹 -->
    <BanNotice />
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BgCarousel from './components/BgCarousel.vue'
import CursorGlow from './components/CursorGlow.vue'
import Leaves from './components/Leaves.vue'
import Mascot from './components/Mascot.vue'
import BanNotice from './components/BanNotice.vue'
import { banNotice, startBanWatch } from './utils/session'

const route = useRoute()
const router = useRouter()
const isConsole = computed(() => route.name === 'admin')

// 被封禁时本地登录态已被清掉，这里顺手把页面送回登录页；
// 弹窗是 fixed 的，不随路由切换消失，用户点了「我知道了」才关闭。
watch(
  () => banNotice.visible,
  (visible) => {
    if (!visible) return
    if (route.name !== 'login') router.replace({ name: 'login' })
  }
)

// 巡检：管理员在后台封禁后，这边最多 15 秒自动登出，不用等用户操作
onMounted(startBanWatch)
</script>

<style scoped>
.app-shell {
  height: 100%;
  position: relative;
  z-index: 1;
}
</style>
