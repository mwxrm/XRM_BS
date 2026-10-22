<template>
  <div class="tag-edit-container">
    <el-container>
      <el-main>
        <el-card class="tag-card">
          <template #header>
            <h2>编辑标签</h2>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
            <el-form-item label="标签名称" prop="name">
              <el-input v-model="form.name" disabled />
            </el-form-item>
            <el-form-item label="标签类型" prop="classification">
              <el-input :value="getClassificationName(form.classification)" disabled />
            </el-form-item>
            <el-form-item label="标签描述" prop="description">
              <div class="editor-wrapper">
                <RichTextEditor v-model="form.description" />
              </div>
            </el-form-item>
            <el-form-item label="标签颜色" prop="color">
              <el-color-picker v-model="form.color" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitForm" :loading="loading">保存修改</el-button>
              <el-button @click="$router.push(`/tags/${tagId}`)">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '@/utils/axios'
import { isAxiosError } from 'axios'
import type { FormInstance, FormRules } from 'element-plus'

import RichTextEditor from '@/views/components/RichTextEditor.vue'

const route = useRoute()
const router = useRouter()
const tagId = route.params.id as string
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  name: '',
  classification: '',
  description: '',
  color: '',
})

const getClassificationName = (classification: string) => {
  const map: Record<string, string> = {
    article: '文章标签',
    character: '人物标签',
    item: '物品标签',
    location: '地区标签',
    background: '背景标签',
    rule: '规则标签',
    custom: '自定义标签',
  }
  return map[classification] || classification
}

const rules: FormRules = {
  description: [],
}

const loadTag = async () => {
  try {
    const res = await axios.get(`/tags/${tagId}`)
    const tag = res.data
    form.name = tag.name
    form.classification = tag.classification
    form.description = tag.description || ''
    form.color = tag.color || ''
  } catch (_error) {
    if (isAxiosError(_error)) {
      ElMessage.error(_error.response?.data?.message || '修改失败')
    } else {
      ElMessage.error('修改失败')
    }
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  loading.value = true
  try {
    await axios.put(`/tags/${tagId}`, {
      description: form.description,
      color: form.color,
    })
    ElMessage.success('标签修改成功')
    router.push(`/tags/${tagId}`)
  } catch (err) {
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '修改失败')
    } else {
      ElMessage.error('修改失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTag()
})
</script>

<style scoped>
.tag-edit-container {
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
  max-width: 800px;
  margin: 0 auto;
}
.editor-wrapper {
  width: 100%;
}
</style>
