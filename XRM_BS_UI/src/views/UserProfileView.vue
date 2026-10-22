<template>
  <div class="user-profile-container">
    <el-main>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="notFound" class="not-found">
        <el-result icon="error" title="用户不存在" sub-title="该用户可能已被删除">
          <template #extra>
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
          </template>
        </el-result>
      </div>
      <div v-else-if="user" class="user-info-card">
        <div class="avatar">
          <el-avatar :size="80" :src="avatarUrl" />
        </div>
        <div class="info">
          <h2>{{ user.username }}</h2>
          <div class="meta">
            <span>ID：{{ user.id }}</span>
            <span>角色：{{ user.role === 'admin' ? '管理员' : '普通用户' }}</span>
            <span>注册时间：{{ formatDate(user.createdAt) }}</span>
          </div>
          <div class="follow-stats">
            <span @click="activeTab = 'following'">{{ followingCount }} 关注</span>
            <span @click="activeTab = 'followers'">{{ followersCount }} 粉丝</span>
          </div>
          <div class="follow-actions" v-if="isSelf === false">
            <el-button
              :type="isFollowing ? 'default' : 'primary'"
              @click="toggleFollow"
              :loading="followLoading"
            >
              {{ isFollowing ? '已关注' : '+ 关注' }}
            </el-button>
          </div>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange" class="content-tabs">
        <el-tab-pane label="文章" name="articles">
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
                <span>浏览：{{ item.viewCount }}</span>
                <span>点赞：{{ item.likeCount }}</span>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="tags">
                <el-tag v-for="tag in item.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <el-pagination
              v-if="articlesTotal > pageSize"
              background
              layout="prev, pager, next"
              :current-page="articlePage"
              :page-size="pageSize"
              :total="articlesTotal"
              @current-change="loadArticlesPage"
            />
            <el-empty v-if="!articlesLoading && articles.length === 0" description="暂无文章" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="设定" name="settings">
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
                <span>浏览：{{ item.viewCount }}</span>
                <span>点赞：{{ item.likeCount }}</span>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="tags">
                <el-tag v-for="tag in item.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <el-pagination
              v-if="settingsTotal > pageSize"
              background
              layout="prev, pager, next"
              :current-page="settingPage"
              :page-size="pageSize"
              :total="settingsTotal"
              @current-change="loadSettingsPage"
            />
            <el-empty v-if="!settingsLoading && settings.length === 0" description="暂无设定" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="关注" name="following">
          <div v-loading="followingLoading" class="user-list">
            <div
              v-for="item in followingList"
              :key="item.id"
              class="user-item"
              @click="viewUser(item.id)"
            >
              <el-avatar :size="40" :src="getAvatarUrl(item.username)" />
              <div class="user-info">
                <span class="username">{{ item.username }}</span>
                <span class="role">{{ item.role === 'admin' ? '管理员' : '用户' }}</span>
              </div>
              <el-button
                v-if="isSelf === false && userStore.user?.id !== item.id"
                :type="item.following ? 'default' : 'primary'"
                size="small"
                @click.stop="toggleFollowUser(item)"
              >
                {{ item.following ? '已关注' : '+ 关注' }}
              </el-button>
            </div>
            <el-empty
              v-if="!followingLoading && followingList.length === 0"
              description="暂无关注"
            />
            <el-pagination
              v-if="followingTotal > pageSize"
              background
              layout="prev, pager, next"
              :current-page="followingPage"
              :page-size="pageSize"
              :total="followingTotal"
              @current-change="loadFollowing"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="粉丝" name="followers">
          <div v-loading="followersLoading" class="user-list">
            <div
              v-for="item in followersList"
              :key="item.id"
              class="user-item"
              @click="viewUser(item.id)"
            >
              <el-avatar :size="40" :src="getAvatarUrl(item.username)" />
              <div class="user-info">
                <span class="username">{{ item.username }}</span>
                <span class="role">{{ item.role === 'admin' ? '管理员' : '用户' }}</span>
              </div>
              <el-button
                v-if="isSelf === false && userStore.user?.id !== item.id"
                :type="item.following ? 'default' : 'primary'"
                size="small"
                @click.stop="toggleFollowUser(item)"
              >
                {{ item.following ? '已关注' : '+ 关注' }}
              </el-button>
            </div>
            <el-empty
              v-if="!followersLoading && followersList.length === 0"
              description="暂无粉丝"
            />
            <el-pagination
              v-if="followersTotal > pageSize"
              background
              layout="prev, pager, next"
              :current-page="followersPage"
              :page-size="pageSize"
              :total="followersTotal"
              @current-change="loadFollowers"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import type { AxiosError } from 'axios' // 新增导入
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userId = route.params.id as string

interface UserInfo {
  id: number
  username: string
  role: string
  createdAt: string
}

interface ContentItem {
  id: number
  title: string
  summary: string
  viewCount: number
  likeCount: number
  createdAt: string
  tags: string[]
}

interface UserPublicResponse {
  id: number
  username: string
  role: string
  createdAt: string
  following?: boolean
}

const user = ref<UserInfo | null>(null)
const loading = ref(false)
const notFound = ref(false)
const avatarUrl = ref('')

const activeTab = ref('articles')
const articles = ref<ContentItem[]>([])
const articlesTotal = ref(0)
const articlePage = ref(1)
const articlesLoading = ref(false)
const settings = ref<ContentItem[]>([])
const settingsTotal = ref(0)
const settingPage = ref(1)
const settingsLoading = ref(false)
const pageSize = ref(10)

const isSelf = computed(() => userStore.user?.id === user.value?.id)
const isFollowing = ref(false)
const followLoading = ref(false)
const followingCount = ref(0)
const followersCount = ref(0)
const followingList = ref<UserPublicResponse[]>([])
const followersList = ref<UserPublicResponse[]>([])

// 关注/粉丝列表相关
const followingLoading = ref(false)
const followersLoading = ref(false)
const followingPage = ref(1)
const followersPage = ref(1)
const followingTotal = ref(0)
const followersTotal = ref(0)

const loadSettingsPage = (page: number) => loadSettings(page)

const viewArticle = (id: number) => router.push(`/articles/${id}`)
const viewSetting = (id: number) => router.push(`/settings/${id}`)

const loadArticlesPage = (page: number) => loadArticles(page)

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const loadUserInfo = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/users/${userId}`)
    user.value = res.data
    avatarUrl.value = `https://ui-avatars.com/api/?name=${user.value?.username || 'User'}&background=409eff&color=fff`

    followingCount.value = res.data.followingCount || 0
    followersCount.value = res.data.followerCount || 0

    await Promise.all([
      fetchFollowStatus(),
      loadFollowCounts(), // 新增
    ])
  } catch (err: unknown) {
    const error = err as AxiosError
    if (error.response?.status === 404) {
      notFound.value = true
    } else {
      ElMessage.error('加载用户信息失败')
    }
  } finally {
    loading.value = false
  }
}

const loadArticles = async (page = 1) => {
  articlesLoading.value = true
  try {
    const res = await axios.get(`/articles/user/${userId}`, {
      params: { page: page - 1, size: pageSize.value },
    })
    articles.value = res.data.content
    articlesTotal.value = res.data.totalElements
    articlePage.value = page
  } catch {
    ElMessage.error('加载文章失败')
  } finally {
    articlesLoading.value = false
  }
}

const loadSettings = async (page = 1) => {
  settingsLoading.value = true
  try {
    const res = await axios.get(`/settings/user/${userId}`, {
      params: { page: page - 1, size: pageSize.value },
    })
    settings.value = res.data.content
    settingsTotal.value = res.data.totalElements
    settingPage.value = page
  } catch {
    ElMessage.error('加载设定失败')
  } finally {
    settingsLoading.value = false
  }
}

// 获取关注状态和计数
const loadFollowInfo = async () => {
  if (!user.value) return
  try {
    const [statusRes, countsRes] = await Promise.all([
      axios.get(`/follow/${user.value.id}/status`),
      axios.get(`/follow/${user.value.id}/counts`),
    ])
    isFollowing.value = statusRes.data.following
    followingCount.value = countsRes.data.following
    followersCount.value = countsRes.data.followers
  } catch (error) {
    console.error('获取关注信息失败', error)
  }
}

// 关注/取关
const toggleFollow = async () => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!user.value) return
  followLoading.value = true
  try {
    if (isFollowing.value) {
      await axios.delete(`/follow/${user.value.id}`)
      isFollowing.value = false
      followersCount.value-- // 被关注者的粉丝数减1
    } else {
      await axios.post(`/follow/${user.value.id}`)
      isFollowing.value = true
      followersCount.value++ // 被关注者的粉丝数加1
    }
    // 同时刷新自己的关注数（如果是本人主页）
    if (isSelf.value) {
      await loadFollowCounts()
    }
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

// 加载关注列表
const loadFollowing = async (page = 1) => {
  if (!user.value) return
  followingLoading.value = true
  try {
    const res = await axios.get(`/follow/${user.value.id}/following`, {
      params: { page: page - 1, size: pageSize.value },
    })
    // 为每个用户附加 following 状态（可批量查询优化）
    const users = res.data.content as UserPublicResponse[]
    // 批量查询当前用户与列表中用户的关注关系（可选优化）
    followingList.value = users
    followingTotal.value = res.data.totalElements
    followingPage.value = page
    console.log('关注列表数据：', res.data.content)
  } catch {
    ElMessage.error('加载关注列表失败')
  } finally {
    followingLoading.value = false
  }
}

// 加载粉丝列表
const loadFollowers = async (page = 1) => {
  if (!user.value) return
  followersLoading.value = true
  try {
    const res = await axios.get(`/follow/${user.value.id}/followers`, {
      params: { page: page - 1, size: pageSize.value },
    })
    const users = res.data.content as UserPublicResponse[]
    followersList.value = users
    followersTotal.value = res.data.totalElements
    followersPage.value = page
  } catch {
    ElMessage.error('加载粉丝列表失败')
  } finally {
    followersLoading.value = false
  }
}

// Tab切换处理
const onTabChange = (tabName: string) => {
  if (tabName === 'following') {
    loadFollowing(1)
  } else if (tabName === 'followers') {
    loadFollowers(1)
  } else if (tabName === 'articles') {
    loadArticles(1)
  } else if (tabName === 'settings') {
    loadSettings(1)
  }
}

const getAvatarUrl = (username: string) => {
  return `https://ui-avatars.com/api/?name=${username}&background=409eff&color=fff`
}

const viewUser = (userId: number) => {
  router.push(`/users/${userId}`)
}

const toggleFollowUser = async (user: UserPublicResponse & { following?: boolean }) => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    if (user.following) {
      await axios.delete(`/follow/${user.id}`)
      user.following = false
      followersCount.value--
      ElMessage.success('已取消关注')
    } else {
      await axios.post(`/follow/${user.id}`)
      user.following = true
      followersCount.value++
      ElMessage.success('关注成功')
    }
    // 如果当前在粉丝页，刷新粉丝列表以更新按钮状态
    if (activeTab.value === 'followers') {
      loadFollowers(followersPage.value)
    } else if (activeTab.value === 'following') {
      loadFollowing(followingPage.value)
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const fetchFollowStatus = async () => {
  if (!user.value || !userStore.isAuthenticated) return
  // 如果查看的是自己，不需要获取关注状态
  if (user.value.id === userStore.user?.id) {
    isFollowing.value = false
    return
  }
  try {
    const res = await axios.get(`/follow/${user.value.id}/status`)
    isFollowing.value = res.data.following
  } catch (error) {
    console.error('获取关注状态失败', error)
  }
}

const loadFollowCounts = async () => {
  if (!user.value) return
  try {
    const res = await axios.get(`/follow/${user.value.id}/counts`)
    followingCount.value = res.data.following
    followersCount.value = res.data.followers
  } catch (error) {
    console.error('获取关注数失败', error)
  }
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadUserInfo() // 重新获取用户信息
      followingList.value = []
      followersList.value = []
      followingTotal.value = 0
      followersTotal.value = 0
    }
  },
)

// 用户信息加载完成后，如果当前在关注/粉丝 tab，自动加载
watch(
  user,
  (newUser) => {
    if (newUser?.id) {
      if (activeTab.value === 'following') loadFollowing(1)
      else if (activeTab.value === 'followers') loadFollowers(1)
    }
  },
  { immediate: true },
)

// 监听标签页切换，自动加载数据
watch(activeTab, (newTab) => {
  if (newTab === 'articles') {
    loadArticles()
  } else if (newTab === 'settings') {
    loadSettings()
  }
})

onMounted(() => {
  loadUserInfo()
  loadFollowInfo()
  if (activeTab.value === 'articles') loadArticles()
  else if (activeTab.value === 'settings') loadSettings()
})
</script>

<style scoped>
/* 保持原有样式不变 */
.user-profile-container {
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
.user-info-card {
  background: white;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 30px;
  display: flex;
  align-items: center;
  gap: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.info h2 {
  margin: 0 0 12px 0;
}
.meta {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 14px;
}
.content-tabs {
  background: white;
  border-radius: 12px;
  padding: 20px;
}
.results {
  margin-top: 20px;
}
.result-card {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.result-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}
.result-card h3 {
  margin: 0 0 8px 0;
  color: #409eff;
}
.result-card p {
  margin: 0 0 12px 0;
  color: #606266;
}
.result-card .meta {
  font-size: 12px;
  gap: 16px;
  margin-bottom: 8px;
}
.result-card .tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.loading {
  text-align: center;
  padding: 50px;
}
.not-found {
  text-align: center;
  padding: 50px;
}

/* 用户列表项 */
.user-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}
.user-item:hover {
  background-color: #f5f7fa;
}

.user-info {
  flex: 1;
  margin-left: 12px;
}
.username {
  font-weight: 500;
  color: #303133;
}
.role {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

/* 确保用户列表容器有基本的内边距和背景 */
.user-list {
  margin-top: 12px;
}
</style>
