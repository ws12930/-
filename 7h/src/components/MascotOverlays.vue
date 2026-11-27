<template>
  <!--
    浮层统一挂在这里，且必须保持绝对定位、不参与布局：
    气泡 / 名片 / 菜单一占布局就会把身体顶下去，鼠标脱靶后浮层消失、身体弹回，来回抖。
    这个组件在收起（mini）时同样渲染 —— 原实现把 overlays 放在 v-else 里，
    导致右键小圆点虽然置了 menuOpen=true，菜单却根本不显示。
  -->
  <div
    class="mc-overlays"
    :class="[`side-${side}`, { 'is-below': below }]"
  >
    <!-- 说话气泡：点一下收起；aria-live 让读屏软件也能听到台词 -->
    <Transition name="bub">
      <p
        v-if="showBubble"
        class="mc-bubble"
        role="status"
        aria-live="polite"
        title="点一下收起"
        @click="$emit('dismiss-bubble')"
      >{{ line }}</p>
    </Transition>

    <!-- 悬停名片 -->
    <Transition name="card">
      <div v-if="showCard && !dragging" class="mc-card">
        <p class="mc-card-name">青禾</p>
        <p class="mc-card-role">博客看板娘</p>
        <p class="mc-card-quote">「慢慢写，好好说」</p>
        <p v-if="meName" class="mc-card-me">hi，{{ meName }}</p>
      </div>
    </Transition>

    <!-- 右键 / 长按 / 「⋯」按钮 / 键盘触发的菜单 -->
    <Transition name="menu">
      <div
        v-if="menuOpen"
        id="mc-menu"
        ref="menuEl"
        class="mc-menu"
        role="menu"
        aria-label="青禾菜单"
        @keydown="onMenuKey"
      >
        <button class="mc-play" type="button" role="menuitem" @click="act('playGame')">
          <span class="mc-play-dot" aria-hidden="true">▶</span>七日回响
        </button>
        <button class="mc-play mc-play-2" type="button" role="menuitem" @click="act('playRift')">
          <span class="mc-play-dot" aria-hidden="true">⚔</span>裂锋对决
        </button>
        <button class="mc-play mc-play-3" type="button" role="menuitem" @click="act('playGuanglun')">
          <span class="mc-play-dot" aria-hidden="true">◈</span>光轮
        </button>
        <button type="button" role="menuitem" @click="act('next')">换个表情</button>
        <button type="button" role="menuitem" @click="act('jump')">跳一下</button>
        <button type="button" role="menuitem" @click="act('wave')">招个手</button>
        <button type="button" role="menuitem" @click="act(napping ? 'wake' : 'sleep')">
          {{ napping ? '叫醒青禾' : '睡一会儿' }}
        </button>
        <button type="button" role="menuitem" @click="act('toggleMute')">
          {{ muted ? '取消静音' : '暂时静音' }}
        </button>
        <button
          type="button"
          role="menuitem"
          :aria-expanded="settingsOpen"
          @click="settingsOpen = !settingsOpen"
        >外观设置</button>

        <!-- 设置区折叠在菜单里，避免菜单一展开就顶到屏幕上边缘 -->
        <div v-if="settingsOpen" class="mc-settings" role="group" aria-label="外观设置">
          <div class="mc-set-row">
            <span>大小</span>
            <div class="mc-set-seg">
              <button
                v-for="(s, i) in sizeSteps"
                :key="s"
                type="button"
                :class="{ on: settings.size === s }"
                @click="change('size', s)"
              >{{ ['小', '中', '大'][i] }}</button>
            </div>
          </div>
          <label class="mc-set-row">
            <span>透明度</span>
            <input
              type="range"
              min="0.6"
              max="1"
              step="0.05"
              :value="settings.opacity"
              @input="change('opacity', Number($event.target.value))"
            />
          </label>
          <label class="mc-set-row mc-set-check">
            <input
              type="checkbox"
              :checked="settings.motion !== false"
              @change="change('motion', $event.target.checked)"
            />
            <span>动态动作</span>
          </label>
          <label class="mc-set-row mc-set-check" :class="{ 'is-off': settings.motion === false }">
            <input
              type="checkbox"
              :checked="settings.roam !== false"
              :disabled="settings.motion === false"
              @change="change('roam', $event.target.checked)"
            />
            <span>自由走动</span>
          </label>
          <!-- 播放倍速：参考 dsh-desktop-pet 的 animationSpeed / dsh-pet 的 0.5x–4x 变速 -->
          <div class="mc-set-row" :class="{ 'is-off': settings.motion === false }">
            <span>动作速度</span>
            <div class="mc-set-seg is-speed">
              <button
                v-for="s in speedSteps"
                :key="s"
                type="button"
                :disabled="settings.motion === false"
                :class="{ on: (settings.speed || 1) === s }"
                @click="change('speed', s)"
              >{{ s }}×</button>
            </div>
          </div>
          <label class="mc-set-row mc-set-check">
            <input
              type="checkbox"
              :checked="settings.nap !== false"
              @change="change('nap', $event.target.checked)"
            />
            <span>待机打盹</span>
          </label>
          <label class="mc-set-row mc-set-check">
            <input
              type="checkbox"
              :checked="settings.eco"
              @change="change('eco', $event.target.checked)"
            />
            <span>省电模式（停动效）</span>
          </label>
          <div class="mc-set-row">
            <span>表情包</span>
            <div class="mc-set-seg">
              <button type="button" :class="{ on: settings.faceSet === 'all' }" @click="change('faceSet', 'all')">全部</button>
              <button type="button" :class="{ on: settings.faceSet === 'soft' }" @click="change('faceSet', 'soft')">温和</button>
            </div>
          </div>
          <button class="mc-set-hide" type="button" @click="act('hide')">隐藏桌宠</button>
        </div>

        <button type="button" role="menuitem" @click="act('resetPos')">回到右下角</button>
        <button type="button" role="menuitem" @click="act('toggleMini')">
          {{ mini ? '展开' : '收起来' }}
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
/**
 * 桌宠的气泡 / 名片 / 菜单。样式在 assets/mascot.css（全局，类名统一 mc- 前缀）。
 * 菜单交互补齐了键盘与触屏路径：Escape 关闭、方向键移动焦点、
 * 打开后自动聚焦第一项（焦点回交由 Mascot.vue 的 closeMenu 处理）。
 * 原来只靠 @mouseleave 关闭，键盘用户一移开鼠标菜单就没了，这里改为由父级
 * 监听「点击外部」关闭，鼠标移出不再误关。
 */
import { ref, watch, nextTick } from 'vue'
import { SIZE_STEPS, SPEED_STEPS } from '../mascot/config'

const props = defineProps({
  line: { type: String, default: '' },
  showBubble: { type: Boolean, default: false },
  showCard: { type: Boolean, default: false },
  meName: { type: String, default: '' },
  menuOpen: { type: Boolean, default: false },
  side: { type: String, default: 'right' },
  below: { type: Boolean, default: false },
  dragging: { type: Boolean, default: false },
  muted: { type: Boolean, default: false },
  mini: { type: Boolean, default: false },
  napping: { type: Boolean, default: false },
  settings: { type: Object, required: true }
})

const emit = defineEmits(['dismiss-bubble', 'close-menu', 'action', 'setting-change'])

const sizeSteps = SIZE_STEPS
const speedSteps = SPEED_STEPS
const menuEl = ref(null)
const settingsOpen = ref(false)

function act(key) {
  emit('action', key)
}

function change(key, value) {
  emit('setting-change', { key, value })
}

function focusables() {
  if (!menuEl.value) return []
  return Array.from(menuEl.value.querySelectorAll('button, input[type="range"]'))
}

/** 打开菜单后把焦点送进第一项；关闭时收起设置区，下次打开是干净的 */
watch(
  () => props.menuOpen,
  (open) => {
    if (open) {
      nextTick(() => focusables()[0]?.focus())
    } else {
      settingsOpen.value = false
    }
  }
)

function onMenuKey(e) {
  if (e.key === 'Escape') {
    e.preventDefault()
    e.stopPropagation()
    emit('close-menu')
    return
  }
  if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return
  const items = focusables()
  if (!items.length) return
  e.preventDefault()
  const at = items.indexOf(document.activeElement)
  const next = e.key === 'ArrowDown'
    ? (at + 1 + items.length) % items.length
    : (at - 1 + items.length) % items.length
  items[next]?.focus()
}

defineExpose({ menuEl })
</script>
