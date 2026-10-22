<template>
  <el-header class="app-header">
    <div class="header-content">
      <!-- Logo -->
      <h1 class="logo" @click="$router.push('/')">📝 文字社区</h1>

      <!-- 搜索框 -->
      <div class="search-input" style="position: relative">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索..."
          clearable
          @keyup.enter="goToSearch"
          @focus="handleInputFocus"
          @blur="handleInputBlur"
        >
          <template #prefix
            ><el-icon><Search /></el-icon
          ></template>
        </el-input>
        <!-- 搜索历史下拉（可保留） -->
        <!-- 下拉面板：根据状态显示历史或联想 -->
        <div
          v-if="(showHistory && searchHistory.length) || suggestionVisible"
          class="search-dropdown"
          @mousedown.prevent
        >
          <!-- 历史记录模式 -->
          <div v-if="showHistory && searchKeyword.trim() === ''">
            <div class="dropdown-header">
              <span>最近搜索</span>
              <el-button link size="small" @click="clearSearchHistory">清空</el-button>
            </div>
            <ul class="dropdown-list">
              <li v-for="item in searchHistory" :key="item" @click="searchFromHistory(item)">
                <el-icon><Search /></el-icon>
                <span>{{ item }}</span>
              </li>
            </ul>
          </div>

          <!-- 联想标签模式 -->
          <div v-if="suggestionVisible">
            <ul class="dropdown-list">
              <li v-for="tag in suggestions" :key="tag.id" @click="goToTag(tag)">
                <el-icon><Search /></el-icon>
                <span>{{ tag.name }}</span>
                <el-tag size="small" type="info" style="margin-left: auto">{{
                  getClassificationName(tag.classification)
                }}</el-tag>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 导航按钮 -->
      <div class="nav-buttons">
        <el-button
          v-if="userStore.user?.role === 'admin'"
          type="info"
          size="default"
          @click="$router.push('/admin')"
          >⚙️ 管理</el-button
        >
      </div>

      <!-- 用户信息与操作 -->
      <div class="user-area">
        <!-- 通知悬浮窗（与之前相同） -->
        <el-popover
          ref="notifyPopover"
          placement="bottom-end"
          width="360"
          trigger="hover"
          :show-after="200"
          :hide-after="300"
          @show="onPopoverShow"
        >
          <template #reference>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
              <el-button :icon="Bell" circle />
            </el-badge>
          </template>
          <!-- 通知内容：与 HomeView 中的相同，可提取为独立组件，但暂时放在此处 -->
          <div class="notification-tabs-popover">
            <div class="tab-header">
              <div
                class="tab-item"
                :class="{ active: activePopoverTab === 'like_favorite' }"
                @click="switchPopoverTab('like_favorite')"
              >
                👍 点赞与收藏
              </div>
              <div
                class="tab-item"
                :class="{ active: activePopoverTab === 'reply' }"
                @click="switchPopoverTab('reply')"
              >
                💬 回复我的
              </div>
              <div
                class="tab-item"
                :class="{ active: activePopoverTab === 'follow' }"
                @click="switchPopoverTab('follow')"
              >
                👤 关注了我
              </div>
            </div>
            <div class="message-list" v-loading="popoverLoading">
              <div
                v-for="group in currentGroups"
                :key="group.groupKey"
                class="message-item"
                :class="{ unread: group.unreadCount > 0 }"
                @click="goToGroupDetail(group)"
              >
                <div class="title">{{ group.title }}</div>
                <div class="preview">{{ group.previewContent }}</div>
                <div class="time">{{ formatDate(group.latestTime) }}</div>
              </div>
              <el-empty
                v-if="!popoverLoading && currentGroups.length === 0"
                description="暂无消息"
              />
            </div>
            <div class="tab-footer">
              <el-button
                link
                type="primary"
                size="small"
                @click="goToNotificationsWithFilter(activePopoverTab)"
              >
                查看全部
              </el-button>
            </div>
          </div>
        </el-popover>

        <el-dropdown v-if="userStore.isAuthenticated" @command="handleCreateCommand">
          <el-button type="primary" circle :icon="Plus" size="large" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="article">写文章</el-dropdown-item>
              <el-dropdown-item divided>设定</el-dropdown-item>
              <el-dropdown-item command="setting_character" style="padding-left: 30px"
                >人物</el-dropdown-item
              >
              <el-dropdown-item command="setting_item" style="padding-left: 30px"
                >物品</el-dropdown-item
              >
              <el-dropdown-item command="setting_location" style="padding-left: 30px"
                >地区</el-dropdown-item
              >
              <el-dropdown-item command="setting_background" style="padding-left: 30px"
                >背景</el-dropdown-item
              >
              <el-dropdown-item command="setting_rule" style="padding-left: 30px"
                >规则</el-dropdown-item
              >
              <el-dropdown-item command="setting_custom" style="padding-left: 30px"
                >自定义</el-dropdown-item
              >
              <el-dropdown-item command="tag">创建标签</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <template v-if="userStore.isAuthenticated">
          <el-avatar :size="36" :src="avatarUrl" />
          <el-dropdown @command="handleCommand">
            <span class="username">
              {{ userStore.user?.username }}
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <div v-else>
          <el-button type="default" @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">注册</el-button>
        </div>
      </div>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Bell, Plus, Search } from '@element-plus/icons-vue'
import axios from '@/utils/axios'

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

interface NotificationItem {
  id: number
  content: string
  isRead: boolean
  createdAt: string
  targetType: string
  targetId: number
  commentId?: number
}

interface TagSuggestion {
  id: number
  name: string
  classification: string
  usageCount: number
}

interface PopoverInstance {
  hide: () => void
}

const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')
const showHistory = ref(false)
const searchHistory = ref<string[]>([])
const unreadCount = ref(0)
const avatarUrl = ref('')
const activePopoverTab = ref<'like_favorite' | 'reply' | 'follow'>('like_favorite')
const followGroups = ref<RawNotificationGroup[]>([])
const likeFavoriteGroups = ref<RawNotificationGroup[]>([])
const replyGroups = ref<RawNotificationGroup[]>([])
const popoverLoading = ref(false)

const suggestions = ref<TagSuggestion[]>([]) // 联想标签列表
const suggestionLoading = ref(false) // 联想加载状态
const suggestionVisible = ref(false) // 是否显示联想下拉
const notifyPopover = ref<PopoverInstance | null>(null)

const currentGroups = computed(() => {
  if (activePopoverTab.value === 'like_favorite') return likeFavoriteGroups.value
  if (activePopoverTab.value === 'reply') return replyGroups.value
  return followGroups.value
})

// 获取未读通知数
const fetchUnreadCount = async () => {
  try {
    const res = await axios.get('/notifications/unread-count')
    unreadCount.value = res.data.count
  } catch (e) {
    console.error('获取未读通知失败', e)
  }
}

const clearToken = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
    })
    .catch(() => {}) // 用户取消
}

// 生成头像  功能正常
const generateAvatar = () => {
  if (userStore.user?.username) {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7']
    const randomColor = colors[Math.floor(Math.random() * colors.length)] || '#FF6B6B'
    avatarUrl.value = `https://ui-avatars.com/api/?name=${userStore.user.username}&background=${randomColor.slice(1)}&color=fff`
  }
}

// 搜索跳转
const goToSearch = () => {
  if (searchKeyword.value.trim()) {
    saveSearchHistory(searchKeyword.value.trim())
    router.push({ path: '/search', query: { q: searchKeyword.value.trim() } })
    showHistory.value = false
  }
}

const saveSearchHistory = (keyword: string) => {
  if (!keyword.trim()) return

  // 1. 从当前内存列表拷贝，避免直接引用响应式变量
  let history = [...searchHistory.value]

  // 2. 去重：移除已存在的相同关键词
  history = history.filter((item) => item !== keyword)

  // 3. 将新关键词插入到数组最前面
  history.unshift(keyword)

  // 4. 限制最多保留 10 条
  if (history.length > 10) {
    history = history.slice(0, 10)
  }

  // 5. 同步更新内存和 localStorage
  searchHistory.value = history
  localStorage.setItem('searchHistory', JSON.stringify(history))
}

// 创建命令
const handleCreateCommand = (command: string) => {
  if (typeof command !== 'string') return //防止浏览器警告
  if (command === 'tag') router.push('/tags/create')
  else if (command === 'article') router.push('/articles/create')
  else if (command.startsWith('setting_')) {
    const category = command.replace('setting_', '')
    router.push(`/settings/create?category=${category}`)
  }
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    clearToken()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

const onPopoverShow = () => {
  loadPopoverMessages(activePopoverTab.value)
}

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

// 清空历史
const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
  ElMessage.success('历史记录已清空')
}

// 点击历史记录搜索
const searchFromHistory = (keyword: string) => {
  searchKeyword.value = keyword
  showHistory.value = false
  goToSearch()
}

// 切换 Tab
const switchPopoverTab = (tab: 'like_favorite' | 'reply' | 'follow') => {
  activePopoverTab.value = tab
  if (tab === 'like_favorite' && likeFavoriteGroups.value.length === 0) {
    loadPopoverMessages('like_favorite')
  } else if (tab === 'reply' && replyGroups.value.length === 0) {
    loadPopoverMessages('reply')
  } else if (tab === 'follow' && followGroups.value.length === 0) {
    loadPopoverMessages('follow')
  }
}

const goToGroupDetail = async (group: RawNotificationGroup) => {
  // 标记该分组内所有未读为已读
  notifyPopover.value?.hide() // 隐藏通知悬浮窗
  if (group.unreadCount > 0) {
    try {
      const res = await axios.get(`/notifications/groups/${group.groupKey}/items`)
      const items = res.data as NotificationItem[]
      const unreadIds = items.filter((i) => !i.isRead).map((i) => i.id)
      if (unreadIds.length > 0) {
        await axios.put('/notifications/read', unreadIds)
        group.unreadCount = 0
        fetchUnreadCount() // 刷新全局未读数
      }
    } catch {}
  }

  if (group.type === 'follow') {
    router.push(`/users/${group.targetId}`)
  } else {
    router.push(`/notifications/group/${group.groupKey}`)
  }
}

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return '未知时间'
  return new Date(dateString).toLocaleString('zh-CN')
}

// 跳转到通知页并携带filter
const goToNotificationsWithFilter = (filter: string) => {
  router.push({ path: '/notifications', query: { filter } })
}

const handleInputFocus = () => {
  if (searchKeyword.value.trim() === '') {
    showHistory.value = true
    suggestionVisible.value = false
  } else {
    showHistory.value = false
    suggestionVisible.value = suggestions.value.length > 0
  }
}

const handleInputBlur = () => {
  setTimeout(() => {
    showHistory.value = false
    suggestionVisible.value = false
  }, 200)
}

const goToTag = (tag: TagSuggestion) => {
  suggestionVisible.value = false
  showHistory.value = false
  router.push(`/tags/${tag.id}`)
}

const getClassificationName = (classification: string) => {
  const map: Record<string, string> = {
    article: '文章',
    character: '人物',
    item: '物品',
    location: '地区',
    background: '背景',
    rule: '规则',
    custom: '自定义',
  }
  return map[classification] || classification
}

watch(
  () => userStore.user,
  (newUser) => {
    if (newUser) generateAvatar()
  },
  { immediate: true },
)

watch(searchKeyword, async (val) => {
  if (!val || val.trim().length === 0) {
    suggestions.value = []
    suggestionVisible.value = false
    showHistory.value = true // 清空输入时显示历史
    return
  }
  // 输入非空：隐藏历史，启动联想
  showHistory.value = false
  suggestionLoading.value = true
  try {
    const res = await axios.get('/tags/search', { params: { keyword: val, size: 8 } })
    suggestions.value = res.data as TagSuggestion[]
    suggestionVisible.value = suggestions.value.length > 0
  } catch {
    suggestions.value = []
    suggestionVisible.value = false
  } finally {
    suggestionLoading.value = false
  }
})

onMounted(() => {
  fetchUnreadCount()

  const stored = localStorage.getItem('searchHistory')
  if (stored) {
    try {
      const parsed = JSON.parse(stored)
      searchHistory.value = Array.isArray(parsed) ? parsed : []
    } catch {
      searchHistory.value = []
    }
  }
})
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 20px;
  color: white;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}
.logo {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
}
.nav-buttons {
  display: flex;
  gap: 10px;
}
.user-area {
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
.search-input {
  flex: 1;
  max-width: 300px;
  margin: 0 20px;
}

.search-input {
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
  transition: background 0.2s;
  color: #303133;
}

.search-history li:hover {
  background-color: #f5f7fa;
}

.search-history li .el-icon {
  color: #909399;
}

.notification-tabs-popover {
  padding: 4px 0;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid #ebeef5;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}

.tab-item.active {
  color: #409eff;
  font-weight: 500;
  border-bottom: 2px solid #409eff;
}

.tab-item:hover {
  color: #409eff;
}

.tab-footer {
  padding: 10px 12px;
  text-align: center;
}

.message-list {
  max-height: 300px;
  overflow-y: auto;
}
.message-item {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.message-item.unread {
  background-color: #ecf5ff;
}

.message-item:hover {
  background-color: #f5f7fa;
}
.message-item .content {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}
.message-item .time {
  font-size: 12px;
  color: #909399;
}

.search-dropdown {
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
.dropdown-header {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
}
.dropdown-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 280px;
  overflow-y: auto;
}
.dropdown-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  transition: background 0.2s;
}
.dropdown-list li:hover {
  background-color: #f5f7fa;
}

/* 未登录按钮通用样式 */
.auth-btn {
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: transparent;
  transition: all 0.2s;
}

.auth-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: white;
}

/* 注册按钮（primary）可以使用 Element Plus 默认，但加强对比 */
.register-btn {
  background: #409eff;
  border-color: #409eff;
  color: white;
}
.register-btn:hover {
  background: #337ecc;
  border-color: #337ecc;
}
</style>
