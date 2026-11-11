<template>
  <Teleport to="body">
    <Transition name="gs">
      <div
        v-if="open"
        class="gs-mask"
        role="dialog"
        aria-modal="true"
        aria-label="七日回响 · Seven Days Echo"
        @click.self="close"
      >
        <div ref="panelEl" class="gs-panel">
          <header class="gs-bar">
            <div class="gs-brand">
              <span class="gs-cn grad-text">七日回响</span>
              <span class="gs-en">SEVEN DAYS ECHO</span>
            </div>
            <div class="gs-acts">
              <button class="gs-btn" type="button" @click="toggleFull">
                {{ isFull ? '还原' : '全屏' }}
              </button>
              <button class="gs-btn" type="button" @click="openTab">新标签页</button>
              <button class="gs-btn gs-x" type="button" title="关闭（Esc）" aria-label="关闭" @click="close">
                ✕
              </button>
            </div>
          </header>

          <div class="gs-frame">
            <!-- 跨源站点，父页面拿不到内部状态，只能靠 load 事件收掉加载层 -->
            <iframe
              v-if="mounted"
              class="gs-iframe"
              :src="src"
              title="七日回响 · Seven Days Echo"
              allow="autoplay; fullscreen; clipboard-write"
              allowfullscreen
              @load="onFrameLoad"
            ></iframe>

            <Transition name="gs-fade">
              <div v-if="!ready" class="gs-loading">
                <span class="gs-ring"></span>
                <p>正在打开七日回响…</p>
                <p v-if="!slow" class="gs-loading-tip">第一次加载要拉整包的剧情和素材，稍等一下</p>
                <button v-else class="gs-btn gs-slow-btn" type="button" @click="openTab">
                  有点慢？在新标签页打开
                </button>
              </div>
            </Transition>
          </div>

          <footer class="gs-foot">
            <span>纯前端 Galgame · 存档保存在游戏自己的本地存储</span>
            <span>关掉窗口会重新加载，进度记得先在游戏里「存档」</span>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
/**
 * 站内游戏窗口：把「七日回响」以 iframe 的形式嵌进博客里，
 * 从看板娘右键菜单进入，关掉就退回博客，不用跳走。
 *
 * 几个刻意为之的点：
 * - iframe 首次打开才挂载，博客首屏不会为一次跨站请求买单；
 * - 每次关闭会销毁 iframe，游戏音效 / BGM 不会在后台接着响，
 *   代价是重新打开要回标题画面，所以底部明写了「先存档」的提示；
 * - 面板用 v-if 挂在自己身上，全屏时直接把 panel 送进 fullscreen，
 *   不用另做一套全屏布局。
 */
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  src: { type: String, default: 'https://seven-days-echo.app.workbuddy.host/' }
})

const emit = defineEmits(['close'])

const panelEl = ref(null)
/** iframe 是否已挂过（首次打开后不再卸，但随面板一起销毁） */
const mounted = ref(false)
/** 首帧是否加载完 */
const ready = ref(false)
/** 迟迟没加载完：把「新标签页」这条路推到眼前，别让人对着转圈干等 */
const slow = ref(false)
const isFull = ref(false)

let slowTimer = null

function onFrameLoad() {
  ready.value = true
  slow.value = false
  clearTimeout(slowTimer)
}

function close() {
  emit('close')
}

function openTab() {
  window.open(props.src, '_blank', 'noopener,noreferrer')
}

async function toggleFull() {
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await panelEl.value?.requestFullscreen?.()
  } catch {
    /* 某些环境（iframe 嵌套 / 权限策略）禁用全屏，静默降级即可 */
  }
}

function onFsChange() {
  isFull.value = !!document.fullscreenElement
}

function onKey(e) {
  if (e.key === 'Escape') close()
}

watch(
  () => props.open,
  (v) => {
    if (v) {
      ready.value = false
      slow.value = false
      mounted.value = true
      clearTimeout(slowTimer)
      slowTimer = setTimeout(() => {
        if (!ready.value) slow.value = true
      }, 12000)
      window.addEventListener('keydown', onKey)
    } else {
      clearTimeout(slowTimer)
      window.removeEventListener('keydown', onKey)
      if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {})
    }
  }
)

onMounted(() => document.addEventListener('fullscreenchange', onFsChange))

onBeforeUnmount(() => {
  clearTimeout(slowTimer)
  window.removeEventListener('keydown', onKey)
  document.removeEventListener('fullscreenchange', onFsChange)
})
</script>

<style scoped>
.gs-mask {
  position: fixed;
  inset: 0;
  z-index: 95;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: radial-gradient(circle at 50% 26%, rgba(22, 30, 26, 0.66), rgba(4, 6, 9, 0.88));
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.gs-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  width: min(1120px, 100%);
  height: min(780px, 100%);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0) 26%),
    rgba(11, 15, 18, 0.97);
  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.62),
    0 0 0 1px rgba(143, 208, 174, 0.08),
    0 0 60px rgba(143, 208, 174, 0.05);
}

/* 全屏时把外壳撑满，圆角和描边都去掉，交给系统接管 */
.gs-panel:fullscreen {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 0;
}

/* ---- 顶栏 ---- */
.gs-bar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px 10px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.025);
}

.gs-brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.gs-cn {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 4px;
  white-space: nowrap;
}

.gs-en {
  font-size: 10px;
  letter-spacing: 2.4px;
  color: var(--c-dim);
  white-space: nowrap;
}

.gs-acts {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
}

.gs-btn {
  padding: 5px 12px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--c-dim);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  transition: color 0.16s ease, border-color 0.16s ease, background 0.16s ease;
}

.gs-btn:hover {
  color: #9fd8bd;
  border-color: rgba(159, 216, 189, 0.42);
  background: rgba(159, 216, 189, 0.1);
}

.gs-x {
  padding: 5px 10px;
  font-size: 13px;
  line-height: 1;
}

/* ---- 舞台 ---- */
.gs-frame {
  position: relative;
  flex: 1;
  min-height: 0;
  background: #05070a;
}

.gs-iframe {
  display: block;
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
}

.gs-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  font-size: 13px;
  color: var(--c-dim);
  background: #05070a;
}

.gs-loading-tip {
  font-size: 11px;
  color: rgba(150, 161, 169, 0.72);
}

.gs-slow-btn {
  margin-top: 4px;
}

.gs-ring {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 2px solid rgba(159, 216, 189, 0.18);
  border-top-color: #9fd8bd;
  animation: gs-spin 0.9s linear infinite;
}

@keyframes gs-spin {
  to { transform: rotate(360deg); }
}

/* ---- 底栏 ---- */
.gs-foot {
  flex: none;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 4px 16px;
  padding: 7px 16px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--c-dim);
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.015);
}

/* ---- 进出场 ---- */
.gs-enter-active,
.gs-leave-active {
  transition: opacity 0.22s ease;
}

.gs-enter-active .gs-panel,
.gs-leave-active .gs-panel {
  transition: transform 0.26s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.22s ease;
}

.gs-enter-from,
.gs-leave-to {
  opacity: 0;
}

.gs-enter-from .gs-panel,
.gs-leave-to .gs-panel {
  transform: translateY(10px) scale(0.985);
  opacity: 0;
}

.gs-fade-leave-active {
  transition: opacity 0.3s ease;
}

.gs-fade-leave-to {
  opacity: 0;
}

/* 手机上直接铺满，别再留一圈背景 */
@media (max-width: 700px) {
  .gs-mask { padding: 0; }
  .gs-panel { width: 100%; height: 100%; border: none; border-radius: 0; }
  .gs-en { display: none; }
  .gs-foot { font-size: 10px; padding: 6px 12px; }
  .gs-foot span:last-child { display: none; }
}
</style>
