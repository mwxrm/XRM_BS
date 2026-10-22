<template>
  <div class="profile-container">
    <el-container>
      <el-main>
        <el-card class="profile-card">
          <template #header>
            <div class="profile-header">
              <h2>个人中心</h2>
              <el-avatar :size="60" :src="avatarUrl" />
            </div>
          </template>
          <div class="user-info-card" v-if="userStore.user">
            <div class="avatar">
              <el-avatar :size="80" :src="avatarUrl" />
            </div>
            <div class="info">
              <h2>{{ userStore.user.username }}</h2>
              <div class="meta">
                <span>ID：{{ userStore.user.id }}</span>
                <span>角色：{{ userStore.user.role === 'admin' ? '管理员' : '普通用户' }}</span>
                <span>注册时间：{{ formatDate(userStore.user.createdAt) }}</span>
              </div>
              <div class="follow-stats">
                <span @click="activeTab = 'following'">{{ followingCount }} 关注</span>
                <span @click="activeTab = 'followers'">{{ followersCount }} 粉丝</span>
                <span>{{ totalLikes }} 获赞</span>
              </div>
            </div>
          </div>

          <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <el-tab-pane label="收藏夹" name="folders">
              <div v-if="foldersLoading">加载中...</div>
              <div v-else>
                <div class="filter-bar">
                  <el-radio-group v-model="itemTypeFilter" @change="onFilterChange">
                    <el-radio-button value="">全部</el-radio-button>
                    <el-radio-button value="article">文章</el-radio-button>
                    <el-radio-button value="setting">设定</el-radio-button>
                  </el-radio-group>
                  <el-button type="primary" @click="showCreateFolderDialog = true"
                    >新建文件夹</el-button
                  >
                </div>

                <el-row :gutter="20" class="folder-grid">
                  <el-col :span="6" v-for="folder in folders" :key="folder.id">
                    <el-card class="folder-card" shadow="hover">
                      <div class="folder-card-header" @click="selectFolder(folder)">
                        <h3>{{ folder.name }}</h3>
                        <p>{{ folder.itemCount }} 个项目</p>
                      </div>
                      <div class="folder-card-actions">
                        <el-button link size="small" @click="editFolder(folder)">
                          <el-icon><Edit /></el-icon> 编辑
                        </el-button>
                        <el-button link size="small" @click="deleteFolder(folder)">
                          <el-icon><Delete /></el-icon> 删除
                        </el-button>
                      </div>
                    </el-card>
                  </el-col>
                </el-row>

                <!-- 文件夹内项目列表 -->
                <div v-if="selectedFolder">
                  <h3>{{ selectedFolder.name }} - 项目列表</h3>
                  <el-button @click="selectedFolder = null">返回</el-button>
                  <el-table :data="folderItems" v-loading="folderItemsLoading">
                    <el-table-column label="标题">
                      <template #default="{ row }">
                        <span :class="{ 'deleted-text': row.deleted }">{{ row.title }}</span>
                        <el-tag
                          v-if="row.deleted"
                          type="danger"
                          size="small"
                          style="margin-left: 8px"
                          >已删除</el-tag
                        >
                      </template>
                    </el-table-column>

                    <el-table-column prop="authorUsername" label="作者" width="150" />
                    <el-table-column label="类型" width="80">
                      <template #default="{ row }">
                        <el-tag
                          :type="row.itemType === 'article' ? 'primary' : 'success'"
                          size="small"
                        >
                          {{ row.itemType === 'article' ? '文章' : '设定' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="favoritedAt" label="收藏时间" width="200" />
                    <el-table-column label="操作" width="200">
                      <template #default="{ row }">
                        <el-button link @click="viewItem(row.itemId, row.itemType, row.deleted)">
                          {{ row.deleted ? '查看（已删除）' : '查看' }}
                        </el-button>
                        <el-button link @click="removeFromFolder(row.itemId, row.itemType)"
                          >移除</el-button
                        >
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-pagination
                    v-if="totalFolderItems > pageSize"
                    :current-page="folderPage"
                    :page-size="pageSize"
                    :total="totalFolderItems"
                    @current-change="loadFolderItems"
                  />
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="最近浏览" name="recent">
              <el-button
                type="danger"
                @click="clearRecentViews"
                :loading="clearingRecent"
                size="small"
              >
                清空历史记录
              </el-button>
              <el-table :data="recentViews" v-loading="recentLoading">
                <el-table-column label="标题">
                  <template #default="{ row }">
                    <span :class="{ 'deleted-text': row.deleted }">{{ row.title }}</span>
                    <el-tag v-if="row.deleted" type="danger" size="small" style="margin-left: 8px"
                      >已删除</el-tag
                    >
                  </template>
                </el-table-column>
                <el-table-column prop="authorUsername" label="作者" width="150" />
                <el-table-column label="类型" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.itemType === 'article' ? 'primary' : 'success'" size="small">
                      {{ row.itemType === 'article' ? '文章' : '设定' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="viewedAt" label="浏览时间" width="200" />
                <!-- 操作列 -->
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button
                      link
                      @click="row.deleted ? null : viewItem(row.itemId, row.itemType, row.deleted)"
                    >
                      {{ row.deleted ? '已删除' : '查看' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-pagination
                v-if="totalRecent > recentPageSize"
                background
                layout="prev, pager, next"
                :current-page="recentPage"
                :page-size="recentPageSize"
                :total="totalRecent"
                @current-change="handleRecentPageChange"
              />
            </el-tab-pane>
            <el-tab-pane label="我的创作" name="creations">
              <div v-loading="creationsLoading">
                <div class="filter-bar">
                  <el-radio-group v-model="creationTypeFilter" @change="loadCreations">
                    <el-radio-button value="">全部</el-radio-button>
                    <el-radio-button value="article">文章</el-radio-button>
                    <el-radio-button value="setting">设定</el-radio-button>
                  </el-radio-group>
                </div>
                <div v-if="creations.length === 0" class="empty-state">
                  <el-empty description="暂无创作" />
                </div>
                <div v-else class="creations-list">
                  <div v-for="item in creations" :key="item.id" class="creation-card">
                    <div class="creation-content" @click="viewCreation(item)">
                      <h4 class="creation-title">
                        {{ item.title }}
                        <el-tag :type="item.status === 'draft' ? 'info' : 'success'" size="small">
                          {{ item.status === 'draft' ? '草稿' : '已发布' }}
                        </el-tag>
                        <el-tag
                          :type="item.type === 'article' ? 'primary' : 'success'"
                          size="small"
                        >
                          {{ item.type === 'article' ? '文章' : '设定' }}
                        </el-tag>
                      </h4>
                      <p class="creation-summary">{{ item.summary }}</p>
                      <div class="creation-meta">
                        <span class="meta-item">
                          <el-icon><Clock /></el-icon>
                          {{ formatDate(item.createdAt) }}
                        </span>
                        <span class="meta-item">
                          <el-icon><View /></el-icon>
                          {{ item.viewCount }}
                        </span>
                        <span class="meta-item">
                          <el-icon><Star /></el-icon>
                          {{ item.likeCount }}
                        </span>
                        <div class="creation-tags">
                          <el-tag v-for="tag in item.tags" :key="tag" size="small" type="info">
                            {{ tag }}
                          </el-tag>
                        </div>
                      </div>
                    </div>
                    <div class="creation-actions">
                      <el-button link @click="editCreation(item)">
                        <el-icon><Edit /></el-icon> 编辑
                      </el-button>
                      <el-button v-if="item.status === 'draft'" link @click="publishCreation(item)">
                        <el-icon><Upload /></el-icon> 发布
                      </el-button>
                      <el-button link @click="deleteCreation(item)">
                        <el-icon><Delete /></el-icon> 删除
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="关注" name="following">
              <div v-loading="followingLoading">
                <div
                  v-for="item in followingList"
                  :key="item.id"
                  class="user-item"
                  @click="viewUser(item.id)"
                >
                  <el-avatar :size="40" :src="getAvatarUrl(item.username)" />
                  <div class="user-info">
                    <span class="username">{{ item.username }}</span>
                    <span class="role">{{ item.role === 'admin' ? '管理员' : '用户' }}</span>
                  </div>
                  <el-button
                    v-if="item.id !== userStore.user?.id"
                    :type="item.following ? 'default' : 'primary'"
                    size="small"
                    @click.stop="toggleFollowUser(item)"
                  >
                    {{ item.following ? '已关注' : '+ 关注' }}
                  </el-button>
                </div>
                <el-empty
                  v-if="!followingLoading && followingList.length === 0"
                  description="暂无关注"
                />
                <el-pagination
                  v-if="followingTotal > pageSize"
                  background
                  layout="prev, pager, next"
                  :current-page="followingPage"
                  :page-size="pageSize"
                  :total="followingTotal"
                  @current-change="loadFollowing"
                />
              </div>
            </el-tab-pane>
            <el-tab-pane label="粉丝" name="followers">
              <div v-loading="followersLoading">
                <div
                  v-for="item in followersList"
                  :key="item.id"
                  class="user-item"
                  @click="viewUser(item.id)"
                >
                  <el-avatar :size="40" :src="getAvatarUrl(item.username)" />
                  <div class="user-info">
                    <span class="username">{{ item.username }}</span>
                    <span class="role">{{ item.role === 'admin' ? '管理员' : '用户' }}</span>
                  </div>
                  <el-button
                    v-if="item.id !== userStore.user?.id"
                    :type="item.following ? 'default' : 'primary'"
                    size="small"
                    @click.stop="toggleFollowUser(item)"
                  >
                    {{ item.following ? '已关注' : '+ 关注' }}
                  </el-button>
                </div>
                <el-empty
                  v-if="!followersLoading && followersList.length === 0"
                  description="暂无粉丝"
                />
                <el-pagination
                  v-if="followersTotal > pageSize"
                  background
                  layout="prev, pager, next"
                  :current-page="followersPage"
                  :page-size="pageSize"
                  :total="followersTotal"
                  @current-change="loadFollowers"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="喜好与屏蔽" name="preferences">
              <!-- ========== 喜好标签区域 ========== -->
              <div class="pref-section">
                <div class="pref-section-title">喜欢的标签</div>
                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px">
                  <el-select
                    v-model="tempSelectedTag"
                    v-model:visible="selectVisible"
                    filterable
                    remote
                    reserve-keyword
                    placeholder="搜索喜欢的标签..."
                    :remote-method="searchTags"
                    :loading="tagSearchLoading"
                    clearable
                    value-key="id"
                    style="width: 300px"
                    @change="addTagToPreferences"
                    @visible-change="handleVisibleChange"
                  >
                    <el-option
                      v-for="tag in tagOptions"
                      :key="tag.id"
                      :label="tag.name"
                      :value="tag.id"
                      :class="{ 'added-tag-option': tag.added }"
                    >
                      <span>{{ tag.name }}</span>
                      <el-tag size="small" type="info" style="margin-left: 8px">{{
                        getClassificationName(tag.classification)
                      }}</el-tag>
                      <el-tag v-if="tag.added" type="success" size="small" style="margin-left: 8px"
                        >已添加</el-tag
                      >
                    </el-option>
                  </el-select>
                  <el-button type="primary" @click="savePreferences" :loading="savingPreferences"
                    >保存</el-button
                  >
                  <span class="selected-count">已选择 {{ selectedTagIds.length }}/50 个标签</span>
                </div>

                <div class="selected-tags" v-if="selectedTagsDetail.length > 0">
                  <el-tag
                    v-for="tag in selectedTagsDetail"
                    :key="tag.id"
                    closable
                    size="large"
                    effect="plain"
                    class="preference-tag"
                    @close="removeTagFromPreferences(tag.id)"
                    @click="goToTag(tag.id)"
                  >
                    {{ tag.name }}
                    <span class="tag-classification">{{
                      getClassificationName(tag.classification)
                    }}</span>
                  </el-tag>
                </div>
                <el-empty v-else description="尚未添加喜好标签" :image-size="60" />
              </div>

              <el-divider />

              <!-- ========== 屏蔽标签区域 ========== -->
              <div class="pref-section">
                <div class="pref-section-title">屏蔽的标签</div>
                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px">
                  <el-select
                    v-model="tempBlockedTag"
                    filterable
                    remote
                    reserve-keyword
                    placeholder="搜索要屏蔽的标签..."
                    :remote-method="searchBlockedTags"
                    :loading="blockedTagSearchLoading"
                    clearable
                    value-key="id"
                    style="width: 300px"
                    @change="addBlockedTag"
                  >
                    <el-option
                      v-for="tag in blockedTagOptions"
                      :key="tag.id"
                      :label="tag.name"
                      :value="tag.id"
                      :class="{ 'added-tag-option': tag.added }"
                    >
                      <span>{{ tag.name }}</span>
                      <el-tag size="small" type="info" style="margin-left: 8px">{{
                        getClassificationName(tag.classification)
                      }}</el-tag>
                      <el-tag v-if="tag.added" type="success" size="small" style="margin-left: 8px"
                        >已添加</el-tag
                      >
                    </el-option>
                  </el-select>
                  <el-button type="danger" @click="saveBlockedTags" :loading="savingBlockedTags"
                    >保存</el-button
                  >
                  <span class="selected-count">已选择 {{ blockedTagIds.length }}/50 个标签</span>
                </div>

                <div class="selected-tags" v-if="blockedTagsDetail.length > 0">
                  <el-tag
                    v-for="tag in blockedTagsDetail"
                    :key="tag.id"
                    closable
                    size="large"
                    effect="plain"
                    type="danger"
                    class="blocked-tag"
                    @close="removeBlockedTag(tag.id)"
                    @click="goToTag(tag.id)"
                  >
                    {{ tag.name }}
                    <span class="tag-classification">{{
                      getClassificationName(tag.classification)
                    }}</span>
                  </el-tag>
                </div>
                <el-empty v-else description="未屏蔽任何标签" :image-size="60" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-main>
    </el-container>

    <!-- 新建文件夹对话框（统一） -->
    <el-dialog v-model="showCreateFolderDialog" title="新建文件夹">
      <el-form :model="newFolder" :rules="folderRules" ref="folderFormRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="newFolder.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="newFolder.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateFolderDialog = false">取消</el-button>
        <el-button type="primary" @click="createFolder">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑文件夹对话框 -->
    <el-dialog v-model="showEditFolderDialog" title="编辑文件夹" width="500px">
      <el-form :model="editingFolder" :rules="folderRules" ref="editFolderFormRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="editingFolder.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editingFolder.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditFolderDialog = false">取消</el-button>
        <el-button type="primary" @click="updateFolder">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Edit, Delete, Upload } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import { useUserStore } from '@/stores/user'
import { isAxiosError } from 'axios'
import type { AxiosError } from 'axios'

// ---------- 类型定义 ----------
interface Folder {
  id: number
  name: string
  description: string
  itemCount: number
  createdAt: string
  updatedAt: string
}

interface FavoriteItem {
  id: number
  itemId: number
  title: string
  summary: string
  authorUsername: string
  favoritedAt: string
  itemType: 'article' | 'setting'
}

interface RecentView {
  id: number
  articleId: number
  articleTitle: string
  articleSummary: string
  authorUsername: string
  viewedAt: string
}

interface CreationItem {
  id: number
  title: string
  summary: string
  type: 'article' | 'setting'
  status: 'draft' | 'published'
  viewCount: number
  likeCount: number
  tags: string[]
  createdAt: string
}

interface ApiArticle {
  id: number
  title: string
  content: string
  summary?: string
  status: 'draft' | 'published'
  viewCount: number
  likeCount: number
  tags: string[]
  createdAt: string
}

interface ApiSetting {
  id: number
  title: string
  content: string
  summary?: string
  status: 'draft' | 'published'
  viewCount: number
  likeCount: number
  tags: string[]
  createdAt: string
}

interface RecentView {
  id: number
  itemId: number
  title: string
  summary: string
  authorUsername: string
  viewedAt: string
  itemType: 'article' | 'setting'
}

interface CreateFolderRequest {
  name: string
  description?: string
}

interface UserPublicResponse {
  id: number
  username: string
  role: string
  createdAt: string
  following?: boolean // 可选，用于显示关注状态
}

interface TagOption {
  id: number
  name: string
  classification: string
  added?: boolean
}

// interface SelectInstance {
//   handleClear: () => void
// }

// ---------- 路由与 Store ----------
const router = useRouter()
const userStore = useUserStore()
const avatarUrl = ref('')

const creations = ref<CreationItem[]>([])
const creationsLoading = ref(false)
const creationTypeFilter = ref('')

// 统计相关
const followingCount = ref(0)
const followersCount = ref(0)
const totalLikes = ref(0)

// 生成头像
const generateAvatar = () => {
  const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7']
  const randomColor = colors[Math.floor(Math.random() * colors.length)] || '#FF6B6B'
  avatarUrl.value = `https://ui-avatars.com/api/?name=${userStore.user?.username || 'User'}&background=${randomColor.slice(1)}&color=fff`
}

// 标签页
const activeTab = ref('folders')

// ---------- 统一收藏夹 ----------
const folders = ref<Folder[]>([])
const foldersLoading = ref(false)
const showCreateFolderDialog = ref(false)
const newFolder = reactive<CreateFolderRequest>({ name: '', description: '' })
const folderFormRef = ref<FormInstance>()
const showEditFolderDialog = ref(false)
const editingFolder = reactive<Partial<Folder>>({})
const editFolderFormRef = ref<FormInstance>()
const folderRules: FormRules = {
  name: [{ required: true, message: '请输入文件夹名称', trigger: 'blur' }],
}

// 类型筛选
const itemTypeFilter = ref('')

// 文件夹内项目
const selectedFolder = ref<Folder | null>(null)
const folderItems = ref<FavoriteItem[]>([])
const folderItemsLoading = ref(false)
const totalFolderItems = ref(0)
const pageSize = ref(20)
const folderPage = ref(1)

// 关注/粉丝列表
const followingList = ref<UserPublicResponse[]>([])
const followersList = ref<UserPublicResponse[]>([])
const followingLoading = ref(false)
const followersLoading = ref(false)
const followingPage = ref(1)
const followersPage = ref(1)
const followingTotal = ref(0)
const followersTotal = ref(0)

const selectedTagIds = ref<number[]>([])
const tagOptions = ref<TagOption[]>([])
const tagSearchLoading = ref(false)
const savingPreferences = ref(false)
const tempSelectedTag = ref<number | null>(null) // 临时选中的标签 id
const selectedTagsDetail = ref<TagOption[]>([]) // 已选标签的详细信息

const selectVisible = ref(false) // 控制下拉框显隐

// ---------- 屏蔽标签相关 ----------
const blockedTagIds = ref<number[]>([])
const tempBlockedTag = ref<number | null>(null)
const blockedTagOptions = ref<TagOption[]>([])
const blockedTagsDetail = ref<TagOption[]>([])
const blockedTagSearchLoading = ref(false)
const savingBlockedTags = ref(false)

const shouldStayOpen = ref(false)
const clearingRecent = ref(false)

// 加载文件夹列表
const loadFolders = async () => {
  foldersLoading.value = true
  try {
    const res = await axios.get('/favorites/folders')
    folders.value = res.data
  } catch (err: unknown) {
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '加载失败')
    } else {
      ElMessage.error('加载失败')
    }
  } finally {
    foldersLoading.value = false
  }
}

// 选择文件夹
const selectFolder = (folder: Folder) => {
  selectedFolder.value = folder
  folderPage.value = 1
  loadFolderItems()
}

// 加载文件夹内项目（支持类型筛选）
const loadFolderItems = async (page = folderPage.value) => {
  if (!selectedFolder.value) return
  folderItemsLoading.value = true
  try {
    const params: { page: number; size: number; itemType?: string } = {
      page: page - 1,
      size: pageSize.value,
    }
    if (itemTypeFilter.value) {
      params.itemType = itemTypeFilter.value
    }
    const res = await axios.get(`/favorites/folders/${selectedFolder.value.id}/items`, { params })
    folderItems.value = res.data.content
    totalFolderItems.value = res.data.totalElements
  } catch (err: unknown) {
    console.error('加载项目失败', err)
  } finally {
    folderItemsLoading.value = false
  }
}

// 查看项目
const viewItem = (itemId: number, itemType: string, deleted: boolean) => {
  if (deleted) {
    ElMessage.warning('该内容已被删除，无法查看')
    return
  }
  if (itemType === 'article') {
    router.push(`/articles/${itemId}`)
  } else if (itemType === 'setting') {
    router.push(`/settings/${itemId}`)
  }
}

// 从文件夹移除项目
const removeFromFolder = async (itemId: number, itemType: string) => {
  if (!selectedFolder.value) return
  try {
    await axios.delete(`/favorites/folders/${selectedFolder.value.id}/${itemType}/${itemId}`)
    ElMessage.success('已移除')
    // 刷新当前文件夹内的项目列表
    await loadFolderItems()
    // 刷新所有文件夹列表（更新每个文件夹的 itemCount）
    await loadFolders()
    // 重新选中当前文件夹（因为 loadFolders 会重建 folders 数组，需要重新绑定）
    const refreshedFolder = folders.value.find((f) => f.id === selectedFolder.value!.id)
    if (refreshedFolder) {
      selectedFolder.value = refreshedFolder
    } else {
      selectedFolder.value = null // 文件夹可能已被删除
    }
  } catch (err) {
    console.error('移除失败', err)
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '移除失败')
    } else {
      ElMessage.error('移除失败')
    }
  }
}

// 筛选变化时重新加载
const onFilterChange = () => {
  if (selectedFolder.value) {
    folderPage.value = 1
    loadFolderItems()
  }
}

// 创建文件夹
const createFolder = async () => {
  if (!folderFormRef.value) return
  try {
    await folderFormRef.value.validate()
    await axios.post('/favorites/folders', newFolder)
    ElMessage.success('创建成功')
    showCreateFolderDialog.value = false
    newFolder.name = ''
    newFolder.description = ''
    loadFolders()
  } catch (err: unknown) {
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '创建失败')
    } else {
      ElMessage.error('创建失败')
    }
  }
}

// 编辑文件夹
const editFolder = (folder: Folder) => {
  editingFolder.id = folder.id
  editingFolder.name = folder.name
  editingFolder.description = folder.description
  showEditFolderDialog.value = true
}

const updateFolder = async () => {
  if (!editFolderFormRef.value || !editingFolder.id) return
  try {
    await editFolderFormRef.value.validate()
    await axios.put(`/favorites/folders/${editingFolder.id}`, {
      name: editingFolder.name,
      description: editingFolder.description,
    })
    ElMessage.success('更新成功')
    showEditFolderDialog.value = false
    loadFolders()
  } catch (err: unknown) {
    if (isAxiosError(err)) {
      ElMessage.error(err.response?.data?.message || '更新失败')
    } else {
      ElMessage.error('更新失败')
    }
  }
}

// 删除文件夹
const deleteFolder = (folder: Folder) => {
  ElMessageBox.confirm(
    `确定要删除文件夹「${folder.name}」吗？其中的收藏也会一并删除。`,
    '删除确认',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
    .then(async () => {
      try {
        await axios.delete(`/favorites/folders/${folder.id}`)
        ElMessage.success('删除成功')
        loadFolders()
        if (selectedFolder.value?.id === folder.id) {
          selectedFolder.value = null
        }
      } catch (err: unknown) {
        if (isAxiosError(err)) {
          ElMessage.error(err.response?.data?.message || '删除失败')
        } else {
          ElMessage.error('删除失败')
        }
      }
    })
    .catch(() => {})
}

// ---------- 最近浏览 ----------
const recentViews = ref<RecentView[]>([])
const recentLoading = ref(false)
const totalRecent = ref(0)
const recentPage = ref(1)
const recentPageSize = ref(20)

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const loadCreations = async () => {
  creationsLoading.value = true
  try {
    let articles: CreationItem[] = []
    let settings: CreationItem[] = []

    if (!creationTypeFilter.value || creationTypeFilter.value === 'article') {
      const articleRes = await axios.get('/articles/my')
      articles = (articleRes.data as ApiArticle[]).map((a) => ({
        id: a.id,
        title: a.title,
        summary: a.summary || a.content?.replace(/<[^>]*>/g, '').slice(0, 100) || '',
        type: 'article',
        status: a.status,
        viewCount: a.viewCount,
        likeCount: a.likeCount,
        tags: a.tags || [],
        createdAt: a.createdAt,
      }))
    }

    if (!creationTypeFilter.value || creationTypeFilter.value === 'setting') {
      const settingRes = await axios.get('/settings/my')
      settings = (settingRes.data as ApiSetting[]).map((s) => ({
        id: s.id,
        title: s.title,
        summary: s.summary || s.content?.replace(/<[^>]*>/g, '').slice(0, 100) || '',
        type: 'setting',
        status: s.status,
        viewCount: s.viewCount,
        likeCount: s.likeCount,
        tags: s.tags || [],
        createdAt: s.createdAt,
      }))
    }

    const all = [...articles, ...settings]
    all.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    creations.value = all
  } catch (error) {
    console.error('加载创作失败', error)
    ElMessage.error('加载创作失败')
  } finally {
    creationsLoading.value = false
  }
}

// 查看创作
const viewCreation = (item: CreationItem) => {
  if (item.type === 'article') {
    if (item.status === 'draft') {
      router.push(`/articles/${item.id}/edit`)
    } else {
      router.push(`/articles/${item.id}`)
    }
  } else {
    if (item.status === 'draft') {
      router.push(`/settings/${item.id}/edit`)
    } else {
      router.push(`/settings/${item.id}`)
    }
  }
}

// 编辑创作
const editCreation = (item: CreationItem) => {
  if (item.type === 'article') {
    router.push(`/articles/${item.id}/edit`)
  } else {
    router.push(`/settings/${item.id}/edit`)
  }
}

// 发布创作
const publishCreation = async (item: CreationItem) => {
  try {
    if (item.type === 'article') {
      await axios.post(`/articles/${item.id}/publish`)
    } else {
      await axios.post(`/settings/${item.id}/publish`)
    }
    ElMessage.success('发布成功')
    await loadCreations() // 刷新列表
  } catch (err) {
    console.error('发布失败', err)
    ElMessage.error('发布失败')
  }
}

// 删除创作
const deleteCreation = async (item: CreationItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除「${item.title}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (item.type === 'article') {
      await axios.delete(`/articles/${item.id}`)
    } else {
      await axios.delete(`/settings/${item.id}`)
    }
    ElMessage.success('删除成功')
    // 刷新创作列表
    await loadCreations()
    // 刷新收藏夹列表（更新 itemCount）
    await loadFolders()
    // 如果当前有选中的文件夹，刷新其内部项目列表
    if (selectedFolder.value) {
      await loadFolderItems()
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除失败', err)
      ElMessage.error('删除失败')
    }
  }
}

// 获取头像
const getAvatarUrl = (username: string) => {
  return `https://ui-avatars.com/api/?name=${username}&background=409eff&color=fff`
}

// 查看用户主页
const viewUser = (userId: number) => {
  router.push(`/users/${userId}`)
}

const loadFollowing = async (page = 1) => {
  if (!userStore.user) return
  followingLoading.value = true
  try {
    const res = await axios.get(`/follow/${userStore.user.id}/following`, {
      params: { page: page - 1, size: pageSize.value },
    })
    const users = res.data.content as UserPublicResponse[]
    followingList.value = users.map((u) => ({ ...u, following: true }))
    followingTotal.value = res.data.totalElements
    followingPage.value = page
  } catch {
    ElMessage.error('加载关注列表失败')
  } finally {
    followingLoading.value = false
  }
}

const loadFollowers = async (page = 1) => {
  if (!userStore.user) return
  followersLoading.value = true
  try {
    const res = await axios.get(`/follow/${userStore.user.id}/followers`, {
      params: { page: page - 1, size: pageSize.value },
    })
    const users = res.data.content as UserPublicResponse[]
    // 批量查询关注状态
    const userIds = users.map((u) => u.id)
    if (userIds.length > 0) {
      try {
        const statusRes = await axios.post('/follow/batch-status', userIds)
        const statusMap = statusRes.data as Record<number, boolean>
        followersList.value = users.map((u) => ({
          ...u,
          following: statusMap[u.id] || false,
        }))
      } catch {
        // 降级单个查询
        const withStatus = await Promise.all(
          users.map(async (u) => {
            if (u.id === userStore.user?.id) return { ...u, following: false }
            try {
              const s = await axios.get(`/follow/${u.id}/status`)
              return { ...u, following: s.data.following }
            } catch {
              return { ...u, following: false }
            }
          }),
        )
        followersList.value = withStatus
      }
    } else {
      followersList.value = []
    }
    followersTotal.value = res.data.totalElements
    followersPage.value = page
  } catch {
    ElMessage.error('加载粉丝列表失败')
  } finally {
    followersLoading.value = false
  }
}

// 关注/取关用户
const toggleFollowUser = async (user: UserPublicResponse & { following?: boolean }) => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    return
  }

  // 先获取最新的关注状态，避免本地缓存导致误判
  let actualFollowing: boolean
  try {
    const statusRes = await axios.get(`/follow/${user.id}/status`)
    actualFollowing = statusRes.data.following
  } catch {
    ElMessage.error('网络错误，请稍后重试')
    return
  }

  // 根据最新状态执行相反操作
  try {
    if (actualFollowing) {
      await axios.delete(`/follow/${user.id}`)
      user.following = false
      ElMessage.success('已取消关注')
    } else {
      await axios.post(`/follow/${user.id}`)
      user.following = true
      ElMessage.success('关注成功')
    }
    // 刷新当前列表，确保所有条目状态正确
    if (activeTab.value === 'following') {
      loadFollowing()
    } else if (activeTab.value === 'followers') {
      loadFollowers()
    }
    // 刷新顶部关注/粉丝计数
    await loadFollowCounts()
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 加载关注/粉丝统计
const loadFollowCounts = async () => {
  if (!userStore.user) return
  try {
    const res = await axios.get(`/follow/${userStore.user.id}/counts`)
    followingCount.value = res.data.following
    followersCount.value = res.data.followers
  } catch (error) {
    console.error('获取关注数失败', error)
  }
}

// 加载总获赞数（需后端接口）
const loadTotalLikes = async () => {
  if (!userStore.user) return
  try {
    // 假设后端提供 /users/{id}/likes/count 接口
    const res = await axios.get(`/users/${userStore.user.id}/likes/count`)
    totalLikes.value = res.data.count || 0
  } catch {
    // 若接口未实现，可暂时设为0或从前端计算（不推荐）
    totalLikes.value = 0
  }
}

// Tab切换时加载对应数据
const onTabChange = (tabName: string) => {
  if (tabName === 'following') {
    loadFollowing()
  } else if (tabName === 'followers') {
    loadFollowers()
  }
}

// 搜索喜好标签时，排除已屏蔽的标签
const searchTags = async (query: string) => {
  if (!query) {
    tagOptions.value = []
    return
  }
  tagSearchLoading.value = true
  try {
    const res = await axios.get('/tags/search', { params: { keyword: query } })
    tagOptions.value = (res.data as TagOption[])
      .filter((tag: TagOption) => !blockedTagIds.value.includes(tag.id))
      .map((tag) => ({
        id: tag.id,
        name: tag.name,
        classification: tag.classification,
        added: selectedTagIds.value.includes(tag.id),
      }))
  } finally {
    tagSearchLoading.value = false
  }
}

// 加载用户已有的喜好标签
const loadPreferences = async () => {
  try {
    const res = await axios.get('/users/me/preferences/tags')
    const tags = res.data as TagOption[]
    selectedTagIds.value = tags.map((t) => t.id)
    selectedTagsDetail.value = tags
    tagOptions.value = tags
  } catch (err) {
    console.error('加载喜好标签失败', err)
  }
}

// 保存喜好标签
const savePreferences = async () => {
  if (selectedTagIds.value.length > 50) {
    ElMessage.warning('最多只能选择50个标签')
    return
  }
  savingPreferences.value = true
  try {
    await axios.put('/users/me/preferences/tags', selectedTagIds.value)
    ElMessage.success('喜好标签已保存')
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    savingPreferences.value = false
  }
}

// 获取分类中文名（可放在工具函数中）
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

// 添加标签
const addTagToPreferences = async (tagId: number | null) => {
  if (blockedTagIds.value.includes(tagId!)) {
    ElMessage.warning('不能添加已屏蔽的标签，请先从屏蔽列表移除')
    tempSelectedTag.value = null
    await nextTick()
    selectVisible.value = true
    return
  }
  if (!tagId) {
    selectVisible.value = false
    return
  }
  if (selectedTagIds.value.length >= 50) {
    ElMessage.warning('最多只能选择50个标签')
    tempSelectedTag.value = null
    selectVisible.value = true
    return
  }
  if (selectedTagIds.value.includes(tagId)) {
    ElMessage.warning('该标签已添加')
    tempSelectedTag.value = null
    selectVisible.value = true
    return
  }
  const tag = tagOptions.value.find((t) => t.id === tagId)
  if (tag) {
    selectedTagIds.value.push(tagId)
    selectedTagsDetail.value.push(tag)
  }
  tempSelectedTag.value = null
  selectVisible.value = true // 保持下拉框打开
}

const removeTagFromPreferences = (tagId: number) => {
  selectedTagIds.value = selectedTagIds.value.filter((id) => id !== tagId)
  selectedTagsDetail.value = selectedTagsDetail.value.filter((tag) => tag.id !== tagId)
}

const goToTag = (id: number) => {
  router.push(`/tags/${id}`)
}

// 搜索屏蔽标签时，排除已喜好的标签
const searchBlockedTags = async (query: string) => {
  if (!query) {
    blockedTagOptions.value = []
    return
  }
  blockedTagSearchLoading.value = true
  try {
    const res = await axios.get('/tags/search', { params: { keyword: query } })
    blockedTagOptions.value = (res.data as TagOption[])
      .filter((tag: TagOption) => !selectedTagIds.value.includes(tag.id))
      .map((tag) => ({
        id: tag.id,
        name: tag.name,
        classification: tag.classification,
        added: blockedTagIds.value.includes(tag.id),
      }))
  } finally {
    blockedTagSearchLoading.value = false
  }
}

// 添加屏蔽标签
const addBlockedTag = (tagId: number | null) => {
  if (!tagId) return
  if (selectedTagIds.value.includes(tagId!)) {
    ElMessage.warning('不能屏蔽已喜欢的标签，请先从喜好列表移除')
    tempBlockedTag.value = null
    return
  }
  if (blockedTagIds.value.length >= 50) {
    ElMessage.warning('最多屏蔽50个标签')
    tempBlockedTag.value = null
    return
  }
  if (blockedTagIds.value.includes(tagId)) {
    ElMessage.warning('该标签已在屏蔽列表中')
    tempBlockedTag.value = null
    return
  }
  const tag = blockedTagOptions.value.find((t) => t.id === tagId)
  if (tag) {
    blockedTagIds.value.push(tagId)
    blockedTagsDetail.value.push(tag)
  }
  tempBlockedTag.value = null
}

// 移除屏蔽标签
const removeBlockedTag = (tagId: number) => {
  blockedTagIds.value = blockedTagIds.value.filter((id) => id !== tagId)
  blockedTagsDetail.value = blockedTagsDetail.value.filter((tag) => tag.id !== tagId)
}

// 保存屏蔽标签
const saveBlockedTags = async () => {
  if (blockedTagIds.value.length > 50) {
    ElMessage.warning('最多只能屏蔽50个标签')
    return
  }
  savingBlockedTags.value = true
  try {
    await axios.put('/users/me/preferences/blocked-tags', blockedTagIds.value)
    ElMessage.success('屏蔽标签已保存')
  } catch (err) {
    const error = err as AxiosError<{ message?: string }>
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    savingBlockedTags.value = false
  }
}

// 加载已保存的屏蔽标签
const loadBlockedTags = async () => {
  try {
    const res = await axios.get('/users/me/preferences/blocked-tags')
    const tags = res.data as TagOption[]
    blockedTagIds.value = tags.map((t) => t.id)
    blockedTagsDetail.value = tags
  } catch (err) {
    console.error('加载屏蔽标签失败', err)
  }
}

const handleVisibleChange = (visible: boolean) => {
  if (!visible && shouldStayOpen.value) {
    nextTick(() => {
      selectVisible.value = true
    })
  }
}

// 加载关注/粉丝数和获赞数
const loadUserStats = () => {
  if (!userStore.user?.id) return
  loadFollowCounts()
  loadTotalLikes()
}

const handleRecentPageChange = (page: number) => {
  recentPage.value = page
  loadRecentViews(page)
}

const loadRecentViews = async (page = 1) => {
  recentLoading.value = true
  try {
    const res = await axios.get('/recent-views', {
      params: { page: page - 1, size: recentPageSize.value },
    })
    recentViews.value = res.data.content
    totalRecent.value = res.data.totalElements
    recentPage.value = page // 确保页码更新
  } catch {
    ElMessage.error('加载最近浏览失败')
  } finally {
    recentLoading.value = false
  }
}

const clearRecentViews = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有浏览记录吗？', '提示', { type: 'warning' })
    clearingRecent.value = true
    await axios.delete('/recent-views')
    recentViews.value = []
    totalRecent.value = 0
    ElMessage.success('已清空')
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('清空失败')
  } finally {
    clearingRecent.value = false
  }
}

// ---------- 生命周期 ----------
watch(activeTab, (newTab) => {
  if (newTab === 'creations' && creations.value.length === 0) {
    loadCreations()
  }
})

onMounted(() => {
  generateAvatar()
  loadFolders()
  loadRecentViews()
  loadPreferences()
  loadBlockedTags()
  loadFollowCounts()
  loadTotalLikes()
  if (activeTab.value === 'creations') {
    loadCreations()
  }
  if (userStore.user?.id) {
    loadUserStats()
  }
})

// 监听用户信息变化（刷新时异步获取完成后触发）
watch(
  () => userStore.user,
  (newUser) => {
    if (newUser?.id) {
      loadUserStats()
    }
  },
  { immediate: true },
)
</script>

<style scoped>
/* 样式与原有相同，无需改动 */
.profile-container {
  min-height: 100vh;
  background-color: #f5f7fa;
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
.profile-card {
  max-width: 1200px;
  margin: 0 auto;
}
.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.folder-grid {
  margin-top: 20px;
}
.folder-card {
  cursor: pointer;
  transition: all 0.3s;
}
.folder-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.folder-card {
  cursor: default;
}
.folder-card-header {
  cursor: pointer;
  padding: 16px;
}
.folder-card-header h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}
.folder-card-header p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
.folder-card-actions {
  display: flex;
  justify-content: space-around;
  border-top: 1px solid #ebeef5;
  padding: 8px 0;
}
.folder-card-actions .el-button {
  padding: 4px 8px;
}
.articles-list {
  margin-top: 20px;
}
.article-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s;
}
.article-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}
.article-content {
  flex: 1;
  cursor: pointer;
}
.article-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.article-summary {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
}
.article-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #909399;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.article-tags {
  display: flex;
  gap: 6px;
}
.article-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 80px;
}
.article-actions .el-button {
  padding: 4px 8px;
}
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.empty-state {
  padding: 60px 0;
}

.creations-list {
  margin-top: 20px;
}
.creation-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s;
}
.creation-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}
.creation-content {
  flex: 1;
  cursor: pointer;
}
.creation-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.creation-summary {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
}
.creation-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #909399;
}
.creation-tags {
  display: flex;
  gap: 6px;
}
.creation-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 80px;
}
.creation-actions .el-button {
  padding: 4px 8px;
}
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.deleted-title,
.deleted-text {
  color: #c0c4cc;
  text-decoration: line-through;
}

.deleted-text {
  color: #c0c4cc;
  text-decoration: line-through;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}
.user-item:hover {
  background-color: #f5f7fa;
}
.user-info {
  flex: 1;
  margin-left: 12px;
}
.username {
  font-weight: 500;
  color: #303133;
}
.role {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.user-info-card {
  background: white;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 30px;
  display: flex;
  align-items: center;
  gap: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.avatar {
  flex-shrink: 0;
}
.info h2 {
  margin: 0 0 12px 0;
}
.meta {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 14px;
  margin-bottom: 12px;
}
.follow-stats {
  display: flex;
  gap: 24px;
}
.follow-stats span {
  cursor: pointer;
  color: #409eff;
  font-weight: 500;
}
.follow-stats span:hover {
  text-decoration: underline;
}

.preferences-container {
  padding: 10px 0;
}
.selected-count {
  margin: 12px 0 8px;
  font-size: 13px;
  color: #909399;
}
.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}
.preference-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.preference-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.tag-classification {
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
}

.added-tag-option {
  background-color: #f0f9eb;
  font-weight: bold;
}

.added-tag-option {
  background-color: #f0f9eb;
  font-weight: bold;
}
.preference-tag {
  cursor: pointer;
}

.blocked-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.blocked-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.tag-classification {
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
}

.pref-section-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #303133;
}
.pref-section {
  margin-bottom: 10px;
}
</style>
