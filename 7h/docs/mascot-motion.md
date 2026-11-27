# 桌宠动作系统（帧动画）说明

> 面向要改这块代码的人。看完应该能回答三个问题：动画是怎么播的、状态是怎么切的、素材从哪来 / 怎么换。

## 1. 改造前后

| | 改造前 | 改造后 |
|---|---|---|
| 身体 | 三张静态 WebP，`<img>` 直接切 `src` | `<canvas>` 逐帧播放 sprite sheet（也支持 GIF） |
| 待机 | 整块 `translateY` 漂浮 + 三段随机 CSS 小动作 | 10 帧呼吸 / 挤压，深夜换成 6 帧慢呼吸 |
| 走动 | 无 | 自主横向移动，左右两套倾斜帧，位置写回存档 |
| 跳跃 | 无（只有 CSS `is-hop` 原地弹一下） | 8 帧蓄力 / 拉伸 / 落地 + 真实离地位移 |
| 拖拽 | 只改 CSS `rotate(-5deg)` | 5 帧「被拎住兜帽」的钟摆姿态；松手播 4 帧落地缓冲 |
| 状态 | 没有状态概念 | 显式状态机：`drag > land > jump > walk > idle` |

静态图片没有被删除：小圆点（收起态）仍然用原图裁成头像，`face.img` 还负责首屏预解码。

## 2. 目录与职责

```
src/mascot/
├── animations.js         帧条清单（帧数 / 帧率 / 是否循环）、状态→素材解析
├── actions.js            动作注册表：优先级 / 能否被打断 / 自然出口、权重、冷却、点击反应
├── assets.js             素材解码缓存：ImageBitmap 优先、失败重试、分层预热
├── useMascotMotion.js    动作状态机：抢占判定、排队补播、走动位移、跳跃抛物线、甩抛物理、打盹
├── config.js             动作参数（速度 / 距离 / 间隔 / 高度 / 摩擦 / 挤压）与设置项
├── useMascotTimers.js    统一的 timer / rAF 登记表
└── ..

src/components/
├── MascotSprite.vue      帧播放器：双 canvas 交叉淡化、sprite sheet / GIF 双格式、播完 emit('ended')
├── Mascot.vue            组装层：手势、菜单、台词、路由联动
└── MascotOverlays.vue    菜单项与设置开关

src/assets/mascot/anim/   33 张帧条（3 个表情 × 11 个动作）
scripts/gen_mascot_frames.py    帧条生成脚本（可复现）
scripts/verify_mascot_motion.mjs 状态机回归测试（npm run verify:mascot）
scripts/verify_mascot_browser.mjs 真实浏览器验证（npm run verify:browser）
```

## 3. 状态机

状态表本身（素材 / 帧率 / 触发）见下表；**谁能打断谁**由 `actions.js` 的 `ACTION_DEFS` 统一决定，
不再散落在各个方法里做 if 判断。

| 状态 | 素材 | 帧率 | 进入条件 | 离开 |
|---|---|---|---|---|
| `idle` | `<face>-idle`（深夜用 `sleep`） | 6 fps / 3 fps | 默认；一次性动作、跳跃、走动、落地结束 | 定时排程自主动作 |
| `walk` | `<face>-walk-l` / `walk-r` | 12 fps | 空闲 11–26s 后按权重抽取（默认 54%） | 到达目标点 → 22% 概率接跳跃，否则 idle |
| `jump` | `<face>-jump` | 15 fps | 自主动作、点击轻跳（8px）、菜单「跳一下」 | 落地 → `land` → idle |
| `throw` | `<face>-drag`（复用被拎住姿态） | 10 fps | 松手速度 ≥ `THROW_MIN_V` | 惯性衰减完 → `land` |
| `drag` | `<face>-drag` | 10 fps | 指针移动超过 6px | 松手 → `throw` 或 `land` |
| `land` | `<face>-land` | 20 fps | 拖拽松手、跳跃落地 | 0.25s 后 → idle |
| `sleep` | `<face>-sleep` | 3 fps | 深夜且无交互、白天长时间无交互（打盹）、菜单「睡一会儿」 | 点击 / 菜单叫醒 → `wave` + 台词 |
| `wave` | `<face>-wave` | 10 fps | 待机权重小动作、点击上半身、菜单「招个手」、首次问候、登录跃迁、新消息 / 通知、叫醒 | 0.40s 后 → idle |
| `celebrate` | `<face>-celebrate` | 12 fps | 发布成功（PUBLISHED）、签到升级 | 0.50s 后 → idle |
| `think` | `<face>-think` | 6 fps | 待机权重小动作、点击上半身、存草稿（DRAFT） | 0.83s 后 → idle |
| `failed` | `<face>-failed` | 6 fps | 保存失败（ERROR） | 0.83s 后 → idle |

> 帧率以 `animations.js` 的 `CLIPS` 为准。本轮新增 `throw` 剪辑（复用 `drag` 帧条，不增加素材体积），
> `sleep` 正式进入状态表（此前只是深夜的 idle 变体）。

### 3.0 优先级与打断规则

```js
drag 100 > land 60 > celebrate / failed 50 > throw 45 > jump 40 > think / wave 30 > walk 20 > sleep 5 > idle 0
```

判定入口只有一个：`request(name)` → `canStart(current, next, { force })`。规则按顺序命中：

1. `drag` 永远能进（用户的手优先）；
2. 同名状态不重进（避免重置进度导致「卡第一帧」）；
3. **自然出口**优先放行：`jump → land/idle`、`throw → land`、`drag → land`。
   这条必须在「不可打断」判定**之前**——否则跳跃/甩抛到了自己的收尾动作会被自己挡住，卡在半空；
4. 当前是不可打断的一次性动作（`jump / throw / celebrate / failed / land`）→ 拒绝；
5. `force`（显式用户操作，如菜单、业务事件）→ 放行；
6. 比优先级：高则抢占，低则拒绝；同级时当前可打断则放行，两个都是一次性动作则拒绝。

被拒绝的一次性动作不会丢：若当前与来者都是一次性动作，会进 `pendingOneShot` 队列（最多 1 个），
当前动作自然收尾回 idle 后 30ms 补播。所以「发布成功」的庆祝即使撞上跳跃也不会被吞掉。

### 3.1 权重与冷却

`behaviorWeights({ route, hour, boredom, roam })` 决定自主动作抽什么：

| 条件 | 影响 |
|---|---|
| 深夜（≥23 或 <6 点） | 走动、跳跃权重归零，`sleep` 权重 32，只轻微思考 |
| 编辑器 / 我的文章 | `think` +20（陪写） |
| 广场 / 私信 / 通知 | `wave` +14（打招呼） |
| 无聊度 > 0.45（久无交互） | `think` +14、`walk` −6 |
| 关掉「自由走动」 | 走动权重归零 |

抽中的动作还要过 `filterCooled()`：同一个待机小动作在 `ACTION_COOLDOWN_MS`（14s）内不重复，
避免「连着三次招手」这种呆板感。

### 3.2 动作过渡

* **剪辑切换交叉淡化**：`MascotSprite.vue` 有两个 canvas。切素材时先把当前画面 1:1 复制到淡出层，
  新素材首帧画好后淡出层 150ms 内 `opacity → 0`（`FADE_MS`，省电模式下降为 0 直接硬切）。
  两套帧条尺寸一致、脚底对齐，所以看起来是「换姿势」而不是「换图」。
* **Q 弹挤压**：点击时 `squashX/squashY` 用 `cos(t·1.5π)·(1−t)` 做一次过冲回弹（峰值 1.085 / 0.905，
  260ms 恢复），由 CSS 变量 `--mc-squash-x/y` 消费，`transform-origin: 50% 100%` 保证脚不离地。
* **悬停微倾**：指针在身体左右半边时 `--mc-lean` 给 ±1.6°，带 180ms 过渡；离开归零。
* **走路的「滑行感」来自步频和位移不匹配**，所以 `WALK_SPEED` 现在是从剪辑推导的：
  `WALK_STRIDE_PX / (WALK_CLIP_FRAMES / WALK_FPS)` ≈ 33 px/s。改帧数或帧率，速度自动跟着变。

### 3.3 待机与打盹

`boredom` 是 0–1 的连续量（距上次交互的时间 / 阈值）。超过阈值（白天 96s、深夜 24s）自动进入
`sleep`，至少睡 `NAP_MIN_MS`；期间点击 / 菜单都能叫醒，并说一句「我刚刚打了个小盹」。
`pause()`（收起、切后台、菜单打开）会把「挂起的时间」从计时里扣掉，所以切回来不会立刻一头栽睡。
设置项 `settings.nap` 可整体关掉（关掉后深夜仍会走 `idle` 的慢呼吸）。

### 3.4 甩抛物理

松手时算指针最近 6 个采样点的速度（px/ms）：

* `< THROW_MIN_V` → 普通放下（`land`），慢放时照旧靠边吸附；
* 否则进 `throw`：每 16ms 乘 `THROW_FRICTION`（0.9）衰减，撞到视口边缘按 `THROW_BOUNCE`（0.55）反弹，
  离地用 `sin(π·air)` 弧线（`THROW_LIFT_PX`），最长 `THROW_MAX_MS`（900ms），落地 `snap()` + 存档 + `land`。
* 速度上限 `THROW_MAX_V`，防止一把甩到屏幕外。

### 3.5 点击部位

点击点落在身体高度 56%（`POKE_UPPER_RATIO`）以上算「摸头」，以下算「戳肚子」，两套反应池
（`POKE_REACTIONS`）加权抽取：摸头偏 `wave/think`，戳肚子偏 `jump`。700ms 内连点只算一次。

几条约束：

* **所有定时器走 `useMascotTimers`**：收起、切后台、组件卸载时统一清空，不会留下后台 rAF。
* **姿态与位移分离**：帧只负责挤压 / 拉伸 / 呼吸；左右倾斜来自 `--mc-lean`，跳跃高度来自 `--mc-lift`，
  与 Q 弹的 `--mc-squash` 一起由 CSS `transform` 合成。改跳跃高度、挤压幅度都不用重烘焙素材。
* **一切切换收口到 `request()`**：新增动作时只改 `actions.js` 一张表，不用去翻各个 `playXxx()`。


![动作帧总览](./mascot-actions.jpg)

> 上图是 welcome 表情的 7 套帧条（idle / walk-l / walk-r / jump / drag / land / sleep），
> 每格就是一帧；走动时的大幅倾斜由运行时 CSS 叠加，所以帧条里看着只差上下起伏。


参数都在 `src/mascot/config.js`，按注释分成「动作系统 / 动作过渡 / 待机打盹 / 甩抛物理 / 点击部位」几节：

```js
WALK_SPEED = WALK_STRIDE_PX / (WALK_CLIP_FRAMES / WALK_FPS) // 由步频推导，≈33 px/s
WALK_MIN_PX / MAX_PX // 单次走动距离
ROAM_MIN_MS / MAX_MS // 两次自主动作的间隔
JUMP_PX = 18         // 跳跃高度
WALK_LEAN = 3.2      // 走动倾斜角度
FADE_MS / SQUASH_MS / SQUASH_X / SQUASH_Y / HOVER_LEAN
BOREDOM_NAP_MS / BOREDOM_NAP_MS_NIGHT / NAP_MIN_MS / ACTION_COOLDOWN_MS
THROW_MIN_V / THROW_MAX_V / THROW_FRICTION / THROW_BOUNCE / THROW_MAX_MS / THROW_LIFT_PX
POKE_UPPER_RATIO / POKE_COOLDOWN_MS
```

### 3.6 事件 → 情绪动作（按代码备注补齐）

`wave / celebrate / think / failed` 的帧条早先就烘焙好了，`useMascotMotion.react()` 也留了
「外部业务事件驱动的情绪动作」入口，但一度没有任何调用点；`config.js` 的 `SAVE_FAILED`
台词、`EditorView.vue` 里「桌宠据此播『沮丧』动作并说一句安慰」的备注同样悬空。
现在这条链路是通的：

| 来源 | 事件 | 台词 | 动作 |
|---|---|---|---|
| `EditorView.vue` | `blog:saved` = PUBLISHED | `POSTED` | `celebrate` |
| `EditorView.vue` | `blog:saved` = DRAFT | `DRAFTED` | `think` |
| `EditorView.vue` | `blog:saved` = ERROR | `SAVE_FAILED` | `failed` |
| `bn:session`（未登录 → 已登录） | 登录跃迁 | 欢迎回来 | `wave` |
| `useMascotLive` | 新通知 / 新私信 | 对应提醒 | `wave` |
| `useMascotLive` | 签到等级提升 | 升级啦 | `celebrate` |
| 菜单「招个手」/ 首屏 / 展开 / 恢复 | 显式打招呼 | 对应问候 | `wave` |

* 三态归一化在 `useMascotSpeech.normalizeSaveStatus()`，映射表在 `config.js` 的
  `SAVED_REACTIONS`；以后加一种保存结果只改这两处，不用去 `Mascot.vue` 补 if/else。
* 保存后 `EditorView` 会立刻跳路由，路由 watch 的场景台词会把反馈顶掉；
  `Mascot.vue` 在 `BUBBLE_MS` 内挂起场景台词（表情照常跟随页面），让情绪动作演完。
* 防打扰沿用旧门禁：静音 / 收起 / 后台不说话，动作也被 `pause()` 挡住；
  `useMascotLive` 只在台词真的说出口时才顺带播动作。
* **倍速对齐**：播放器把帧延迟除以 `playbackRate`，所以一次性动作的「播完回 idle」
  定时与跳跃位移曲线现在也除以同一个倍速。旧版固定用 `clipDuration()`，
  2× 时动作播一半就被切回 idle，0.5× 时又提前空等一截。

## 4. 素材管线

`scripts/gen_mascot_frames.py` 用 Pillow 把三张静态立绘烘焙成帧条（不是运行时算的，是真实图片）：

```bash
pip install Pillow
python scripts/gen_mascot_frames.py
```

约定（播放器和脚本必须一致）：

* 单元格固定 **128 × 134**，横向条带，第 i 帧 = `(i*128, 0, (i+1)*128, 134)`；
* 人物底边永远贴单元格底边，切换状态时脚不跳；
* 所有帧共用同一画布，不做自动裁剪，避免不同状态下人物忽大忽小；
* 帧数写在 `animations.js` 的 `CLIPS` 里，改帧数要两边一起改。

体积（实测）：

| 项目 | 数值 |
|---|---|
| 33 张帧条合计 | 1101 KB |
| 单个表情的 idle 帧条 | 52–58 KB |
| 首屏实际加载 | 只有当前表情的 idle（≈52 KB） |
| 预热顺序 | `preloadPrimary()` 先只等 idle；`preloadMotion()` 再按 `CORE_SHEETS`（idle/walk-r/walk-l/jump/drag/land）→ `EXTRA_SHEETS`（sleep/wave/celebrate/think/failed）分两层，`requestIdleCallback` 串行装载 |

体积主要花在 alpha 通道和头发细节上，降 quality 收益很小（q82 → q66 只省约 20%），所以选了 128px + q76 的平衡点。

### 资源加载（`assets.js`）

所有帧条走同一张缓存表，`MascotSprite.vue` 和预热逻辑不再各存一份：

```js
const bmp = await loadSheet(url)   // fetch(cache:'force-cache') → createImageBitmap()
                                   // 不支持 / 失败时回落 <img>，画出来一样
```

* **`createImageBitmap` 优先**：解码在 worker 侧完成，切素材时不会在主线程解析 PNG 造成掉帧；
  浏览器不支持时自动回落 `<img>`，`drawImage` 两者通用。
* **失败只重试一次**（800ms 后），仍失败就记进 `failed` 集合，后续直接跳过，避免每次切状态都发请求。
* **省流降级**：`navigator.connection.saveData` 或 `effectiveType` 为 2g/3g 时只预热 `CORE_SHEETS`。
* `clearSheetCache()` 供调试 / 换主题时手动清空。


### 换成手绘帧 / Shimeji 素材

保持「横向条带 + 128×134 + 底边对齐 + 帧数一致」，覆盖同名文件即可，代码零改动。
如果美术给的是单张 PNG 序列，用脚本里 `render()` 的思路拼成条带，或者直接改 `CLIPS` 指向新的 sheet。

### 用 GIF

`animations.js` 里留了覆盖入口：

```js
import idleGif from '../assets/mascot/anim/gif/welcome-idle.gif'
CUSTOM_SHEETS['welcome-idle'] = idleGif
```

播放器检测到 `.gif` 会自动走 GIF 解码分支，帧数 / 帧率以 GIF 自身为准。
代价：GIF 只有 1bit 透明，边缘会有锯齿，所以默认仍是 WebP 帧条。

## 5. 引入 / 参考的开源项目

| 项目 | 许可证 | 用法 |
|---|---|---|
| [gifuct-js](https://github.com/matt-way/gifuct-js) | MIT | 把 GIF 解成帧（`parseGIF` + `decompressFrames`），合成与计时由 `MascotSprite.vue` 接管 |
| [dsh-pet](https://github.com/PC2005-cloud/dsh-pet) | MIT | **仅参考设计，未引入代码**：多段动画首尾衔接、点击 Q 弹、拖拽甩抛 + 边缘反弹、睡觉三段式、工作状态分级 |
| [Shimeji-ee](https://github.com/kilkakon/Shimeji-ee) | BSD-2-Clause | **仅参考设计，未引入代码**：Behavior / Action 双层结构、带权重的行为抽取、交互热点、`ActionReference` 串动作 |
| [吱吱吉 Shimeji](https://github.com/YuZhu412/zhizhiji-shimeji) | 仅参考思路，未引入代码 | 「一张静态图生成 idle / walk / drag / fall 帧序列」的管线：统一画布、底部锚点、垂直位移模拟重心 |
| Shimeji-ee 的动作命名 | 仅参考设计 | `idle / walk / drag / fall` 这套状态划分是桌宠类应用的通用约定，本项目的状态机按它对齐，方便以后直接吃 Shimeji 素材包 |

各项目「哪些机制被移植、哪些刻意没做」的逐条对照，见 [docs/mascot-refactor.md](./mascot-refactor.md)。


### gifuct-js 具体怎么接的

依赖是**按需加载**的：只有真的用到 `.gif` 素材时才会 `import('gifuct-js')`，默认的帧条路径不会把它打进首屏。

```js
// MascotSprite.vue
const mod = await import('gifuct-js')
const api = mod.default && mod.default.parseGIF ? mod.default : mod
const parsed = api.parseGIF(arrayBuffer)
const raw = api.decompressFrames(parsed, true) // true = 顺便生成 canvas 可直接用的 patch

// GIF 的 disposal 语义要自己处理：2 = 清掉上一帧区域，3 = 还原到上一帧之前
if (pendingDisposal === 2) gctx.clearRect(...)
else if (pendingDisposal === 3) gctx.putImageData(prevSnapshot, 0, 0)
gctx.putImageData(frame.patch, frame.dims.left, frame.dims.top)
```

注意 v2 是**函数式 API**（`parseGIF` / `decompressFrames`），网上不少教程还是旧版的 `new GIF(buffer)`，直接照抄会报 `GIF is not a constructor`。

安装：`npm install gifuct-js`（已写进 `package.json`）。

## 6. 与原有能力的兼容

以下行为保持不变，改这块时别破坏：

* 拖拽跟手、松手靠边吸附、坐标 / 尺寸 / 透明度 / 静音 / 表情包存档与 v2→v3 迁移；
* 单击说话、长按 / 右键 / ⋯ 开菜单、Esc 关闭、方向键走焦点；
  **双击收起改为手动 320ms 判定**（原来挂 `@dblclick`，触屏上没有这个事件，只能走菜单收起）；
* 静音 / 收起 / 后台时不说台词，轮询跟着 timer registry 一起停；
* 深夜压暗 + Zzz、路由场景表情、编辑器保存提醒、未读消息播报；
* 浮层在顶部自动翻到下方；小屏（<700px）自动收起；
* 省电模式：帧动画冻结在第一帧（不再是简单粗暴地 `animation: none`），交叉淡化同时降为硬切。

设置项 `motion`（动态动作）、`roam`（自由走动）、`nap`（待机打盹）在 `DEFAULT_SETTINGS` 里都有默认值，
旧存档读出来会自动补齐，不需要迁移版本号。台词池、尺寸档、倍速档、表情池、存档结构全部沿用原样。

## 7. 已验证

### 状态机回归测试 —— `npm run verify:mascot`

用 Vite 的 `ssrLoadModule` 在 Node 里加载**真实**的 `actions.js` / `useMascotMotion.js`
（喂假的 timers / position / window），不依赖浏览器，29 条断言覆盖：

| 分组 | 覆盖点 |
|---|---|
| 动作表 | 11 个状态的优先级单调、`drag` 最高、`idle` 最低 |
| 打断规则 | 高优先级抢占、同级一次性动作互相拒绝、`force` 放行、自然出口见 3.0 |
| 排队补播 | 跳跃进行中收到 celebrate → 落地后补播 → 再回 idle |
| 甩抛 | 高速松手进 `throw`、衰减后落定回 idle、低速松手只走 `land` |
| 睡眠 | 打盹阈值、`wake(greet)` 后回 idle、睡着时点击会醒 |
| 冷却 | `filterCooled` 剔除冷却中的动作、冷却到期重新可选 |
| 挂起 | `pause()` 期间不切动作、`resume()` 补上挂起时长 |

### 真实浏览器验证 —— `npm run verify:browser`

无头 Chrome + CDP（`Input.dispatchMouseEvent` 真事件）实测 14 项，全部通过：

```
OK   页面挂载桌宠
OK   桌宠尺寸正常 — 112x117
OK   canvas 已绘制人物 — alpha>8 像素 9073
OK   初始为待机/睡眠 — state-idle
OK   点击有动作反馈 — state-wave          （点上半身）
OK   transform 生效（挤压/位移） — matrix(1.005, 0, 0, 0.994, …)
OK   快速甩动进入甩抛 — state-throw
OK   甩抛落定后恢复 — state-idle
OK   菜单存在「睡一会儿」/ 进入睡眠状态 / Zzz 出现 / 睡眠剪辑生效 — state-sleep
OK   点击叫醒 — 退出 sleeping
OK   无 JS 运行时异常
```

### 构建

`npm run build` 通过，产物 `index-*.js ≈ 301 kB`（gzip 108 kB）+ `index-*.css 96 kB`（gzip 17.6 kB），
帧条按素材 id 独立产出，GIF 解码器仍是独立 chunk（7.4 kB），没有因为新逻辑把 gifuct-js 拖进首屏。

### 历史结论（仍成立）

* 无头浏览器实测过：点击后 `state-jump` 持续约 0.67s、最大离地 7.9px；`startWalk()` 横移 74px、
  `state-walk` 保持、倾斜角生效；关掉动态动作 / 开省电后画面冻结且 `clip` 变成 `*-static`。
* 自主行为端到端：`idle → 24s 自主走动（横移 182px）→ 顺势跳跃 → idle`。
* 用临时 GIF 验证过解码分支：`src` 指向 `.gif` 后画面正常逐帧推进，控制台无报错。
* 事件动作：`normalizeSaveStatus` 三态（含大小写、空值）与 `SAVED_REACTIONS` 映射、三个台词池非空，函数级自检通过。
