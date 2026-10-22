<template>
  <div class="notification-panel">
    <div class="notification-header">
      <span>通知</span>
      <el-button link type="primary" @click="markAllAsRead" v-if="showMarkAll">全部已读</el-button>
    </div>
    <div v-loading="loading" class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleClick(item)"
      >
        <div class="content">{{ item.content }}</div>
        <div class="time">{{ formatDate(item.createdAt) }}</div>
      </div>
      <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
    </div>
    <!-- 可选：如果需要在悬浮窗显示更多，可以加一个查看全部链接 -->
    <div v-if="showViewAll" class="view-all" @click="$emit('view-all')">查看全部通知</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

interface NotificationItem {
  id: number
  content: string
  isRead: boolean
  createdAt: string
  targetType: string
  targetId: number
}

defineProps<{
  notifications: NotificationItem[]
  showMarkAll?: boolean
  showViewAll?: boolean
}>()

const emit = defineEmits<{
  (e: 'item-click', item: NotificationItem): void
  (e: 'view-all'): void
}>()

const loading = ref(false)

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const handleClick = (item: NotificationItem) => {
  emit('item-click', item)
}

const markAllAsRead = async () => {
  try {
    await axios.put('/notifications/read-all')
    // 重新加载通知（父组件需提供刷新方法）
    emit('view-all') // 或单独定义刷新事件
    ElMessage.success('已全部标记为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
/* 样式复用之前的 */
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 500;
}
.notification-list {
  max-height: 400px;
  overflow-y: auto;
}
.notification-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}
.notification-item:hover {
  background-color: #f5f7fa;
}
.notification-item.unread {
  background-color: #ecf5ff;
}
.notification-item .content {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}
.notification-item .time {
  font-size: 12px;
  color: #909399;
}
.view-all {
  text-align: center;
  padding: 8px;
  border-top: 1px solid #ebeef5;
  cursor: pointer;
  color: #409eff;
}
.view-all:hover {
  background-color: #f5f7fa;
}
</style>
