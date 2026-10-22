import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from '@/utils/axios'
import { isAxiosError } from 'axios'

interface User {
  id: number
  username: string
  email: string
  role: string
  createdAt: string
  updatedAt: string
}

interface LoginResponse {
  token: string
  message?: string
  user: User
}

interface RegisterResponse {
  id: number
  username: string
  email: string
  role: string
  createdAt: string
  updatedAt: string
}

interface LoginData {
  email: string
  password: string
}

interface RegisterData {
  email: string
  password: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<User | null>(null)
  const isAuthenticated = ref<boolean>(!!token.value)

  // 登录
  const login = async (credentials: LoginData): Promise<LoginResponse> => {
    try {
      console.log('发送登录请求:', credentials)

      // 现在 axios 返回的是完整的响应对象
      const response = await axios.post('/auth/login', credentials)
      console.log('收到响应:', response)

      // 从 response.data 中获取实际数据
      const responseData = response.data
      console.log('响应数据:', responseData)

      localStorage.setItem('user', JSON.stringify(responseData.user))

      if (responseData.token && responseData.user) {
        const { token: responseToken, user: responseUser } = responseData

        // 保存token和用户信息
        token.value = responseToken
        user.value = responseUser
        isAuthenticated.value = true
        localStorage.setItem('token', responseToken)

        console.log('登录成功，token已保存')
        return responseData
      } else {
        console.error('响应格式错误:', responseData)
        throw new Error('登录响应格式错误')
      }
    } catch (error) {
      console.error('登录失败:', error)
      throw error
    }
  }
  // 注册
  const register = async (data: RegisterData): Promise<RegisterResponse> => {
    try {
      const response = await axios.post('/auth/register', data)
      return response.data // 返回 response.data 而不是整个 response
    } catch (error) {
      throw error
    }
  }

  const getCurrentUser = async (): Promise<User> => {
    try {
      const response = await axios.get('/auth/user')
      const userData = response.data

      // 现在后端已经不会返回密码字段，所以直接使用
      user.value = userData
      isAuthenticated.value = true
      return userData
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 如果获取用户信息失败，清除 token
      logout()
      throw error
    }
  }
  // 登出
  const logout = () => {
    token.value = ''
    user.value = null
    isAuthenticated.value = false
    localStorage.removeItem('token')
  }

  // 初始化时尝试获取用户信息
  const init = async () => {
    const storedToken = localStorage.getItem('token')
    if (storedToken) {
        try {
            await getCurrentUser()
        } catch (error: unknown) {
            console.error('初始化用户信息失败:', error)
            // 使用 isAxiosError 安全地判断状态码
            if (isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) {
                logout()
            }
        }
    }
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    register,
    getCurrentUser,
    logout,
    init,
  }
})
