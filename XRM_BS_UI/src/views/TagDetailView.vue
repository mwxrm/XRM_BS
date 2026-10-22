<template>
  <div class="tag-detail-container">
    <el-container>
      <el-main>
        <!-- 正常显示标签内容 -->
        <div class="tag-detail" v-if="tag">
          <div class="tag-header">
            <h1 class="tag-name">
              {{ tag.name }}
              <el-tag :type="getTagType(tag.classification)" size="large">
                {{ getClassificationName(tag.classification) }}
              </el-tag>
            </h1>
            <div class="tag-meta">
              <span class="meta-item">使用次数：{{ tag.usageCount }} 次</span>
              <span class="meta-item">创建时间：{{ formatDate(tag.createdAt) }}</span>
              <span v-if="tag.lastUsedAt" class="meta-item"
                >最后使用：{{ formatDate(tag.lastUsedAt) }}</span
              >
            </div>
          </div>
          <div
            class="tag-description editor-content"
            v-html="sanitizedDescription"
            @click="handleContentClick"
          ></div>

          <CommentSection target-type="tag" :target-id="tag.id" />

          <div class="tag-actions" v-if="userStore.isAuthenticated">
            <el-button type="primary" @click="editTag">编辑标签</el-button>
            <el-button type="info" @click="openVersionDialog">版本历史</el-button>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-else-if="loading" class="loading-container">
          <el-skeleton :rows="5" animated />
        </div>

        <!-- 404 -->
        <div v-else-if="notFound" class="not-found">
          <el-result icon="error" title="标签不存在" sub-title="该标签可能已被删除">
            <template #extra>
              <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
            </template>
          </el-result>
        </div>

        <!-- 版本历史对话框 -->
        <el-dialog v-model="showVersionDialog" title="版本历史" width="800px">
          <el-table :data="versions" v-loading="versionsLoading" stripe>
            <el-table-column prop="version" label="版本号" width="80" />
            <el-table-column prop="authorUsername" label="修改者" width="120" />
            <el-table-column prop="createdAt" label="修改时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link @click="viewVersion(row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <template #footer>
            <el-button @click="showVersionDialog = false">关闭</el-button>
          </template>
        </el-dialog>

        <!-- 版本详情对话框 -->
        <el-dialog
          v-model="showVersionDetailDialog"
          :title="`版本 ${selectedVersion?.version} 详情`"
          width="700px"
        >
          <div class="version-detail">
            <div class="meta">
              <span>修改者：{{ selectedVersion?.authorUsername }}</span>
              <span>修改时间：{{ formatDate(selectedVersion?.createdAt || '') }}</span>
            </div>
            <div
              class="description editor-content"
              v-html="sanitizedVersionDescription"
              @click="handleContentClick"
            ></div>
          </div>
          <template #footer>
            <el-button @click="showVersionDetailDialog = false">关闭</el-button>
          </template>
        </el-dialog>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '@/utils/axios'
import { isAxiosError } from 'axios'
import DOMPurify from 'dompurify'
import { useUserStore } from '@/stores/user'
import CommentSection from '@/views/components/CommentSection.vue'

interface Tag {
  id: number
  name: string
  description: string
  classification: string
  usageCount: number
  createdAt: string
  lastUsedAt?: string
}

interface TagVersion {
  id: number
  version: number
  description: string
  color?: string
  createdBy: number
  authorUsername: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tag = ref<Tag | null>(null)
const loading = ref(false)
const notFound = ref(false)

// 版本历史相关
const showVersionDialog = ref(false)
const versions = ref<TagVersion[]>([])
const versionsLoading = ref(false)
const showVersionDetailDialog = ref(false)
const selectedVersion = ref<TagVersion | null>(null)

import { useContentDisplay } from '@/composables/useContentDisplay'
const { handleContentClick, disableBilibiliAutoplay } = useContentDisplay()

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

const getTagType = (classification: string) => {
  if (classification === 'article') return 'primary'
  return 'success'
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '未知时间'
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const sanitizedDescription = computed(() => {
  if (!tag.value) return ''
  return DOMPurify.sanitize(tag.value.description, {
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

const sanitizedVersionDescription = computed(() => {
  if (!selectedVersion.value?.description) return '无内容'
  return DOMPurify.sanitize(selectedVersion.value.description, {
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

const loadTag = async () => {
  if (!route.query.commentId) {
    window.scrollTo(0, 0)
  }
  loading.value = true
  notFound.value = false
  try {
    const res = await axios.get(`/tags/${route.params.id}`)
    tag.value = res.data
  } catch (err) {
    if (isAxiosError(err) && err.response?.status === 404) {
      notFound.value = true
    } else {
      ElMessage.error('加载标签失败')
    }
  } finally {
    loading.value = false
  }
}

const editTag = () => {
  if (tag.value) {
    router.push(`/tags/${tag.value.id}/edit`)
  }
}

const openVersionDialog = () => {
  showVersionDialog.value = true
  loadVersions()
}

const loadVersions = async () => {
  if (!tag.value) return
  versionsLoading.value = true
  try {
    const res = await axios.get(`/tags/${tag.value.id}/versions`)
    versions.value = res.data
  } catch {
    ElMessage.error('加载版本历史失败')
  } finally {
    versionsLoading.value = false
  }
}

const viewVersion = (version: TagVersion) => {
  selectedVersion.value = version
  showVersionDetailDialog.value = true
}

watch(
  [tag, selectedVersion],
  async () => {
    await nextTick()
    disableBilibiliAutoplay()
  },
  { deep: true },
)

onMounted(() => {
  loadTag()
})
</script>

<style scoped>
.tag-detail-container {
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
.tag-detail {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.tag-header {
  margin-bottom: 30px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 20px;
}
.tag-name {
  margin: 0 0 15px 0;
  font-size: 32px;
  display: flex;
  align-items: center;
  gap: 15px;
}
.tag-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #909399;
}
.tag-description {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 30px;
}
.tag-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  justify-content: flex-end;
}
.loading-container,
.not-found {
  max-width: 900px;
  margin: 0 auto;
}
.version-detail .meta {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #909399;
}
.version-detail .description {
  font-size: 15px;
  line-height: 1.6;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
}
</style>
