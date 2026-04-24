import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://127.0.0.1:3000',
  timeout: 10000,
})

console.log('VITE_API_BASE =', import.meta.env.VITE_API_BASE)
console.log('axios baseURL =', http.defaults.baseURL)

http.interceptors.request.use((config) => {
  const token =
    window.localStorage.getItem('TOKEN') ||
    window.sessionStorage.getItem('TOKEN')

  config.headers = config.headers || {}

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  console.log('请求地址:', `${config.baseURL || ''}${config.url || ''}`)
  console.log('axios token =', token)

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API error:', error)

    if (error?.response?.status === 401) {
      window.localStorage.removeItem('TOKEN')
      window.localStorage.removeItem('USER_INFO')
      window.localStorage.removeItem('STORE_NAME')
      window.localStorage.removeItem('storeSetup')

      window.sessionStorage.removeItem('TOKEN')
      window.sessionStorage.removeItem('USER_INFO')
      window.sessionStorage.removeItem('STORE_NAME')
      window.sessionStorage.removeItem('storeSetup')

      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  },
)

export default http