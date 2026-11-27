/**
 * 临时 CDP 验证：真实 Chrome 里打开 /#/blog，驱动桌宠的点击 / 甩抛 / 睡眠 / 唤醒。
 */
import { writeFileSync } from 'node:fs'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const BASE = 'http://127.0.0.1:5199/#/blog'
const OUT = 'C:/Users/Administrator/WorkBuddy/2026-11-27-13-56-28/mascot-verify.png'

const targets = await (await fetch('http://127.0.0.1:9333/json/list')).json()
const target = targets.find((t) => t.type === 'page')
if (!target) throw new Error('no page target')

const ws = new WebSocket(target.webSocketDebuggerUrl)
let seq = 0
const pending = new Map()
const errors = []
ws.onmessage = (ev) => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) {
    const { resolve, reject } = pending.get(msg.id)
    pending.delete(msg.id)
    if (msg.error) reject(new Error(JSON.stringify(msg.error)))
    else resolve(msg.result)
    return
  }
  if (msg.method === 'Runtime.exceptionThrown') {
    errors.push(msg.params.exceptionDetails?.exception?.description || msg.params.exceptionDetails?.text || 'exception')
  }
  if (msg.method === 'Runtime.consoleAPICalled' && msg.params.type === 'error') {
    errors.push(msg.params.args.map((a) => a.description || a.value).join(' '))
  }
}
await new Promise((r) => (ws.onopen = r))

function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++seq
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params }))
  })
}
async function js(expression) {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text)
  return r.result.value
}

let failed = 0
function check(name, ok, extra = '') {
  if (ok) console.log('OK  ' + name + (extra ? ' — ' + extra : ''))
  else {
    failed += 1
    console.error('FAIL ' + name + (extra ? ' — ' + extra : ''))
  }
}

try {
  await send('Page.enable')
  await send('Runtime.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 800, deviceScaleFactor: 1, mobile: false })
  await send('Page.navigate', { url: BASE })

  // 等桌宠挂载
  let ready = false
  for (let i = 0; i < 60; i++) {
    await sleep(250)
    ready = await js("!!document.querySelector('.mc-body')").catch(() => false)
    if (ready) break
  }
  check('页面挂载桌宠', ready === true)
  await sleep(1200)

  const rect = await js("(() => { const r = document.querySelector('.mc-body').getBoundingClientRect(); return {x:r.x,y:r.y,w:r.width,h:r.height} })()")
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

  const idleState = await js("document.querySelector('.mc-body').className")
  check('初始为待机状态', /state-idle|state-sleep/.test(idleState), idleState)

  // 点击上半身：应有反应（wave / think / jump）+ Q 弹
  const cx = rect.x + rect.w / 2
  const headY = rect.y + rect.h * 0.3
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: cx, y: headY })
  await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: cx, y: headY, button: 'left', clickCount: 1, buttons: 1 })
  await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: cx, y: headY, button: 'left', clickCount: 1, buttons: 0 })
  await sleep(180)
  const clickState = await js("document.querySelector('.mc-body').className")
  check('点击有动作反馈', /state-(wave|think|jump)/.test(clickState), clickState)
  const squash = await js("getComputedStyle(document.querySelector('.mc-body')).transform")
  check('transform 已包含挤压/位移', typeof squash === 'string' && squash !== 'none', squash)

  await sleep(1100)

  // 拖拽 + 快速甩出：应进入 throw
  const r2 = await js("(() => { const r = document.querySelector('.mc-body').getBoundingClientRect(); return {x:r.x,y:r.y,w:r.width,h:r.height} })()")
  const sx = r2.x + r2.w / 2
  const sy = r2.y + r2.h / 2
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: sx, y: sy })
  await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: sx, y: sy, button: 'left', clickCount: 1, buttons: 1 })
  for (let i = 1; i <= 7; i++) {
    await send('Input.dispatchMouseEvent', {
      type: 'mouseMoved', x: sx - i * 26, y: sy - i * 3, button: 'left', buttons: 1
    })
    await sleep(8)
  }
  await send('Input.dispatchMouseEvent', {
    type: 'mouseReleased', x: sx - 182, y: sy - 21, button: 'left', clickCount: 1, buttons: 0
  })
  await sleep(60)
  const throwState = await js("document.querySelector('.mc-body').className")
  check('快速甩动进入甩抛', /state-throw/.test(throwState), throwState)
  await sleep(1400)
  const afterThrow = await js("document.querySelector('.mc-body').className")
  check('甩抛落定后恢复', /state-(idle|land|sleep)/.test(afterThrow), afterThrow)

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
  const sleepClass = await js("document.querySelector('.mascot').className")
  check('进入睡眠状态', /sleeping/.test(sleepClass), sleepClass)
  check('Zzz 出现', (await js("!!document.querySelector('.mc-zzz')")) === true)
  const sleepCanvas = await js("document.querySelector('.mc-body').className")
  check('睡眠剪辑生效', /state-sleep/.test(sleepCanvas), sleepCanvas)

  // 点一下叫醒
  const r3 = await js("(() => { const r = document.querySelector('.mc-body').getBoundingClientRect(); return {x:r.x,y:r.y,w:r.width,h:r.height} })()")
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: r3.x + r3.w / 2, y: r3.y + r3.h / 2 })
  await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: r3.x + r3.w / 2, y: r3.y + r3.h / 2, button: 'left', clickCount: 1, buttons: 1 })
  await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: r3.x + r3.w / 2, y: r3.y + r3.h / 2, button: 'left', clickCount: 1, buttons: 0 })
  await sleep(400)
  const wokeClass = await js("document.querySelector('.mascot').className")
  check('点击叫醒', !/sleeping/.test(wokeClass), wokeClass)

  // 截图留证
  const shot = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(OUT, Buffer.from(shot.data, 'base64'))
  console.log('截图：' + OUT)

  const realErrors = errors.filter((e) => !/Failed to fetch|ERR_CONNECTION|NetworkError|net::/i.test(e))
  check('无 JS 运行时异常', realErrors.length === 0, realErrors.slice(0, 3).join(' | '))
} catch (e) {
  failed += 1
  console.error('EXCEPTION', e)
} finally {
  ws.close()
}

console.log(failed === 0 ? '\nALL PASS' : `\n${failed} FAILED`)
process.exit(failed === 0 ? 0 : 1)
