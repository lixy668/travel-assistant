import axios from 'axios'

const request = axios.create({ baseURL: '/api', timeout: 60000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (res) => res.data,
  (err) => Promise.reject(new Error(err.response?.data?.msg || '网络错误')),
)

export async function fetchStream(url, data, onChunk, onComplete, onError) {
  const controller = new AbortController()
  try {
    const response = await fetch(`/api/travel/${url}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify(data),
      signal: controller.signal,
    })
    if (!response.ok) throw new Error(`请求失败，状态码: ${response.status}`)
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        if (onComplete) onComplete()
        break
      }
      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n').filter((l) => l.trim() !== '')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const jsonStr = line.substring(5).trim()
          if (jsonStr === '[DONE]') {
            if (onComplete) onComplete()
            return
          }
          try {
            const parsed = JSON.parse(jsonStr)
            if (onChunk) onChunk(parsed)
          } catch (e) {
            console.warn('解析JSON片段失败:', e, jsonStr)
          }
        }
      }
    }
  } catch (e) {
    if (onError) onError(e)
  }
}

export default request
