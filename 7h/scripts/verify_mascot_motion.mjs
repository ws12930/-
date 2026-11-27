/**
 * 桌宠动作状态机的回归验证（不需要浏览器）。
 *
 * 做法：用 Vite 的 ssrLoadModule 加载真实的 useMascotMotion / actions，
 * 把 timers、position、window 换成可控的假实现，然后驱动点击、拖拽、甩抛、
 * 打盹、唤醒、优先级抢占等路径，断言状态迁移与兜底行为。
 *
 *   npm run verify:mascot
 *
 * 覆盖不到的部分：canvas 绘制、交叉淡化的实际观感、pointer 手势 —— 这些需要浏览器，
 * 见 docs/mascot-refactor.md 的「验证结果」一节。
 */
import { createServer } from 'vite'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

globalThis.window = {
  innerWidth: 1280,
  innerHeight: 800,
  matchMedia: () => ({
    matches: false,
    addEventListener() {},
    removeEventListener() {},
    addListener() {},
    removeListener() {}
  }),
  setTimeout,
  clearTimeout,
  setInterval,
  clearInterval,
  requestAnimationFrame: (cb) => setTimeout(() => cb(performance.now()), 16),
  cancelAnimationFrame: (id) => clearTimeout(id)
}

const server = await createServer({
  root: ROOT,
  server: { middlewareMode: true },
  appType: 'custom',
  logLevel: 'error'
})

let failed = 0
function check(name, ok, extra = '') {
  if (ok) console.log('OK  ' + name)
  else {
    failed += 1
    console.error('FAIL ' + name + (extra ? ' — ' + extra : ''))
  }
}

try {
  const { useMascotMotion } = await server.ssrLoadModule('/src/mascot/useMascotMotion.js')
  const {
    ACTION_DEFS, behaviorWeights, canStart, createCooldowns
  } = await server.ssrLoadModule('/src/mascot/actions.js')

  // ---- 动作表 / 权重 / 优先级（纯逻辑） ----
  const night = behaviorWeights({ route: 'home', hour: 2, boredom: 0, roam: true })
  check('深夜不安排走动', !night.some((x) => x.state === 'walk'))
  check('深夜有打盹权重', night.some((x) => x.state === 'sleep' && x.weight > 0))
  const editor = behaviorWeights({ route: 'editor', hour: 14, boredom: 0, roam: true })
  const home = behaviorWeights({ route: 'home', hour: 14, boredom: 0, roam: true })
  check(
    '编辑器页发呆权重更高',
    editor.find((x) => x.state === 'think').weight > home.find((x) => x.state === 'think').weight
  )
  check('关掉自由走动后不抽到 walk', !behaviorWeights({ route: 'home', hour: 14, roam: false }).some((x) => x.state === 'walk'))
  check('跳跃不可被 celebrate 抢占', canStart('jump', 'celebrate') === false)
  check('甩抛可自然接落地', canStart('throw', 'land') === true)
  check('拖拽优先级最高', canStart('walk', 'drag') === true && canStart('drag', 'celebrate') === false)
  check(
    '优先级顺序 drag > land > celebrate > throw > jump > wave > walk > idle',
    ACTION_DEFS.drag.priority > ACTION_DEFS.land.priority &&
      ACTION_DEFS.land.priority > ACTION_DEFS.celebrate.priority &&
      ACTION_DEFS.celebrate.priority > ACTION_DEFS.throw.priority &&
      ACTION_DEFS.throw.priority > ACTION_DEFS.jump.priority &&
      ACTION_DEFS.jump.priority > ACTION_DEFS.wave.priority &&
      ACTION_DEFS.wave.priority > ACTION_DEFS.walk.priority &&
      ACTION_DEFS.walk.priority > ACTION_DEFS.idle.priority
  )
  const cd = createCooldowns(1000)
  check('冷却：首次可用', cd.ready('wave') === true)
  cd.mark('wave')
  check('冷却：标记后不可用', cd.ready('wave') === false)

  // 假 timers：timeout / rAF 都走真实定时器，clear 生效，便于观察动作收尾
  const timeouts = new Map()
  let seq = 0
  const timers = {
    timeout(fn, ms) {
      const id = ++seq
      const h = setTimeout(() => {
        timeouts.delete(id)
        fn()
      }, ms)
      timeouts.set(id, h)
      return id
    },
    interval(fn, ms) {
      const id = ++seq
      timeouts.set(id, setInterval(fn, ms))
      return id
    },
    raf(fn) {
      const id = ++seq
      timeouts.set(id, setTimeout(() => fn(performance.now()), 16))
      return id
    },
    clear(id) {
      const h = timeouts.get(id)
      if (h) {
        clearTimeout(h)
        clearInterval(h)
        timeouts.delete(id)
      }
    },
    clearAll() {
      timeouts.forEach((h) => {
        clearTimeout(h)
        clearInterval(h)
      })
      timeouts.clear()
    },
    pause() {},
    resume() {}
  }

  const pos = { value: { x: 600, y: 500 } }
  const position = {
    pos,
    dims: () => ({ w: 112, h: 190 }),
    defaultPos: () => ({ x: 600, y: 500 }),
    clampPos: (p) => p,
    snap() {},
    save() {}
  }

  const settings = { eco: false, motion: true, roam: true, nap: true, speed: 1 }
  const mouth = { ref: (v) => ({ value: v }) }

  const m = useMascotMotion({
    timers,
    position,
    settings,
    isMini: mouth.ref(false),
    isSleepy: mouth.ref(false),
    routeName: mouth.ref('home'),
    faceKey: mouth.ref('welcome')
  })

  check('初始 idle', m.state.value === 'idle', m.state.value)

  // 点击（上半身）：应该给一个反应，并触发 Q 弹
  const poked = m.pokeAt(0.2)
  check('点击有反应', poked === true, String(poked))
  check('点击后进入一次性动作', ['wave', 'think', 'jump'].includes(m.state.value), m.state.value)
  check('Q 弹挤压生效', m.squashX.value !== 1 || m.squashY.value !== 1)
  await sleep(1300)
  check('一次性动作播完回 idle', m.state.value === 'idle', m.state.value)
  check('确认挤压已回正', Math.abs(m.squashX.value - 1) < 1e-6 && Math.abs(m.squashY.value - 1) < 1e-6)

  // 拖拽：最高优先级，业务动作不能抢
  m.beginDrag()
  check('拖拽接管', m.state.value === 'drag', m.state.value)
  check('拖拽中 celebrate 被拒', m.react('celebrate') === false, m.state.value)

  // 快速甩出：进入 throw，落定后回 idle
  m.endDrag(2.4, 0)
  check('快甩进入 throw', m.state.value === 'throw', m.state.value)
  await sleep(1400)
  check('甩抛落定后回 idle', m.state.value === 'idle', m.state.value)

  // 打盹 / 唤醒
  check('可以入睡', m.sleep() === true, m.state.value)
  check('入睡状态为 sleep', m.state.value === 'sleep' && m.sleeping.value === true, m.state.value)
  check('可以叫醒', m.wake(true) === true, m.state.value)
  check('叫醒后回到 idle/wave', ['idle', 'wave'].includes(m.state.value), m.state.value)
  await sleep(700)

  // 优先级：跳跃不可打断
  m.playJump(18, true)
  const celebrateBlocked = m.react('celebrate') === false
  check('跳跃期间 celebrate 被排队而非抢占', celebrateBlocked, m.state.value)
  await sleep(1000)
  check('跳跃结束后补播排队动作', m.state.value === 'celebrate', m.state.value)
  await sleep(900)
  check('排队动作播完回 idle', m.state.value === 'idle', m.state.value)

  // 暂停 / 恢复
  m.pause()
  check('暂停后 tick 停住', m.frozen.value === true)
  m.resume()
  check('恢复后不再冻结', m.frozen.value === false)

  timers.clearAll()
} catch (e) {
  failed += 1
  console.error('EXCEPTION', e)
} finally {
  await server.close()
}

console.log(failed === 0 ? '\nALL PASS' : `\n${failed} FAILED`)
process.exit(failed === 0 ? 0 : 1)
