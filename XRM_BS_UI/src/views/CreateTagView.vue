<template>
  <div class="tag-create-container">
    <el-container>
      <el-main>
        <el-card class="tag-card">
          <template #header>
            <h2>创建新标签</h2>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
            <el-form-item label="标签名称" prop="name">
              <el-input v-model="form.name" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="标签类型" prop="category">
              <el-radio-group v-model="form.category">
                <el-radio value="article">文章标签</el-radio>
                <el-radio value="setting">设定标签</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item
              v-if="form.category === 'setting'"
              label="设定分类"
              prop="settingCategory"
            >
              <el-select v-model="form.settingCategory" placeholder="请选择设定分类">
                <el-option label="人物" value="character" />
                <el-option label="物品" value="item" />
                <el-option label="地区" value="location" />
                <el-option label="背景" value="background" />
                <el-option label="规则" value="rule" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
            <el-form-item label="标签描述" prop="description">
              <div class="editor-wrapper">
                <RichTextEditor v-model="form.description" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitForm" :loading="loading">创建标签</el-button>
              <el-button @click="$router.push('/')">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import axios from '@/utils/axios'
import { isAxiosError } from 'axios'

import RichTextEditor from '@/views/components/RichTextEditor.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  name: '',
  description: '',
  category: 'article',
  settingCategory: '',
})

// 自定义验证器：当 category 为 setting 时，要求 settingCategory 必填
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const validateSettingCategory = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (form.category === 'setting' && !value) {
    callback(new Error('请选择设定分类'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度在1-20个字符', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择标签类型', trigger: 'change' }],
  settingCategory: [{ validator: validateSettingCategory, trigger: 'change' }],
}

const submitForm = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    await axios.post('/tags', form)
    ElMessage.success('标签创建成功')
    router.push('/')
  } catch (err: unknown) {
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '创建失败')
    } else {
      ElMessage.error('创建失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tag-create-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 20px;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  color: white;
}
.logo {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  cursor: pointer;
}
.tag-card {
  max-width: 600px;
  margin: 0 auto;
}

.editor-wrapper {
  width: 100%;
}
</style>
