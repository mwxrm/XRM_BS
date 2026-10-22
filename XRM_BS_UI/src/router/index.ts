import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { hideHeader: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { hideHeader: true },
    },
    {
      path: '/articles/create',
      name: 'CreateArticle',
      component: () => import('@/views/CreateArticleView.vue'),
      meta: { requiresAuth: true }, // 需要认证
    },
    {
      path: '/articles/:id',
      name: 'ArticleDetail',
      component: () => import('@/views/ArticleDetailView.vue'),
      meta: { requiresAuth: false }, // 详情页可以公开访问
    },
    {
      path: '/articles/:id/edit',
      name: 'EditArticle',
      component: () => import('@/views/CreateArticleView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/my/articles',
      name: 'MyArticles',
      component: () => import('@/views/MyArticlesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/:catchAll(.*)',
      redirect: '/',
    },
    {
      path: '/settings/create',
      name: 'CreateSetting',
      component: () => import('@/views/CreateSettingView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings/:id',
      name: 'SettingDetail',
      component: () => import('@/views/SettingDetailView.vue'),
    },
    {
      path: '/settings/:id/edit',
      name: 'EditSetting',
      component: () => import('@/views/CreateSettingView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/tags/create',
      name: 'CreateTag',
      component: () => import('@/views/CreateTagView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/tags/:id',
      name: 'TagDetail',
      component: () => import('@/views/TagDetailView.vue'),
    },
    {
      path: '/tags/:id/edit',
      name: 'TagEdit',
      component: () => import('@/views/TagEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/search',
      name: 'Search',
      component: () => import('@/views/SearchView.vue'),
    },
    {
      path: '/users/:id',
      name: 'UserProfile',
      component: () => import('@/views/UserProfileView.vue'),
    },
    {
      path: '/notifications',
      name: 'Notifications',
      component: () => import('@/views/NotificationsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/notifications/group/:groupKey',
      name: 'NotificationGroupDetail',
      component: () => import('@/views/NotificationGroupDetail.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/hot',
      name: 'HotRank',
      component: () => import('@/views/HotRankView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/recommend',
      name: 'Recommend',
      component: () => import('@/views/RecommendView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'AdminDashboard',
      component: () => import('@/views/AdminDashboard.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/carousel',
      name: 'CarouselManager',
      component: () => import('@/views/CarouselManager.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/recommend',
      name: 'Recommend',
      component: () => import('@/views/RecommendView.vue'),
      meta: { requiresAuth: true },
    },
  ],
  // 添加滚动行为控制
  scrollBehavior(to, from, savedPosition) {
    // 如果浏览器前进/后退时有保存的位置，优先恢复
    if (savedPosition) {
      return savedPosition
    }
    // 默认所有路由都滚动到顶部
    return { top: 0 }
  },
})


// 路由守卫 - 保护需要认证的路由
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const publicPages = ['/login', '/register', '/']
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  console.log('路由守卫:', to.path, '用户角色:', user?.role)
  if (!publicPages.includes(to.path) && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  // 管理员路由检查
  if (to.meta.requiresAdmin) {
    if (user?.role !== 'admin') {
      console.log('非管理员，重定向到首页')
      next('/')
      return
    }
  }
  next()
})
export default router
