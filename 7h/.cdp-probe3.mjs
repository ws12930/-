const sleep=(ms)=>new Promise(r=>setTimeout(r,ms))
const targets=await (await fetch('http://127.0.0.1:9333/json/list')).json()
const t=targets.find(x=>x.type==='page')
const ws=new WebSocket(t.webSocketDebuggerUrl)
let seq=0;const pend=new Map()
ws.onmessage=(ev)=>{const m=JSON.parse(ev.data);if(m.id&&pend.has(m.id)){const{resolve,reject}=pend.get(m.id);pend.delete(m.id);m.error?reject(new Error(JSON.stringify(m.error))):resolve(m.result)}}
await new Promise(r=>ws.onopen=r)
const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pend.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}))})
const js=async(e)=>{const r=await send('Runtime.evaluate',{expression:e,returnByValue:true,awaitPromise:true,userGesture:true});if(r.exceptionDetails)return 'ERR: '+(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value}
await send('Page.enable')
await send('Page.addScriptToEvaluateOnNewDocument',{source:`
window.__errs=[];
window.addEventListener('error',e=>window.__errs.push('ERROR: '+(e.message||'')+' @ '+(e.filename||'')+':'+(e.lineno||'')));
window.addEventListener('unhandledrejection',e=>window.__errs.push('REJECT: '+(e.reason&&(e.reason.stack||e.reason.message)||e.reason)));
`})
await send('Page.navigate',{url:'http://127.0.0.1:5199/'})
await sleep(7000)
console.log('errs:',JSON.stringify(await js('window.__errs'),null,1))
console.log('vueApp:',await js("!!document.querySelector('#app').__vue_app__"))
console.log('appLen:',await js("document.querySelector('#app').innerHTML.length"))
ws.close();process.exit(0)
