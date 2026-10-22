<template>
  <div class="setting-create-container">
    <el-container>
      <el-main>
        <div class="setting-editor">
          <!-- 标题 -->
          <el-input
            v-model="form.title"
            placeholder="输入设定标题..."
            size="large"
            class="title-input"
            :maxlength="200"
            show-word-limit
          />
          <!-- 引用设定区域 -->
          <div class="reference-section">
            <div class="reference-label">引用设定</div>
            <div v-if="selectedParentSetting" class="current-reference">
              <el-tag closable @close="removeReference" type="info" size="large">
                引用自：{{ selectedParentSetting.title }}
                <span class="author">(作者：{{ selectedParentSetting.authorUsername }})</span>
              </el-tag>
            </div>
            <div v-else>
              <el-input
                v-model="searchKeywordSetting"
                placeholder="搜索设定标题（已发布的设定）"
                @keyup.enter="searchSettings"
                clearable
              >
                <template #append>
                  <el-button @click="searchSettings" :loading="searchSettingsLoading"
                    >搜索</el-button
                  >
                </template>
              </el-input>
              <div v-if="searchResultsSettings.length" class="search-results">
                <div
                  v-for="setting in searchResultsSettings"
                  :key="setting.id"
                  class="search-item"
                  @click="selectParentSetting(setting)"
                >
                  <div class="title">{{ setting.title }}</div>
                  <div class="meta">
                    作者：{{ setting.authorUsername }} | 浏览：{{ setting.viewCount }}
                  </div>
                </div>
              </div>
              <div
                v-else-if="
                  hasSearched &&
                  searchKeywordSetting &&
                  !searchSettingsLoading &&
                  searchResultsSettings.length === 0
                "
                class="search-tip"
              >
                未找到相关设定
              </div>
            </div>
          </div>

          <!-- 标签选择 -->
          <div class="tags-section">
            <div class="tags-header">
              <div class="tags-title">标签（可选，最多添加5个）</div>
              <span class="tags-count">{{ form.tags.length }}/5</span>
            </div>
            <div class="selected-tags">
              <el-tag
                v-for="tag in form.tags"
                :key="tag"
                closable
                @close="removeTag(tag)"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>

            <el-autocomplete
              v-model="tagInput"
              :fetch-suggestions="querySearchTags"
              placeholder="搜索已有标签或输入新标签"
              @select="handleSelectTag"
              :disabled="form.tags.length >= 5"
              size="large"
              class="tag-autocomplete"
            >
              <template #suffix>
                <el-icon @click="addCustomTag"><Plus /></el-icon>
              </template>
              <template #default="{ item }">
                <div class="tag-suggestion">
                  <span class="tag-name">{{ item.name }}</span>
                  <span v-if="item.usageCount" class="tag-stats">
                    <el-icon><View /></el-icon> {{ item.usageCount }}
                  </span>
                </div>
              </template>
            </el-autocomplete>

            <!-- 热门标签-->
            <div class="recommended-tags" v-if="hotTags.length > 0 && form.tags.length < 5">
              <div class="recommended-title">热门标签：</div>
              <div class="recommended-tags-list">
                <el-tag
                  v-for="tag in hotTags"
                  :key="tag.id"
                  @click="addTagFromRecommendation(tag)"
                  class="recommended-tag-item"
                  size="large"
                  effect="plain"
                >
                  {{ tag.name }}
                </el-tag>
              </div>
            </div>
          </div>

          <!-- 内容编辑器（富文本） -->
          <div class="content-section">
            <div class="editor-wrapper">
              <RichTextEditor v-model="form.content" />
            </div>
            <div class="content-footer">
              <span class="char-count">字符数: {{ plainTextLength }}</span>
            </div>
          </div>

          <div class="action-buttons">
            <el-button
              @click="saveDraft"
              :loading="loading.save"
              :disabled="!form.title || !form.content"
              size="large"
            >
              保存草稿
            </el-button>
            <el-button
              type="primary"
              @click="publishSetting"
              :loading="loading.publish"
              :disabled="!form.title || !form.content"
              size="large"
            >
              {{ settingId ? '更新并发布' : '发布设定' }}
            </el-button>
            <el-button @click="$router.push('/')" size="large">取消</el-button>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, View } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import type { AxiosError } from 'axios'

import RichTextEditor from '@/views/components/RichTextEditor.vue'

interface ErrorResponse {
  message?: string
  [key: string]: unknown
}

interface Tag {
  id: number
  name: string
  usageCount: number
  isRecommended: boolean
  type: 'article' | 'setting'
  settingCategory?: string
}

interface SettingRequest {
  title: string
  content: string
  tags: string[]
  parentSettingId?: number
}

// 父设定信息结构
interface ParentSettingInfo {
  id: number
  title: string
  authorUsername: string
  viewCount?: number
}

const route = useRoute()
const router = useRouter()

const settingId = ref<string | null>((route.params.id as string) || null)
const isEditMode = ref(!!settingId.value)

// 引用相关（添加明确类型）
const selectedParentSetting = ref<ParentSettingInfo | null>(null)
const searchKeywordSetting = ref('')
const searchResultsSettings = ref<ParentSettingInfo[]>([])
const searchSettingsLoading = ref(false)

const hasSearched = ref(false) // 是否已执行过搜索

const form = reactive({
  title: '',
  content: '',
  category: 'character', // 默认分类
  tags: [] as string[],
  parentSettingId: undefined as number | undefined,
})

const tagInput = ref('')
const hotTags = ref<Tag[]>([])
const loadingTags = ref(false)

const loading = reactive({
  save: false,
  publish: false,
})

const plainTextLength = computed(() => {
  if (!form.content) return 0
  const text = form.content.replace(/<[^>]*>/g, '')
  return text.length
})

// 加载热门标签
const loadHotTags = async () => {
  try {
    const res = await axios.get('/tags/popular', {
      params: { classification: form.category, size: 10 },
    })
    hotTags.value = res.data
  } catch (error) {
    console.error('加载热门标签失败', error)
  }
}

// 搜索标签
const querySearchTags = async (
  queryString: string,
  cb: (suggestions: Array<{ value: string; [key: string]: unknown }>) => void,
) => {
  if (queryString.trim().length < 1) {
    cb([])
    return
  }
  loadingTags.value = true
  try {
    const res = await axios.get('/tags/search', {
      params: { keyword: queryString, classification: form.category },
    })
    const tags = res.data as Tag[]
    cb(tags.map((tag: Tag) => ({ ...tag, value: tag.name })))
  } catch (error) {
    console.error('搜索标签失败', error)
    cb([])
  } finally {
    loadingTags.value = false
  }
}

const handleSelectTag = (tag: Tag) => {
  if (form.tags.length >= 5) {
    ElMessage.warning('最多只能添加5个标签')
    return
  }
  if (!form.tags.includes(tag.name)) {
    form.tags.push(tag.name)
    tagInput.value = ''
  } else {
    ElMessage.warning('该标签已添加')
  }
}

const addTagFromRecommendation = (tag: Tag) => {
  if (form.tags.length >= 5) {
    ElMessage.warning('最多只能添加5个标签')
    return
  }
  if (!form.tags.includes(tag.name)) {
    form.tags.push(tag.name)
    ElMessage.success(`已添加标签: ${tag.name}`)
  } else {
    ElMessage.warning('该标签已添加')
  }
}

// 创建标签
const addCustomTag = () => {
  if (tagInput.value.trim()) {
    const tagName = tagInput.value.trim()
    if (!tagName) return

    if (form.tags.length >= 5) {
      ElMessage.warning('最多只能添加5个标签')
      return
    }

    if (!form.tags.includes(tagName)) {
      // 创建标签时根据当前设定分类传递正确的参数
      axios
        .post('/tags/user-create', {
          name: tagName,
          category: 'setting',
          settingCategory: form.category,
        })
        .then(() => {
          form.tags.push(tagName)
          tagInput.value = ''
          ElMessage.success(`标签“${tagName}”已创建并添加`)
        })
        .catch((err) => {
          ElMessage.error(err.response?.data?.message || '创建标签失败')
        })
    } else {
      ElMessage.warning('该标签已添加')
    }
  }
}

const removeTag = (tag: string) => {
  const index = form.tags.indexOf(tag)
  if (index !== -1) {
    form.tags.splice(index, 1)
  }
}

// 保存草稿
const saveDraft = async () => {
  if (!form.title || !validateContent()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  loading.save = true
  try {
    const requestData: SettingRequest = {
      title: form.title,
      content: form.content,
      tags: form.tags,
      parentSettingId: form.parentSettingId,
    }

    let response
    if (isEditMode.value && settingId.value) {
      response = await axios.put(`/settings/${settingId.value}`, requestData)
    } else {
      response = await axios.post(`/settings?category=${form.category}`, requestData)
    }

    const settingData = response.data
    let savedId
    if (settingData.data && settingData.data.id) savedId = settingData.data.id
    else if (settingData.id) savedId = settingData.id
    else savedId = settingData

    if (!isEditMode.value && savedId) {
      settingId.value = savedId.toString()
      isEditMode.value = true
      router.replace(`/settings/${savedId}/edit`)
    }

    ElMessage.success(isEditMode.value ? '设定已更新' : '设定已保存为草稿')
  } catch (error) {
    console.error('保存失败:', error)
    const err = error as AxiosError<ErrorResponse>
    const errorMessage = err.response?.data?.message || err.message || '保存失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.save = false
  }
}

// 发布设定
const publishSetting = async () => {
  if (!form.title || !validateContent()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  loading.publish = true
  try {
    const saveData: SettingRequest = {
      title: form.title,
      content: form.content,
      tags: form.tags,
      parentSettingId: form.parentSettingId,
    }

    let settingIdToPublish = settingId.value
    if (!settingIdToPublish) {
      const createResponse = await axios.post(`/settings?category=${form.category}`, saveData)
      let createdId
      if (createResponse.data.data && createResponse.data.data.id)
        createdId = createResponse.data.data.id.toString()
      else if (createResponse.data.id) createdId = createResponse.data.id.toString()
      else createdId = createResponse.data.toString()
      settingIdToPublish = createdId
    } else {
      await axios.put(`/settings/${settingIdToPublish}`, saveData)
    }

    await axios.post(`/settings/${settingIdToPublish}/publish`)
    ElMessage.success('设定已发布！')
    router.push('/')
  } catch (error) {
    console.error('发布失败:', error)
    const err = error as AxiosError<ErrorResponse>
    const errorMessage = err.response?.data?.message || err.message || '发布失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.publish = false
  }
}

const validateContent = () => {
  const plain = form.content.replace(/<[^>]*>/g, '').trim()
  return plain.length > 0
}

// 搜索设定
const searchSettings = async () => {
  if (!searchKeywordSetting.value.trim()) return
  hasSearched.value = true // 标记已搜索
  searchSettingsLoading.value = true
  try {
    const res = await axios.post('/settings/search', {
      keyword: searchKeywordSetting.value,
      page: 0,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    })
    searchResultsSettings.value = res.data.content
  } catch {
    ElMessage.error('搜索失败')
  } finally {
    searchSettingsLoading.value = false
  }
}

// 选择引用设定
const selectParentSetting = (setting: ParentSettingInfo) => {
  selectedParentSetting.value = setting
  form.parentSettingId = setting.id
  searchKeywordSetting.value = ''
  searchResultsSettings.value = []
}

// 移除引用
const removeReference = () => {
  selectedParentSetting.value = null
  form.parentSettingId = undefined
}

// 监听分类变化，重新加载热门标签
watch(
  () => form.category,
  () => {
    loadHotTags()
  },
)

watch(searchKeywordSetting, (newVal) => {
  if (!newVal) {
    searchResultsSettings.value = []
    hasSearched.value = false
  }
})

onMounted(async () => {
  // 1. 从 URL 获取分类（新建模式）
  const presetCategory = route.query.category as string
  if (
    presetCategory &&
    ['character', 'item', 'location', 'background', 'rule', 'custom'].includes(presetCategory)
  ) {
    form.category = presetCategory
  } else if (!isEditMode.value) {
    ElMessage.warning('请从正确的入口创建')
    router.push('/')
    return
  }

  // 2. 如果是编辑模式，加载设定数据
  if (isEditMode.value && settingId.value) {
    try {
      const response = await axios.get(`/settings/${settingId.value}`)
      const settingData = response.data
      form.title = settingData.title
      form.content = settingData.content
      form.category = settingData.category
      form.tags = settingData.tags || []

      if (settingData.parentSetting) {
        selectedParentSetting.value = settingData.parentSetting
        form.parentSettingId = settingData.parentSetting.id
      }
    } catch (error) {
      console.error('加载设定失败:', error)
      ElMessage.error('加载设定失败')
      router.push('/')
      return
    }
  } else {
    //处理父设定ID
    const parentId = route.query.parentId
    if (parentId) {
      try {
        const res = await axios.get(`/settings/${parentId}`)
        const parentData = res.data
        selectedParentSetting.value = {
          id: parentData.id,
          title: parentData.title,
          authorUsername: parentData.authorUsername,
        }
        form.parentSettingId = parentData.id // 关键赋值
      } catch (error) {
        console.error('获取父设定信息失败', error)
      }
    }
  }

  // 3. 加载热门标签
  await loadHotTags()
})
</script>

<style scoped>
/* 样式与之前相同，不再重复 */
.setting-create-container {
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
.setting-editor {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.title-input {
  margin-bottom: 30px;
}
.title-input :deep(.el-input__inner) {
  font-size: 28px;
  font-weight: bold;
  border: none;
  padding: 0;
}
.category-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}
.category-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}
.tags-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}
.tags-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.selected-tags {
  margin-bottom: 15px;
  min-height: 32px;
}
.tag-item {
  margin-right: 8px;
  margin-bottom: 8px;
}
.tag-autocomplete {
  width: 100%;
  margin-bottom: 20px;
}
.recommended-tags {
  margin-top: 20px;
}
.recommended-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  font-weight: 500;
}
.recommended-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.recommended-tag-item {
  cursor: pointer;
  transition: all 0.3s ease;
}
.recommended-tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.content-section {
  margin-bottom: 30px;
}
.editor-wrapper {
  min-height: 400px;
  margin-bottom: 20px;
}

.content-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.char-count {
  font-size: 14px;
  color: #909399;
}
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
}

.reference-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}
.reference-label {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #606266;
}
.current-reference {
  margin-bottom: 8px;
}
.search-results {
  margin-top: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  max-height: 240px;
  overflow-y: auto;
  background: white;
}
.search-item {
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.2s;
}
.search-item:hover {
  background-color: #f5f7fa;
}
.search-item .title {
  font-weight: 500;
  margin-bottom: 4px;
}
.search-item .meta {
  font-size: 12px;
  color: #909399;
}
.search-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
.author {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
