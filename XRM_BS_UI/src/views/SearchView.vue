<template>
  <div class="search-container">
    <el-main>
      <el-tabs v-model="activeTab" @tab-click="onTabChange">
        <!-- 文章标签页 -->
        <el-tab-pane label="文章" name="articles">
          <div class="filter-bar">
            <el-select
              v-model="articleFilters.tags"
              multiple
              filterable
              remote
              reserve-keyword
              placeholder="标签筛选"
              clearable
              collapse-tags
              :remote-method="searchArticleTags"
              :loading="articleTagSearchLoading"
            >
              <el-option v-for="tag in articleTagOptions" :key="tag" :label="tag" :value="tag" />
            </el-select>
            <el-select v-model="articleFilters.sortBy" placeholder="排序" style="width: 120px">
              <el-option label="发布时间" value="createdAt" />
              <el-option label="浏览量" value="viewCount" />
              <el-option label="点赞量" value="likeCount" />
              <el-option label="引用数" value="referenceCount" />
            </el-select>
            <el-radio-group v-model="articleFilters.sortDirection">
              <el-radio label="DESC">降序</el-radio>
              <el-radio label="ASC">升序</el-radio>
            </el-radio-group>
            <el-button type="primary" @click="searchArticles">筛选</el-button>
          </div>
          <div v-loading="articlesLoading" class="results">
            <div
              v-for="item in articles"
              :key="item.id"
              class="result-card"
              @click="viewArticle(item.id)"
            >
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <div class="meta">
                <span>作者：{{ item.authorUsername }}</span>
                <span>浏览：{{ item.viewCount }}</span>
                <span>点赞：{{ item.likeCount }}</span>
                <span>引用数：{{ item.referenceCount || 0 }}</span>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="tags">
                <el-tag v-for="tag in item.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <el-pagination
              v-if="articlesTotal > articlePageSize"
              background
              layout="prev, pager, next"
              :current-page="articlePage"
              :page-size="articlePageSize"
              :total="articlesTotal"
              @current-change="loadArticlesPage"
              class="pagination"
            />
            <el-empty v-if="!articlesLoading && articles.length === 0" description="暂无文章" />
          </div>
        </el-tab-pane>

        <!-- 设定标签页 -->
        <el-tab-pane label="设定" name="settings">
          <div class="filter-bar">
            <el-select
              v-model="settingFilters.category"
              placeholder="设定分类"
              clearable
              style="width: 120px"
            >
              <el-option label="人物" value="character" />
              <el-option label="物品" value="item" />
              <el-option label="地区" value="location" />
              <el-option label="背景" value="background" />
              <el-option label="规则" value="rule" />
              <el-option label="自定义" value="custom" />
            </el-select>
            <el-select
              v-model="settingFilters.tags"
              multiple
              filterable
              remote
              reserve-keyword
              placeholder="标签筛选"
              clearable
              collapse-tags
              :remote-method="searchSettingTags"
              :loading="settingTagSearchLoading"
            >
              <el-option v-for="tag in settingTagOptions" :key="tag" :label="tag" :value="tag" />
            </el-select>
            <el-select v-model="settingFilters.sortBy" placeholder="排序" style="width: 120px">
              <el-option label="发布时间" value="createdAt" />
              <el-option label="浏览量" value="viewCount" />
              <el-option label="点赞量" value="likeCount" />
            </el-select>
            <el-radio-group v-model="settingFilters.sortDirection">
              <el-radio label="DESC">降序</el-radio>
              <el-radio label="ASC">升序</el-radio>
            </el-radio-group>
            <el-button type="primary" @click="searchSettings">筛选</el-button>
          </div>
          <div v-loading="settingsLoading" class="results">
            <div
              v-for="item in settings"
              :key="item.id"
              class="result-card"
              @click="viewSetting(item.id)"
            >
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <div class="meta">
                <span>作者：{{ item.authorUsername }}</span>
                <span>浏览：{{ item.viewCount }}</span>
                <span>点赞：{{ item.likeCount }}</span>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="tags">
                <el-tag v-for="tag in item.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <el-pagination
              v-if="settingsTotal > settingPageSize"
              background
              layout="prev, pager, next"
              :current-page="settingPage"
              :page-size="settingPageSize"
              :total="settingsTotal"
              @current-change="loadSettingsPage"
              class="pagination"
            />
            <el-empty v-if="!settingsLoading && settings.length === 0" description="暂无设定" />
          </div>
        </el-tab-pane>

        <!-- 标签标签页 -->
        <el-tab-pane label="标签" name="tags">
          <div class="filter-bar">
            <el-select
              v-model="tagFilters.classification"
              placeholder="标签类型"
              clearable
              style="width: 150px"
            >
              <el-option label="文章标签" value="article" />
              <el-option label="人物标签" value="character" />
              <el-option label="物品标签" value="item" />
              <el-option label="地区标签" value="location" />
              <el-option label="背景标签" value="background" />
              <el-option label="规则标签" value="rule" />
              <el-option label="自定义标签" value="custom" />
            </el-select>
            <el-button type="primary" @click="searchTags">筛选</el-button>
          </div>
          <div v-loading="tagsLoading" class="results">
            <div v-for="tag in tags" :key="tag.id" class="result-card" @click="viewTag(tag.id)">
              <h3>{{ tag.name }}</h3>
              <p>{{ tag.description?.replace(/<[^>]*>/g, '').slice(0, 100) }}</p>
              <div class="meta">
                <span>使用次数：{{ tag.usageCount }}</span>
                <span>分类：{{ getClassificationName(tag.classification) }}</span>
              </div>
            </div>
            <el-pagination
              v-if="tagsTotal > tagPageSize"
              background
              layout="prev, pager, next"
              :current-page="tagPage"
              :page-size="tagPageSize"
              :total="tagsTotal"
              @current-change="loadTagsPage"
              class="pagination"
            />
            <el-empty v-if="!tagsLoading && tags.length === 0" description="暂无标签" />
          </div>
        </el-tab-pane>

        <!-- 用户标签页 -->
        <el-tab-pane label="用户" name="users">
          <div v-loading="usersLoading" class="results">
            <div v-for="user in users" :key="user.id" class="result-card">
              <div class="user-content" @click="viewUser(user.id)">
                <h3>{{ user.username }}</h3>
                <p>ID：{{ user.id }}</p>
                <div class="meta">
                  <span>角色：{{ user.role === 'admin' ? '管理员' : '普通用户' }}</span>
                  <span>注册时间：{{ formatDate(user.createdAt) }}</span>
                </div>
              </div>
              <div class="follow-action">
                <el-button
                  v-if="userStore.isAuthenticated && user.id !== userStore.user?.id"
                  :type="user.following ? 'default' : 'primary'"
                  size="small"
                  @click.stop="toggleFollowUser(user)"
                  :loading="user.followLoading"
                >
                  {{ user.following ? '已关注' : '+ 关注' }}
                </el-button>
              </div>
            </div>
            <el-pagination
              v-if="usersTotal > userPageSize"
              background
              layout="prev, pager, next"
              :current-page="userPage"
              :page-size="userPageSize"
              :total="usersTotal"
              @current-change="loadUsersPage"
              class="pagination"
            />
            <el-empty v-if="!usersLoading && users.length === 0" description="暂无用户" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { AxiosError } from 'axios'

interface ArticleItem {
  id: number
  title: string
  summary: string
  authorUsername: string
  viewCount: number
  likeCount: number
  referenceCount?: number
  createdAt: string
  tags: string[]
}

interface SettingItem {
  id: number
  title: string
  summary: string
  authorUsername: string
  viewCount: number
  likeCount: number
  createdAt: string
  tags: string[]
}

interface TagItem {
  id: number
  name: string
  description?: string
  usageCount: number
  classification: string
}

interface UserSearchItem {
  id: number
  username: string
  email: string
  role: string
  createdAt: string
  following?: boolean
  followLoading?: boolean
}

interface UserSearchItem {
  id: number
  username: string
  email: string
  role: string
  createdAt: string
  following?: boolean // 当前用户是否关注了该用户
  followLoading?: boolean // 关注按钮加载状态
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 全局关键词
const globalKeyword = ref('')
const activeTab = ref('articles')

// ---------- 文章搜索 ----------
const articles = ref<ArticleItem[]>([])
const articlesTotal = ref(0)
const articlePage = ref(1)
const articlePageSize = ref(10)
const articlesLoading = ref(false)
const articleFilters = ref({
  tags: [] as string[],
  sortBy: 'createdAt',
  sortDirection: 'DESC',
})

const articleTagOptions = ref<string[]>([])
const searchHistory = ref<string[]>([])
// const showHistory = ref(false)
// const searchInputRef = ref(null)

const loadUsersPage = (page: number) => searchUsers(page)

const loadArticlesPage = (page: number) => searchArticles(page)

const loadSettingsPage = (page: number) => searchSettings(page)

// ---------- 标签搜索 ----------
const tags = ref<TagItem[]>([])
const tagsTotal = ref(0)
const tagPage = ref(1)
const tagPageSize = ref(20)
const tagsLoading = ref(false)
const tagFilters = ref({
  classification: '',
})

interface TagNameItem {
  name: string
  [key: string]: unknown
}

const loadTagsPage = (page: number) => searchTags(page)

// ---------- 用户搜索 ----------
const users = ref<UserSearchItem[]>([])
const usersTotal = ref(0)
const userPage = ref(1)
const userPageSize = ref(10)
const usersLoading = ref(false)

// ---------- 设定搜索 ----------
const settings = ref<SettingItem[]>([])
const settingsTotal = ref(0)
const settingPage = ref(1)
const settingPageSize = ref(10)
const settingsLoading = ref(false)
const settingFilters = ref({
  category: '',
  tags: [] as string[],
  sortBy: 'createdAt',
  sortDirection: 'DESC',
})
const settingTagOptions = ref<string[]>([])

// 跳转
const viewArticle = (id: number) => router.push(`/articles/${id}`)
const viewSetting = (id: number) => router.push(`/settings/${id}`)
const viewTag = (id: number) => router.push(`/tags/${id}`)
const viewUser = (id: number) => {
  router.push(`/users/${id}`)
}

const searchSettings = async (page = 1) => {
  settingsLoading.value = true
  try {
    const res = await axios.post('/settings/search', {
      keyword: globalKeyword.value,
      category: settingFilters.value.category,
      tags: settingFilters.value.tags,
      sortBy: settingFilters.value.sortBy,
      sortDirection: settingFilters.value.sortDirection,
      page: page - 1,
      size: settingPageSize.value,
    })
    settings.value = res.data.content
    settingsTotal.value = res.data.totalElements
    settingPage.value = page
  } catch {
    ElMessage.error('搜索设定失败')
  } finally {
    settingsLoading.value = false
  }
}

const searchArticles = async (page = 1) => {
  articlesLoading.value = true
  try {
    const res = await axios.post('/articles/search', {
      keyword: globalKeyword.value,
      tags: articleFilters.value.tags,
      sortBy: articleFilters.value.sortBy,
      sortDirection: articleFilters.value.sortDirection,
      page: page - 1,
      size: articlePageSize.value,
    })
    articles.value = res.data.content
    articlesTotal.value = res.data.totalElements
    articlePage.value = page
  } catch {
    ElMessage.error('搜索文章失败')
  } finally {
    articlesLoading.value = false
  }
}

const searchTags = async (page = 1) => {
  tagsLoading.value = true
  try {
    const res = await axios.post('/tags/search', {
      keyword: globalKeyword.value,
      classification: tagFilters.value.classification || null,
      page: page - 1,
      size: tagPageSize.value,
    })
    tags.value = res.data.content
    tagsTotal.value = res.data.totalElements
    tagPage.value = page
  } catch {
    ElMessage.error('搜索标签失败')
  } finally {
    tagsLoading.value = false
  }
}

const searchUsers = async (page = 1) => {
  if (!globalKeyword.value.trim()) {
    ElMessage.warning('请输入用户名或ID')
    return
  }
  usersLoading.value = true
  try {
    const res = await axios.post('/users/search', {
      keyword: globalKeyword.value,
      page: page - 1,
      size: userPageSize.value,
    })
    users.value = res.data.content
    usersTotal.value = res.data.totalElements
    userPage.value = page
    // 附加关注状态
    await attachFollowStatusToUsers(users.value)
  } catch {
    ElMessage.error('搜索用户失败')
  } finally {
    usersLoading.value = false
  }
}

const attachFollowStatusToUsers = async (userList: UserSearchItem[]) => {
  if (!userStore.isAuthenticated || userList.length === 0) return
  const userIds = userList.map((u) => u.id).filter((id) => id !== userStore.user?.id)
  if (userIds.length === 0) return
  try {
    const res = await axios.post('/follow/batch-status', userIds)
    const statusMap = res.data as Record<number, boolean>
    userList.forEach((u) => {
      if (u.id !== userStore.user?.id) {
        u.following = statusMap[u.id] || false
      }
    })
  } catch {
    await Promise.all(
      userList.map(async (u) => {
        if (u.id === userStore.user?.id) return
        try {
          const s = await axios.get(`/follow/${u.id}/status`)
          u.following = s.data.following
        } catch {
          u.following = false
        }
      }),
    )
  }
}

const toggleFollowUser = async (user: UserSearchItem) => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    return
  }
  user.followLoading = true
  try {
    if (user.following) {
      await axios.delete(`/follow/${user.id}`)
      user.following = false
      ElMessage.success('已取消关注')
    } else {
      await axios.post(`/follow/${user.id}`)
      user.following = true
      ElMessage.success('关注成功')
    }
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    user.followLoading = false
  }
}

// ---------- 辅助函数 ----------
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

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

// 切换标签页时触发搜索
const onTabChange = () => {
  if (activeTab.value === 'articles') searchArticles()
  else if (activeTab.value === 'settings') searchSettings()
  else if (activeTab.value === 'tags') searchTags()
  else if (activeTab.value === 'users') searchUsers()
}

// 修改 searchAll，保存历史
const searchAll = () => {
  if (!globalKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  // saveSearchHistory(globalKeyword.value.trim())
  // 重置页码等...
  articlePage.value = 1
  settingPage.value = 1
  tagPage.value = 1
  userPage.value = 1
  onTabChange()
}

// 加载历史
const loadSearchHistory = () => {
  const stored = localStorage.getItem('searchHistory')
  if (stored) {
    searchHistory.value = JSON.parse(stored)
  } else {
    searchHistory.value = []
  }
}

// 文章标签远程搜索
const articleTagSearchLoading = ref(false)
const searchArticleTags = async (query: string) => {
  if (!query.trim()) {
    articleTagOptions.value = []
    return
  }
  articleTagSearchLoading.value = true
  try {
    const res = await axios.get('/tags/search', {
      params: { keyword: query, classification: 'article', size: 20 },
    })
    const tags = res.data.data || res.data
    articleTagOptions.value = (tags as TagNameItem[]).map((t) => t.name)
  } catch (e) {
    console.error('搜索文章标签失败', e)
  } finally {
    articleTagSearchLoading.value = false
  }
}

// 设定标签远程搜索
const settingTagSearchLoading = ref(false)
const searchSettingTags = async (query: string) => {
  if (!query.trim()) {
    settingTagOptions.value = []
    return
  }
  settingTagSearchLoading.value = true
  try {
    const res = await axios.get('/tags/search', {
      params: { keyword: query, size: 20 },
    })
    const tags = res.data.data || res.data
    settingTagOptions.value = (tags as TagNameItem[]).map((t) => t.name)
  } catch (e) {
    console.error('搜索设定标签失败', e)
  } finally {
    settingTagSearchLoading.value = false
  }
}

// 监听标签页切换，自动搜索
watch(activeTab, (newTab) => {
  if (globalKeyword.value.trim()) {
    if (newTab === 'articles') searchArticles()
    else if (newTab === 'settings') searchSettings()
    else if (newTab === 'tags') searchTags()
    else if (newTab === 'users') searchUsers()
  }
})

// 监听路由 query.q 变化，自动触发搜索
watch(
  () => route.query.q,
  (newKeyword) => {
    if (newKeyword && typeof newKeyword === 'string') {
      globalKeyword.value = newKeyword
      searchAll()
    }
  },
  { immediate: true }, // 页面首次加载也会执行
)

// 初始化时从路由 query 获取关键词
onMounted(() => {
  loadSearchHistory()
})
</script>

<style scoped>
.search-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 10;
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
.search-bar {
  display: flex;
  gap: 10px;
}
.el-main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding-top: 20px;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}
.filter-tip {
  font-size: 14px;
  color: #606266;
}
.results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.result-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
  cursor: pointer;
}
.result-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
.result-card h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #409eff;
}
.result-card p {
  /* 原有样式 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2; /* 新增标准属性 */
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-card .meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.result-card .tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pagination {
  margin-top: 20px;
  text-align: center;
}
.search-bar-wrapper {
  position: relative;
  display: inline-block;
}

.search-bar {
  position: relative;
}
.search-history {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  margin-top: 4px;
}
.history-header {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
}
.search-history ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.search-history li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
  color: #303133 !important;
  transition: background 0.2s;
}
.search-history li:hover {
  background-color: #f5f7fa;
}
.search-history li .el-icon {
  color: #909399;
}

.result-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.user-content {
  flex: 1;
  cursor: pointer;
}
.follow-action {
  margin-left: 16px;
}
</style>
