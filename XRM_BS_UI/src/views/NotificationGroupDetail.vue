<template>
  <div class="group-detail-container">
    <el-main>
      <el-card v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>{{ groupTitle }}</span>
            <el-button type="primary" link @click="markAllAsRead">全部已读</el-button>
          </div>
        </template>
        <div v-if="!loading && items.length === 0" class="empty">
          <el-empty description="暂无通知详情" />
        </div>
        <div v-else class="item-list">
          <div
            v-for="item in items"
            :key="item.id"
            class="detail-item"
            :class="{ unread: !item.isRead }"
            @click="handleItemClick(item)"
          >
            <el-avatar :size="40" :src="getAvatarUrl(item.sourceUsername)" />
            <div class="detail-content">
              <div class="top-line">
                <span class="username">{{ item.sourceUsername }}</span>
                <span class="action">{{ getActionText(item.type) }}</span>
                <span class="time">{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="content-text editor-content">{{ item.content }}</div>
            </div>
            <el-button
              link
              type="danger"
              size="small"
              @click.stop="deleteSingle(item.id)"
              class="delete-btn"
              >删除</el-button
            >
          </div>
        </div>
      </el-card>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

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

const route = useRoute()
const router = useRouter()
const groupKey = route.params.groupKey as string
const items = ref<NotificationItem[]>([])
const loading = ref(false)
const groupTitle = ref('通知详情')

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getAvatarUrl = (username: string) => {
  return `https://ui-avatars.com/api/?name=${username}&background=409eff&color=fff`
}

const getActionText = (type: string) => {
  if (type.startsWith('like_')) return '赞了'
  if (type.startsWith('comment_')) return '评论了'
  if (type.startsWith('reply_')) return '回复了'
  if (type.startsWith('favorite_')) return '收藏了'
  if (type === 'follow') return '关注了你'
  return ''
}

const loadItems = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/notifications/groups/${groupKey}/items`)
    items.value = res.data

    // 调试：打印关注通知数据
    if (groupKey.startsWith('follow_')) {
      console.log('关注通知数据:', items.value)
      if (items.value.length > 0) {
        console.log('第一条通知内容:')
      }
    }

    if (items.value.length > 0) {
      const first = items.value[0]! // 非空断言，告诉 TypeScript 此处一定有值

      // 根据第一条生成标题
      if (first.type.startsWith('like_')) {
        groupTitle.value = first.type.includes('article')
          ? '文章获赞'
          : first.type.includes('setting')
            ? '设定获赞'
            : '评论获赞'
      } else if (first.type.startsWith('comment_')) {
        groupTitle.value = '评论'
      } else if (first.type.startsWith('reply_')) {
        groupTitle.value = '回复'
      } else if (first.type.startsWith('favorite_')) {
        groupTitle.value = first.type.includes('article') ? '文章被收藏' : '设定被收藏'
      } else {
        groupTitle.value = '通知详情'
      }

      // 自动标记未读为已读
      const unreadIds = items.value.filter((i) => !i.isRead).map((i) => i.id)
      if (unreadIds.length > 0) {
        await axios.put('/notifications/read', unreadIds)
        items.value.forEach((i) => (i.isRead = true))
      }
    } else {
      groupTitle.value = '通知详情'
    }
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const handleItemClick = async (item: NotificationItem) => {
  if (!item.isRead) {
    await axios.put('/notifications/read', [item.id])
    item.isRead = true
  }

  if (item.targetType === 'comment') {
    try {
      const res = await axios.get(`/comments/${item.targetId}/parent`)
      const { targetType, targetId } = res.data

      // 根据父级类型生成不同路径
      let path = '/'
      if (targetType === 'article') path = `/articles/${targetId}`
      else if (targetType === 'setting') path = `/settings/${targetId}`
      else if (targetType === 'tag') path = `/tags/${targetId}` // ← 新增

      const scrollToId = item.commentId || item.targetId
      router.push(`${path}?commentId=${scrollToId}`)
    } catch {
      router.push('/')
    }
  } else if (item.targetType === 'user') {
    router.push(`/users/${item.sourceUserId}`)
  } else {
    let path = '/'
    if (item.targetType === 'article') path = `/articles/${item.targetId}`
    else if (item.targetType === 'setting') path = `/settings/${item.targetId}`
    else if (item.targetType === 'tag') path = `/tags/${item.targetId}`
    if (item.commentId) path += `?commentId=${item.commentId}`
    router.push(path)
  }
}

const markAllAsRead = async () => {
  try {
    const unreadIds = items.value.filter((i) => !i.isRead).map((i) => i.id)
    if (unreadIds.length === 0) return
    await axios.put('/notifications/read', unreadIds)
    items.value.forEach((i) => (i.isRead = true))
    ElMessage.success('已全部标记为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

const deleteSingle = async (id: number) => {
  try {
    await axios.delete(`/notifications/${id}`)
    items.value = items.value.filter((i) => i.id !== id)
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadItems()
})
</script>

<style scoped>
.group-detail-container {
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
.item-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detail-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
  background: white;
  border-radius: 8px;
  transition: background 0.2s;
}
.detail-item:hover {
  background-color: #f5f7fa;
}
.detail-item.unread {
  background-color: #ecf5ff;
}
.detail-content {
  flex: 1;
  margin-left: 16px;
}
.top-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}
.username {
  font-weight: 600;
  color: #303133;
}
.action {
  color: #606266;
  font-size: 14px;
}
.time {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}
.content-text {
  color: #606266;
  font-size: 14px;
}
.delete-btn {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}
.detail-item:hover .delete-btn {
  opacity: 1;
}
.empty {
  padding: 40px;
  text-align: center;
}
</style>
