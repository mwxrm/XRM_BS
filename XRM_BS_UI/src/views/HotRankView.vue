<template>
  <div class="hot-rank-container">
    <el-main>
      <div class="actions-bar">
        <el-button
          v-if="userStore.user?.role === 'admin'"
          type="warning"
          @click="refreshHotRank"
          :loading="refreshing"
        >
          🔄 刷新热榜
        </el-button>
        <el-button
          v-if="userStore.user?.role === 'admin'"
          type="success"
          @click="generateTestData"
          :loading="generating"
        >
          🧪 生成测试数据
        </el-button>
      </div>

      <el-tabs v-model="activeTab" @tab-click="loadData">
        <el-tab-pane label="热门文章" name="articles">
          <div v-loading="loading" class="rank-list">
            <div
              v-for="(item, index) in articles"
              :key="item.id"
              class="rank-card"
              @click="goArticle(item.id)"
            >
              <div class="rank-number">{{ index + 1 }}</div>
              <div class="rank-content">
                <h5>{{ item.title }}</h5>
                <div class="meta">
                  <span>作者：{{ item.authorUsername }}</span>
                  <span>浏览：{{ item.viewCount }}</span>
                  <span>点赞：{{ item.likeCount }}</span>
                  <span>得分：{{ formatScore(item.score ?? 0) }}</span>
                </div>
              </div>
            </div>

            <div ref="articleTrigger" class="load-more-trigger"></div>
            <div v-if="articlesLoading" class="load-more-text">加载中...</div>
            <div v-if="articleNoMore && articles.length > 0" class="load-more-text">没有更多了</div>
            <el-empty v-if="!articlesLoading && articles.length === 0" description="暂无热门文章" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="热门设定" name="settings">
          <div v-loading="loading" class="rank-list">
            <div
              v-for="(item, index) in settings"
              :key="item.id"
              class="rank-card"
              @click="goSetting(item.id)"
            >
              <div class="rank-number">{{ index + 1 }}</div>
              <div class="rank-content">
                <h5>{{ item.title }}</h5>
                <div class="meta">
                  <span>作者：{{ item.authorUsername }}</span>
                  <span>浏览：{{ item.viewCount }}</span>
                  <span>点赞：{{ item.likeCount }}</span>
                  <span>得分：{{ formatScore(item.score ?? 0) }}</span>
                </div>
              </div>
            </div>

            <div ref="settingTrigger" class="load-more-trigger"></div>
            <div v-if="settingsLoading" class="load-more-text">加载中...</div>
            <div v-if="settingNoMore && settings.length > 0" class="load-more-text">没有更多了</div>
            <el-empty v-if="!settingsLoading && settings.length === 0" description="暂无热门设定" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from '@/utils/axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { AxiosError } from 'axios'

interface ContentCard {
  id: number
  title: string
  authorUsername: string
  viewCount: number
  likeCount?: number
  score?: number
  type?: string
}

interface RawContentItem {
  id: number | string
  title?: string
  author?: string
  authorUsername?: string
  viewCount?: number | string
  likeCount?: number | string
  score?: number | string
  [key: string]: unknown
}

const goArticle = (id: number) => router.push(`/articles/${id}`)
const goSetting = (id: number) => router.push(`/settings/${id}`)

// const refreshing = ref(false)

const articlePage = ref(1)
const settingPage = ref(1)
// const pageSize = 10
// const generating = ref(false)

const MAX_ITEMS = 100

const formatScore = (score: number) => score.toFixed(2)

const router = useRouter()
const route = useRoute()
const activeTab = ref(route.query.tab === 'settings' ? 'settings' : 'articles')
const userStore = useUserStore()
const generating = ref(false)
const refreshing = ref(false)

// 响应式变量
const articles = ref<ContentCard[]>([])
const settings = ref<ContentCard[]>([])
const loading = ref(false)

// 无限滚动状态
const articleNoMore = ref(false)
const settingNoMore = ref(false)
const articlesLoading = ref(false)
const settingsLoading = ref(false)

// Observer 实例
let articleObserver: IntersectionObserver | null = null
let settingObserver: IntersectionObserver | null = null

// 模板引用（触发器元素）
const articleTrigger = ref<HTMLElement | null>(null)
const settingTrigger = ref<HTMLElement | null>(null)

// 通用加载文章数据
const loadArticles = async (page: number) => {
  // 已达100条上限，停止加载
  if (articles.value.length >= MAX_ITEMS) {
    articleNoMore.value = true
    return
  }
  if (articleNoMore.value || articlesLoading.value) return
  articlesLoading.value = true
  try {
    const res = await axios.get('/hot/articles', { params: { page: page - 1, size: 10 } })
    const newItems = (res.data as RawContentItem[]).map((item) => ({
      id: Number(item.id),
      title: item.title || '',
      authorUsername: item.authorUsername || item.author || '',
      viewCount: Number(item.viewCount ?? 0),
      likeCount: Number(item.likeCount ?? 0),
      score: Number(item.score ?? 0),
    }))
    if (newItems.length < 10) articleNoMore.value = true
    if (page === 1) {
      articles.value = newItems
    } else {
      articles.value.push(...newItems)
    }
    articlePage.value = page
  } catch {
    ElMessage.error('加载热门文章失败')
  } finally {
    articlesLoading.value = false
  }

  // 数据更新后重新设置观察器
  await nextTick()
  if (articleObserver) {
    articleObserver.disconnect()
  }
  setupArticleObserver()
}

const loadSettings = async (page: number) => {
  if (settings.value.length >= MAX_ITEMS) {
    settingNoMore.value = true
    return
  }
  if (settingNoMore.value || settingsLoading.value) return
  settingsLoading.value = true
  try {
    const res = await axios.get('/hot/settings', { params: { page: page - 1, size: 10 } })
    const newItems = (res.data as RawContentItem[]).map((item) => ({
      id: Number(item.id),
      title: item.title || '',
      authorUsername: item.authorUsername || item.author || '',
      viewCount: Number(item.viewCount ?? 0),
      likeCount: Number(item.likeCount ?? 0),
      score: Number(item.score ?? 0),
    }))
    if (newItems.length < 10) settingNoMore.value = true
    if (page === 1) {
      settings.value = newItems
    } else {
      settings.value.push(...newItems)
    }
    settingPage.value = page
  } catch {
    ElMessage.error('加载热门设定失败')
  } finally {
    settingsLoading.value = false
  }

  await nextTick()
  if (settingObserver) {
    settingObserver.disconnect()
  }
  setupSettingObserver()
}

const setupArticleObserver = () => {
  if (!articleTrigger.value) return
  articleObserver = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && !articlesLoading.value) {
        loadArticles(articlePage.value + 1)
      }
    },
    { threshold: 0.1 },
  )
  articleObserver.observe(articleTrigger.value)
}

const setupSettingObserver = () => {
  if (!settingTrigger.value) return
  settingObserver = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && !settingsLoading.value) {
        loadSettings(settingPage.value + 1)
      }
    },
    { threshold: 0.1 },
  )
  settingObserver.observe(settingTrigger.value)
}

// 清理观察器
const cleanupObservers = () => {
  articleObserver?.disconnect()
  settingObserver?.disconnect()
}

const loadData = () => {
  if (activeTab.value === 'articles') {
    if (articles.value.length === 0) loadArticles(articlePage.value)
  } else {
    if (settings.value.length === 0) loadSettings(settingPage.value)
  }
}

const generateTestData = async () => {
  generating.value = true
  try {
    const res = await axios.post('/dev/generate-test-data')
    const data = res.data as Record<string, number>
    ElMessage.success(`测试数据已生成！文章：${data.articles ?? 0}，设定：${data.settings ?? 0}`)
    // 自动刷新热门榜单
    await refreshHotRank()
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '生成失败')
  } finally {
    generating.value = false
  }
}

const refreshHotRank = async () => {
  refreshing.value = true
  try {
    // 调用后端刷新热榜接口
    await axios.post('/hot/compute')
    // 清空已有数据并重新加载当前 tab
    articles.value = []
    settings.value = []
    articleNoMore.value = false
    settingNoMore.value = false
    articlePage.value = 1
    settingPage.value = 1
    // 重新加载两个列表（即使另一个不可见）
    loadArticles(1)
    loadSettings(1)
    ElMessage.success('热榜已刷新')
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

watch(activeTab, () => {
  window.scrollTo(0, 0)
})

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'settings' || tab === 'articles') {
      activeTab.value = tab
    }
  },
)

onUnmounted(() => {
  cleanupObservers()
})

onMounted(() => {
  loadArticles(1)
  loadSettings(1)
})
</script>

<style scoped>
.hot-rank-container {
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
  height: 60px;
  color: white;
}
.logo {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  cursor: pointer;
}
.rank-list {
  margin-top: 20px;
}
.rank-card {
  display: flex;
  align-items: center;
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: 0.2s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}
.rank-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.rank-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  width: 60px;
  text-align: center;
}
.rank-content {
  flex: 1;
}
.rank-content h5 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}
.meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
  flex-wrap: wrap;
}

.load-more-trigger {
  height: 20px;
  margin: 10px 0;
}
.load-more-text {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}
</style>
