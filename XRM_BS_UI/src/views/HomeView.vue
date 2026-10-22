<template>
  <div class="home-container">
    <el-container>
      <el-main>
        <el-carousel v-if="carouselItems.length" :interval="4000" arrow="always" height="300px">
          <el-carousel-item
            v-for="item in carouselItems"
            :key="item.id"
            @click="handleAdClick(item.linkUrl)"
          >
            <img
              :src="item.imageUrl"
              style="width: 100%; height: 100%; object-fit: cover; cursor: pointer"
            />
          </el-carousel-item>
        </el-carousel>

        <!-- 推荐区块 -->
        <section class="section">
          <div class="section-header">
            <h2>✨ 为你推荐</h2>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="subsection-header">
                <span>推荐文章</span>
                <el-button link type="primary" @click="$router.push('/recommend?type=article')"
                  >查看全部</el-button
                >
              </div>
              <div class="card-grid">
                <div
                  v-for="item in recommendArticles"
                  :key="item.id"
                  class="mini-card"
                  @click="viewArticle(item.id)"
                >
                  <h5>{{ item.title || '无标题' }}</h5>
                  <p class="meta">{{ item.authorUsername }} · {{ item.viewCount }} 阅读</p>
                </div>
                <el-empty
                  v-if="!recommendArticles.length"
                  description="暂无推荐文章"
                  :image-size="40"
                />
              </div>
            </el-col>
            <el-col :span="12">
              <div class="subsection-header">
                <span>推荐设定</span>
                <el-button link type="primary" @click="$router.push('/recommend?type=setting')"
                  >查看全部</el-button
                >
              </div>
              <div class="card-grid">
                <div
                  v-for="item in recommendSettings"
                  :key="item.id"
                  class="mini-card"
                  @click="viewSetting(item.id)"
                >
                  <h5>{{ item.title || '无标题' }}</h5>
                  <p class="meta">{{ item.authorUsername }} · {{ item.viewCount }} 浏览</p>
                </div>
                <el-empty
                  v-if="!recommendSettings.length"
                  description="暂无推荐设定"
                  :image-size="40"
                />
              </div>
            </el-col>
          </el-row>
        </section>

        <!-- 热门区块 -->
        <section class="section">
          <div class="section-header">
            <h2>🔥 热门内容</h2>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="subsection-header">
                <span>热门文章</span>
                <el-button link type="primary" @click="$router.push('/hot?tab=articles')"
                  >查看全部 →</el-button
                >
              </div>
              <div class="card-grid">
                <div
                  v-for="item in hotArticles"
                  :key="item.id"
                  class="mini-card"
                  @click="viewArticle(item.id)"
                >
                  <h5>{{ item.title || '无标题' }}</h5>
                  <p class="meta">{{ item.authorUsername }} · {{ item.viewCount }} 阅读</p>
                </div>
                <el-empty v-if="!hotArticles.length" description="暂无热门文章" :image-size="40" />
              </div>
            </el-col>
            <el-col :span="12">
              <div class="subsection-header">
                <span>热门设定</span>
                <el-button link type="primary" @click="$router.push('/hot?tab=settings')"
                  >查看全部 →</el-button
                >
              </div>
              <div class="card-grid">
                <div
                  v-for="item in hotSettings"
                  :key="item.id"
                  class="mini-card"
                  @click="viewSetting(item.id)"
                >
                  <h5>{{ item.title || '无标题' }}</h5>
                  <p class="meta">{{ item.authorUsername }} · {{ item.viewCount }} 浏览</p>
                </div>
                <el-empty v-if="!hotSettings.length" description="暂无热门设定" :image-size="40" />
              </div>
            </el-col>
          </el-row>
        </section>

        <!-- 最新区块 -->
        <section class="section">
          <div class="section-header">
            <h2>🕒 最新发布</h2>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="subsection-header">
                <span>最新文章</span>
              </div>
              <div class="card-grid">
                <div
                  v-for="item in latestArticles"
                  :key="item.id"
                  class="mini-card"
                  @click="viewArticle(item.id)"
                >
                  <h5>{{ item.title }}</h5>
                  <p class="meta">{{ item.authorUsername }} · {{ item.viewCount }} 阅读</p>
                </div>
                <el-empty
                  v-if="!latestArticles.length"
                  description="暂无最新文章"
                  :image-size="40"
                />
              </div>
            </el-col>
            <el-col :span="12">
              <div class="subsection-header">
                <span>最新设定</span>
              </div>
              <div class="card-grid">
                <div
                  v-for="item in latestSettings"
                  :key="item.id"
                  class="mini-card"
                  @click="viewSetting(item.id)"
                >
                  <h5>{{ item.title }}</h5>
                  <p class="meta">{{ item.authorUsername }} · {{ item.viewCount }} 浏览</p>
                </div>
                <el-empty
                  v-if="!latestSettings.length"
                  description="暂无最新设定"
                  :image-size="40"
                />
              </div>
            </el-col>
          </el-row>
        </section>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
// import { DocumentCopy, Delete, Plus } from '@element-plus/icons-vue'
import type { AxiosError } from 'axios'
import axios from '@/utils/axios'

const popoverLoading = ref(false)

// 悬浮窗相关状态
const activePopoverTab = ref<'like_favorite' | 'reply' | 'follow'>('like_favorite')
const followGroups = ref<RawNotificationGroup[]>([])
const latestGroups = ref<RawNotificationGroup[]>([])

interface RawNotificationGroup {
  groupKey: string
  type: string
  title: string
  previewContent: string
  targetId: number
  targetType: string
  commentId?: number
  totalCount: number
  unreadCount: number
  latestTime: string
}

interface Tag {
  id: number
  name: string
  usageCount: number
}

interface TagGroup {
  name: string
  displayName: string
  classification: string
  tags: Tag[]
  limit: number
  loadedAll: boolean
}

// 在现有接口定义旁边添加
interface Setting {
  id: number
  title: string
  content: string
  category: string
  authorUsername: string
  viewCount: number
  likeCount: number
  createdAt: string
  status: 'draft' | 'published'
}

interface RawContentItem {
  id: number | string
  title?: string
  author?: string
  authorUsername?: string
  viewCount?: number | string
  likeCount?: number | string
  score?: number | string
  // 其他未知字段保留
  [key: string]: unknown
}

// 定义错误响应接口
interface ErrorResponse {
  message?: string
  [key: string]: unknown
}

interface CarouselItem {
  id: number
  imageUrl: string
  linkUrl: string
}

interface ContentCard {
  id: number
  title: string
  authorUsername: string
  viewCount: number
  likeCount?: number
  score?: number
  type?: string
}

interface RawPublishedItem {
  id: number | string
  title?: string
  authorUsername?: string
  viewCount?: number | string
  likeCount?: number | string
  [key: string]: unknown
}

const carouselItems = ref<CarouselItem[]>([])

const router = useRouter()
const userStore = useUserStore()
// const articles = ref<Article[]>([])
const loadingArticles = ref(false)

const settings = ref<Setting[]>([])
const loadingSettings = ref(false)

const unreadCount = ref(0)

// 搜索历史
const searchHistory = ref<string[]>([])

const likeFavoriteGroups = ref<RawNotificationGroup[]>([])
const replyGroups = ref<RawNotificationGroup[]>([])

const carouselAds = ref([])

const recommendArticles = ref<ContentCard[]>([])
const recommendSettings = ref<ContentCard[]>([])
const hotArticles = ref<ContentCard[]>([])
const hotSettings = ref<ContentCard[]>([])
const latestArticles = ref<ContentCard[]>([])
const latestSettings = ref<ContentCard[]>([])

// 刷新定时器
let refreshTimer: number | null = null

// 启动刷新定时器
const startRefreshTimer = () => {
  // 清除现有的定时器
  stopRefreshTimer()

  // 只有在用户已登录时才启动定时器
  if (userStore.isAuthenticated) {
    refreshTimer = window.setInterval(async () => {
      try {
        await userStore.getCurrentUser()
      } catch (error) {
        console.error('刷新用户信息失败:', error)
        // 如果刷新失败，停止定时器
        stopRefreshTimer()
      }
    }, 300000) // 300秒刷新一次
  }
}

// 停止刷新定时器
const stopRefreshTimer = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const loadCarouselItems = async () => {
  try {
    const res = await axios.get('/public/carousel')
    carouselItems.value = res.data as CarouselItem[]
  } catch (e) {
    console.error('轮播加载失败', e)
  }
}

// 加载已发布文章
const loadPublishedArticles = async () => {
  loadingArticles.value = true
  try {
    const response = await axios.get('/articles/published', { params: { page: 0, size: 50 } })
    const pageData = response.data
    if (pageData && Array.isArray(pageData.content)) {
      latestArticles.value = (pageData.content as RawPublishedItem[]).map((item) => ({
        id: Number(item.id),
        title: String(item.title ?? ''),
        authorUsername: String(item.authorUsername ?? ''),
        viewCount: Number(item.viewCount ?? 0),
        likeCount: Number(item.likeCount ?? 0),
      }))
    } else {
      console.warn('文章分页数据格式不正确', pageData)
      latestArticles.value = []
    }
  } catch (error) {
    const axiosError = error as AxiosError<ErrorResponse>
    if (axiosError.response?.status === 403) {
      localStorage.removeItem('token')
      userStore.logout()
      ElMessage.warning('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error('加载文章失败，请稍后重试')
    }
  } finally {
    loadingArticles.value = false
  }
}

const viewArticle = (id: number) => {
  router.push(`/articles/${id}`)
}

const loadSettings = async () => {
  loadingSettings.value = true
  try {
    const res = await axios.get('/settings/published', { params: { page: 0, size: 10 } })
    settings.value = res.data.content // 假设后端返回 { content: Setting[] }
  } catch (error) {
    console.error('加载设定失败', error)
  } finally {
    loadingSettings.value = false
  }
}

const viewSetting = (id: number) => {
  router.push(`/settings/${id}`)
}

// 热门标签分组
const tagGroups = ref<TagGroup[]>([
  {
    name: 'article',
    displayName: '文章标签',
    classification: 'article',
    tags: [],
    limit: 10,
    loadedAll: false,
  },
  {
    name: 'character',
    displayName: '人物设定',
    classification: 'character',
    tags: [],
    limit: 10,
    loadedAll: false,
  },
  {
    name: 'item',
    displayName: '物品设定',
    classification: 'item',
    tags: [],
    limit: 10,
    loadedAll: false,
  },
  {
    name: 'location',
    displayName: '地区设定',
    classification: 'location',
    tags: [],
    limit: 10,
    loadedAll: false,
  },
  {
    name: 'background',
    displayName: '背景设定',
    classification: 'background',
    tags: [],
    limit: 10,
    loadedAll: false,
  },
  {
    name: 'rule',
    displayName: '规则设定',
    classification: 'rule',
    tags: [],
    limit: 10,
    loadedAll: false,
  },
  {
    name: 'custom',
    displayName: '自定义设定',
    classification: 'custom',
    tags: [],
    limit: 10,
    loadedAll: false,
  },
])

const tagsLoading = ref(false)

// 加载所有分类的热门标签
const loadHotTagsByCategory = async () => {
  tagsLoading.value = true
  try {
    const promises = tagGroups.value.map((group) =>
      axios
        .get('/tags/popular', {
          params: { classification: group.classification, size: group.limit },
        })
        .then((res) => ({ group, tags: res.data as Tag[] })),
    )
    const results = await Promise.all(promises)
    results.forEach(({ group, tags }) => {
      group.tags = tags
      group.loadedAll = tags.length < group.limit
    })
  } catch (error) {
    console.error('加载热门标签失败', error)
  } finally {
    tagsLoading.value = false
  }
}

// 加载历史记录
const loadSearchHistory = () => {
  const history = localStorage.getItem('searchHistory')
  if (history) {
    searchHistory.value = JSON.parse(history)
  } else {
    searchHistory.value = []
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await axios.get('/notifications/unread-count')
    unreadCount.value = res.data.count
  } catch (error) {
    console.error('获取未读通知数失败', error)
  }
}

const loadLatestNotifications = async () => {
  try {
    const filter = activePopoverTab.value // 'like_favorite' 或 'reply'
    const res = await axios.get('/notifications', {
      params: { page: 0, size: 5, filter },
    })
    latestGroups.value = res.data.content
  } catch (error) {
    console.error('加载最新通知失败', error)
  }
}

// 加载指定分类的最新消息（取前5条）
const loadPopoverMessages = async (filter: 'like_favorite' | 'reply' | 'follow') => {
  popoverLoading.value = true
  try {
    const res = await axios.get('/notifications', {
      params: { page: 0, size: 4, filter },
    })
    const groups = res.data.content as RawNotificationGroup[]
    if (filter === 'like_favorite') {
      likeFavoriteGroups.value = groups
    } else if (filter === 'reply') {
      replyGroups.value = groups
    } else {
      followGroups.value = groups
    }
  } catch (error) {
    console.error('加载悬浮通知失败', error)
  } finally {
    popoverLoading.value = false
  }
}

const loadRecommendations = async (type: string, forceRefresh = false) => {
  try {
    const params: Record<string, string | number | boolean> = {
      page: 0,
      size: 6,
      type,
      _t: Date.now(),
    }
    if (forceRefresh) params.refresh = true
    const res = await axios.get('/recommend', { params })
    const raw = res.data as RawContentItem[]
    const items: ContentCard[] = raw.map((item) => ({
      id: Number(item.id),
      title: String(item.title ?? ''),
      authorUsername: String(item.authorUsername || item.author || ''),
      viewCount: Number(item.viewCount ?? 0),
      score: Number(item.score ?? 0),
      likeCount: Number(item.likeCount ?? 0),
    }))
    if (type === 'article') recommendArticles.value = items
    else recommendSettings.value = items
  } catch (e) {
    console.error('加载推荐失败', e)
  }
}

const loadHotArticles = async () => {
  try {
    const res = await axios.get('/hot/articles', { params: { page: 0, size: 20 } })
    hotArticles.value = res.data as ContentCard[]
  } catch {
    ElMessage.error('加载热门文章失败')
  }
}

const loadHotSettings = async () => {
  try {
    const res = await axios.get('/hot/settings', { params: { page: 0, size: 20 } })
    const raw = res.data as RawContentItem[]
    hotSettings.value = raw.map((item) => ({
      id: Number(item.id),
      title: String(item.title ?? ''),
      authorUsername: String(item.authorUsername || item.author || ''),
      viewCount: Number(item.viewCount ?? 0),
    }))
  } catch {
    ElMessage.error('加载热门设定失败')
  }
}

const loadLatestArticles = async () => {
  try {
    const res = await axios.get('/articles/published', { params: { page: 0, size: 50 } }) // 加上 page
    latestArticles.value = (res.data.content ?? res.data) as ContentCard[]
  } catch {
    ElMessage.error('加载最新文章失败')
  }
}

const loadLatestSettings = async () => {
  loadingSettings.value = true
  try {
    const res = await axios.get('/settings/published', { params: { page: 0, size: 50 } })
    const pageData = res.data
    if (pageData && Array.isArray(pageData.content)) {
      latestSettings.value = (pageData.content as RawPublishedItem[]).map((item) => ({
        id: Number(item.id),
        title: String(item.title ?? ''),
        authorUsername: String(item.authorUsername ?? ''),
        viewCount: Number(item.viewCount ?? 0),
        likeCount: Number(item.likeCount ?? 0),
      }))
    } else {
      console.warn('设定分页数据格式不正确', pageData)
      latestSettings.value = []
    }
  } catch {
    ElMessage.error('加载最新设定失败')
  } finally {
    loadingSettings.value = false
  }
}

const handleAdClick = (linkUrl: string) => {
  if (!linkUrl) return

  // 1. 相对路径 → 直接走路由（如 /articles/192）
  if (linkUrl.startsWith('/')) {
    router.push(linkUrl)
    return
  }

  // 2. 绝对路径 → 判断是否本站地址
  if (/^https?:\/\//i.test(linkUrl)) {
    const currentHost = window.location.host // 例如 "localhost:5173"
    try {
      const url = new URL(linkUrl)
      if (url.host === currentHost) {
        // 本站绝对路径 → 提取路径部分，走路由
        router.push(url.pathname + url.search + url.hash)
      } else {
        // 外站 → 新窗口打开
        window.open(linkUrl, '_blank')
      }
    } catch {
      // URL 解析失败，兜底新窗口打开
      window.open(linkUrl, '_blank')
    }
    return
  }

  // 3. 其他情况（如 www.baidu.com 缺协议）→ 补全 https 后新窗口打开
  window.open('https://' + linkUrl, '_blank')
}

// 监听用户认证状态变化
watch(
  () => userStore.isAuthenticated,
  (isAuthenticated) => {
    if (isAuthenticated) {
      startRefreshTimer()
    } else {
      stopRefreshTimer()
    }
  },
  { immediate: true },
)

onMounted(async () => {
  try {
    await userStore.init()
  } catch (e) {
    console.warn('用户初始化失败，稍后重试', e)
  }
  console.log('当前用户信息：', userStore.user)
  try {
    const res = await axios.get('/public/carousel')
    carouselAds.value = res.data
  } catch (e) {
    console.error('轮播加载失败', e)
  }
  await userStore.init()

  loadPublishedArticles()
  loadSettings()
  loadSearchHistory()
  fetchUnreadCount()
  loadCarouselItems()
  loadLatestNotifications()
  loadRecommendations('article')
  loadRecommendations('setting')
  loadHotArticles()
  loadHotSettings()
  loadLatestArticles()
  loadLatestSettings()
  await loadHotTagsByCategory()
  Promise.all([loadPopoverMessages('like_favorite'), loadPopoverMessages('reply')])
})

// 组件卸载时清理定时器
onUnmounted(() => {
  stopRefreshTimer()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background-color: #f5f7fa;
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
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.api-test-section {
  max-width: 1200px;
  margin: 0 auto;
}

.test-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.response {
  margin-top: 15px;
  padding: 12px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  max-height: 300px;
  overflow-y: auto;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.response pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.user-details {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.detail-item:last-child {
  border-bottom: none;
}

.label {
  font-weight: bold;
  color: #606266;
  margin-right: 12px;
  min-width: 80px;
}

.value {
  color: #303133;
}

.mt-2 {
  margin-top: 8px;
}

.mt-4 {
  margin-top: 16px;
}

.ml-2 {
  margin-left: 8px;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-card {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.article-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.1);
}

.article-title {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.article-summary {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.article-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-container {
  padding: 20px 0;
}

.empty-state {
  padding: 40px 0;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.setting-card {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}
.setting-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.1);
}
.setting-title {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}
.setting-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #909399;
  align-items: center;
}

.tags-container {
  padding: 8px 0;
}
.tag-group {
  margin-bottom: 24px;
}
.tag-group h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #303133;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.tag-item {
  cursor: pointer;
  transition: all 0.2s;
}
.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.tag-usage {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}
.empty-tip {
  font-size: 14px;
  color: #c0c4cc;
}

.more-btn {
  text-align: right;
  margin-top: 8px;
}
.more-btn .el-button {
  font-size: 13px;
  color: #409eff;
}

.search-input {
  flex: 1;
  max-width: 300px;
  margin: 0 20px;
}

@media (max-width: 768px) {
  .search-input {
    display: none; /* 移动端可隐藏，或改为其他方式 */
  }
}

.search-input {
  position: relative;
}

.search-history li:hover {
  background-color: #f5f7fa;
}

.notification-popover-content {
  padding: 4px 0;
}
.popover-list {
  max-height: 300px;
  overflow-y: auto;
}
.popover-item {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}
.popover-item:hover {
  background-color: #f5f7fa;
}
.item-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  font-size: 13px;
}
.item-preview {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.popover-footer {
  padding: 8px 12px;
  text-align: center;
  border-top: 1px solid #ebeef5;
}

.header-content .el-button {
  height: 36px;
}

.section {
  margin-bottom: 30px;
}
.section-header h2 {
  font-size: 20px;
  margin-bottom: 15px;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}
.subsection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 500;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.mini-card {
  background: white;
  border-radius: 8px;
  padding: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s;
}
.mini-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.mini-card h5 {
  margin: 0 0 6px;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mini-card .meta {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.mini-card {
  flex: 1 1 220px; /* 基础宽度 220px，可放大可缩小 */
  min-width: 220px;
}
</style>
