import axios from 'axios'

// 创建 axios 实例
const instance = axios.create({
  // baseURL: 'http://localhost:8080/api',
  baseURL: import.meta.env.VITE_API_BASE_URL + '/api', // 使用环境变量
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 - 添加 token
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    } else {
      console.warn('没有找到token，请求头未设置Authorization')
    }
    return config
  },
  (error) => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  },
)

// 响应拦截器 - 重要修改：返回完整的 response，而不是 response.data
instance.interceptors.response.use(
  (response) => {
    return response
  },
    // 返回完整的响应对象，让调用方处理数据结构
  (error) => {
    if (error.response?.status === 401) {
      // token 过期或无效
      console.warn('Token无效或过期:', error.config.url)
      localStorage.removeItem('token')

      // 避免在登录页或注册页时跳转
      const currentPath = window.location.pathname
      if (currentPath !== '/login' && currentPath !== '/register') {
        // 延迟跳转，让错误信息有显示的机会
        setTimeout(() => {
          window.location.href = '/login'
        }, 1000)
      }
    }
    console.error('响应错误:', error.response?.status, error.response?.data || error.message)
    return Promise.reject(error)
  },
)

export default instance
