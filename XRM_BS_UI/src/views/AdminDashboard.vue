<template>
  <div class="admin-container">
    <el-main>
      <el-row :gutter="20" v-loading="loading">
        <el-col :span="8" v-for="card in statCards" :key="card.label">
          <el-card shadow="hover" class="stat-card">
            <div class="card-value">{{ card.value }}</div>
            <div class="card-label">{{ card.label }}</div>
          </el-card>
        </el-col>
      </el-row>
      <el-button type="primary" @click="$router.push('/admin/carousel')">轮播管理</el-button>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const stats = reactive({
  userCount: 0,
  articleCount: 0,
  settingCount: 0,
  tagCount: 0,
  commentCount: 0,
})

const statCards = computed(() => [
  { label: '用户总数', value: stats.userCount },
  { label: '文章总数', value: stats.articleCount },
  { label: '设定总数', value: stats.settingCount },
  { label: '标签总数', value: stats.tagCount },
  { label: '评论总数', value: stats.commentCount },
])

const loading = ref(false)

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await axios.get('/admin/dashboard')
    if (res.data) {
      stats.userCount = res.data.userCount ?? 0
      stats.articleCount = res.data.articleCount ?? 0
      stats.settingCount = res.data.settingCount ?? 0
      stats.tagCount = res.data.tagCount ?? 0
      stats.commentCount = res.data.commentCount ?? 0
    }
  } catch {
    ElMessage.error('无法加载管理数据')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
.header {
  background: #2c3e50;
  padding: 0 20px;
  color: white;
}
.header-content {
  display: flex;
  align-items: center;
  height: 60px;
  gap: 20px;
}
.logo {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  cursor: pointer;
}
.admin-title {
  font-size: 18px;
  opacity: 0.8;
}
.stat-card {
  text-align: center;
  padding: 20px;
}
.card-value {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
}
.card-label {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}
</style>
