<template>
  <div class="app">
    <header class="topbar" v-if="!isWide">
      <div class="brand">🧭 智能旅游助手</div>
      <nav class="nav">
        <router-link to="/home">行程规划</router-link>
        <router-link to="/chat">AI 对话</router-link>
      </nav>
      <div class="user">
        <span>{{ username }}</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </header>
    <main class="main" :class="{ wide: isWide }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const username = ref(localStorage.getItem('username') || '游客')

// 这些页面自带顶部栏和全宽布局，隐藏全局顶栏、放开宽度
const wideRoutes = ['/home', '/hotel', '/hotel-detail', '/flight', '/flight-result', '/train', '/train-result', '/travel', '/spot-detail', '/city', '/chat', '/plan', '/orders', '/detail', '/pay', '/login', '/register', '/notices', '/my-trips']
const isWide = computed(() => wideRoutes.includes(route.path))

// localStorage 不是响应式的，登录/切换路由后手动同步一次
watch(
  () => route.fullPath,
  () => {
    username.value = localStorage.getItem('username') || '游客'
  },
)

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style>
body,
html {
  margin: 0;
  padding: 0;
  background: #f5f7fa;
}
.app {
  min-height: 100vh;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: #1890ff;
  color: #fff;
}
.brand {
  font-size: 18px;
  font-weight: 600;
}
.nav a {
  color: #fff;
  margin-right: 20px;
  text-decoration: none;
  opacity: 0.9;
}
.nav a.router-link-active {
  opacity: 1;
  font-weight: 700;
  border-bottom: 2px solid #fff;
  padding-bottom: 4px;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
}
.main {
  max-width: 960px;
  margin: 24px auto;
  padding: 0 16px;
}
.main.wide {
  max-width: none;
  margin: 0;
  padding: 0;
}
</style>
