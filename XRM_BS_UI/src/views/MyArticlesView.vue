<template>
  <div class="my-articles-container">
    <el-container>
      <el-main>
        <div class="articles-management">
          <div class="page-header">
            <h2>我的文章</h2>
            <el-button type="primary" @click="$router.push('/articles/create')">
              <el-icon><Plus /></el-icon> 新建文章
            </el-button>
          </div>

          <!-- 文章筛选 -->
          <el-tabs v-model="activeTab" @tab-click="loadArticles" class="article-tabs">
            <el-tab-pane label="全部文章" name="all"></el-tab-pane>
            <el-tab-pane label="草稿" name="drafts"></el-tab-pane>
            <el-tab-pane label="已发布" name="published"></el-tab-pane>
          </el-tabs>

          <!-- 加载状态 -->
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>

          <!-- 文章列表 -->
          <div v-else-if="articles.length > 0" class="articles-list">
            <div v-for="article in articles" :key="article.id" class="article-card">
              <div class="article-content">
                <h3 class="article-title" @click="viewArticle(article)">
                  {{ article.title }}
                  <el-tag v-if="article.status === 'DRAFT'" type="info" size="small">草稿</el-tag>
                </h3>
                <p class="article-summary">{{ article.summary }}</p>

                <div class="article-meta">
                  <span class="meta-item">
                    <el-icon><User /></el-icon>
                    {{ article.authorUsername }}
                  </span>
                  <span class="meta-item">
                    <el-icon><Clock /></el-icon>
                    {{ formatDate(article.createdAt) }}
                  </span>
                  <span class="meta-item">
                    <el-icon><View /></el-icon>
                    浏览: {{ article.viewCount }}
                  </span>
                  <span class="meta-item">
                    <el-icon><Star /></el-icon>
                    点赞: {{ article.likeCount }}
                  </span>

                  <div class="article-tags">
                    <el-tag
                      v-for="tag in article.tags"
                      :key="tag"
                      size="small"
                      type="info"
                      class="tag"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
              </div>

              <div class="article-actions">
                <el-button @click="editArticle(article)" :disabled="article.status === 'PUBLISHED'">
                  <el-icon><Edit /></el-icon> 编辑
                </el-button>
                <el-button
                  type="primary"
                  @click="publishArticle(article)"
                  v-if="article.status === 'DRAFT'"
                >
                  <el-icon><Upload /></el-icon> 发布
                </el-button>
                <el-button type="danger" @click="deleteArticle(article)" plain>
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <el-empty description="暂无文章">
              <el-button type="primary" @click="$router.push('/articles/create')">
                开始写作
              </el-button>
            </el-empty>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User, Clock, View, Star, Edit, Upload, Delete } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import type { AxiosError } from 'axios'

// 定义文章接口
interface Article {
  id: string
  title: string
  summary: string
  content: string
  authorUsername: string
  authorId: string
  createdAt: string
  updatedAt: string
  viewCount: number
  likeCount: number
  tags: string[]
  deleted?: boolean
  status: 'DRAFT' | 'PUBLISHED'
  [key: string]: unknown
}

// 定义API响应接口
interface ApiResponse<T> {
  data: T
  message?: string
  code: number
}

// 定义错误响应接口
interface ErrorResponse {
  message?: string
  [key: string]: unknown
}

const router = useRouter()

const activeTab = ref('all')
const articles = ref<Article[]>([])
const loading = ref(false)

// 加载文章
const loadArticles = async () => {
  loading.value = true
  try {
    let response
    if (activeTab.value === 'drafts') {
      response = await axios.get<ApiResponse<Article[]>>('/articles/drafts')
      articles.value = response.data.data
    } else if (activeTab.value === 'published') {
      response = await axios.get<ApiResponse<Article[]>>('/articles/my')
      // 在前端过滤已发布的文章
      articles.value = response.data.data.filter((article) => article.status === 'PUBLISHED')
    } else {
      response = await axios.get<ApiResponse<Article[]>>('/articles/my')
      articles.value = response.data.data
    }
  } catch (error) {
    const err = error as AxiosError<ErrorResponse>
    const errorMessage = err.response?.data?.message || err.message || '加载文章失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

// 查看文章
const viewArticle = (article: Article) => {
  if (article.status === 'DRAFT') {
    editArticle(article)
  } else {
    router.push(`/articles/${article.id}`)
  }
}

// 编辑文章
const editArticle = (article: Article) => {
  router.push(`/articles/${article.id}/edit`)
}

// 发布文章
const publishArticle = async (article: Article) => {
  try {
    await ElMessageBox.confirm('确定要发布这篇文章吗？', '发布确认', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await axios.post(`/articles/${article.id}/publish`)
    ElMessage.success('文章已发布')
    loadArticles()
  } catch (error) {
    if (error !== 'cancel') {
      const err = error as AxiosError<ErrorResponse>
      const errorMessage = err.response?.data?.message || err.message || '发布失败'
      ElMessage.error(errorMessage)
    }
  }
}

// 删除文章
const deleteArticle = async (article: Article) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？此操作不可恢复', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await axios.delete(`/articles/${article.id}`)
    ElMessage.success('文章已删除')
    loadArticles()
  } catch (error) {
    if (error !== 'cancel') {
      const err = error as AxiosError<ErrorResponse>
      const errorMessage = err.response?.data?.message || err.message || '删除失败'
      ElMessage.error(errorMessage)
    }
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadArticles()
})
</script>

<style scoped>
.my-articles-container {
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

.actions {
  display: flex;
  gap: 10px;
}

.articles-management {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.article-tabs {
  margin-bottom: 30px;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.article-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.1);
}

.article-content {
  flex: 1;
  margin-right: 20px;
}

.article-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
}

.article-title:hover {
  color: #409eff;
}

.article-summary {
  margin: 0 0 15px 0;
  color: #666;
  line-height: 1.5;
  font-size: 14px;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 20px;
  font-size: 12px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-tags {
  display: flex;
  gap: 5px;
  margin-top: 5px;
}

.tag {
  font-size: 12px;
}

.article-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 150px;
}

.loading-container {
  padding: 40px 0;
}

.empty-state {
  padding: 60px 0;
}
</style>
