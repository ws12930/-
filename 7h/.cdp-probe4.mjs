const sleep=(ms)=>new Promise(r=>setTimeout(r,ms))
const targets=await (await fetch('http://127.0.0.1:9333/json/list')).json()
const t=targets.find(x=>x.type==='page')
const ws=new WebSocket(t.webSocketDebuggerUrl)
let seq=0;const pend=new Map()
ws.onmessage=(ev)=>{const m=JSON.parse(ev.data);if(m.id&&pend.has(m.id)){const{resolve,reject}=pend.get(m.id);pend.delete(m.id);m.error?reject(new Error(JSON.stringify(m.error))):resolve(m.result)}}
await new Promise(r=>ws.onopen=r)
const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pend.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}))})
const js=async(e)=>{const r=await send('Runtime.evaluate',{expression:e,returnByValue:true,awaitPromise:true,userGesture:true});if(r.exceptionDetails)return 'ERR: '+(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value}
await send('Runtime.enable')
const mods=['/src/main.js','/src/App.vue','/src/router/index.js','/src/components/Mascot.vue','/src/mascot/useMascotMotion.js','/src/mascot/actions.js','/src/mascot/assets.js','/src/mascot/animations.js','/src/mascot/useMascotLive.js']
for(const m of mods){
  const r=await js(`import('${m}').then(()=>'ok').catch(e=>'FAIL '+e.message)`)
  const s=await js(`fetch('${m}').then(r=>r.status)`)
  console.log(m.padEnd(38), 'status='+s, r)
}
ws.close();process.exit(0)
