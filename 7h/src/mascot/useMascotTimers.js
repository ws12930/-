/**
 * 桌宠统一的定时器登记表。
 *
 * 原实现用 7 个模块级变量分别保存 rotate/bubble/idle/speak/hour/saveNudge/raf，
 * pauseAll() 只清了其中 5 个：playIdle 内部那个 2600ms 的清理 timeout、它的
 * requestAnimationFrame 回调、以及 hourTimer 都漏在登记之外 —— 切后台 / 收起后
 * 它们照样触发，注释里写的「停掉所有定时器」其实没做到，回来时还容易叠出多套链。
 *
 * 这里改成「所有延时任务必须登记，暂停即全清，恢复由调用方统一重新排程」：
 * - 登记过的 id 不会漏清；
 * - 每个回调执行前再检查一次 paused，万一有 rAF 之类的漏网之鱼也不会跑；
 * - 组件卸载自动 dispose，不用逐个手写 clearTimeout。
 *
 * 注意：pause 是「清空」而不是「冻结」，因为桌宠这些定时器本来就是周期性重排的
 * （playIdle / scheduleChatter 自己递归），恢复时由 Mascot.vue 的 scheduleAll()
 * 重新起一套即可，不必保存剩余时间。
 */
import { onBeforeUnmount } from 'vue'

export function useMascotTimers() {
  const timeouts = new Set()
  const intervals = new Set()
  const rafs = new Set()
  let paused = false

  function timeout(fn, ms) {
    const id = window.setTimeout(() => {
      timeouts.delete(id)
      if (!paused) fn()
    }, ms)
    timeouts.add(id)
    return id
  }

  function interval(fn, ms) {
    const id = window.setInterval(() => {
      if (!paused) fn()
    }, ms)
    intervals.add(id)
    return id
  }

  function raf(fn) {
    const id = window.requestAnimationFrame(() => {
      rafs.delete(id)
      if (!paused) fn()
    })
    rafs.add(id)
    return id
  }

  /** 清掉任意一个登记过的 timeout / interval / rAF（跨类型调用也安全） */
  function clear(id) {
    if (id == null) return
    if (timeouts.delete(id)) window.clearTimeout(id)
    if (intervals.delete(id)) window.clearInterval(id)
    if (rafs.delete(id)) window.cancelAnimationFrame(id)
  }

  function clearAll() {
    timeouts.forEach((id) => window.clearTimeout(id))
    intervals.forEach((id) => window.clearInterval(id))
    rafs.forEach((id) => window.cancelAnimationFrame(id))
    timeouts.clear()
    intervals.clear()
    rafs.clear()
  }

  /** 暂停：先立标记再全清，保证排队中的回调即使触发也因 paused 被丢弃 */
  function pause() {
    paused = true
    clearAll()
  }

  /** 恢复：只解除标记，重新排程交给调用方的 scheduleAll() */
  function resume() {
    paused = false
  }

  function isPaused() {
    return paused
  }

  onBeforeUnmount(clearAll)

  return { timeout, interval, raf, clear, clearAll, pause, resume, isPaused }
}
