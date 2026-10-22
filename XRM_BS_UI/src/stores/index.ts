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
      path: '/:catchAll(.*)',
      redirect: '/',
    },
  ],
})

router.beforeEach((to, from, next) => {
  console.log(`路由守卫: ${from.path} -> ${to.path}`)

  // 对于写文章页面，只检查是否有 token
  if (to.path === '/articles/create' || to.name === 'CreateArticle') {
    const token = localStorage.getItem('token')
    if (!token) {
      console.log('未登录，重定向到登录页')
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  // 其他页面直接放行
  next()
})

export default router
