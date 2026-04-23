const API_BASE = import.meta.env.VITE_API_BASE || 'http://127.0.0.1:3000'

type RequestOptions = RequestInit & {
  auth?: boolean
}

export async function request(url: string, options: RequestOptions = {}) {
  const { auth = true, headers, ...rest } = options
  const token = localStorage.getItem('TOKEN') || sessionStorage.getItem('TOKEN')

  const finalHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(headers as Record<string, string> || {}),
  }

  if (auth && token) {
    finalHeaders.Authorization = `Bearer ${token}`
  }

  const fullUrl = `${API_BASE}${url}`
  console.log('fetch 请求地址:', fullUrl)

  const res = await fetch(fullUrl, {
    ...rest,
    headers: finalHeaders,
  })

  const result = await res.json().catch(() => ({}))

  if (res.status === 401) {
    localStorage.removeItem('TOKEN')
    localStorage.removeItem('USER_INFO')
    window.location.href = '/login'
    throw new Error('登录已失效，请重新登录')
  }

  if (!res.ok) {
    throw new Error(result.msg || `请求失败: ${res.status}`)
  }

  if (result.code !== undefined && result.code !== 200) {
    throw new Error(result.msg || '请求失败')
  }

  return result
}