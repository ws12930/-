/**
 * 文件上传接口封装。
 * 注意：这里不能手动设置 Content-Type，交给浏览器自动带 multipart boundary。
 */
const BASE = '/api'

function getToken() {
  return localStorage.getItem('bn-token') || ''
}

/**
 * 上传单个文件。
 * @returns {Promise<{url:string,name:string,size:number,image:boolean}>}
 */
export async function uploadFile(file) {
  if (!file) throw new Error('请选择文件')

  const form = new FormData()
  form.append('file', file)

  let res
  try {
    res = await fetch(`${BASE}/upload`, {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + getToken() },
      body: form
    })
  } catch (e) {
    throw new Error('无法连接后端服务，请确认服务已启动（端口 8081）')
  }

  let data = null
  try {
    data = await res.json()
  } catch (e) {
    throw new Error('后端返回内容无法解析')
  }

  if (!res.ok || data.code !== 0) {
    throw new Error(data.message || '上传失败，请稍后重试')
  }
  return data.data
}
