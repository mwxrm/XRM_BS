<template>
  <div class="register-container">
    <el-card class="register-card" shadow="always">
      <template #header>
        <div class="card-header">
          <h2>注册新账号</h2>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister">
        <!-- 邮箱 -->
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱地址"
            :prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <!-- 图形验证码 -->
        <el-form-item prop="mathAnswer">
          <div class="captcha-row">
            <img :src="mathImage" alt="验证码" class="captcha-img" @click="refreshMath" />
            <el-input
              v-model="form.mathAnswer"
              placeholder="计算结果"
              size="large"
              style="width: 120px"
            />
          </div>
        </el-form-item>

        <!-- 邮箱验证码 -->
        <el-form-item prop="emailCode">
          <el-input v-model="form.emailCode" placeholder="邮箱验证码" size="large">
            <template #append>
              <el-button @click="sendEmailCode" :loading="sendingCode" :disabled="countdown > 0">
                {{ countdown > 0 ? `${countdown}秒后重发` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
            style="width: 100%"
          >
            注册
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button link @click="$router.push('/login')" style="width: 100%">
            已有账号？立即登录
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
// import { useUserStore } from '@/stores/user'
import type { FormInstance, FormRules } from 'element-plus'
import type { AxiosError } from 'axios'
import axios from '@/utils/axios'

const router = useRouter()
// const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMessage = ref('')
const sendingCode = ref(false)
const countdown = ref(0)
let timer: number | null = null

const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  mathAnswer: '',
  emailCode: '',
  mathToken: '',
})

const mathImage = ref('')

const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  mathAnswer: [{ required: true, message: '请输入验证码结果', trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}

// 获取算术验证码
const refreshMath = async () => {
  try {
    const res = await axios.get('/auth/captcha/math')
    mathImage.value = res.data.image
    form.mathToken = res.data.token
  } catch {
    ElMessage.error('获取验证码失败')
  }
}

// 发送邮箱验证码
const sendEmailCode = async () => {
  if (!form.mathToken || !form.mathAnswer) {
    ElMessage.warning('请先完成图形验证码')
    return
  }
  try {
    sendingCode.value = true
    await axios.post('/auth/captcha/send-email', {
      email: form.email,
      mathToken: form.mathToken,
      mathAnswer: form.mathAnswer,
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    // 倒计时
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '发送失败')
  } finally {
    sendingCode.value = false
  }
}

const handleRegister = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    errorMessage.value = ''
    await axios.post('/auth/register', {
      email: form.email,
      password: form.password,
      emailCode: form.emailCode,
      mathToken: form.mathToken,
      mathAnswer: parseInt(form.mathAnswer),
    })
    ElMessage.success('注册成功！请登录')
    router.push('/login')
  } catch (error) {
    const axiosError = error as AxiosError<{ message?: string }>
    errorMessage.value = axiosError.response?.data?.message || axiosError.message || '注册失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshMath()
})
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.register-card {
  width: 420px;
  border-radius: 12px;
}

.card-header {
  text-align: center;
  color: #333;
}

.card-header h2 {
  margin: 0;
  font-weight: 600;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.captcha-img {
  height: 40px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
