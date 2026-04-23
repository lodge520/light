import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://127.0.0.1:3000',
  timeout: 10000,
})

console.log('VITE_API_BASE =', import.meta.env.VITE_API_BASE)
console.log('axios baseURL =', http.defaults.baseURL)

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('TOKEN')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
   console.log('请求地址:', `${config.baseURL || ''}${config.url || ''}`)

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API error:', error)

    if (error?.response?.status === 401) {
      window.localStorage.removeItem('TOKEN')
      window.localStorage.removeItem('USERNAME')
      window.localStorage.removeItem('STORE_NAME')

      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  },
)

export default http