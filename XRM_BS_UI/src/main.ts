import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import 'highlight.js/styles/github.css'
import '@wangeditor/editor/dist/css/style.css'

import '@/styles/editor-content.css'

import App from './App.vue'
import router from './router'

import 'highlight.js/styles/github.css'

const app = createApp(App)
const pinia = createPinia()

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 添加全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue 错误:', err)
  console.error('组件实例:', instance)
  console.error('错误信息:', info)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 等待路由准备就绪
router
  .isReady()
  .then(() => {
    app.mount('#app')
    // console.log('应用已挂载')
  })
  .catch((err) => {
    console.error('路由初始化失败:', err)
  })
