/**
 * 桌宠「青禾」的静态配置：表情、台词、场景映射、全部时长 / 距离常量、可见路由。
 *
 * 原先这些都堆在 Mascot.vue 里，一个 988 行文件既管数据又管交互；
 * 拆出来之后改一句台词、调一个节奏不用再碰组件逻辑，也不会因为顺手改数字
 * 把拖拽 / 定时器改坏。所有数值保持与原实现一致，行为不变。
 */
import mWelcome from '../assets/mascot/mascot-welcome.webp'
import mNofight from '../assets/mascot/mascot-nofight.webp'
import mWriting from '../assets/mascot/mascot-writing.webp'

export const NAME = '青禾'

/**
 * 三张表情。源文件已从 PNG 换成带 alpha 的 WebP：
 * 486~511px 的原图三张共 872K，而桌宠实际只显示 112px 宽；
 * 转 WebP（quality 86）后合计约 160K，视觉无损，首屏解压与解码压力都小很多。
 */
export const FACES = [
  {
    key: 'welcome',
    img: mWelcome,
    label: '欢迎呀',
    lines: [
      `欢迎来到 7h 呀～我是${NAME}`,
      `新朋友你好，我是${NAME}！`,
      '逛累了就歇一会儿吧',
      '今天也来逛逛啦',
      '我在这儿陪你，慢慢看',
      '有想找的就用上面的搜索框'
    ]
  },
  {
    key: 'nofight',
    img: mNofight,
    label: '不许吵架',
    lines: [
      `评论区要文明，不许吵架哦！——${NAME}`,
      '有话好好说嘛～',
      '理性发言最帅啦',
      '意见不同很正常，别上头',
      '看到不友善的可以举报给我'
    ]
  },
  {
    key: 'writing',
    img: mWriting,
    label: '加油写文',
    lines: [
      `今天也要好好写文，${NAME}给你加油～`,
      '想到什么就先记下来吧',
      '你的文章值得被看到',
      '写不出来就先写个大概，之后再改',
      '标题想好了吗？可以先占个位',
      '一段一段来，别急着追求完美'
    ]
  }
]

/** 各页面的场景台词 */
export const SCENE_LINES = {
  login: [
    '登录以后就能写文章啦',
    '账号密码填一下，我在这儿等你',
    '忘记密码的话点下面那个链接'
  ],
  register: [
    '注册一下就有自己的主页啦',
    '昵称可以之后再改，别纠结太久',
    '密码记得设复杂一点哦'
  ],
  forgot: ['别急，按步骤来就能找回', '验证完就能重设密码啦'],
  home: ['今天想写点什么？', '首页有新的动态，往下翻翻看', '累了就先歇会儿，我帮你看着'],
  blog: ['慢慢挑，好文章值得细读', '看到喜欢的就点个赞吧', '用标签筛选会快一点'],
  mine: ['这里都是你写的文章', '草稿也可以随时改', '要不要再写一篇？'],
  post: ['这篇读完了吗？', '有想法就在评论区聊聊', '看到这里了，进度不错呀'],
  editor: ['写吧写吧，我在这儿陪你', '记得时不时保存一下', '写不出来就先写个大概，之后再改'],
  profile: ['换个新头像试试？', '写一句签名吧，别人能看到', '资料填全一点，更容易被关注'],
  messages: ['有新消息吗？', '回消息的时候别急', '想找人聊聊就搜一下昵称'],
  notifications: ['看看谁来找过你', '有未读的话顺手处理一下'],
  community: ['遇到合拍的人就关注吧', '这里可以认识新朋友', '互动多了，社区就热闹了'],
  user: ['这是别人的主页哦', '喜欢的话就关注一下']
}

/**
 * 场景 -> 表情：0 欢迎 / 1 不许吵架 / 2 加油写文
 * 让表情跟着页面语义走，而不是随机乱切，表达才立得住。
 */
export const SCENE_FACE = {
  login: 0,
  register: 0,
  forgot: 0,
  home: 0,
  blog: 0,
  profile: 0,
  user: 0,
  post: 1,
  community: 1,
  messages: 1,
  notifications: 1,
  editor: 2,
  mine: 2
}

/**
 * 不渲染桌宠的路由。
 * admin：浮层 fixed 右下角，正好压住列表行的「置顶 / 删除」按钮（原来的散装 v-if 规避）。
 * login / register / forgot：这三个页面左下角已有大幅 .auth-mascot 立绘，
 * 再叠一个会说话的浮动桌宠就是「两个青禾」，视觉与语义都重复，欢迎话术改由立绘旁气泡承载。
 * 集中成一张表，以后新增页面不用再去 App.vue 里补一个 v-if。
 */
export const MASCOT_HIDDEN_ROUTES = ['admin', 'login', 'register', 'forgot']

export function mascotVisibleOn(routeName) {
  return !MASCOT_HIDDEN_ROUTES.includes(routeName)
}

export const POSTED = ['发布成功！这下大家都能看到了', '写完了，辛苦啦～', '恭喜，又一篇完成']
export const DRAFTED = ['草稿存好啦，随时回来改', '先存着，慢慢改']
export const SAVE_FAILED = ['保存好像没成功，再点一次试试？', '唔…这次没存上，检查下网络吧', '别急，内容还在，稍后再试一次']
export const GRABBED = ['哎呀，轻点拎～', '把我放哪儿呀？', '诶——我还没站稳呢']
export const DROPPED = ['放手吧，我就在这儿待着～', '这里视野不错！', `被你拎来拎去的啦——${NAME}`]

/** 分时段问候 */
export function daypartLine() {
  const h = new Date().getHours()
  if (h >= 5 && h < 8) return '早，起得真早呀'
  if (h >= 8 && h < 11) return '上午好，今天状态怎么样？'
  if (h >= 11 && h < 14) return '中午啦，先吃饭再写吧'
  if (h >= 14 && h < 18) return '下午好，写累了记得看看窗外'
  if (h >= 18 && h < 22) return `晚上好呀，${NAME}陪你慢慢逛`
  if (h >= 22) return '有点晚了，别熬太狠'
  return '深夜了，写完这段就早点休息吧'
}

/** ===== 全部节奏 / 距离常量（原先散落在组件各处的魔法数字）===== */
// 表情自动轮转间隔。加入逐帧动画后，8s 换一次表情会把一段呼吸 / 走动打断得太频繁，
// 放宽到 14s，让每套动作有完整的表演时间。
export const IDLE_MS = 14000
export const BUBBLE_MS = 6200            // 气泡停留时长
export const SAVE_NUDGE_MS = 180000      // 编辑器待多久提醒保存
export const IDLE_CLEAR_MS = 2600        // 待机小动作播放完清空 class
export const IDLE_MIN_MS = 6000          // 下一次小动作最短间隔
export const IDLE_JITTER_MS = 9000       // 下一次小动作随机抖动
export const CHATTER_MIN_MS = 25000      // 随机冒话最短间隔
export const CHATTER_JITTER_MS = 25000   // 随机冒话随机抖动
export const TAP_DELAY_MS = 260          // 单击 / 双击区分窗口
export const MOVE_THRESHOLD = 6          // 拖拽判定阈值(px)
export const SNAP_RANGE = 72             // 靠边多少 px 内吸附
export const EDGE = 20                   // 吸附后的左右留白
export const TOP_LIMIT = 56              // 拖拽时顶边最小 y
export const MIN_GAP = 8                 // 拖拽 / clamp 时四周最小间距
export const RESIZE_DEBOUNCE_MS = 200    // resize 防抖
export const HOUR_TICK_MS = 60000        // 深夜判定刷新间隔
export const LONG_PRESS_MS = 500         // 触屏长按打开菜单
export const OVERLAY_SAFE_GAP = 12       // 浮层与身体 / 视口边缘的余量
export const OVERLAY_ESTIMATE_PX = 200   // 浮层还没渲染时的高度估算值
export const LIVE_POLL_MS = 60000        // 未读消息 / 通知轮询间隔
export const LIVE_COOLDOWN_MS = 120000   // 同一类提醒的最短间隔
export const LIVE_MAX_PER_ROUTE = 2      // 每个路由最多主动提醒几句（防打扰）
export const SMALL_SCREEN_PX = 700       // 小屏自动收起阈值

/** ===== 动作系统（idle / walk / jump / drag / land）===== */
export const WALK_SPEED = 34              // 自主走动速度 px/s
export const WALK_MIN_MS = 900            // 单次走动最短时长
export const WALK_MAX_MS = 4200           // 单次走动最长时长（防止横穿整屏）
export const WALK_MIN_PX = 46             // 单次走动最短距离
export const WALK_MAX_PX = 190            // 单次走动最长距离
export const WALK_LEAN = 3.2              // 走动时整体倾斜角度（deg）
export const ROAM_MIN_MS = 11000          // 两次自主动作之间的最短间隔
export const ROAM_MAX_MS = 26000          // 最长间隔
export const ROAM_RETRY_MS = 6000         // 被悬停 / 菜单打断后重试的间隔
export const JUMP_AFTER_WALK = 0.22       // 走到目的地后顺势跳一下的概率
export const JUMP_PX = 18                 // 跳跃高度
export const JUMP_PX_TAP = 8              // 点击时轻跳的高度
export const HOLD_RELEASE_MS = 1400       // 悬停 / 菜单关闭后恢复自主动作的延迟

/**
 * 播放倍速档位。参考 dsh-desktop-pet 的 animationSpeed（0.25–4）与
 * dsh-pet 的 0.5x–4x 变速；桌宠场景 0.25x 太慢、4x 像抽筋，取中间四档。
 * 倍速直接折算进播放器的帧延迟，不额外增加调度层级。
 */
export const SPEED_MIN = 0.5
export const SPEED_MAX = 2
export const SPEED_STEPS = [0.5, 1, 1.5, 2]

/** 菜单里「外观设置」可循环切换的尺寸档位 */
export const SIZE_STEPS = [88, 112, 140]
export const DEFAULT_SIZE = 112

/** 默认外观 / 行为设置（v3 存档结构） */
export const DEFAULT_SETTINGS = {
  enabled: true,       // 关掉后只留一个极小的「显示青禾」恢复入口
  size: DEFAULT_SIZE,
  opacity: 1,
  eco: false,          // 省电模式：停掉帧动画 / 自主走动 / 模糊等高消耗效果
  faceSet: 'all',      // all = 三张表情；soft = 只用「欢迎 / 加油写文」两张
  motion: true,        // 动态动作总开关：关掉后退回单帧静态图（等同旧版观感）
  roam: true,          // 自由走动：关掉后只在原地待机 / 偶尔跳一下
  speed: 1             // 播放倍速：0.5 / 1 / 1.5 / 2（帧延迟等比例缩放）
}

export function facePool(faceSet) {
  return faceSet === 'soft' ? [FACES[0], FACES[2]] : FACES
}
