<template>
  <div class="article-detail-container">
    <el-container>
      <!-- 顶部导航栏 -->

      <el-main>
        <div class="article-detail" v-if="article">
          <!-- 父文章卡片（引用来源） -->
          <div v-if="article.parentArticle" class="parent-article-card">
            <div class="card-header">
              <el-icon><Link /></el-icon>
              <span>本文引用自</span>
            </div>
            <div
              class="card-content"
              @click="article.parentArticle.deleted ? null : viewArticle(article.parentArticle.id)"
            >
              <h3>{{ article.parentArticle.title }}</h3>
              <div class="meta">
                <span>作者：{{ article.parentArticle.authorUsername }}</span>
                <span>发布于：{{ formatDate(article.parentArticle.createdAt) }}</span>
                <el-tag v-if="article.parentArticle.deleted" type="danger" size="small"
                  >已删除</el-tag
                >
              </div>
            </div>
          </div>

          <!-- 文章头部 -->
          <div class="article-header">
            <h1 class="article-title">{{ article.title }}</h1>
            <div class="article-meta">
              <div class="author-info">
                <el-avatar :size="40" :src="authorAvatarUrl" />
                <div class="author-details">
                  <span class="author-name" @click="viewUserProfile(article.authorId)">{{
                    article.authorUsername
                  }}</span>
                  <span class="publish-date">{{ formatDate(article.createdAt) }}</span>
                </div>
                <el-button
                  v-if="userStore.isAuthenticated && currentUserId !== article.authorId"
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
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }} 次浏览
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ article.likeCount }} 个赞
                </span>
              </div>
            </div>

            <!-- 标签 -->
            <div class="article-tags">
              <el-tag
                v-for="tag in article.tags"
                :key="tag"
                type="primary"
                size="large"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>

          <!-- 文章内容 -->

          <div v-if="article && article.deleted" class="deleted-notice">
            <el-alert title="该文章已被删除" type="warning" show-icon :closable="false" />
            <el-button type="primary" style="margin-top: 16px" @click="$router.push('/')"
              >返回首页</el-button
            >
          </div>
          <div v-else>
            <div class="article-content">
              <div
                class="content-text editor-content"
                v-html="sanitizedContent"
                @click="handleContentClick"
              ></div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="article-actions">
            <el-button
              :type="isLiked ? 'primary' : 'default'"
              @click="likeArticle"
              :loading="loadingLike"
              :icon="isLiked ? StarFilled : Star"
            >
              点赞 ({{ article.likeCount }})
            </el-button>

            <el-button
              @click="openFavoriteDialog"
              :type="isFavorited ? 'warning' : 'default'"
              :icon="isFavorited ? StarFilled : Star"
            >
              {{ isFavorited ? '已收藏' : '收藏' }}
            </el-button>

            <el-button @click="copyLink">
              <el-icon><Link /></el-icon>
              分享
            </el-button>

            <el-button @click="referenceArticle">
              <el-icon><Connection /></el-icon> 引用此文
            </el-button>
          </div>

          <!-- 子文章列表（分页卡片） -->
          <div v-if="childrenTotal > 0" class="children-section">
            <h4>引用本文的文章</h4>
            <div class="children-list">
              <div
                v-for="child in childrenList"
                :key="child.id"
                class="child-card"
                @click="child.deleted ? null : viewArticle(child.id)"
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

          <div v-if="articleSettings.length > 0" class="settings-section">
            <h4>关联设定</h4>
            <div class="settings-list">
              <div
                v-for="setting in articleSettings"
                :key="setting.id"
                class="setting-card"
                @click="viewSetting(setting.id)"
              >
                <h5>{{ setting.title }}</h5>
                <div class="meta">
                  <span>{{ setting.authorUsername }}</span>
                  <span>{{ formatDate(setting.createdAt) }}</span>
                  <el-tag v-if="setting.deleted" type="danger" size="small">已删除</el-tag>
                </div>
              </div>
            </div>
          </div>

          <CommentSection
            ref="commentSectionRef"
            target-type="article"
            :target-id="article.id"
            :highlight-comment-id="Number(route.query.commentId) || undefined"
          />
        </div>

        <!-- 加载状态 -->
        <div v-else-if="loading.article" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>

        <!-- 404状态 -->
        <div v-else-if="notFound" class="not-found">
          <el-result icon="error" title="文章不存在" sub-title="您访问的文章可能已被删除或不存在">
            <template #extra>
              <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
            </template>
          </el-result>
        </div>

        <el-dialog v-model="showFolderDialog" title="选择收藏夹" width="500px">
          <div v-loading="loadingFolders">
            <!-- 文件夹列表 -->
            <div class="folder-list">
              <div
                v-for="folder in foldersWithStatus"
                :key="folder.id"
                class="folder-item"
                :class="{ selected: folder.hasArticle }"
                @click="handleFolderSelect(folder)"
              >
                <div class="folder-info">
                  <span class="folder-name">{{ folder.name }}</span>
                  <span class="folder-count">{{ folder.itemCount }} 篇项目</span>
                </div>
                <el-tag v-if="folder.hasArticle" type="success" size="small">已收藏</el-tag>
                <el-tag v-else type="info" size="small">未收藏</el-tag>
              </div>
            </div>

            <!-- 新建文件夹折叠区 -->
            <div class="new-folder-section">
              <el-button link @click="showNewFolderForm = !showNewFolderForm">
                {{ showNewFolderForm ? '取消' : '+ 新建文件夹' }}
              </el-button>
              <el-form
                v-if="showNewFolderForm"
                :model="newFolderForm"
                :rules="newFolderRules"
                ref="newFolderFormRef"
                class="new-folder-form"
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
            <span class="dialog-footer">
              <el-button @click="showFolderDialog = false">关闭</el-button>
            </span>
          </template>
        </el-dialog>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage, ElDialog } from 'element-plus'
import { View, Star, Link, StarFilled } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import type { AxiosError } from 'axios'
import { useUserStore } from '@/stores/user'
import type { FormInstance, FormRules } from 'element-plus'
import DOMPurify from 'dompurify'

import CommentSection from '@/views/components/CommentSection.vue'

import { useContentDisplay } from '@/composables/useContentDisplay'

interface Setting {
  id: number
  title: string
  authorUsername: string // 其他字段按需
}

let startTime: number | null = null
let maxScrollPercent = 0
let viewReported = false
let intervalTimer: number | null = null

interface SimpleSettingInfo {
  id: number
  title: string
  authorUsername: string
  createdAt: string
  deleted?: boolean
}

interface FolderWithStatus {
  id: number
  name: string
  description?: string
  itemCount: number
  hasArticle: boolean
  createdAt: string
  updatedAt: string
}

// 定义错误响应接口
interface ErrorResponse {
  message?: string
  [key: string]: unknown
}

// 定义简单的文章信息结构（用于 parentArticle 和 children）
interface SimpleArticleInfo {
  id: number
  title: string
  authorUsername: string
  deleted?: boolean
  createdAt: string // 发布时间
}

interface Article {
  id: number
  title: string
  content: string
  authorId: number
  authorUsername: string
  viewCount: number
  likeCount: number
  tags: string[]
  createdAt: string
  deleted?: boolean
  status: 'draft' | 'published'
  // 新增引用相关字段，均为可选（因为可能没有引用或被引用）
  parentArticle?: SimpleArticleInfo
  children?: SimpleArticleInfo[]
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
  deleted?: boolean
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref<Article | null>(null)
const notFound = ref(false)
const currentUserId = ref<number | null>(null)
const authorAvatarUrl = ref('')

// 子文章分页数据
const childrenList = ref<SimpleArticleInfo[]>([])
const childrenTotal = ref(0)
const childrenPage = ref(1)
const pageSize = ref(10)

// const commentPage = ref(1)

// 关注作者相关
const isAuthorFollowed = ref(false)
const followLoading = ref(false)

const commentSectionRef = ref<InstanceType<typeof CommentSection> | null>(null)
const loading = reactive({
  article: false,
  like: false,
})

// 点赞和收藏状态
const isFavorited = ref(false) // 是否被收藏（任何文件夹）
const isLiked = ref(false)
// const isFavorited = ref(false)
const loadingLike = ref(false)
// const loadingFavorite = ref(false)

// 收藏相关状态
const showFolderDialog = ref(false)
const foldersWithStatus = ref<FolderWithStatus[]>([])
// const selectedFolderId = ref<number | null>(null) // 单选选中的文件夹ID
const loadingFolders = ref(false)

const articleSettings = ref<SimpleSettingInfo[]>([])
const relatedSettings = ref<Setting[]>([])

// 新建文件夹表单
const showNewFolderForm = ref(false)
const newFolderForm = reactive({
  name: '',
  description: '',
})
const newFolderRules: FormRules = {
  name: [{ required: true, message: '请输入文件夹名称', trigger: 'blur' }],
}
const newFolderFormRef = ref<FormInstance>()

const { handleContentClick, disableBilibiliAutoplay } = useContentDisplay()

// 加载带状态的文件夹列表
const loadFoldersWithStatus = async () => {
  if (!article.value) {
    ElMessage.error('文章不存在')
    return
  }
  loadingFolders.value = true
  try {
    const res = await axios.get('/favorites/folders/with-status', {
      params: { itemType: 'article', itemId: article.value.id },
    })
    // 假设后端返回的数据包含 hasItem 字段
    const backendData: Array<{
      id: number
      name: string
      description?: string
      itemCount: number
      hasItem: boolean
      createdAt: string
      updatedAt: string
    }> = res.data
    foldersWithStatus.value = backendData.map((folder) => ({
      id: folder.id,
      name: folder.name,
      description: folder.description ?? '',
      itemCount: folder.itemCount,
      hasArticle: folder.hasItem,
      createdAt: folder.createdAt,
      updatedAt: folder.updatedAt,
    }))
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || error.message || '加载文件夹失败')
  } finally {
    loadingFolders.value = false
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

// 处理文件夹选择（单选，点击即切换收藏状态）
const handleFolderSelect = async (folder: FolderWithStatus) => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!article.value) {
    ElMessage.error('文章不存在')
    return
  }

  try {
    if (folder.hasArticle) {
      await axios.delete(`/favorites/folders/${folder.id}/article/${article.value.id}`)
      folder.hasArticle = false
    } else {
      await axios.post(`/favorites/folders/${folder.id}/article/${article.value.id}`)
      folder.hasArticle = true
    }
    // 刷新整体收藏状态
    await fetchFavoriteAnyStatus()
    // 关键：刷新文件夹列表，更新 itemCount
    await loadFoldersWithStatus()
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || error.message || '操作失败')
  }
}

// 创建新文件夹
const createNewFolder = async () => {
  if (!newFolderFormRef.value) return
  try {
    await newFolderFormRef.value.validate()
    const res = await axios.post('/favorites/folders', newFolderForm)
    const newFolder = res.data
    // 将新文件夹添加到列表，并设置 hasArticle = false
    foldersWithStatus.value.push({
      ...newFolder,
      hasArticle: false,
    })
    // 清空表单并隐藏
    newFolderForm.name = ''
    newFolderForm.description = ''
    showNewFolderForm.value = false
    ElMessage.success('文件夹创建成功')
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || error.message || '创建失败')
  }
}

// 加载文章详情
const loadArticle = async () => {
  if (!route.query.commentId) {
    window.scrollTo(0, 0)
  }
  loading.article = true
  notFound.value = false
  try {
    const response = await axios.get(`/articles/${route.params.id}`)
    article.value = response.data
    generateAuthorAvatar()
    if (userStore.user) {
      currentUserId.value = userStore.user.id
    }
    if (userStore.isAuthenticated) {
      await Promise.all([fetchLikeStatus(), fetchFavoriteAnyStatus(), fetchAuthorFollowStatus()])
    }
    if (article.value && article.value.status === 'published') {
      startViewTracking()
    }

    if (article.value) {
      await fetchRelatedSettings(article.value.id)
    }
    // 加载子文章分页
    await loadChildrenPage()

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
      ElMessage.error('该文章已被删除')
    } else {
      const errorMessage = err.response?.data?.message || err.message || '加载文章失败'
      ElMessage.error(errorMessage)
      notFound.value = true
    }
  } finally {
    loading.article = false
  }
}

// 获取点赞状态
const fetchLikeStatus = async () => {
  try {
    const res = await axios.get(`/likes/${article.value!.id}/status`)
    isLiked.value = res.data.liked
  } catch (error) {
    console.error('获取点赞状态失败', error)
  }
}

// 生成作者头像
const generateAuthorAvatar = () => {
  if (article.value?.authorUsername) {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7']
    const randomColor = colors[Math.floor(Math.random() * colors.length)] || '#FF6B6B'
    authorAvatarUrl.value = `https://ui-avatars.com/api/?name=${article.value.authorUsername}&background=${randomColor.slice(1)}&color=fff`
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 点赞文章
const likeArticle = async () => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  loadingLike.value = true
  try {
    if (isLiked.value) {
      // 取消点赞
      await axios.delete(`/likes/${article.value!.id}`)
      article.value!.likeCount -= 1
      isLiked.value = false
      ElMessage.success('已取消点赞')
    } else {
      // 点赞
      await axios.post(`/likes/${article.value!.id}`)
      article.value!.likeCount += 1
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

// 复制文章链接
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

// 获取文章是否被当前用户收藏（任何文件夹）
const fetchFavoriteAnyStatus = async () => {
  if (!article.value || !userStore.isAuthenticated) return
  try {
    const res = await axios.get(`/favorites/article/${article.value.id}/status`)
    isFavorited.value = res.data.favorited
  } catch (error) {
    console.error('获取收藏状态失败', error)
  }
}

const referenceArticle = async () => {
  if (!article.value) {
    ElMessage.error('文章不存在')
    return
  }
  try {
    await ElMessageBox.confirm('确定要引用这篇文章吗？', '引用确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    // 跳转到新建文章页面，将父文章ID作为查询参数传递
    router.push({
      name: 'CreateArticle',
      query: { parentId: article.value.id },
    })
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 加载子文章分页
const loadChildrenPage = async (page = childrenPage.value) => {
  if (!article.value) return
  try {
    const res = await axios.get(`/articles/${article.value.id}/children`, {
      params: { page: page - 1, size: pageSize.value },
    })
    childrenList.value = res.data.content
    childrenTotal.value = res.data.totalElements
  } catch (error) {
    console.error('加载子文章失败', error)
    ElMessage.error('加载子文章失败')
  }
}

// 查看文章（跳转）
const viewArticle = (articleId: number) => {
  router.push(`/articles/${articleId}`)
}

const viewUserProfile = (userId: number) => router.push(`/users/${userId}`)

// 滚动检测函数
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
  if (!article.value) return
  try {
    await axios.post(`/articles/${article.value.id}/view`, {
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
  if (!article.value || !userStore.isAuthenticated) return
  if (article.value.authorId === currentUserId.value) return // 不关注自己
  try {
    const res = await axios.get(`/follow/${article.value.authorId}/status`)
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
  if (!article.value) return
  followLoading.value = true
  try {
    if (isAuthorFollowed.value) {
      await axios.delete(`/follow/${article.value.authorId}`)
      isAuthorFollowed.value = false
      ElMessage.success('已取消关注')
    } else {
      await axios.post(`/follow/${article.value.authorId}`)
      isAuthorFollowed.value = true
      ElMessage.success('关注成功')
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    followLoading.value = false
  }
}

const sanitizedContent = computed(() => {
  if (!article.value) return ''
  return DOMPurify.sanitize(article.value.content, {
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

const fetchArticleSettings = async () => {
  if (!article.value) return
  try {
    const res = await axios.get(`/articles/${article.value.id}/settings`)
    articleSettings.value = res.data
  } catch (err) {
    console.error('加载关联设定失败', err)
  }
}

const viewSetting = (id: number) => {
  router.push(`/settings/${id}`)
}

const fetchRelatedSettings = async (articleId: number) => {
  try {
    const res = await axios.get(`/articles/${articleId}/settings`)
    relatedSettings.value = res.data
  } catch (e) {
    console.error('获取关联设定失败', e)
  }
}

watch(
  article,
  async (newVal) => {
    if (!newVal) return
    await nextTick()
    disableBilibiliAutoplay()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  stopViewTracking()
})

// 监听路由参数变化，重新加载文章
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      loadArticle()
    }
  },
)

onMounted(async () => {
  await loadArticle()
  await fetchArticleSettings()
})
</script>

<style scoped>
.article-detail-container {
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

.article-detail {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  padding: 50px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.article-header {
  margin-bottom: 40px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 30px;
}

.article-title {
  margin: 0 0 30px 0;
  font-size: 36px;
  line-height: 1.3;
  color: #333;
  font-weight: bold;
}

.article-meta {
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

.article-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tag-item {
  font-size: 14px;
}

.article-content {
  margin-bottom: 40px;
}

.content-text {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.article-actions {
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

.content-text {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.content-text iframe,
.content-text video {
  max-width: 100%;
  height: auto;
}

/* 父文章卡片 */
.parent-article-card {
  margin-bottom: 30px;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  background-color: #f9fafc;
}
.parent-article-card .card-header {
  background-color: #f5f7fa;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}
.parent-article-card .card-content {
  padding: 20px;
  cursor: pointer;
}
.parent-article-card .card-content:hover {
  background-color: #f5f7fa;
}
.parent-article-card h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
}
.parent-article-card .meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
}

/* 子文章列表 */
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
}
.children-pagination {
  margin-top: 20px;
  text-align: center;
}

.settings-section {
  margin-top: 40px;
  border-top: 1px solid #e4e7ed;
  padding-top: 30px;
}
.settings-section h4 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
}
.settings-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.setting-card {
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  width: 220px;
}
.setting-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.1);
}
.setting-card h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
}
.setting-card .meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 12px;
}

.related-settings {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}
.settings-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.setting-card {
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.setting-card:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}

.related-settings {
  margin-top: 30px;
  border-top: 1px solid #e4e7ed;
  padding-top: 20px;
}
.settings-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.setting-card {
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.setting-card:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}
</style>
