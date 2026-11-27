# 桌宠动作系统 · 第二轮改造说明

> 本轮目标：**只做动作表现与工程质量的深化**，主题、台词、设置项、存档格式、菜单结构一律不动。
> 参考 GitHub 上的开源桌宠（dsh-pet / Shimeji-ee 为主）重构动作层，但不引入它们的运行时代码。
>
> 想先了解「现在怎么跑」看 [mascot-motion.md](./mascot-motion.md)；本文只讲**改了什么、为什么、验证到什么程度**。

---

## 1. 参考项目：哪些机制补进来了，哪些刻意没做

### 1.1 [dsh-pet](https://github.com/PC2005-cloud/dsh-pet)（MIT，DSH 桌面宠物插件）

它的核心是「一堆透明动画按权重首尾相接」，加上一套很轻的手势反馈。对照下来：

| dsh-pet 的机制 | 本项目 | 落地位置 | 说明 |
|---|---|---|---|
| 100+ 段动画按权重衔接 | **部分采用** | `actions.js` `behaviorWeights()` | 拿走「按权重抽 + 按情境改权重」的思路，动画数量仍是 11 段（素材只有 33 张帧条，加动作要重烘，属于无关扩张） |
| 点击 Q 弹（挤压回弹） | **采用** | `pokeSquash()` + `--mc-squash-x/y` | 用 `cos(t·1.5π)·(1−t)` 做过冲，比 dsh 的线性回弹更有弹性 |
| 拖拽甩出 + 边缘反弹 | **采用** | `throwPet()` + 新状态 `throw` | 采样最近 6 点算速度，摩擦衰减 + 撞边反弹，落定后 `snap()` + 存档 |
| 睡觉 start → loop → end 三段 | **部分采用** | `sleep` 状态 + `wake(greet)` | 素材只烘了循环段，所以「入睡 / 醒来」用 idle ↔ sleep 的交叉淡化 + 一句台词代替 end 段 |
| 睡着时拒绝打扰 | **采用** | `napCheck()` 的 `NAP_MIN_MS` | 睡着后至少要睡满 9s，但**用户点击立即叫醒**（比 dsh 更尊重用户） |
| 工作状态分级（thinking / working / success / error） | **采用映射** | `SAVED_REACTIONS` → `think/celebrate/failed` | 第一轮已接好三态；本轮补上「睡着时收到保存事件先叫醒再反应」 |
| 屏幕漫游 / 左右转身 | **已有，保留** | `startWalk()` / `facing` | 原实现已满足，不重写 |
| 独立的「心情值」引擎 | **不做** | — | 主题里没有心情系统，加了就是无关功能；用 `boredom`（无聊度）一个标量代替 |

### 1.2 [Shimeji-ee](https://github.com/kilkakon/Shimeji-ee)（BSD-2-Clause）

工程结构上最值得抄的是它的「行为 / 动作」两层分离，以及 `actions.xml` 里声明式的字段：

| Shimeji-ee 的机制 | 本项目 | 落地位置 |
|---|---|---|
| Behavior（想去做什么）/ Action（怎么演）分层 | **采用（简化为一层 + 队列）** | `actions.js` 的 `ACTION_DEFS` + `useMascotMotion` 的 `request()` |
| `NextBehaviorList` 带权重抽取 | **采用** | `behaviorWeights()` + `pickWeighted()` |
| Action = 图片 + 锚点 + 速度 + 时长 | **部分采用** | 锚点由「128×134 单元格 + 底边对齐」统一表达；时长来自 `CLIPS` 的帧数/帧率；速度来自 `config.js` |
| `ActionReference` 串动作（A 演完接 B） | **采用** | `NATURAL_EXITS`（jump/throw/drag → land）+ `pendingOneShot` 补播队列 |
| 交互热点（`SitAffordance` / `RunAffordance`） | **采用简化版** | `pokeAt(ratio)` 按点击高度分「摸头 / 戳肚子」两套反应池 |
| 固定 25 FPS 主循环 | **不采用** | 现有「按帧延迟自调度 rAF」已经能做到掉帧不补、倍速缩放、省电冻结；换成固定 tick 反而要在主循环里再拆一层计时 |
| 物理（重力 / 爬到窗口边缘 / 挂窗口） | **不做** | 网页里没有窗口边框概念，做成「贴浏览器边缘」会和现有靠边吸附冲突 |

### 1.3 其他

* [吱吱吉 Shimeji](https://github.com/YuZhu412/zhizhiji-shimeji)：延续第一轮的「统一画布 + 底部锚点」素材约定。
* `gifuct-js`（MIT）：唯一的运行时依赖，保持按需 `import()`，不进首屏。

> **没有引入任何第三方运行时代码**（除了原有的 gifuct-js）。以上都是设计层面的移植，
> 因为 dsh-pet / Shimeji 是桌面端（Qt / Java），其动作调度与渲染和 canvas Web 完全不同。

---

## 2. 动作表现的改动

### 2.1 待机：从「定时随机」变成「权重 + 冷却 + 无聊度」

改之前：`ROAM_MIN_MS ~ ROAM_MAX_MS` 到点后 `Math.random()` 一把，结果就是
「连着三次招手」「深夜还在满屏乱走」这种不自然。

现在（`actions.js`）：

```js
behaviorWeights({ route, hour, boredom, roam })
```

| 情境 | 权重变化 |
|---|---|
| 深夜 23–6 点 | `walk` / `jump` 归零，`sleep` 32 —— 夜里该安静 |
| 编辑器 / 我的文章 | `think` +20 —— 陪写 |
| 广场 / 私信 / 通知 | `wave` +14 —— 打招呼 |
| 无聊度 > 0.45 | `think` +14、`walk` −6 —— 待太久就发呆，而不是一直走 |
| 关掉「自由走动」 | `walk` 归零 |

抽中后还要过 `filterCooled()`：同一动作 14s 内不重复。于是「待机」有了节奏感，而不是纯随机。

**打盹**：`boredom` 是 0–1 连续量，白天 96s / 深夜 24s 无交互就自己睡着（`sleep` 状态 + Zzz），
菜单和点击都能叫醒并说一句「我刚刚打了个小盹」。新增设置项 `nap` 可关闭。

### 2.2 互动：手势从「有反应」变成「反应得对」

| 手势 | 改之前 | 现在 |
|---|---|---|
| 单击上半身 | 统一抽一个随机动作 | 按点击高度分头 / 身两套反应池：摸头偏 `wave/think`，戳肚子偏 `jump` |
| 连点 | 每次都触发，动作互相打断 | 700ms 内只算一次（`POKE_COOLDOWN_MS`） |
| Q 弹 | 无 | 按下即挤压回弹（260ms，峰值 1.085 / 0.905） |
| 悬停 | 只暂停 Zzz | 指针在左/右半边时朝鼠标微倾 ±1.6°，离开归零 |
| 双击收起 | `@dblclick`，触屏无效 | 手动 320ms 判定，鼠标和触屏都生效 |
| 快速甩出 | 一律当「放下」 | 松手速度 ≥ 0.45 px/ms 进 `throw`，有惯性、反弹、离地弧线 |

甩抛只复用 `drag` 帧条，**没有新增素材**，但读起来是「被甩出去」而不是「被放下」。

### 2.3 状态切换：一张表决定谁打断谁

改之前，优先级分散在 `beginDrag()` / `playJump()` / `playOneShot()` 各自的 if 里，
新增动作时很容易漏一个入口。现在只有一个门：`request(name)` → `canStart(current, next)`。

```js
drag 100 > land 60 > celebrate/failed 50 > throw 45 > jump 40 > think/wave 30 > walk 20 > sleep 5 > idle 0
```

三个容易踩的坑，这轮都写进规则里了：

1. **自然出口必须在「不可打断」判定之前**。
   改造中出现过一个真实 bug：`throw` 是不可打断的一次性动作，它到点要自己接 `land`，
   结果被自己挡住，桌宠冻在半空。修法就是 `NATURAL_EXITS = { jump:['land','idle'], throw:['land'], drag:['land'] }`
   优先放行。
2. **同名状态不重进**，否则每次 `request` 都重置进度，表现为「卡在第一帧」。
3. **被拒绝的一次性动作不丢**：进 `pendingOneShot`（最多 1 个），当前动作回 idle 后 30ms 补播。
   发布成功的庆祝撞上跳跃时，庆祝会在跳跃落地后补演，而不是被吞掉。

### 2.4 动作过渡：消除「换图感」

* **交叉淡化**：`MascotSprite.vue` 改成双 canvas。切素材时先把当前画面 1:1 复制到淡出层，
  新素材首帧画好后淡出层 150ms 渐隐。因为两套帧条同尺寸、脚底对齐，看起来是「换姿势」。
  `FADE_MS = 0` 或省电模式下退化为硬切。
* **走路的滑行感**：`WALK_SPEED` 不再手写常数，而是由剪辑推导
  `WALK_STRIDE_PX / (WALK_CLIP_FRAMES / WALK_FPS)` ≈ 33 px/s。改帧数或帧率，位移自动跟上。
* **倍速对齐**：一次性动作的收尾定时、跳跃时长都除以 `playbackRate`。
  改之前 2× 时动作播一半就被切回 idle。

### 2.5 循环与触发条件

* 循环 / 一次性由 `CLIPS[name].loop` 声明：`idle / sleep / walk / drag / throw` 循环，
  `jump / land / wave / celebrate / think / failed` 一次性。
* 一次性动作的收尾统一走 `enterState()` 里排的 settle 定时
  （`clipDuration(name) / playbackRate + 60ms`），不再每个方法各算一遍。
* `MANUAL_STATES`（idle/walk/jump/throw/drag/sleep）自己负责收尾，
  其余状态靠 settle 定时回 idle —— 这条区分避免了「idle 永远排 settle 定时」的空转。

---

## 3. 工程审查：发现的问题与处理

| # | 维度 | 问题 | 处理 |
|---|---|---|---|
| 1 | 代码结构 | 优先级判断散落在 4 个方法里，新增动作要改多处 | 抽 `actions.js`，一切切换走 `request()` |
| 2 | 代码结构 | `IDLE_VARIANTS` 定义在 `animations.js`（素材层），权重却是行为层的概念 | 移到 `actions.js`，`animations.js` 用 `export {} from` 保持旧导入可用 |
| 3 | 资源加载 | 图片缓存有两份（`MascotSprite` 一份 `imgCache`、`animations.js` 一份预热数组） | 合并到 `assets.js`，单一 `loadSheet()` + 单一 `failed` 集合 |
| 4 | 资源加载 | 预热一上来就并排拉 11 张帧条，弱网下和首屏抢带宽 | 分 `CORE_SHEETS` / `EXTRA_SHEETS` 两层，`saveData` / 2g-3g 只预热 core |
| 5 | 资源加载 | `new Image()` 解码在主线程，切状态会掉帧 | 优先 `createImageBitmap()`（worker 侧解码），不支持时回落 `<img>` |
| 6 | 资源加载 | 加载失败没有记忆，每次切状态都重发请求 | 失败重试一次（800ms）后记入 `failed`，后续直接跳过 |
| 7 | 性能 | 桌宠在页面里，父级任何 reflow 都会带着它重排 | `.mc-body` 加 `contain: layout paint style` |
| 8 | 性能 | 拖拽时每帧改 `left/top`，容器上的 `transition` 造成「拖尾」 | 原有 `.is-roaming { transition: none }` 之外，跳跃期间单独关 `transform` 过渡 |
| 9 | 性能 | 双 canvas 增加一个图层 | 淡出层只在切换时写一次，其余时间 `opacity: 0` 且 `pointer-events: none`（在 `.mc-canvas` 规则内），`prefers-reduced-motion` 下不参与合成 |
| 10 | 交互 | `pause()` 后 `lastInteractAt` 不推进，切回前台立刻打盹 | `resume()` 把挂起时长补进 `lastInteractAt` |
| 11 | 交互 | 睡着时的业务事件（保存成功）会被睡眠状态挡掉 | `onBlogSaved` 先 `wake(false)` 再播情绪动作 |
| 12 | 交互 | 拖拽 / 长按结束后浏览器仍派发 `click`，会误触单击反应 | 拖拽结束置标记，`onTap` 消费掉一次（原有逻辑，本轮保留并补注释） |
| 13 | 兼容性 | 触屏没有 `dblclick`，收起只能走菜单 | 手动 320ms 双击判定 |
| 14 | 兼容性 | 省电模式只冻帧，交叉淡化仍在跑 | `prefers-reduced-motion` 下 `FADE_MS → 0`、淡出层 `transition: none` |
| 15 | 兼容性 | 老存档没有 `nap` 字段 | `DEFAULT_SETTINGS.nap = true`，读出时补齐，不升版本号 |
| 16 | 可测性 | 整块逻辑只能靠肉眼看浏览器，回归靠人 | `npm run verify:mascot`（Node 里加载真实模块，29 条断言）+ `npm run verify:browser`（CDP 真事件，14 项） |

---

## 4. 逐文件改动

| 文件 | 行数 | 改动 |
|---|---|---|
| `src/mascot/actions.js` | 189 | **新增**。`ACTION_DEFS` 优先级表、`NATURAL_EXITS`、`canStart()`、`IDLE_VARIANTS`、`POKE_REACTIONS`、`behaviorWeights()`、`pickWeighted()`、`filterCooled()`、`createCooldowns()` |
| `src/mascot/assets.js` | 150 | **新增**。`loadSheet()`（ImageBitmap 优先 / 失败重试 / `failed` 记忆）、`preloadPrimary()`、分层 `preloadMotion()`、`clearSheetCache()` |
| `src/mascot/useMascotMotion.js` | 692 | **重写**。统一 `request()` 门禁 + `enterState()`；`pendingOneShot` 补播；`throwPet()` 甩抛物理；`pokeSquash()` / `pokeAt()`；`boredom` + `napCheck()` / `sleep()` / `wake()`；`hoverTilt()`；`pause/resume` 时间修正；`behaviorWeights` 驱动的 `behave()` |
| `src/mascot/animations.js` | 127 | 新增 `throw` 剪辑（复用 `drag` 帧条）；`sheetUrl` 转 export；`preloadMotion` 迁至 `assets.js` 并以 `export {} from './actions'` 兼容旧导入 |
| `src/mascot/config.js` | 257 | 新增「动作过渡 / 待机打盹 / 甩抛物理 / 点击部位」四组参数；`WALK_SPEED` 改为由步频推导；`DEFAULT_SETTINGS.nap`；`THROWN` 台词池 |
| `src/components/MascotSprite.vue` | 364 | 双 canvas 交叉淡化（`captureFade/revealFade`）；改用 `loadSheet()`；新增 `fadeMs` prop；GIF 分支与帧调度逻辑不变 |
| `src/components/Mascot.vue` | 786 | `napping` 与 `nightTime` 分离；`--mc-squash-*` 下发；`onHoverMove` 微倾；`onTap` 部位点击 + 手动双击；`onUp` 速度采样与甩抛；`wakeUp()`；菜单加「睡一会儿 / 叫醒 / 招个手」；`preloadPrimary + preloadMotion` |
| `src/components/MascotOverlays.vue` | 238 | `napping` prop；菜单按状态显示「睡一会儿 / 叫醒青禾」；设置区加「待机打盹」开关 |
| `src/assets/mascot.css` | 429 | `--mc-squash` 并入 `transform` + `transform-origin: 50% 100%` + `contain`；新增 `.mc-stage` / `.mc-canvas-fade`；`reduced-motion` 关淡化 |
| `scripts/verify_mascot_motion.mjs` | 207 | **新增**。`ssrLoadModule` 加载真实模块 + 假 timers/position，29 条断言 |
| `scripts/verify_mascot_browser.mjs` | 177 | **新增**。CDP 真事件驱动，14 项断言 + 截图 |
| `package.json` | — | 新增 `verify:mascot` / `verify:browser` |
| `docs/mascot-motion.md` | — | 状态表补 `throw` / `sleep`；新增 3.0–3.6（优先级、权重冷却、过渡、打盹、甩抛、点击部位）；素材加载与验证章节重写 |
| `docs/mascot-refactor.md` | — | 本文 |

**没有改**：主题色 / 立绘 / 台词池（除新增 `THROWN`）、菜单信息结构、`localStorage` 结构与 v3 迁移、
路由联动逻辑、`useMascotPosition` / `useMascotSpeech` / `useMascotLive` 的对外接口、后端与启动脚本。

---

## 5. 刻意没做的

* **不加第 12 个状态**：现有 11 段剪辑已覆盖主题所需，再加要重烘素材，属于无关扩张。
* **不做心情值 / 好感度 / 养成**：需要新 UI 和新存档字段，超出「动作表现」范围。
* **不做窗口边缘攀爬 / 重力掉落**：网页里没有桌面窗口概念，与现有靠边吸附语义冲突。
* **不引入动画库**（GSAP / anime.js）：现有 rAF + CSS 变量已够，且多一个依赖多一份首屏体积。
* **不换渲染层**（WebGL / Live2D）：canvas 2D 画 128×134 的帧条已远低于性能阈值。

---

## 6. 验证结果

### 6.1 构建

```
npm run build   ✓  built in 2.6s
dist/assets/index-BWeuXtx3.js   300.89 kB │ gzip: 108.14 kB
dist/assets/index-CbTNsOSJ.js     7.41 kB │ gzip:   2.79 kB   ← gifuct-js 独立 chunk
dist/assets/index-D7y8VSpz.css   96.36 kB │ gzip:  17.59 kB
```

33 张帧条按素材 id 独立产出；`gifuct-js` 仍是独立 chunk（≈7 KB），未进首屏。

### 6.2 状态机回归（`npm run verify:mascot`）

在 Node 里用 Vite `ssrLoadModule` 加载**真实**的 `actions.js` / `useMascotMotion.js`
（假 timers / position / `window.matchMedia`），29 条断言 **ALL PASS**，覆盖：
动作表优先级单调性、抢占 / 同级拒绝 / `force`、自然出口（jump/throw/drag → land）、
排队补播、甩抛衰减落定、打盹阈值与唤醒、冷却过筛、`pause/resume` 时间补偿。

> 这套测试抓到了本轮的 2 个真 bug：`throw` 被自身的「不可打断」挡住而冻在半空；
> 以及我一开始对「排队补播」的预期写错（实现是对的，断言是错的）。

### 6.3 真实浏览器（`npm run verify:browser`）

无头 Chrome 145 + CDP，`Input.dispatchMouseEvent` 打真事件，`/#/blog` 路由下 14 项全部通过：

```
OK   页面挂载桌宠
OK   桌宠尺寸正常 — 112x117
OK   canvas 已绘制人物 — alpha>8 像素 9073
OK   初始为待机/睡眠 — state-idle
OK   点击有动作反馈 — state-wave
OK   transform 生效（挤压/位移） — matrix(1.005, 0, 0, 0.994, …)
OK   快速甩动进入甩抛 — state-throw
OK   甩抛落定后恢复 — state-idle
OK   菜单存在「睡一会儿」 → 进入睡眠状态（Zzz 出现、state-sleep 生效）
OK   点击叫醒 — 退出 sleeping
OK   无 JS 运行时异常
```

截图留证：`mascot-verify.png`（叫醒后气泡「被你叫醒了，我刚刚打了个小盹」+ 悬停名片）。

### 6.4 排查记录：一次「页面白屏」的误判

浏览器验证第一次跑失败在「页面挂载桌宠」，排查后发现**不是代码问题**：
`node_modules/.vite/deps/` 的预构建产物损坏——`_metadata.json` 里登记了 `gifuct-js.js`
和若干 `chunk-*.js`，磁盘上却没有，于是任何 `import 'vue'` 的模块都 504，
整站白屏且无控制台报错。删掉 `node_modules/.vite` 重启 dev server 即恢复。

> 碰到「Vite 页面空白且无报错」时先查这个，别改业务代码。

---

## 7. 以后加一个动作要改哪

1. `animations.js` 的 `CLIPS` 加一条（素材名 / 帧数 / 帧率 / `loop`），并把帧条丢进 `src/assets/mascot/anim/`；
2. `actions.js` 的 `ACTION_DEFS` 给它优先级与 `oneShot`，需要自动接下一个动作就加进 `NATURAL_EXITS`；
3. `useMascotMotion.react()` 能直接播它（`react('新状态')`），业务侧不用改；
4. 想让它出现在待机抽取里，在 `behaviorWeights()` 里给个权重。

四处都在一张表或一个函数里，不需要再翻 `Mascot.vue` 的 if/else。
