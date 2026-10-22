<template>
  <div class="comment-section">
    <h3>评论 ({{ total }})</h3>

    <!-- 发表评论 -->
    <div v-if="userStore.isAuthenticated" class="comment-input">
      <el-avatar :size="32" :src="userAvatar" />
      <div class="input-wrapper">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          resize="none"
        />
        <div class="input-actions">
          <el-popover placement="top" width="200" trigger="click">
            <div class="emoji-list">
              <span
                v-for="emoji in emojis"
                :key="emoji"
                class="emoji-item"
                @click="insertEmoji(emoji)"
                >{{ emoji }}</span
              >
            </div>
            <template #reference>
              <el-button size="small">😀</el-button>
            </template>
          </el-popover>
          <el-button type="primary" size="small" @click="submitComment" :loading="submitting"
            >发表评论</el-button
          >
        </div>
      </div>
    </div>
    <div v-else class="login-tip">
      <el-alert title="请登录后评论" type="info" :closable="false" show-icon />
    </div>

    <!-- 评论列表 -->
    <div v-loading="loading" class="comment-list" ref="commentListRef">
      <!-- 置顶评论（特殊样式） -->
      <div v-if="isTopMode && topCommentData" class="pinned-comment">
        <div class="pinned-badge">
          <el-icon><Top /></el-icon> 相关评论
        </div>
        <!-- 渲染置顶评论及其二级回复 -->
        <div :id="`comment-${topCommentData.id}`" class="comment-item">
          <div class="comment-avatar">
            <el-avatar :size="36" :src="getAvatarUrl(topCommentData.authorUsername)" />
          </div>
          <div class="comment-body">
            <div class="comment-header">
              <span class="author">{{ topCommentData.authorUsername }}</span>
              <span class="time">{{ formatDate(topCommentData.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ topCommentData.content }}</div>
            <div class="comment-actions">
              <el-button link size="small" @click="toggleReply(topCommentData.id)">回复</el-button>
              <el-button
                link
                size="small"
                @click="likeComment(topCommentData.id)"
                :type="topCommentData.likedByCurrentUser ? 'primary' : 'default'"
              >
                ️赞 ({{ topCommentData.likeCount }})
              </el-button>
              <el-button
                v-if="canDelete(topCommentData.userId)"
                link
                size="small"
                type="danger"
                @click="deleteComment(topCommentData.id)"
                >删除</el-button
              >
            </div>
            <!-- 回复输入框 -->
            <div v-if="replyTargetId === topCommentData.id" class="reply-input">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                placeholder="写下回复..."
                resize="none"
              />
              <div class="reply-actions">
                <el-button size="small" @click="replyTargetId = null">取消</el-button>
                <el-button type="primary" size="small" @click="submitReply(topCommentData.id)"
                  >回复</el-button
                >
              </div>
            </div>
            <!-- 二级评论列表 -->
            <div v-if="topCommentData.replies && topCommentData.replies.length" class="replies">
              <div
                v-for="reply in getDisplayReplies(topCommentData)"
                :key="reply.id"
                :id="`comment-${reply.id}`"
                class="reply-item"
              >
                <div class="reply-header">
                  <span class="author">{{ reply.authorUsername }}</span>
                  <span class="time">{{ formatDate(reply.createdAt) }}</span>
                </div>
                <div class="reply-content">{{ reply.content }}</div>
                <div class="reply-actions">
                  <el-button
                    link
                    size="small"
                    @click="likeComment(reply.id)"
                    :type="reply.likedByCurrentUser ? 'primary' : 'default'"
                  >
                    赞 ({{ reply.likeCount }})
                  </el-button>
                  <el-button
                    v-if="canDelete(reply.userId)"
                    link
                    size="small"
                    type="danger"
                    @click="deleteComment(reply.id)"
                    >删除</el-button
                  >
                </div>
              </div>
              <div
                v-if="topCommentData.replies.length > getDisplayReplies(topCommentData).length"
                class="load-more-replies"
                @click="loadMoreReplies(topCommentData)"
              >
                加载更多回复 ({{
                  topCommentData.replies.length - getDisplayReplies(topCommentData).length
                }})
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 常规评论列表（原有 v-for 渲染） -->
      <div
        v-for="comment in comments"
        :key="comment.id"
        :id="`comment-${comment.id}`"
        class="comment-item"
      >
        <div class="comment-avatar">
          <el-avatar :size="36" :src="getAvatarUrl(comment.authorUsername)" />
        </div>
        <div class="comment-body">
          <div class="comment-header">
            <span class="author">{{ comment.authorUsername }}</span>
            <span class="time">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          <div class="comment-actions">
            <el-button link size="small" @click="toggleReply(comment.id)">回复</el-button>
            <el-button
              link
              size="small"
              @click="likeComment(comment.id)"
              :type="comment.likedByCurrentUser ? 'primary' : 'default'"
            >
              ️赞 ({{ comment.likeCount }})
            </el-button>
            <el-button
              v-if="canDelete(comment.userId)"
              link
              size="small"
              type="danger"
              @click="deleteComment(comment.id)"
              >删除</el-button
            >
          </div>
          <!-- 回复输入框 -->
          <div v-if="replyTargetId === comment.id" class="reply-input">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="2"
              placeholder="写下回复..."
              resize="none"
            />
            <div class="reply-actions">
              <el-button size="small" @click="replyTargetId = null">取消</el-button>
              <el-button type="primary" size="small" @click="submitReply(comment.id)"
                >回复</el-button
              >
            </div>
          </div>
          <!-- 二级评论列表（按需加载） -->
          <div v-if="comment.replies && comment.replies.length" class="replies">
            <div
              v-for="reply in getDisplayReplies(comment)"
              :key="reply.id"
              :id="`comment-${reply.id}`"
              class="reply-item"
            >
              <div class="reply-header">
                <span class="author">{{ reply.authorUsername }}</span>
                <span class="time">{{ formatDate(reply.createdAt) }}</span>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
              <div class="reply-actions">
                <el-button
                  link
                  size="small"
                  @click="likeComment(reply.id)"
                  :type="reply.likedByCurrentUser ? 'primary' : 'default'"
                >
                  赞 ({{ reply.likeCount }})
                </el-button>
                <el-button
                  v-if="canDelete(reply.userId)"
                  link
                  size="small"
                  type="danger"
                  @click="deleteComment(reply.id)"
                  >删除</el-button
                >
              </div>
            </div>
            <div
              v-if="comment.replies.length > getDisplayReplies(comment).length"
              class="load-more-replies"
              @click="loadMoreReplies(comment)"
            >
              加载更多回复 ({{ comment.replies.length - getDisplayReplies(comment).length }})
            </div>
          </div>
        </div>
      </div>
      <!-- 加载更多触发器 -->
      <div v-if="hasMore" ref="loadMoreTrigger" class="load-more-trigger"></div>
      <div v-else-if="comments.length > 0" class="no-more">没有更多评论了</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from '@/utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Top } from '@element-plus/icons-vue'

const props = defineProps<{
  targetType: 'article' | 'setting' | 'tag'
  targetId: number
  highlightCommentId?: number
}>()

const route = useRoute()
const userStore = useUserStore()

const userAvatar = computed(() => {
  if (userStore.user?.username) {
    return `https://ui-avatars.com/api/?name=${userStore.user.username}&background=409eff&color=fff`
  }
  return ''
})

const getAvatarUrl = (username: string) => {
  return `https://ui-avatars.com/api/?name=${username}&background=409eff&color=fff`
}

const emojis = [
  '😀',
  '😃',
  '😄',
  '😁',
  '😆',
  '😅',
  '😂',
  '🤣',
  '😊',
  '😇',
  '🙂',
  '🙃',
  '😉',
  '😌',
  '😍',
  '🥰',
  '😘',
  '😗',
  '😙',
  '😚',
  '😋',
  '😛',
  '😝',
  '😜',
  '🤪',
  '🤨',
  '🧐',
  '🤓',
  '😎',
  '🤩',
  '🥳',
  '😏',
  '😒',
  '😞',
  '😔',
  '😟',
  '😕',
  '🙁',
  '☹️',
  '😣',
  '😖',
  '😫',
  '😩',
  '🥺',
  '😢',
  '😭',
  '😤',
  '😠',
  '😡',
  '🤬',
  '🤯',
  '😳',
  '🥵',
  '🥶',
  '😱',
  '😨',
  '😰',
  '😥',
  '😓',
  '🤗',
  '🤔',
  '🤭',
  '🤫',
  '🤥',
  '😶',
  '😐',
  '😑',
  '😬',
  '🙄',
  '😯',
  '😦',
  '😧',
  '😮',
  '😲',
  '🥱',
  '😴',
  '🤤',
  '😪',
  '😵',
  '🤐',
  '🥴',
  '🤢',
  '🤮',
  '🤧',
  '😷',
  '🤒',
  '🤕',
  '🤑',
  '🤠',
  '😈',
  '👿',
  '👹',
  '👺',
  '💀',
  '👻',
  '👽',
  '🤖',
  '😺',
  '😸',
  '😹',
  '😻',
  '😼',
  '😽',
  '🙀',
  '😿',
  '😾',
]

const insertEmoji = (emoji: string) => {
  newComment.value += emoji
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const canDelete = (userId: number) => {
  return (
    userStore.isAuthenticated && (userId === userStore.user?.id || userStore.user?.role === 'admin')
  )
}

interface CommentItem {
  id: number
  content: string
  userId: number
  authorUsername: string
  parentId?: number | null
  likeCount: number
  likedByCurrentUser: boolean
  createdAt: string
  replies: CommentItem[]
}

interface BackendComment {
  id: number
  content: string
  userId: number
  authorUsername: string
  parentId?: number | null
  likeCount: number
  likedByCurrentUser?: boolean
  createdAt: string
  replies?: BackendComment[]
}

const newComment = ref('')
const submitting = ref(false)
const comments = ref<CommentItem[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const replyTargetId = ref<number | null>(null)
const replyContent = ref('')
const hasMore = ref(true)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const replyLimits = ref<Map<number, number>>(new Map())

// 置顶模式相关
const isTopMode = ref(false)
const topCommentData = ref<CommentItem | null>(null)

const transformComment = (c: BackendComment): CommentItem => ({
  id: c.id,
  content: c.content,
  userId: c.userId,
  authorUsername: c.authorUsername,
  parentId: c.parentId ?? null,
  likeCount: c.likeCount,
  likedByCurrentUser: c.likedByCurrentUser ?? false,
  createdAt: c.createdAt,
  replies: (c.replies || []).map((r) => transformComment(r)),
})

const getDisplayReplies = (comment: CommentItem) => {
  const limit = replyLimits.value.get(comment.id) || 10
  return comment.replies.slice(0, limit)
}

const loadMoreReplies = (comment: CommentItem) => {
  const currentLimit = replyLimits.value.get(comment.id) || 10
  replyLimits.value.set(comment.id, currentLimit + 10)
}

const loadComments = async (targetPage: number, append: boolean) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await axios.get(`/comments/${props.targetType}/${props.targetId}`, {
      params: { page: targetPage - 1, size: pageSize.value },
    })
    const backendComments = res.data.content as BackendComment[]
    const newComments = backendComments.map(transformComment)
    if (append) {
      comments.value = [...comments.value, ...newComments]
    } else {
      comments.value = newComments
    }
    total.value = res.data.totalElements
    page.value = targetPage
    hasMore.value = !res.data.last
  } catch {
    ElMessage.error('加载评论失败')
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!hasMore.value || loading.value) return
  await loadComments(page.value + 1, true)
}

const initObserver = () => {
  if (observer) observer.disconnect()
  if (!loadMoreTrigger.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.length > 0 && entries[0]?.isIntersecting && hasMore.value && !loading.value) {
        loadMore()
      }
    },
    { threshold: 0.1 },
  )
  observer.observe(loadMoreTrigger.value)
}

const submitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await axios.post(`/comments/${props.targetType}/${props.targetId}`, {
      content: newComment.value,
    })
    ElMessage.success('评论成功')
    newComment.value = ''
    page.value = 1
    hasMore.value = true
    isTopMode.value = false
    topCommentData.value = null
    await loadComments(1, false)
    await nextTick()
    initObserver()
  } catch {
    ElMessage.error('评论失败')
  } finally {
    submitting.value = false
  }
}

const submitReply = async (parentId: number) => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  try {
    await axios.post(`/comments/${props.targetType}/${props.targetId}`, {
      content: replyContent.value,
      parentId,
    })
    ElMessage.success('回复成功')
    replyContent.value = ''
    replyTargetId.value = null
    if (isTopMode.value) {
      const commentId = props.highlightCommentId || Number(route.query.commentId)
      if (commentId) {
        await loadTopCommentContext(commentId)
      } else {
        await loadComments(page.value, false)
      }
    } else {
      await loadComments(page.value, false)
    }
    await nextTick()
    initObserver()
  } catch {
    ElMessage.error('回复失败')
  }
}

const likeComment = async (commentId: number) => {
  const findComment = (id: number, list = comments.value): CommentItem | null => {
    for (const c of list) {
      if (c.id === id) return c
      if (c.replies && c.replies.length) {
        const found = findComment(id, c.replies)
        if (found) return found
      }
    }
    return null
  }
  let comment = findComment(commentId)
  if (!comment && isTopMode.value && topCommentData.value) {
    comment = findComment(commentId, [topCommentData.value])
  }
  if (!comment) return

  const wasLiked = comment.likedByCurrentUser
  try {
    if (wasLiked) {
      await axios.delete(`/comments/${commentId}/like`)
      comment.likedByCurrentUser = false
      comment.likeCount--
    } else {
      await axios.post(`/comments/${commentId}/like`)
      comment.likedByCurrentUser = true
      comment.likeCount++
    }
  } catch {
    ElMessage.error('操作失败')
    comment.likedByCurrentUser = wasLiked
    comment.likeCount = wasLiked ? comment.likeCount + 1 : comment.likeCount - 1
  }
}

const deleteComment = async (commentId: number) => {
  try {
    await ElMessageBox.confirm('确定删除该评论吗？', '提示', { type: 'warning' })
    await axios.delete(`/comments/${commentId}`)
    ElMessage.success('删除成功')
    if (isTopMode.value) {
      const targetId = props.highlightCommentId || Number(route.query.commentId)
      if (targetId) {
        await loadTopCommentContext(targetId)
      } else {
        await loadComments(1, false)
        isTopMode.value = false
        topCommentData.value = null
      }
    } else {
      await loadComments(page.value, false)
    }
    await nextTick()
    initObserver()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('删除失败')
  }
}

const toggleReply = (id: number) => {
  if (replyTargetId.value === id) {
    replyTargetId.value = null
  } else {
    replyTargetId.value = id
    replyContent.value = ''
  }
}

const loadCommentsExcluding = async (targetPage: number, excludeId: number) => {
  try {
    const res = await axios.get(`/comments/${props.targetType}/${props.targetId}`, {
      params: { page: targetPage - 1, size: pageSize.value },
    })
    const backendComments = res.data.content as BackendComment[]
    const filtered = backendComments.filter((c) => c.id !== excludeId)
    comments.value = filtered.map(transformComment)
    page.value = targetPage
    hasMore.value = !res.data.last
  } catch (err) {
    console.error('加载评论失败', err)
  }
}

const loadTopCommentContext = async (commentId: number) => {
  loading.value = true
  try {
    const res = await axios.get(`/comments/${commentId}/context`)
    const data = res.data

    const topComment = transformComment(data.topComment)
    replyLimits.value.set(topComment.id, 10)
    topCommentData.value = topComment

    await loadCommentsExcluding(1, topComment.id)

    isTopMode.value = true
    total.value = data.total

    const highlightId = props.highlightCommentId || commentId
    await nextTick()
    const el = document.getElementById(`comment-${highlightId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      el.classList.add('highlight-comment')
      setTimeout(() => el.classList.remove('highlight-comment'), 2000)
    }
  } catch (err) {
    console.error('加载置顶评论失败', err)
    await loadComments(1, false)
    isTopMode.value = false
    topCommentData.value = null
  } finally {
    loading.value = false
  }
}

// 暴露给父组件的方法（保持兼容）
const scrollToComment = async (commentId: number) => {
  await loadTopCommentContext(commentId)
}

defineExpose({ scrollToComment })

// 监听 targetId 变化
watch(
  () => props.targetId,
  async () => {
    page.value = 1
    hasMore.value = true
    comments.value = []
    replyLimits.value.clear()
    isTopMode.value = false
    topCommentData.value = null

    const commentId = props.highlightCommentId || Number(route.query.commentId)
    if (commentId) {
      await loadTopCommentContext(commentId)
    } else {
      await loadComments(1, false)
    }
    await nextTick()
    initObserver()
  },
)

onMounted(async () => {
  const commentId = props.highlightCommentId || Number(route.query.commentId)
  if (commentId) {
    await loadTopCommentContext(commentId)
  } else {
    await loadComments(1, false)
  }
  await nextTick()
  initObserver()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.comment-section {
  margin-top: 40px;
  border-top: 1px solid #e4e7ed;
  padding-top: 20px;
}
.comment-input {
  display: flex;
  gap: 12px;
  margin-bottom: 30px;
}
.input-wrapper {
  flex: 1;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}
.emoji-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}
.emoji-item {
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  transition: transform 0.1s;
}
.emoji-item:hover {
  transform: scale(1.2);
}
.login-tip {
  margin-bottom: 20px;
}
.comment-list {
  margin-top: 20px;
}
.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.comment-body {
  flex: 1;
}
.comment-header {
  display: flex;
  gap: 12px;
  align-items: baseline;
  margin-bottom: 6px;
}
.author {
  font-weight: 600;
  color: #409eff;
}
.time {
  font-size: 12px;
  color: #909399;
}
.comment-content {
  margin-bottom: 8px;
  line-height: 1.5;
}
.comment-actions {
  display: flex;
  gap: 16px;
}
.reply-input {
  margin-top: 12px;
  padding-left: 40px;
}
.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.replies {
  margin-top: 12px;
  padding-left: 40px;
  border-left: 2px solid #e4e7ed;
}
.reply-item {
  margin-bottom: 12px;
}
.reply-header {
  display: flex;
  gap: 12px;
  align-items: baseline;
  margin-bottom: 4px;
}
.reply-content {
  margin-bottom: 6px;
}
.reply-actions {
  display: flex;
  gap: 16px;
}
.load-more-trigger {
  height: 20px;
  margin: 10px 0;
  text-align: center;
}
.no-more {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 10px 0;
}
.load-more-replies {
  text-align: center;
  padding: 8px;
  color: #409eff;
  cursor: pointer;
  font-size: 12px;
}
.load-more-replies:hover {
  background-color: #f5f7fa;
}
.highlight-comment {
  background-color: #f0f9eb;
  transition: background 0.3s;
}
.pinned-comment {
  margin-bottom: 20px;
  border-left: 4px solid #409eff;
  background-color: #f0f9ff;
  border-radius: 8px;
  padding: 16px;
}
.pinned-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #409eff;
  font-size: 12px;
  margin-bottom: 8px;
}
</style>
