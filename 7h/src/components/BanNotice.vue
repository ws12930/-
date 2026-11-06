<template>
  <div
    v-if="banNotice.visible"
    class="bn-mask"
    role="alertdialog"
    aria-modal="true"
    aria-labelledby="ban-notice-title"
    @keydown.esc="dismissBan"
  >
    <div class="panel bn-dialog">
      <span class="bn-kicker">账号状态</span>
      <h3 id="ban-notice-title" class="bn-title grad-text">已自动退出登录</h3>
      <p class="bn-text">{{ banNotice.message }}</p>
      <p class="bn-hint">如需恢复使用，请使用其它未封禁的账号，或联系站点管理员解除封禁。</p>
      <div class="bn-actions">
        <button class="g-btn g-btn-sm" type="button" @click="dismissBan">我知道了</button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 封禁提示弹窗。
 *
 * 由 utils/session.js 的 banNotice 驱动：任何接口返回 40301 都会
 * 清掉本地登录态并把这个弹窗亮起来，App.vue 同时把路由送回登录页。
 * 单独做成组件（而不是塞进某个视图）是因为它要盖在任何页面上都能显示。
 */
import { onMounted, onBeforeUnmount } from 'vue'
import { banNotice, dismissBan } from '../utils/session'

function onKey(e) {
  if (e.key === 'Escape' && banNotice.visible) dismissBan()
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.bn-mask {
  position: fixed;
  inset: 0;
  /* 高于 ConfirmDialog(90) 与顶栏(30)，保证盖在页面最上层 */
  z-index: 120;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(5, 7, 10, 0.78);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  animation: bn-fade 0.2s ease-out;
}

.bn-dialog {
  position: relative;
  width: min(400px, 100%);
  padding: 24px 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-color: rgba(255, 138, 122, 0.35);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.05) inset,
    0 12px 40px rgba(0, 0, 0, 0.45);
  animation: bn-rise 0.22s ease-out;
}

/* 顶部一条暖红细线，和「出错 / 警告」语义呼应，不抢渐变标题 */
.bn-dialog::before {
  content: '';
  position: absolute;
  top: 0;
  left: 18px;
  right: 18px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 138, 122, 0.75), transparent);
}

.bn-kicker {
  font-size: 11px;
  letter-spacing: 3px;
  color: var(--c-dim);
}

.bn-title {
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 3px;
  margin-bottom: 2px;
}

.bn-text {
  font-size: 14px;
  line-height: 1.75;
  color: var(--c-text);
  word-break: break-word;
}

.bn-hint {
  font-size: 11px;
  line-height: 1.65;
  color: var(--c-dim);
  padding: 8px 10px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.04);
  border-left: 2px solid rgba(255, 138, 122, 0.45);
}

.bn-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

@keyframes bn-fade {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes bn-rise {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .bn-mask,
  .bn-dialog {
    animation: none;
  }
}
</style>
