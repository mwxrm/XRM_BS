<template>
  <div class="setting-detail-container">
    <el-container>
      <el-main>
        <div class="setting-detail" v-if="setting">
          <div v-if="setting.parentSetting" class="parent-setting-card">
            <div class="card-header">
              <el-icon><Link /></el-icon>
              <span>本设定引用自</span>
            </div>
            <div
              class="card-content"
              @click="setting.parentSetting.deleted ? null : viewSetting(setting.parentSetting.id)"
            >
              <h3 :class="{ 'deleted-title': setting.parentSetting.deleted }">
                {{ setting.parentSetting.title }}
              </h3>
              <div class="meta">
                <span>作者：{{ setting.parentSetting.authorUsername }}</span>
                <span>发布于：{{ formatDate(setting.parentSetting.createdAt) }}</span>
                <el-tag v-if="setting.parentSetting.deleted" type="danger" size="small"
                  >已删除</el-tag
                >
              </div>
            </div>
          </div>

          <div class="setting-header">
            <h1 class="setting-title">{{ setting.title }}</h1>
            <div class="setting-meta">
              <div class="author-info">
                <el-avatar :size="40" :src="authorAvatarUrl" />
                <div class="author-details">
                  <span class="author-name" @click="viewUserProfile(setting.authorId)">
                    {{ setting.authorUsername }}
                  </span>
                  <span class="publish-date">{{ formatDate(setting.createdAt) }}</span>
                </div>
                <el-button
                  v-if="userStore.isAuthenticated && currentUserId !== setting.authorId"
                  :type="isAuthorFollowed ? 'default' : 'primary'"
                  size="small"
                  @click="toggleFollowAuthor"
                  :loading="followLoading"
                >
                  {{ isAuthorFollowed ? '已关注' : '+ 关注' }}
                </el-button>
              </div>
              <div class="stats">
                <span class="stat-item">
                  <el-icon><View /></el-icon> {{ setting.viewCount }} 次浏览
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon> {{ setting.likeCount }} 个赞
                </span>
              </div>
            </div>
            <div class="setting-category">
              <el-tag type="info" size="large">
                分类：{{ getCategoryName(setting.category) }}
              </el-tag>
            </div>
          </div>

          <el-dialog v-model="showCategoryDialog" title="选择新设定的分类" width="400px">
            <el-radio-group v-model="selectedCategory" size="large">
              <el-radio-button value="character">人物</el-radio-button>
              <el-radio-button value="item">物品</el-radio-button>
              <el-radio-button value="location">地区</el-radio-button>
              <el-radio-button value="background">背景</el-radio-button>
              <el-radio-button value="rule">规则</el-radio-button>
              <el-radio-button value="custom">自定义</el-radio-button>
            </el-radio-group>
            <template #footer>
              <el-button @click="showCategoryDialog = false">取消</el-button>
              <el-button type="primary" @click="confirmReference">确定</el-button>
            </template>
          </el-dialog>

          <!-- 设定内容（富文本） -->
          <div v-if="setting && setting.deleted" class="deleted-notice">
            <el-alert title="该设定已被删除" type="warning" show-icon :closable="false" />
            <el-button type="primary" style="margin-top: 16px" @click="$router.push('/')"
              >返回首页</el-button
            >
          </div>
          <div v-else>
            <div
              class="setting-content content-text editor-content"
              v-html="sanitizedContent"
              @click="handleContentClick"
            ></div>
          </div>
          <!-- 标签 -->
          <div class="setting-tags" v-if="setting.tags && setting.tags.length">
            <el-tag
              v-for="tag in setting.tags"
              :key="tag"
              type="primary"
              size="large"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
          </div>

          <!-- 操作按钮 -->
          <div class="setting-actions">
            <el-button
              :type="isLiked ? 'primary' : 'default'"
              @click="likeSetting"
              :loading="loadingLike"
              :icon="isLiked ? StarFilled : Star"
            >
              点赞 ({{ setting.likeCount }})
            </el-button>

            <el-button
              @click="openFavoriteDialog"
              :type="isFavorited ? 'warning' : 'default'"
              :icon="isFavorited ? StarFilled : Star"
            >
              {{ isFavorited ? '已收藏' : '收藏' }}
            </el-button>

            <el-button @click="copyLink">
              <el-icon><Link /></el-icon> 分享
            </el-button>

            <el-button @click="referenceSetting">
              <el-icon><Connection /></el-icon> 引用此设定
            </el-button>
          </div>

          <div v-if="childrenTotal > 0" class="children-section">
            <h4>引用本设定的设定</h4>
            <div class="children-list">
              <div
                v-for="child in childrenList"
                :key="child.id"
                class="child-card"
                @click="child.deleted ? null : viewSetting(child.id)"
              >
                <h5 :class="{ 'deleted-title': child.deleted }">{{ child.title || '无标题' }}</h5>
                <div class="child-meta">
                  <span>作者：{{ child.authorUsername }}</span>
                  <span>发布于：{{ formatDate(child.createdAt) }}</span>
                  <el-tag v-if="child.deleted" type="danger" size="small">已删除</el-tag>
                </div>
              </div>
            </div>
            <el-pagination
              v-if="childrenTotal > pageSize"
              background
              layout="prev, pager, next"
              :current-page="childrenPage"
              :page-size="pageSize"
              :total="childrenTotal"
              @current-change="loadChildrenPage"
              class="children-pagination"
            />
          </div>
          <CommentSection
            ref="commentSectionRef"
            target-type="setting"
            :target-id="setting.id"
            :highlight-comment-id="Number(route.query.commentId) || undefined"
          />
        </div>

        <!-- 加载状态 -->
        <div v-else-if="loading.setting" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>

        <!-- 404状态 -->
        <div v-else-if="notFound" class="not-found">
          <el-result icon="error" title="设定不存在" sub-title="您访问的设定可能已被删除或不存在">
            <template #extra>
              <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
            </template>
          </el-result>
        </div>

        <el-dialog v-model="showFolderDialog" title="选择收藏夹" width="500px">
          <div v-loading="loadingFolders">
            <div class="folder-list">
              <div
                v-for="folder in foldersWithStatus"
                :key="folder.id"
                class="folder-item"
                :class="{ selected: folder.hasSetting }"
                @click="handleFolderSelect(folder)"
              >
                <div class="folder-info">
                  <span class="folder-name">{{ folder.name }}</span>
                  <span class="folder-count">{{ folder.itemCount }} 篇项目</span>
                </div>
                <el-tag v-if="folder.hasSetting" type="success" size="small">已收藏</el-tag>
                <el-tag v-else type="info" size="small">未收藏</el-tag>
              </div>
            </div>
            <div class="new-folder-section">
              <el-button link @click="showNewFolderForm = !showNewFolderForm">
                {{ showNewFolderForm ? '取消' : '+ 新建文件夹' }}
              </el-button>
              <el-form
                v-if="showNewFolderForm"
                :model="newFolderForm"
                :rules="newFolderRules"
                ref="newFolderFormRef"
              >
                <el-form-item label="名称" prop="name">
                  <el-input v-model="newFolderForm.name" maxlength="100" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input v-model="newFolderForm.description" type="textarea" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="createNewFolder">创建</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
          <template #footer>
            <el-button @click="showFolderDialog = false">关闭</el-button>
          </template>
        </el-dialog>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Star, Link, StarFilled } from '@element-plus/icons-vue'
import type { AxiosError } from 'axios'
import { useUserStore } from '@/stores/user'
import DOMPurify from 'dompurify'
import axios from '@/utils/axios'
import { isAxiosError } from 'axios'
import CommentSection from '@/views/components/CommentSection.vue'

import { useContentDisplay } from '@/composables/useContentDisplay'
const { handleContentClick, disableBilibiliAutoplay } = useContentDisplay()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const setting = ref<Setting | null>(null)
const notFound = ref(false)
const currentUserId = ref<number | null>(null)
const authorAvatarUrl = ref('')
const loading = reactive({ setting: false })
const isLiked = ref(false)
const loadingLike = ref(false)

// 收藏相关状态
const showFolderDialog = ref(false)
const foldersWithStatus = ref<FolderWithStatus[]>([])
const isFavorited = ref(false)
const loadingFolders = ref(false)

// 新建文件夹表单
const showNewFolderForm = ref(false)
const newFolderForm = reactive({ name: '', description: '' })
const newFolderFormRef = ref()
const newFolderRules = { name: [{ required: true, message: '请输入文件夹名称', trigger: 'blur' }] }

// 子设定分页数据
const childrenList = ref<SimpleSettingInfo[]>([])
const childrenTotal = ref(0)
const childrenPage = ref(1)
const pageSize = ref(10)

const showCategoryDialog = ref(false)
const selectedCategory = ref('character')

const commentSectionRef = ref<InstanceType<typeof CommentSection> | null>(null)

// 关注作者相关
const isAuthorFollowed = ref(false)
const followLoading = ref(false)

// 声明评论页码
// const commentPage = ref(1)

interface ErrorResponse {
  message?: string
  [key: string]: unknown
}

interface Setting {
  id: number
  title: string
  content: string
  category: string
  authorId: number
  authorUsername: string
  viewCount: number
  likeCount: number
  tags: string[]
  createdAt: string
  status: 'draft' | 'published'
  parentSetting?: SimpleSettingInfo
  children?: SimpleSettingInfo[]
  deleted?: boolean
}

interface FolderWithStatus {
  id: number
  name: string
  description?: string
  itemCount: number
  hasSetting: boolean // 用于设定收藏的状态
  createdAt: string
  updatedAt: string
}

interface BackendFolderWithStatus {
  id: number
  name: string
  description?: string
  itemCount: number
  hasItem: boolean
  createdAt: string
  updatedAt: string
}

interface SimpleSettingInfo {
  id: number
  title: string
  authorUsername: string
  createdAt: string
  deleted?: boolean
}

let startTime: number | null = null
let maxScrollPercent = 0
let viewReported = false
let intervalTimer: number | null = null

const getCategoryName = (category: string) => {
  const map: Record<string, string> = {
    character: '人物',
    item: '物品',
    location: '地区',
    background: '背景',
    rule: '规则',
    custom: '自定义',
  }
  return map[category] || category
}

const viewUserProfile = (userId: number) => router.push(`/users/${userId}`)

const loadSetting = async () => {
  if (!route.query.commentId) {
    window.scrollTo(0, 0)
  }
  loading.setting = true
  notFound.value = false
  try {
    const response = await axios.get(`/settings/${route.params.id}`)
    setting.value = response.data
    await loadChildrenPage()
    generateAuthorAvatar()
    if (userStore.user) currentUserId.value = userStore.user.id
    if (userStore.isAuthenticated) {
      await fetchLikeStatus()
      await fetchFavoriteStatus()
      await fetchAuthorFollowStatus()
    }
    if (setting.value && setting.value.status === 'published') {
      startViewTracking()
    }

    // 处理评论跳转
    const commentId = route.query.commentId
    if (commentId && commentSectionRef.value) {
      await nextTick()
      commentSectionRef.value.scrollToComment(Number(commentId))
    }
  } catch (error) {
    const err = error as AxiosError<ErrorResponse>
    if (err.response?.status === 404 || err.response?.status === 403) {
      notFound.value = true
    } else {
      const errorMessage = err.response?.data?.message || err.message || '加载设定失败'
      ElMessage.error(errorMessage)
    }
  } finally {
    loading.setting = false
  }
}

const fetchLikeStatus = async () => {
  if (!setting.value) return
  try {
    const res = await axios.get(`/settings/${setting.value.id}/like/status`)
    isLiked.value = res.data.liked
  } catch (error) {
    console.error('获取点赞状态失败', error)
  }
}

const generateAuthorAvatar = () => {
  if (setting.value?.authorUsername) {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7']
    const randomColor = colors[Math.floor(Math.random() * colors.length)] || '#FF6B6B'
    authorAvatarUrl.value = `https://ui-avatars.com/api/?name=${setting.value.authorUsername}&background=${randomColor.slice(1)}&color=fff`
  }
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const likeSetting = async () => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loadingLike.value = true
  if (!setting.value) return
  try {
    if (isLiked.value) {
      await axios.delete(`/settings/${setting.value.id}/like`)
      setting.value!.likeCount -= 1
      isLiked.value = false
      ElMessage.success('已取消点赞')
    } else {
      await axios.post(`/settings/${setting.value.id}/like`)
      setting.value!.likeCount += 1
      isLiked.value = true
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    const err = error as AxiosError<ErrorResponse>
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    loadingLike.value = false
  }
}

const copyLink = async () => {
  const url = window.location.href
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制链接')
  }
}

const sanitizedContent = computed(() => {
  if (!setting.value) return ''
  return DOMPurify.sanitize(setting.value.content, {
    ALLOWED_TAGS: [
      'p',
      'br',
      'strong',
      'em',
      'u',
      's',
      'h1',
      'h2',
      'h3',
      'h4',
      'h5',
      'h6',
      'ol',
      'ul',
      'li',
      'blockquote',
      'pre',
      'code',
      'a',
      'img',
      'iframe',
      'div',
      'span',
      'table',
      'thead',
      'tbody',
      'tr',
      'th',
      'td',
      'hr',
      'sup',
      'sub',
    ],
    ALLOWED_ATTR: [
      'href',
      'target',
      'src',
      'alt',
      'width',
      'height',
      'class',
      'style',
      'frameborder',
      'allowfullscreen',
      'align',
    ],
    ALLOWED_URI_REGEXP: /^(?:(?:(?:f|ht)tps?|mailto|tel):|[^a-z]|[a-z+.-]+(?:[^a-z+.-:]|$))/i,
  })
})

// 获取收藏状态
const fetchFavoriteStatus = async () => {
  if (!setting.value || !userStore.isAuthenticated) return
  try {
    const res = await axios.get(`/favorites/setting/${setting.value.id}/status`)
    isFavorited.value = res.data.favorited
  } catch (error) {
    console.error('获取收藏状态失败', error)
  }
}

// 打开收藏对话框
const openFavoriteDialog = async () => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await loadFoldersWithStatus()
  showFolderDialog.value = true
}

// 加载文件夹状态
const loadFoldersWithStatus = async () => {
  if (!setting.value) return
  loadingFolders.value = true
  try {
    const res = await axios.get('/favorites/folders/with-status', {
      params: { itemType: 'setting', itemId: setting.value.id },
    })
    foldersWithStatus.value = (res.data as BackendFolderWithStatus[]).map((folder) => ({
      ...folder,
      hasSetting: folder.hasItem,
    }))
  } finally {
    loadingFolders.value = false
  }
}

// 处理文件夹选择
const handleFolderSelect = async (folder: FolderWithStatus) => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!setting.value) return

  try {
    if (folder.hasSetting) {
      await axios.delete(`/favorites/folders/${folder.id}/setting/${setting.value.id}`)
      folder.hasSetting = false
    } else {
      await axios.post(`/favorites/folders/${folder.id}/setting/${setting.value.id}`)
      folder.hasSetting = true
    }
    await fetchFavoriteStatus()
    await loadFoldersWithStatus() // 刷新文件夹列表
  } catch (err) {
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '操作失败')
    } else {
      ElMessage.error('操作失败')
    }
  }
}

// 创建新文件夹（复用文章收藏的接口）
const createNewFolder = async () => {
  if (!newFolderFormRef.value) return
  try {
    await newFolderFormRef.value.validate()
    const res = await axios.post('/favorites/folders', {
      name: newFolderForm.name,
      description: newFolderForm.description,
    })
    const newFolder = res.data
    foldersWithStatus.value.push({
      ...newFolder,
      hasSetting: false,
      itemCount: 0,
    })
    newFolderForm.name = ''
    newFolderForm.description = ''
    showNewFolderForm.value = false
    ElMessage.success('文件夹创建成功')
  } catch (err: unknown) {
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '操作失败')
    } else {
      ElMessage.error('操作失败')
    }
  }
}

// 加载子设定分页
const loadChildrenPage = async (page = childrenPage.value) => {
  if (!setting.value) return
  try {
    const res = await axios.get(`/settings/${setting.value.id}/children`, {
      params: { page: page - 1, size: pageSize.value },
    })
    childrenList.value = res.data.content
    childrenTotal.value = res.data.totalElements
  } catch (error) {
    console.error('加载子设定失败', error)
    ElMessage.error('加载子设定失败')
  }
}

// 引用设定
const referenceSetting = async () => {
  if (!setting.value) {
    ElMessage.error('设定不存在')
    return
  }
  try {
    await ElMessageBox.confirm('确定要引用这个设定吗？', '引用确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    // router.push({
    //   name: 'CreateSetting',
    //   query: { parentId: setting.value.id, category: setting.value.category },
    // })
    selectedCategory.value = setting.value.category
    showCategoryDialog.value = true
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 查看设定
const viewSetting = (id: number) => {
  router.push(`/settings/${id}`)
}

const confirmReference = () => {
  router.push({
    name: 'CreateSetting',
    query: {
      parentId: setting.value!.id,
      category: selectedCategory.value,
    },
  })
  showCategoryDialog.value = false
}

// 滚动检测
const handleScroll = () => {
  if (viewReported) return
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const docHeight = document.documentElement.scrollHeight
  const percent = ((scrollTop + windowHeight) / docHeight) * 100
  if (percent > maxScrollPercent) maxScrollPercent = percent
}

// 检查并上报
const checkAndReport = () => {
  if (viewReported) return
  if (startTime === null) return
  const now = Date.now()
  const duration = (now - startTime) / 1000
  if (duration >= 15 && maxScrollPercent >= 60) {
    reportView()
    viewReported = true
    window.removeEventListener('scroll', handleScroll)
    window.removeEventListener('beforeunload', checkAndReport)
    if (intervalTimer) clearInterval(intervalTimer)
  }
}

// 上报浏览量
const reportView = async () => {
  if (!setting.value) return
  try {
    await axios.post(`/settings/${setting.value.id}/view`, {
      scrollPercent: maxScrollPercent,
      duration: (Date.now() - (startTime || 0)) / 1000,
    })
    console.log('浏览量上报成功')
  } catch (error) {
    console.error('上报失败', error)
  }
}

// 启动跟踪
const startViewTracking = () => {
  if (viewReported) return
  startTime = Date.now()
  maxScrollPercent = 0
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('beforeunload', checkAndReport)
  intervalTimer = setInterval(checkAndReport, 5000) as unknown as number
}

// 停止跟踪
const stopViewTracking = () => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('beforeunload', checkAndReport)
  if (intervalTimer) clearInterval(intervalTimer)
}

const fetchAuthorFollowStatus = async () => {
  if (!setting.value || !userStore.isAuthenticated) return
  if (setting.value.authorId === currentUserId.value) return // 不关注自己
  try {
    const res = await axios.get(`/follow/${setting.value.authorId}/status`)
    isAuthorFollowed.value = res.data.following
  } catch (error) {
    console.error('获取关注状态失败', error)
  }
}

const toggleFollowAuthor = async () => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!setting.value) return
  followLoading.value = true
  try {
    if (isAuthorFollowed.value) {
      await axios.delete(`/follow/${setting.value.authorId}`)
      isAuthorFollowed.value = false
      ElMessage.success('已取消关注')
    } else {
      await axios.post(`/follow/${setting.value.authorId}`)
      isAuthorFollowed.value = true
      ElMessage.success('关注成功')
    }
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

watch(
  setting,
  async (val) => {
    if (!val) return
    await nextTick()
    disableBilibiliAutoplay()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  stopViewTracking()
})

// 监听路由参数变化
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      loadSetting() // 重新加载设定数据
    }
  },
)

onMounted(async () => {
  await loadSetting()
  await loadChildrenPage()
  if (userStore.isAuthenticated) {
    await fetchLikeStatus()
    await fetchFavoriteStatus()
  }
})
</script>

<style scoped>
/* 样式与之前一致，不再重复 */
.setting-detail-container {
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
.setting-detail {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  padding: 50px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.setting-header {
  margin-bottom: 40px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 30px;
}
.setting-title {
  margin: 0 0 30px 0;
  font-size: 36px;
  line-height: 1.3;
  color: #333;
  font-weight: bold;
}
.setting-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.author-info {
  display: flex;
  align-items: center;
  gap: 15px;
}
.author-details {
  display: flex;
  flex-direction: column;
}
.author-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.publish-date {
  font-size: 14px;
  color: #909399;
}
.stats {
  display: flex;
  gap: 20px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #909399;
}
.setting-category {
  margin-top: 10px;
}
.setting-content {
  margin-bottom: 40px;
}
.content-text {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  word-wrap: break-word;
}
.setting-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 30px;
}
.setting-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding-top: 30px;
  border-top: 1px solid #f0f0f0;
}
.loading-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 50px;
  background: white;
  border-radius: 12px;
}
.not-found {
  max-width: 900px;
  margin: 0 auto;
}

.folder-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}
.folder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.folder-item:hover {
  background-color: #f5f7fa;
  border-color: #409eff;
}
.folder-item.selected {
  background-color: #ecf5ff;
  border-color: #409eff;
}
.folder-info {
  display: flex;
  flex-direction: column;
}
.folder-name {
  font-weight: 600;
  margin-bottom: 4px;
}
.folder-count {
  font-size: 12px;
  color: #909399;
}
.new-folder-section {
  margin-top: 16px;
}
.new-folder-form {
  margin-top: 12px;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 6px;
}

.parent-setting-card {
  margin-bottom: 30px;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  background-color: #f9fafc;
}
.parent-setting-card .card-header {
  background-color: #f5f7fa;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}
.parent-setting-card .card-content {
  padding: 20px;
  cursor: pointer;
}
.parent-setting-card .card-content:hover {
  background-color: #f5f7fa;
}
.parent-setting-card h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
}
.parent-setting-card .meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
}

.children-section {
  margin-top: 40px;
  border-top: 1px solid #e4e7ed;
  padding-top: 30px;
}
.children-section h4 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
}

.deleted-title {
  color: #c0c4cc;
  text-decoration: line-through;
}

/* 子设定列表样式（与文章子列表一致） */
.children-section {
  margin-top: 40px;
  border-top: 1px solid #e4e7ed;
  padding-top: 30px;
}
.children-section h4 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
}
.children-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.child-card {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.3s;
  cursor: pointer;
}
.child-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.1);
}
.child-card h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #409eff;
}
.child-card .child-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
  align-items: center;
}
.children-pagination {
  margin-top: 20px;
  text-align: center;
}
.deleted-title {
  color: #c0c4cc;
  text-decoration: line-through;
}

.author-info {
  gap: 12px;
}
.author-info .el-button {
  margin-left: auto;
}
</style>
