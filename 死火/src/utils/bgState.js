import { ref } from 'vue'

/**
 * 全屏背景轮播是否自动播放。
 * 首页挂载了独立的首屏轮播栏，两个轮播同时运动会互相干扰，
 * 因此进入首页时置为 false 让背景定格，离开首页后恢复。
 */
export const bgAutoPlay = ref(true)
