<template>
  <AppHeader v-if="!route.meta.hideHeader" />
  <div :class="['main-content', { 'no-header': route.meta.hideHeader }]">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from './stores/user'
import { useRoute } from 'vue-router'
import AppHeader from '@/views/components/AppHeader.vue'

const route = useRoute()
const userStore = useUserStore()

onMounted(() => {
  userStore.init()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family:
    'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑',
    Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .login-card,
  .register-card {
    width: 90% !important;
  }

  .api-test-section {
    padding: 0 16px;
  }
}

.main-content {
  padding-top: 60px;
}

.main-content {
  padding-top: 60px;
}
.main-content.no-header {
  padding-top: 0;
}
</style>
