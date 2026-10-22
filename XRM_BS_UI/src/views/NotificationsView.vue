<template>
  <div class="notifications-page">
    <el-main>
      <el-card>
        <template #header>
          <div class="card-header">
            <span>我的通知</span>
            <div>
              <el-button type="danger" link @click="clearAll">清空所有</el-button>
              <el-button type="primary" link @click="markAllAsRead">全部已读</el-button>
            </div>
          </div>
        </template>

        <!-- 分类 Tab -->
        <el-tabs v-model="activeFilter" @tab-change="onFilterChange">
          <el-tab-pane label="点赞与收藏" name="like_favorite" />
          <el-tab-pane label="回复我的" name="reply" />
          <el-tab-pane label="关注了我" name="follow" />
        </el-tabs>

        <div v-loading="loading" class="group-list">
          <div
            v-for="group in groups"
            :key="group.groupKey"
            class="group-card"
            :class="{ unread: group.unreadCount > 0 }"
            @click="goToGroupDetail(group)"
          >
            <div class="group-title">
              <span class="title-text">{{ group.title }}</span>
              <el-badge v-if="group.unreadCount > 0" :value="group.unreadCount" type="danger" />
            </div>
            <div class="group-preview">{{ group.previewContent }}</div>
            <div class="group-footer">
              <span class="time">{{ formatDate(group.latestTime) }}</span>
              <span class="total">共 {{ group.totalCount }} 条</span>
            </div>
          </div>
          <el-empty v-if="!loading && groups.length === 0" description="暂无通知" />
          <el-pagination
            v-if="total > pageSize"
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            @current-change="loadPage"
            class="pagination"
          />
        </div>
      </el-card>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

// 类型定义
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
  items?: NotificationItem[]
}

interface NotificationGroup extends RawNotificationGroup {
  items: NotificationItem[]
  loading: boolean
}

interface NotificationItem {
  id: number
  type: string
  sourceUserId: number
  sourceUsername: string
  targetId: number
  targetType: string
  content: string
  isRead: boolean
  createdAt: string
  commentId?: number
}

interface LoadGroupsParams {
  page: number
  size: number
  filter?: string
}

const route = useRoute()
const router = useRouter()
const groups = ref<NotificationGroup[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const activeGroups = ref<string[]>([])
const activeFilter = ref('like_favorite')

const unreadCount = ref(0)

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const params: LoadGroupsParams = { page: page.value - 1, size: pageSize.value }
if (activeFilter.value !== 'like_favorite') {
  params.filter = activeFilter.value
}

const goToGroupDetail = async (group: NotificationGroup) => {
  // 1. 先标记该分组内所有未读为已读
  if (group.unreadCount > 0) {
    try {
      const res = await axios.get(`/notifications/groups/${group.groupKey}/items`)
      const items = res.data as NotificationItem[]
      const unreadIds = items.filter((i) => !i.isRead).map((i) => i.id)
      if (unreadIds.length > 0) {
        await axios.put('/notifications/read', unreadIds)
        group.unreadCount = 0
        // 同时更新顶部未读总数（可调用父组件方法或刷新全局状态）
        fetchUnreadCount() // 如果有这个方法
      }
    } catch (error) {
      console.error('标记已读失败', error)
    }
  }

  // 2. 根据类型跳转
  if (group.type === 'follow') {
    router.push(`/users/${group.targetId}`)
  } else {
    router.push(`/notifications/group/${group.groupKey}`)
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

// 加载分组列表
const loadGroups = async () => {
  loading.value = true
  try {
    const params: LoadGroupsParams = { page: page.value - 1, size: pageSize.value }

    let filterParam: string | undefined
    if (activeFilter.value === 'like_favorite') filterParam = 'like_favorite'
    else if (activeFilter.value === 'reply') filterParam = 'reply'
    else if (activeFilter.value === 'follow') filterParam = 'follow'

    if (filterParam) {
      params.filter = filterParam
    }

    if (activeFilter.value !== 'all') {
      params.filter = activeFilter.value
    }
    const res = await axios.get('/notifications', { params })
    groups.value = (res.data.content as RawNotificationGroup[]).map((g) => ({
      ...g,
      items: [],
      loading: false,
    }))
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

// 分页
const loadPage = (p: number) => {
  page.value = p
  loadGroups()
}

// 切换分类
const onFilterChange = () => {
  page.value = 1
  activeGroups.value = []
  loadGroups()
}

// 全部已读
const markAllAsRead = async () => {
  try {
    await axios.put('/notifications/read-all')
    groups.value.forEach((g) => {
      g.unreadCount = 0
      g.items.forEach((i) => (i.isRead = true))
    })
    ElMessage.success('已全部标记为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

// 根据路由参数初始化 activeFilter
const initFilterFromRoute = () => {
  const filter = route.query.filter as string
  if (filter === 'like_favorite' || filter === 'reply') {
    activeFilter.value = filter
  }
}

// 清空所有
const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有通知吗？', '提示', { type: 'warning' })
    await axios.delete('/notifications/all')
    groups.value = []
    total.value = 0
    page.value = 1
    ElMessage.success('已清空')
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('清空失败')
  }
}

onMounted(() => {
  initFilterFromRoute()
  loadGroups()
})

// 监听路由变化（例如从悬浮窗跳转时）
watch(
  () => route.query.filter,
  () => {
    initFilterFromRoute()
    page.value = 1
    loadGroups()
  },
)
</script>

<style scoped>
.notifications-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}
.header {
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
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.right {
  display: flex;
  align-items: center;
  gap: 15px;
  color: #909399;
  font-size: 13px;
}
.preview {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
}
.detail-item:hover {
  background-color: #f5f7fa;
}
.detail-item.unread {
  background-color: #ecf5ff;
}
.detail-content {
  flex: 1;
  margin-left: 12px;
}
.username {
  font-weight: 500;
  margin-right: 8px;
}
.action {
  color: #606266;
}
.time {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
.delete-btn {
  position: absolute;
  right: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}
.detail-item:hover .delete-btn {
  opacity: 1;
}
.pagination {
  margin-top: 20px;
  text-align: center;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.group-card {
  background: white;
  border-radius: 8px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 4px solid transparent;
}
.group-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
.group-card.unread {
  border-left-color: #409eff;
  background-color: #f0f9ff;
}

.group-preview {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
}
.group-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}
</style>
