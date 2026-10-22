<template>
  <div class="recommend-container">
    <el-main>
      <!-- 操作按钮区域 -->
      <div class="actions-bar">
        <el-button type="primary" @click="refreshRecommend" :loading="refreshing">
          🔄 刷新推荐
        </el-button>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="推荐文章" name="article">
          <div class="rank-list">
            <div
              v-for="(item, index) in articles"
              :key="item.id"
              class="rank-card"
              @click="goArticle(item.id)"
            >
              <div class="rank-number">{{ index + 1 }}</div>
              <div class="rank-content">
                <h5>{{ item.title }}</h5>
                <div class="meta">
                  <span>作者：{{ item.authorUsername }}</span>
                  <span>浏览：{{ item.viewCount }}</span>
                  <span>得分：{{ formatScore(item.score ?? 0) }}</span>
                </div>
              </div>
            </div>
            <div ref="articleTrigger" class="load-more-trigger"></div>
            <div v-if="articleLoading" class="load-more-text">加载中...</div>
            <div v-if="articleNoMore && articles.length > 0" class="load-more-text">没有更多了</div>
            <el-empty v-if="!articleLoading && articles.length === 0" description="暂无推荐文章" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="推荐设定" name="setting">
          <div class="rank-list">
            <div
              v-for="(item, index) in settings"
              :key="item.id"
              class="rank-card"
              @click="goSetting(item.id)"
            >
              <div class="rank-number">{{ index + 1 }}</div>
              <div class="rank-content">
                <h5>{{ item.title }}</h5>
                <div class="meta">
                  <span>作者：{{ item.authorUsername }}</span>
                  <span>浏览：{{ item.viewCount }}</span>
                  <span>得分：{{ formatScore(item.score ?? 0) }}</span>
                </div>
              </div>
            </div>
            <div ref="settingTrigger" class="load-more-trigger"></div>
            <div v-if="settingLoading" class="load-more-text">加载中...</div>
            <div v-if="settingNoMore && settings.length > 0" class="load-more-text">没有更多了</div>
            <el-empty v-if="!settingLoading && settings.length === 0" description="暂无推荐设定" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

interface ContentCard {
  id: number
  title: string
  authorUsername: string
  viewCount: number
  likeCount?: number
  score?: number
}

interface RawContentItem {
  id: number | string
  title?: string
  author?: string
  authorUsername?: string
  viewCount?: number | string
  likeCount?: number | string
  score?: number | string
  [key: string]: unknown
}

const router = useRouter()
const route = useRoute()
const activeTab = ref<'article' | 'setting'>(
  (route.query.type as string) === 'setting' ? 'setting' : 'article',
)

const articles = ref<ContentCard[]>([])
const settings = ref<ContentCard[]>([])

const articlePage = ref(1)
const settingPage = ref(1)
const pageSize = 8

const articleNoMore = ref(false)
const settingNoMore = ref(false)
const articleLoading = ref(false)
const settingLoading = ref(false)
const refreshing = ref(false)

// 触发器元素
const articleTrigger = ref<HTMLElement | null>(null)
const settingTrigger = ref<HTMLElement | null>(null)
let articleObserver: IntersectionObserver | null = null
let settingObserver: IntersectionObserver | null = null

const formatScore = (score: number) => score.toFixed(2)

const goArticle = (id: number) => router.push(`/articles/${id}`)
const goSetting = (id: number) => router.push(`/settings/${id}`)

// 加载推荐数据（支持刷新）
const loadRecommend = async (type: 'article' | 'setting', page: number, refresh = false) => {
  if (type === 'article') {
    if (articleNoMore.value || articleLoading.value) return
    articleLoading.value = true
  } else {
    if (settingNoMore.value || settingLoading.value) return
    settingLoading.value = true
  }

  try {
    const params: Record<string, string | number | boolean> = {
      page: page - 1,
      size: pageSize,
      type,
    }
    if (refresh) params.refresh = true
    const res = await axios.get('/recommend', { params })
    const raw = res.data as RawContentItem[]
    const items: ContentCard[] = raw.map((item) => ({
      id: Number(item.id),
      title: String(item.title ?? ''),
      authorUsername: String(item.authorUsername || item.author || ''),
      viewCount: Number(item.viewCount ?? 0),
      likeCount: Number(item.likeCount ?? 0),
      score: Number(item.score ?? 0),
    }))

    if (type === 'article') {
      if (refresh || page === 1) {
        articles.value = items
      } else {
        articles.value.push(...items)
      }
      if (items.length < pageSize) articleNoMore.value = true
      articlePage.value = page
    } else {
      if (refresh || page === 1) {
        settings.value = items
      } else {
        settings.value.push(...items)
      }
      if (items.length < pageSize) settingNoMore.value = true
      settingPage.value = page
    }
    // 数据更新后，重新设置观察器
    await nextTick()
    cleanupObservers() // 先清理旧的
    setupObservers() // 再绑定新的
  } catch {
    ElMessage.error('加载推荐失败')
  } finally {
    if (type === 'article') articleLoading.value = false
    else settingLoading.value = false
  }
}

// 刷新推荐
const refreshRecommend = async () => {
  refreshing.value = true
  articles.value = []
  settings.value = []
  articlePage.value = 1
  settingPage.value = 1
  articleNoMore.value = false
  settingNoMore.value = false
  // 强制刷新当前激活 tab 的数据
  await loadRecommend(activeTab.value as 'article' | 'setting', 1, true)
  refreshing.value = false
}

// 无限滚动
const setupObservers = () => {
  if (articleTrigger.value) {
    articleObserver = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !articleLoading.value) {
          loadRecommend('article', articlePage.value + 1)
        }
      },
      { threshold: 0.1 },
    )
    articleObserver.observe(articleTrigger.value)
  }
  if (settingTrigger.value) {
    settingObserver = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !settingLoading.value) {
          loadRecommend('setting', settingPage.value + 1)
        }
      },
      { threshold: 0.1 },
    )
    settingObserver.observe(settingTrigger.value)
  }
}

const cleanupObservers = () => {
  articleObserver?.disconnect()
  settingObserver?.disconnect()
}

// Tab 切换
const onTabChange = (tab: string) => {
  if (tab === 'article' && articles.value.length === 0) loadRecommend('article', 1)
  else if (tab === 'setting' && settings.value.length === 0) loadRecommend('setting', 1)
  nextTick(() => {
    cleanupObservers()
    setupObservers()
  })
}

watch(
  () => route.query.type,
  (type) => {
    if (type === 'article' || type === 'setting') {
      activeTab.value = type as 'article' | 'setting'
    }
  },
)

onMounted(() => {
  loadRecommend(activeTab.value, 1) // 首次加载数据，内部会设置观察器
})

onUnmounted(() => {
  cleanupObservers()
})
</script>

<style scoped>
.recommend-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
.actions-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.rank-list {
  margin-top: 10px;
}
.rank-card {
  display: flex;
  align-items: center;
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: 0.2s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}
.rank-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.rank-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  width: 60px;
  text-align: center;
}
.rank-content {
  flex: 1;
}
.rank-content h5 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}
.meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
  flex-wrap: wrap;
}
.load-more-trigger {
  height: 20px;
  margin: 10px 0;
}
.load-more-text {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}
</style>
