/**
 * 全站共享的字符串常量。
 *
 * 为什么单独抽一个文件：登录态键名、桌宠存储键、跨组件事件名原先以字面量散落在
 * api/ views/ components/ router/ 里，改一个键要全局搜一遍，漏一处就是「登录了但
 * 桌宠还当没登录」这类静默不一致。这里统一收口，读写两端引用同一个符号。
 * 值本身保持原样，不改动任何已有存储数据 / 后端契约。
 */

/** 本地登录态：用户信息 JSON */
export const SESSION_ME_KEY = 'bn-user'
/** 本地登录态：访问令牌 */
export const SESSION_TOKEN_KEY = 'bn-token'

/**
 * 同标签页登录态变化事件。
 * storage 事件只在「别的标签页」写入时触发，本标签页 setItem/removeItem 不会触发，
 * 所以涉及登录 / 登出 / 资料更新 / 封禁清态的地方要显式派发这个事件，
 * 让 TopBar、桌宠名片等订阅方立刻刷新，而不是等下一次路由跳转。
 */
export const EVT_SESSION_CHANGED = 'bn:session'

/** 编辑器保存成功后派发，detail.status 为 'PUBLISHED' / 'DRAFT' */
export const EVT_BLOG_SAVED = 'blog:saved'

/** 桌宠状态存储键（v3 起结构带 version 字段，支持迁移） */
export const MASCOT_STORE_KEY = 'qh-mascot-v3'
/** 旧版桌宠状态键：读不到 v3 时尝试迁移 */
export const MASCOT_STORE_KEY_V2 = 'qh-mascot-v2'

/** 通知一次登录态变化（供任何写入 bn-user / bn-token 的地方调用） */
export function notifySessionChanged() {
  try {
    window.dispatchEvent(new CustomEvent(EVT_SESSION_CHANGED))
  } catch (e) {
    /* 非浏览器环境 / CustomEvent 不可用时静默降级，不能因为一条通知把登录流程打断 */
  }
}
