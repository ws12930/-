import { ref } from 'vue'

/**
 * 背景轮播的全局开关。
 *
 * 首页自带一个首屏轮播栏，此时把 bgAutoPlay 置为 false，
 * 让背景定格在当前图层，避免两个轮播同时动、画面互相打架。
 * 离开首页时再恢复为 true。
 */
export const bgAutoPlay = ref(true)

/** 首页挂载首屏轮播时调用 */
export function pauseBg() {
  bgAutoPlay.value = false
}

/** 离开首页时调用 */
export function resumeBg() {
  bgAutoPlay.value = true
}
