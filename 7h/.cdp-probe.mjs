/** 临时诊断：打印页面实际渲染内容与控制台异常 */
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const targets = await (await fetch('http://127.0.0.1:9333/json/list')).json()
const target = targets.find((t) => t.type === 'page')
const ws = new WebSocket(target.webSocketDebuggerUrl)
let seq = 0
const pending = new Map()
const logs = []
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
    logs.push('EXC ' + (msg.params.exceptionDetails?.exception?.description || msg.params.exceptionDetails?.text))
  }
  if (msg.method === 'Runtime.consoleAPICalled') {
    logs.push('LOG[' + msg.params.type + '] ' + msg.params.args.map((a) => a.description || a.value).join(' '))
  }
}
await new Promise((r) => (ws.onopen = r))
const send = (method, params = {}) =>
  new Promise((resolve, reject) => {
    const id = ++seq
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params }))
  })
const js = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true, userGesture: true })
  if (r.exceptionDetails) return 'ERR: ' + (r.exceptionDetails.exception?.description || r.exceptionDetails.text)
  return r.result.value
}

await send('Page.enable')
await send('Runtime.enable')
await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 800, deviceScaleFactor: 1, mobile: false })
await send('Page.navigate', { url: 'http://127.0.0.1:5199/' })
await sleep(6000)

console.log('TITLE   :', await js('document.title'))
console.log('URL     :', await js('location.href'))
console.log('#app len:', await js("(document.querySelector('#app')?.innerHTML || '').length"))
console.log('body txt:', JSON.stringify((await js('document.body.innerText')) || '').slice(0, 300))
console.log('mc-body :', await js("!!document.querySelector('.mc-body')"))
console.log('mascot  :', await js("!!document.querySelector('.mascot')"))
console.log('mc-dot  :', await js("!!document.querySelector('.mc-dot')"))
console.log('mc-restore:', await js("!!document.querySelector('.mc-restore')"))
console.log('html head:', JSON.stringify((await js("document.body.innerHTML")) || '').slice(0, 700))
console.log('--- logs ---')
console.log(logs.slice(0, 25).join('\n'))
ws.close()
process.exit(0)
