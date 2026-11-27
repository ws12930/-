## [14:28] - [功能补齐]: 桌宠「青禾」按代码备注补齐事件驱动情绪动作

- **文件**:
  - 修改 `7h/src/mascot/config.js`（新增 `SAVED_REACTIONS`：PUBLISHED→celebrate / DRAFT→think / ERROR→failed）
  - 修改 `7h/src/mascot/useMascotSpeech.js`（`normalizeSaveStatus` 三态归一化 + 保存三态台词，启用此前未使用的 `SAVE_FAILED`）
  - 修改 `7h/src/mascot/useMascotMotion.js`（一次性动作「播完回 idle」定时、跳跃位移时长随 `playbackRate` 缩放）
  - 修改 `7h/src/mascot/useMascotLive.js`（新增 `react` 回调：新通知 / 私信 → wave，签到升级 → celebrate）
  - 修改 `7h/src/components/Mascot.vue`（保存事件接线、`BUBBLE_MS` 场景台词保护窗口、登录跃迁 / 首屏 / 展开 / 恢复 → wave、菜单 wave 动作）
  - 修改 `7h/src/components/MascotOverlays.vue`（菜单新增「招个手」）
  - 修改 `7h/docs/mascot-motion.md`（状态机表补齐 9 个状态并同步帧率 / 素材统计，新增 3.1「事件 → 情绪动作」）
  - 环境：`7h/node_modules` 补齐缺失的 `gifuct-js`（package-lock 本就有条目，npm install 补装 2 包，否则 vite build 无法解析动态 import）
- **决策**: 按代码里留的备注把「素材 / 剪辑 / 入口已备好但没有调用点」的链路接完整——EditorView 保存失败分支注明「桌宠据此播『沮丧』动作并说一句安慰」、`react()` 注明 celebrate=任务完成 / failed=出错 / think=摸鱼 / wave=打招呼、animations.js 与 gen_mascot_frames.py 注明「参考 DSH 桌宠插件补齐的状态」。三态归一化集中在 useMascotSpeech、动作映射集中在 config，以后加保存结果不用去 Mascot.vue 补 if/else；保存后 EditorView 立刻跳路由，故在气泡停留期内挂起场景台词（表情仍跟随页面），避免「发布成功 / 没存上」被顶掉；动作只在台词真的说出口时附带，静音 / 收起 / 后台仍被防打扰门禁与 pause() 挡住；倍速修复让状态机计时与播放器帧延迟同除 playbackRate，修掉 2× 动作播一半被切回 idle、0.5× 提前空等。
- **验证**: `npm run build` ✓（98 modules，33 张帧条全部产出，GIF 解码器仍是独立 chunk）；函数级自检 ✓（normalizeSaveStatus 6 组用例含大小写 / 空值、SAVED_REACTIONS 3 组映射、SAVE_FAILED / POSTED / DRAFTED 台词池非空）；未做浏览器实测，事件触发链路靠静态接线检查确认。
