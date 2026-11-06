/**
 * 二次确认弹窗的轻量封装。
 *
 * 用法（配合 components/ConfirmDialog.vue）：
 *   const { state, ask, confirm, close } = useConfirm()
 *   ask({ title:'删 除 文 章', message:'确定删除？', detail:'删除后无法恢复',
 *         confirmText:'删 除', danger:true, onConfirm: async () => { ... } })
 *
 * 模板里固定接三个口子：
 *   <ConfirmDialog :state="state" @confirm="confirm" @close="close" />
 */
import { ref } from 'vue'

export function useConfirm() {
  const state = ref(null)
  // 待执行的回调：用户点「确定」后跑，异步失败由调用方自己 toast
  let pending = null

  function ask(options = {}) {
    pending = typeof options.onConfirm === 'function' ? options.onConfirm : null
    state.value = {
      title: options.title || '确认操作',
      message: options.message || '',
      detail: options.detail || '',
      confirmText: options.confirmText || '确 定',
      cancelText: options.cancelText || '取 消',
      danger: !!options.danger
    }
  }

  function confirm() {
    const run = pending
    state.value = null
    pending = null
    if (run) run()
  }

  function close() {
    state.value = null
    pending = null
  }

  return { state, ask, confirm, close }
}
