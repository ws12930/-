<template>
  <div class="app-shell">
    <BgCarousel />
    <div class="site-grid" aria-hidden="true"></div>
    <CursorGlow />
    <Leaves />
    <router-view />
    <!-- 哪些页面显示桌宠由 mascot/config.js 的可见路由表统一决定：
         后台管理页的浮层会压住行内「置顶 / 删除」按钮；
         登录 / 注册 / 找回密码页左下角已有大幅立绘，再叠一个会说话的桌宠就是「两个青禾」。
         集中成一张表，以后加页面不用再来这里补一个 v-if。 -->
    <Mascot v-if="showMascot" />
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
import { mascotVisibleOn } from './mascot/config'

const route = useRoute()
const router = useRouter()
const showMascot = computed(() => mascotVisibleOn(route.name))

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
  z-index: var(--z-base);
}
</style>
