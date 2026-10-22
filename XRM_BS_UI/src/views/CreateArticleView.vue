<template>
  <div class="article-create-container">
    <el-container>
      <el-main>
        <div class="article-editor">
          <!-- 标题输入 -->
          <el-input
            v-model="form.title"
            placeholder="输入文章标题..."
            size="large"
            class="title-input"
            :maxlength="200"
            show-word-limit
          />

          <!-- 引用文章区域 -->
          <div class="reference-section">
            <div class="reference-label">引用文章</div>
            <div v-if="selectedParentArticle" class="current-reference">
              <el-tag closable @close="removeReference" type="info" size="large">
                引用自：{{ selectedParentArticle.title }}
                <span class="author">(作者：{{ selectedParentArticle.authorUsername }})</span>
              </el-tag>
            </div>
            <div v-else>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索文章标题（已发布的文章）"
                @keyup.enter="searchArticles"
                clearable
              >
                <template #append>
                  <el-button @click="searchArticles" :loading="searchLoading">搜索</el-button>
                </template>
              </el-input>
              <div v-if="searchResults.length" class="search-results">
                <div
                  v-for="article in searchResults"
                  :key="article.id"
                  class="search-item"
                  @click="selectParentArticle(article)"
                >
                  <div class="title">{{ article.title }}</div>
                  <div class="meta">
                    作者：{{ article.authorUsername }} | 浏览：{{ article.viewCount }}
                  </div>
                </div>
              </div>
              <div
                v-else-if="
                  hasSearched && searchKeyword && !searchLoading && searchResults.length === 0
                "
                class="search-tip"
              >
                未找到相关文章
              </div>
            </div>
          </div>

          <!-- 标签输入 -->
          <div class="tags-section">
            <div class="tags-header">
              <div class="tags-title">标签（可选，最多添加5个标签）</div>
              <span class="tags-count">{{ form.tags.length }}/5</span>
            </div>

            <div class="selected-tags">
              <el-tag
                v-for="tag in form.tags"
                :key="tag"
                closable
                @close="removeTag(tag)"
                class="tag-item"
                :type="getTagType(tag)"
              >
                {{ tag }}
              </el-tag>
            </div>

            <div class="tag-search-wrapper">
              <el-autocomplete
                v-model="tagInput"
                :fetch-suggestions="querySearchTags"
                placeholder="搜索已有标签或输入新标签"
                @select="handleSelectTag"
                @keyup.enter="addCustomTag"
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
                    <el-tag v-if="item.isRecommended" size="small" type="success" class="ml-2">
                      推荐
                    </el-tag>
                  </div>
                </template>
              </el-autocomplete>

              <div class="popular-tags" v-if="popularTags.length > 0 && form.tags.length < 5">
                <div class="popular-title">热门标签：</div>
                <div class="popular-tags-list">
                  <el-tag
                    v-for="tag in popularTags"
                    :key="tag.id"
                    @click="addTagFromRecommendation(tag)"
                    class="popular-tag-item"
                    :type="tag.isRecommended ? 'success' : 'info'"
                    size="large"
                    effect="plain"
                  >
                    {{ tag.name }}
                    <el-icon v-if="tag.usageCount > 100" class="ml-1">
                      <StarFilled />
                    </el-icon>
                  </el-tag>
                </div>
              </div>
            </div>
          </div>

          <!-- 关联设定区域 -->
          <div class="settings-section">
            <div class="settings-header">
              <div class="settings-title">关联设定</div>
              <span class="settings-count">{{ form.settings.length }}/20</span>
            </div>

            <div class="selected-settings" v-if="selectedSettings.length > 0">
              <el-tag
                v-for="setting in selectedSettings"
                :key="setting.id"
                closable
                size="large"
                effect="plain"
                class="setting-tag"
                @close="removeSetting(setting.id)"
              >
                {{ setting.title }}
              </el-tag>
            </div>

            <el-select
              v-model="searchSetting"
              filterable
              remote
              reserve-keyword
              placeholder="搜索设定添加..."
              :remote-method="searchSettings"
              :loading="settingSearchLoading"
              clearable
              value-key="id"
              style="width: 100%; margin-top: 8px"
              @change="addSetting"
            >
              <el-option
                v-for="item in settingOptions"
                :key="item.id"
                :label="item.title"
                :value="item.id"
              />
            </el-select>
          </div>

          <!-- 内容编辑器 -->
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
              @click="publishArticle"
              :loading="loading.publish"
              :disabled="!form.title || !form.content"
              size="large"
            >
              {{ articleId ? '更新并发布' : '发布文章' }}
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
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus, View, StarFilled } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import type { AxiosError } from 'axios'
import RichTextEditor from '@/views/components/RichTextEditor.vue'

// 定义错误响应接口
interface ErrorResponse {
  message?: string
  [key: string]: unknown
}

interface ParentArticle {
  id: number
  title: string
  authorUsername: string
  viewCount: number
  createdAt: string
}

// 定义标签接口
interface Tag {
  id: number
  name: string
  description?: string
  category?: string
  color?: string
  usageCount: number
  isRecommended: boolean
  createdAt: string
  lastUsedAt?: string
}

interface SimpleSettingInfo {
  id: number
  title: string
}

const route = useRoute()
const router = useRouter()

const articleId = ref<string | null>((route.params.id as string) || null)
const isEditMode = ref(!!articleId.value)

const tagInput = ref('') // 只保留这一个声明
const popularTags = ref<Tag[]>([])
const loadingTags = ref(false)

// 存储初始状态，用于判断是否有未保存的修改
const initialTitle = ref('')
const initialContent = ref('')
const initialTags = ref<string[]>([])

const isSaving = ref(false)

// 引用相关
const selectedParentArticle = ref<ParentArticle | null>(null)
const searchKeyword = ref('')
const searchResults = ref<ParentArticle[]>([])
const searchLoading = ref(false)
const hasSearched = ref(false)

// 搜索设定

const selectedSettings = ref<{ id: number; title: string }[]>([])
const selectedSettingsList = ref<SimpleSettingInfo[]>([])
const settingSearchLoading = ref(false)
const settingOptions = ref<SimpleSettingInfo[]>([])
const searchSetting = ref('')

// 设置初始值
const setInitialValues = () => {
  initialTitle.value = form.title
  initialContent.value = form.content
  initialTags.value = [...form.tags]
}

// 检查是否有未保存的修改
const hasUnsavedChanges = () => {
  return (
    form.title !== initialTitle.value ||
    form.content !== initialContent.value ||
    JSON.stringify(form.tags) !== JSON.stringify(initialTags.value)
  )
}

const form = reactive({
  title: '',
  content: '',
  tags: [] as string[],
  parentArticleId: undefined as number | undefined,
  settings: [] as number[],
})

const loading = reactive({
  save: false,
  publish: false,
})

// 保存草稿
const saveDraft = async () => {
  if (!form.title || !validateContent()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  loading.save = true
  try {
    const savedId = await performSave(false)
    // 如果是新建文章，保存后跳转到编辑页
    if (!isEditMode.value && savedId) {
      router.replace(`/articles/${savedId}/edit`)
    }
    ElMessage.success(isEditMode.value ? '文章已更新' : '文章已保存为草稿')
  } catch (error) {
    const err = error as AxiosError<ErrorResponse>
    ElMessage.error(err.response?.data?.message || err.message || '保存失败')
  } finally {
    loading.save = false
  }
}

// 发布文章
const publishArticle = async () => {
  if (!form.title || !validateContent()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  loading.publish = true
  try {
    await performSave(true)
    ElMessage.success('文章已发布！')
    router.push('/')
  } catch (error) {
    const err = error as AxiosError<ErrorResponse>
    ElMessage.error(err.response?.data?.message || err.message || '发布失败')
  } finally {
    loading.publish = false
  }
}

// 加载热门标签
const loadTagRecommendations = async () => {
  try {
    const popularResponse = await axios.get('/tags/popular')
    popularTags.value = popularResponse.data.data || popularResponse.data
  } catch (error) {
    console.error('加载热门标签失败:', error)
  }
}

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
    // 直接使用分类参数请求
    const response = await axios.get('/tags/search', {
      params: { keyword: queryString, classification: 'article' },
    })
    const tags = response.data.data || response.data
    const suggestions = tags.map((tag: Tag) => ({
      ...tag,
      value: tag.name,
    }))
    cb(suggestions)
  } catch (error) {
    console.error('搜索标签失败', error)
    cb([])
  } finally {
    loadingTags.value = false
  }
}

// 从建议中选择标签
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

// 从推荐中添加标签
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

// 添加自定义标签
const addCustomTag = () => {
  if (tagInput.value.trim()) {
    const tagName = tagInput.value.trim()

    if (form.tags.length >= 5) {
      ElMessage.warning('最多只能添加5个标签')
      return
    }

    if (!form.tags.includes(tagName)) {
      form.tags.push(tagName)
      tagInput.value = ''
    } else {
      ElMessage.warning('该标签已添加')
    }
  }
}

// 移除标签
const removeTag = (tag: string) => {
  const index = form.tags.indexOf(tag)
  if (index !== -1) {
    form.tags.splice(index, 1)
  }
}

// 获取标签类型（根据使用次数）
const getTagType = (tagName: string) => {
  // 如果是热门标签，显示不同样式
  const isPopular = popularTags.value.some((tag) => tag.name === tagName)

  if (isPopular) return 'warning'
  return 'info'
}

// 计算纯文本长度
const plainTextLength = computed(() => {
  if (!form.content) return 0
  // 移除HTML标签，只保留文本
  const text = form.content.replace(/<[^>]*>/g, '')
  return text.length
})

// 保存/发布时的非空校验也需要修改（使用纯文本）
const validateContent = () => {
  const plain = form.content.replace(/<[^>]*>/g, '').trim()
  return plain.length > 0
}

const performSave = async (isPublish = false) => {
  const requestData = {
    title: form.title,
    content: form.content,
    tags: form.tags,
    parentArticleId: form.parentArticleId,
  }

  let response
  if (isEditMode.value && articleId.value) {
    response = await axios.put(`/articles/${articleId.value}`, requestData)
  } else {
    response = await axios.post('/articles', requestData)
  }

  const savedId = response.data?.data?.id || response.data?.id || response.data
  if (!isEditMode.value && savedId) {
    articleId.value = savedId.toString()
    isEditMode.value = true
  }

  // 更新关联设定
  await axios.put(`/articles/${savedId}/settings`, form.settings)

  if (isPublish) {
    await axios.post(`/articles/${savedId}/publish`)
  }
  setInitialValues()
  return savedId
}

// 搜索文章（仅已发布）
const searchArticles = async () => {
  if (!searchKeyword.value.trim()) return
  hasSearched.value = true // 添加这一行
  searchLoading.value = true
  try {
    const res = await axios.post('/articles/search', {
      keyword: searchKeyword.value,
      page: 0,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    })
    searchResults.value = res.data.content
  } catch {
    ElMessage.error('搜索失败')
  } finally {
    searchLoading.value = false
  }
}

// 选择引用文章
const selectParentArticle = (article: ParentArticle) => {
  selectedParentArticle.value = article
  form.parentArticleId = article.id
  searchKeyword.value = ''
  searchResults.value = []
}

// 移除引用
const removeReference = () => {
  selectedParentArticle.value = null
  form.parentArticleId = undefined
}

const searchSettings = async (query: string) => {
  if (!query) {
    settingOptions.value = []
    return
  }
  settingSearchLoading.value = true
  try {
    const res = await axios.post('/settings/search', {
      keyword: query,
      page: 0,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    })
    settingOptions.value = res.data.content.map((s: SimpleSettingInfo) => ({
      id: s.id,
      title: s.title,
    }))
  } finally {
    settingSearchLoading.value = false
  }
}

const addSetting = (settingId: number | null) => {
  if (!settingId) return
  if (form.settings.length >= 20) {
    ElMessage.warning('最多关联20个设定')
    searchSetting.value = ''
    return
  }
  if (form.settings.includes(settingId)) {
    ElMessage.warning('该设定已添加')
    searchSetting.value = ''
    return
  }
  form.settings.push(settingId)
  // 同时更新 selectedSettings 用于展示标签
  const setting = settingOptions.value.find((s) => s.id === settingId)
  if (setting) selectedSettings.value.push(setting)
  searchSetting.value = ''
}

const removeSetting = (settingId: number) => {
  selectedSettings.value = selectedSettings.value.filter((s) => s.id !== settingId)
  form.settings = form.settings.filter((id) => id !== settingId)
}

const loadArticleSettings = async () => {
  if (!articleId.value) return
  try {
    const res = await axios.get(`/articles/${articleId.value}/settings`)
    const data = res.data as SimpleSettingInfo[]
    selectedSettings.value = data
    form.settings = data.map((s) => s.id)
  } catch (e) {
    console.error('加载关联设定失败', e)
  }
}

watch(searchKeyword, (newVal) => {
  if (!newVal) {
    searchResults.value = []
    hasSearched.value = false
  }
})

// ================== 路由守卫：离开时检查未保存空白草稿 ==================
onBeforeRouteLeave(async (to, from, next) => {
  // 如果正在保存，直接放行（防止递归）
  if (isSaving.value) {
    next()
    return
  }

  // 检查是否有未保存的修改
  if (hasUnsavedChanges()) {
    try {
      await ElMessageBox.confirm('您有未保存的修改，是否保存？', '提示', {
        confirmButtonText: '保存并离开',
        cancelButtonText: '不保存',
        type: 'warning',
      })
      // 用户选择保存：先阻止当前导航，保存后再手动跳转
      next(false) // 取消当前离开
      isSaving.value = true
      try {
        await performSave(false) // 只保存，不跳转
        // 保存成功后，手动跳转到目标路由
        await router.push(to.fullPath)
      } catch (err) {
        console.error('保存失败', err)
        ElMessage.error('保存失败，请稍后重试')
      } finally {
        isSaving.value = false
      }
    } catch {
      // 用户选择不保存，直接离开
      next()
    }
  } else {
    next()
  }
})

onMounted(async () => {
  await loadTagRecommendations()

  if (isEditMode.value && articleId.value) {
    // 编辑模式：加载文章数据
    try {
      const response = await axios.get(`/articles/${articleId.value}`)
      const articleData = response.data.data || response.data
      form.title = articleData.title
      form.content = articleData.content
      form.tags = articleData.tags || []
      if (articleData.parentArticle) {
        selectedParentArticle.value = articleData.parentArticle
        form.parentArticleId = articleData.parentArticle.id
      }
      setInitialValues()
      const settingsRes = await axios.get(`/articles/${articleId.value}/settings`)
      form.settings = settingsRes.data.map((s: SimpleSettingInfo) => s.id)
      selectedSettingsList.value = settingsRes.data
      await loadArticleSettings()
    } catch (error) {
      console.error('加载文章失败:', error)
      ElMessage.error('加载文章失败')
      router.push('/')
    }
  } else {
    // 新建模式：处理父文章ID（从 URL 参数获取）
    const parentId = route.query.parentId
    if (parentId) {
      try {
        const res = await axios.get(`/articles/${parentId}`)
        const parentData = res.data.data || res.data
        selectedParentArticle.value = {
          id: parentData.id,
          title: parentData.title,
          authorUsername: parentData.authorUsername,
          viewCount: parentData.viewCount,
          createdAt: parentData.createdAt,
        }
        form.parentArticleId = parentData.id
        setInitialValues()
      } catch (error) {
        console.error('获取父文章信息失败', error)
      }
    }
  }
})
</script>

<style scoped>
.article-create-container {
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

.article-editor {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.title-input {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 30px;
}

.title-input :deep(.el-input__inner) {
  font-size: 28px;
  font-weight: bold;
  border: none;
  padding: 0;
}

.title-input :deep(.el-input__inner::placeholder) {
  color: #c0c4cc;
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

.tags-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.tags-count {
  font-size: 12px;
  color: #909399;
}

.selected-tags {
  margin-bottom: 15px;
  min-height: 32px;
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 8px;
}

.tag-search-wrapper {
  position: relative;
}

.tag-autocomplete {
  width: 100%;
  margin-bottom: 20px;
}

.tag-suggestion {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.tag-name {
  flex: 1;
}

.tag-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
  margin-right: 8px;
}

.popular-tags {
  margin-top: 20px;
}

.popular-title,
.recommended-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  font-weight: 500;
}

.popular-tags-list,
.recommended-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.popular-tag-item,
.recommended-tag-item {
  cursor: pointer;
  transition: all 0.3s ease;
}

.popular-tag-item:hover,
.recommended-tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.recommended-tag-item {
  border-width: 2px;
}

.ml-1 {
  margin-left: 4px;
}

.ml-2 {
  margin-left: 8px;
}

.content-section {
  margin-bottom: 30px;
}

.content-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
  font-weight: 500;
}

.content-textarea :deep(.el-textarea__inner) {
  font-size: 16px;
  line-height: 1.6;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
  font-family: 'Helvetica Neue', Arial, sans-serif;
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

.editor-wrapper {
  min-height: 400px;
}

.editor-wrapper {
  min-height: 400px;
  margin-bottom: 20px;
}

.editor-wrapper {
  min-height: 400px;
  margin-bottom: 20px;
}

.content-text iframe,
.content-text video {
  max-width: 100%;
  height: auto;
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

.settings-section-editor {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.selected-settings {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.setting-tag {
  cursor: default;
}

.settings-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.settings-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.settings-count {
  font-size: 12px;
  color: #909399;
}

.selected-settings {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.setting-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.setting-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
