/**
 * 真实浏览器验证（可选，需要手工起服务）
 *
 * 1) 起开发服务器：npm run dev -- --port 5199 --strictPort
 * 2) 起无头 Chrome：
 *      chrome --headless=new --disable-gpu --remote-debugging-port=9333 \
 *             --user-data-dir=%TEMP%/mascot-chrome
 * 3) 跑：npm run verify:browser
 *
 * 环境变量：MASCOT_URL（默认 http://127.0.0.1:5199/#/blog）、CDP_PORT（默认 9333）
 *
 * 只用 Node 内置 WebSocket + CDP，不依赖 puppeteer。
 */
import { writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const CDP_PORT = process.env.CDP_PORT || 9333
const URL_UNDER_TEST = process.env.MASCOT_URL || 'http://127.0.0.1:5199/#/blog'
// 截图默认丢临时目录，避免污染仓库；需要留证时用 MASCOT_SHOT 指定路径
const OUT = process.env.MASCOT_SHOT || join(tmpdir(), 'mascot-verify.png')

const targets = await (await fetch(`http://127.0.0.1:${CDP_PORT}/json/list`)).json()
const target = targets.find((t) => t.type === 'page')
if (!target) throw new Error('CDP 里没有 page target')

const ws = new WebSocket(target.webSocketDebuggerUrl)
let seq = 0
const pending = new Map()
const errors = []
ws.onmessage = (ev) => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) {
    const { resolve: ok, reject } = pending.get(msg.id)
    pending.delete(msg.id)
    if (msg.error) reject(new Error(JSON.stringify(msg.error)))
    else ok(msg.result)
    return
  }
  if (msg.method === 'Runtime.exceptionThrown') {
    errors.push(
      msg.params.exceptionDetails?.exception?.description ||
        msg.params.exceptionDetails?.text ||
        'exception'
    )
  }
  if (msg.method === 'Runtime.consoleAPICalled' && msg.params.type === 'error') {
    errors.push(msg.params.args.map((a) => a.description || a.value).join(' '))
  }
}
await new Promise((r) => (ws.onopen = r))

function send(method, params = {}) {
  return new Promise((ok, reject) => {
    const id = ++seq
    pending.set(id, { resolve: ok, reject })
    ws.send(JSON.stringify({ id, method, params }))
  })
}
async function js(expression) {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text)
  return r.result.value
}
const rectOf = (sel = '.mc-body') =>
  js(`(() => { const e = document.querySelector('${sel}'); if (!e) return null;
      const r = e.getBoundingClientRect(); return { x: r.x, y: r.y, w: r.width, h: r.height } })()`)
const cls = (sel = '.mc-body') => js(`(document.querySelector('${sel}')?.className) || ''`)

let failed = 0
function check(name, ok, extra = '') {
  if (ok) console.log('OK   ' + name + (extra ? ' — ' + extra : ''))
  else {
    failed += 1
    console.error('FAIL ' + name + (extra ? ' — ' + extra : ''))
  }
}

async function click(x, y) {
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y })
  await send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1, buttons: 1 })
  await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1, buttons: 0 })
}

try {
  await send('Page.enable')
  await send('Runtime.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 800, deviceScaleFactor: 1, mobile: false })
  await send('Page.navigate', { url: URL_UNDER_TEST })

  let ready = false
  for (let i = 0; i < 60; i++) {
    await sleep(250)
    ready = await js("!!document.querySelector('.mc-body')").catch(() => false)
    if (ready) break
  }
  check('页面挂载桌宠', ready === true, ready ? '' : '未找到 .mc-body，检查控制台/依赖预构建缓存')
  if (!ready) throw new Error('桌宠未挂载，终止后续断言')
  await sleep(1200)

  const rect = await rectOf()
  check('桌宠尺寸正常', rect.w > 40 && rect.h > 60, `${Math.round(rect.w)}x${Math.round(rect.h)}`)

  const painted = await js(`(() => {
    const c = document.querySelector('.mc-body canvas.mc-canvas:not(.mc-canvas-fade)')
    if (!c) return -1
    const d = c.getContext('2d').getImageData(0, 0, c.width, c.height).data
    let n = 0
    for (let i = 3; i < d.length; i += 4) if (d[i] > 8) n++
    return n
  })()`)
  check('canvas 已绘制人物', painted > 500, `alpha>8 像素 ${painted}`)

  check('初始为待机/睡眠', /state-(idle|sleep)/.test(await cls()), await cls())

  // 点上半身 → 摸头反应
  const cx = rect.x + rect.w / 2
  const headY = rect.y + rect.h * 0.3
  await click(cx, headY)
  await sleep(180)
  check('点击有动作反馈', /state-(wave|think|jump)/.test(await cls()), await cls())
  const tf = await js("getComputedStyle(document.querySelector('.mc-body')).transform")
  check('transform 生效（挤压/位移）', typeof tf === 'string' && tf !== 'none', tf)
  await sleep(1100)

  // 拖拽 + 快速甩出 → throw → 落定
  const r2 = await rectOf()
  const sx = r2.x + r2.w / 2
  const sy = r2.y + r2.h / 2
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: sx, y: sy })
  await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: sx, y: sy, button: 'left', clickCount: 1, buttons: 1 })
  for (let i = 1; i <= 7; i++) {
    await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: sx - i * 26, y: sy - i * 3, button: 'left', buttons: 1 })
    await sleep(8)
  }
  await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: sx - 182, y: sy - 21, button: 'left', clickCount: 1, buttons: 0 })
  await sleep(60)
  check('快速甩动进入甩抛', /state-throw/.test(await cls()), await cls())
  await sleep(1400)
  check('甩抛落定后恢复', /state-(idle|land|sleep)/.test(await cls()), await cls())

  // 菜单 → 睡一会儿
  await js("document.querySelector('.mc-more').click()")
  await sleep(220)
  const slept = await js(`(() => {
    const b = Array.from(document.querySelectorAll('.mc-menu button')).find((x) => x.textContent.includes('睡一会儿'))
    if (b) b.click()
    return !!b
  })()`)
  check('菜单存在「睡一会儿」', slept === true)
  await sleep(300)
  check('进入睡眠状态', /sleeping/.test(await cls('.mascot')), await cls('.mascot'))
  check('Zzz 出现', (await js("!!document.querySelector('.mc-zzz')")) === true)
  check('睡眠剪辑生效', /state-sleep/.test(await cls()), await cls())

  // 点一下叫醒
  const r3 = await rectOf()
  await click(r3.x + r3.w / 2, r3.y + r3.h / 2)
  await sleep(400)
  check('点击叫醒', !/sleeping/.test(await cls('.mascot')), await cls('.mascot'))

  const shot = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(OUT, Buffer.from(shot.data, 'base64'))
  console.log('截图：' + OUT)

  const real = errors.filter((e) => !/Failed to fetch|ERR_CONNECTION|NetworkError|net::/i.test(e))
  check('无 JS 运行时异常', real.length === 0, real.slice(0, 3).join(' | '))
} catch (e) {
  failed += 1
  console.error('EXCEPTION', e)
} finally {
  ws.close()
}

console.log(failed === 0 ? '\nALL PASS' : `\n${failed} FAILED`)
process.exit(failed === 0 ? 0 : 1)
