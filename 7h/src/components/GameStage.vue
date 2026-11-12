<template>
  <Teleport to="body">
    <Transition name="gs">
      <div
        v-if="open"
        class="gs-mask"
        role="dialog"
        aria-modal="true"
        :aria-label="title + ' · ' + en"
        @click.self="close"
      >
        <div ref="panelEl" class="gs-panel">
          <header class="gs-bar">
            <div class="gs-brand">
              <span class="gs-cn grad-text">{{ title }}</span>
              <span class="gs-en">{{ en }}</span>
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
            <!-- 跨源站点，父页面拿不到内部状态，只能靠 load 事件收掉加载层。
                 探活（probeGameUrl）判定链接已失效时，这个 iframe 根本不会挂载 ——
                 否则用户看到的就是 WorkBuddy 那张「链接不完整」的 403 页面。 -->
            <iframe
              v-if="mounted"
              :key="frameKey"
              class="gs-iframe"
              :src="gameUrl"
              :title="title + ' · ' + en"
              allow="autoplay; fullscreen; clipboard-write"
              allowfullscreen
              @load="onFrameLoad"
              @error="onFrameError"
            ></iframe>

            <Transition name="gs-fade">
              <div v-if="failed" class="gs-fail">
                <span class="gs-fail-badge" aria-hidden="true">!</span>
                <h3 class="gs-fail-title">{{ failTitle }}</h3>
                <p class="gs-fail-desc">{{ failed }}</p>
                <div class="gs-fail-acts">
                  <button class="gs-btn gs-btn-main" type="button" @click="retry">重试</button>
                  <button class="gs-btn" type="button" @click="openTab">在新标签页打开</button>
                </div>
                <p class="gs-fail-tip">
                  游戏托管在外部链接上，链接过期或下架后就打不开了；站长重新发布后
                  把新地址填进游戏配置即可，也可以把游戏产物放进站点目录改成站内托管。
                </p>
              </div>
            </Transition>

            <Transition name="gs-fade">
              <div v-if="!ready && !failed" class="gs-loading">
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
            <span>{{ footLeft }}</span>
            <span>{{ footRight }}</span>
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
 *   不用另做一套全屏布局；
 * - 打开前先用 HEAD 探一下托管链接还活着没有。WorkBuddy 的分享链接失效时，
 *   它的域名会返回一张 403「链接不完整」页面 —— 这种页面挂在 iframe 里照样
 *   load 成功，父页面收不到任何错误信号，用户只会看到别人家的错误页。
 *   所以这里主动探活，把「链接失效」翻译成站内的说明 + 重试入口。
 *   换链接不用改代码：见 utils/gameConfig.js 的三层优先级。
 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { resolveGameUrl, probeGameUrl, isSameOrigin } from '../utils/gameConfig'

const props = defineProps({
  open: { type: Boolean, default: false },
  src: { type: String, default: () => resolveGameUrl() },
  /* 面板标题：站内可能挂不止一个游戏，标题跟着 src 一起传 */
  title: { type: String, default: '七日回响' },
  en: { type: String, default: 'SEVEN DAYS ECHO' },
  /* 底部两行说明：不同游戏的「存档/进度」说法不一样，跟着传 */
  /* 键盘驱动的游戏（格斗）打开后把焦点送进 iframe，不然按键全被博客收走 */
  autoFocus: { type: Boolean, default: false },
  footLeft: { type: String, default: '纯前端 Galgame · 存档保存在游戏自己的本地存储' },
  footRight: { type: String, default: '关掉窗口会重新加载，进度记得先在游戏里「存档」' }
})

const emit = defineEmits(['close'])

const panelEl = ref(null)
/** 当前生效的游戏地址：调用方传了就用它，否则走配置解析 */
const gameUrl = computed(() => (props.src || resolveGameUrl()).trim())
/** 探活结论：空串表示正常 / 判断不了；非空就是给用户看的失败原因 */
const failed = ref('')
const failTitle = ref('游戏暂时打不开')
/** iframe 是否已挂过（首次打开后不再卸，但随面板一起销毁） */
const mounted = ref(false)
/** 用 key 强制重建 iframe，重试时能绕开浏览器缓存 */
const frameKey = ref(0)
/** 首帧是否加载完 */
const ready = ref(false)
/** 迟迟没加载完：把「新标签页」这条路推到眼前，别让人对着转圈干等 */
const slow = ref(false)
const isFull = ref(false)

let slowTimer = null
/** 每次打开 +1；异步探活回来时靠它判断这批结果是不是已经过期了 */
let runId = 0

/** 把状态码翻成人话：403/404/410 是「链接没了」，其余是「服务端出问题」 */
function failWith(status) {
  const gone = status === 403 || status === 404 || status === 410
  failTitle.value = gone ? '游戏链接已失效' : '游戏暂时打不开'
  failed.value = gone
    ? `游戏托管链接已失效（HTTP ${status}），不是网络问题。`
    : `游戏服务返回了 HTTP ${status}，暂时打不开。`
  ready.value = false
  mounted.value = false
}

async function start(id) {
  ready.value = false
  slow.value = false
  failed.value = ''
  mounted.value = false
  clearTimeout(slowTimer)

  const verdict = await probeGameUrl(gameUrl.value)
  // 面板已经关掉 / 重开过，这次探活结果作废
  if (id !== runId || !props.open) return

  if (verdict.ok === false) {
    failWith(verdict.status)
    return
  }

  // ok === true（链接正常）或 null（跨域读不到状态、网络判断不了）
  // 都照常挂 iframe：探活失败不等于游戏挂了，绝不能把入口整个判死
  mounted.value = true
  frameKey.value += 1
  slowTimer = setTimeout(() => {
    if (!ready.value && !failed.value) slow.value = true
  }, 12000)
}

function retry() {
  runId += 1
  start(runId)
}

function onFrameLoad() {
  // 同源托管时才敢读内部：反向代理把错误页当 200 返回也能认出来
  if (isSameOrigin(gameUrl.value)) {
    try {
      const text = document.querySelector('.gs-iframe')?.contentDocument?.body?.innerText || ''
      if (/链接不完整|缺少必要信息|not\s*found|404/i.test(text)) {
        failTitle.value = '游戏链接已失效'
        failed.value = '游戏页面返回的是错误提示，托管链接可能已经失效。'
        ready.value = false
        mounted.value = false
        return
      }
    } catch {
      /* 读不到就当作正常，交给用户自己看 */
    }
  }
  if (props.autoFocus && isSameOrigin(gameUrl.value)) {
    try {
      document.querySelector('.gs-iframe')?.contentWindow?.focus()
    } catch {
      /* 拿不到焦点就让用户点一下 */
    }
  }
  ready.value = true
  slow.value = false
  clearTimeout(slowTimer)
}

function onFrameError() {
  failTitle.value = '游戏暂时打不开'
  failed.value = '浏览器没能加载这个页面，可能是网络或托管服务的问题。'
  ready.value = false
  mounted.value = false
}

function close() {
  emit('close')
}

function openTab() {
  window.open(gameUrl.value, '_blank', 'noopener,noreferrer')
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
      runId += 1
      start(runId)
      window.addEventListener('keydown', onKey)
    } else {
      clearTimeout(slowTimer)
      window.removeEventListener('keydown', onKey)
      if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {})
    }
  },
  // immediate：组件挂载时就带着 open=true（被别处直接摆出来）也能正常工作，
  // 不依赖「先 false 再 true」这一次翻转
  { immediate: true }
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

/* ---- 链接失效 / 打不开 ---- */
.gs-fail {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 28px;
  text-align: center;
  background:
    radial-gradient(circle at 50% 38%, rgba(143, 208, 174, 0.07), rgba(5, 7, 10, 0) 62%),
    #05070a;
}

.gs-fail-badge {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  margin-bottom: 2px;
  font-family: var(--font-display);
  font-size: 22px;
  line-height: 1;
  color: #f0c674;
  border: 1px solid rgba(240, 198, 116, 0.4);
  border-radius: 50%;
  background: rgba(240, 198, 116, 0.1);
}

.gs-fail-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 2px;
  color: var(--c-text);
}

.gs-fail-desc {
  margin: 0;
  max-width: 42ch;
  font-size: 13px;
  line-height: 1.7;
  color: var(--c-dim);
}

.gs-fail-acts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 6px;
}

.gs-btn-main {
  color: #0b0e11;
  border-color: transparent;
  background: linear-gradient(135deg, #9fd8bd, #8fd0ae);
}

.gs-btn-main:hover {
  color: #0b0e11;
  border-color: transparent;
  background: linear-gradient(135deg, #b6e5cd, #9fd8bd);
}

.gs-fail-tip {
  margin: 8px 0 0;
  max-width: 56ch;
  font-size: 11px;
  line-height: 1.75;
  color: rgba(150, 161, 169, 0.72);
}

.gs-fail-tip code {
  padding: 1px 5px;
  font-size: 10px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
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
  .gs-fail { padding: 20px 18px; }
  .gs-fail-tip { display: none; }
}
</style>
