<template>
  <div class="login-page">
    <!-- 顶部：只有 logo + 右上角首页 -->
    <header class="lp-header">
      <div class="brand">🧭 智能旅游助手</div>
      <div class="home" @click="goHome">
        <span class="home-ico">🏠</span> 首页
      </div>
    </header>

    <!-- 主体：中间一张卡 -->
    <div class="lp-body">
      <div class="login-card">
        <div class="card-title">账号密码登录</div>

        <div class="row">
          <label>用户名</label>
          <div class="ipt">
            <el-input v-model="form.username" placeholder="请输入用户名" @keyup.enter="handleLogin" />
          </div>
        </div>

        <div class="row">
          <label>密码</label>
          <div class="ipt">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            />
          </div>
        </div>

        <el-button class="login-btn" type="primary" :loading="loading" @click="handleLogin">登录</el-button>

        <div class="to-register" @click="router.push('/register')">没有账号？去注册</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const goHome = () => router.push('/home')

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/auth/login', {
      username: form.username,
      password: form.password,
    })
    if (res.code === 200) {
      localStorage.setItem('token', res.data)
      localStorage.setItem('username', form.username)
      ElMessage.success('登录成功')
      router.replace(route.query.redirect || '/home')
    } else {
      ElMessage.error(res.msg || '用户名或密码错误')
    }
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* refresh v3 */
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eaf4ff 0%, #dbeafe 100%);
  display: flex;
  flex-direction: column;
}

/* 顶部：logo + 首页 */
.lp-header {
  height: 72px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.05);
}
.brand {
  font-size: 22px;
  font-weight: 700;
  color: #0086f6;
}
.home {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #333;
  cursor: pointer;
}
.home:hover {
  color: #0086f6;
}
.home-ico {
  font-size: 17px;
}

/* 主体 */
.lp-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px 80px;
}
.login-card {
  width: 420px;
  background: #fff;
  border-radius: 10px;
  padding: 32px 36px 26px;
  box-shadow: 0 8px 28px rgba(0, 120, 220, 0.12);
  box-sizing: border-box;
}
.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #222;
  margin-bottom: 26px;
}

/* 用户名 / 密码 一行：标签定宽 + 输入框撑满，保证上下严格对齐 */
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.row label {
  width: 56px;
  flex: 0 0 56px;
  text-align: right;
  font-size: 14px;
  color: #666;
}
.row .ipt {
  flex: 1;
  min-width: 0;
}
.row .ipt :deep(.el-input) {
  width: 100%;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 16px;
  margin-top: 6px;
}
.to-register {
  margin-top: 16px;
  text-align: center;
  color: #0086f6;
  font-size: 14px;
  cursor: pointer;
}
.to-register:hover {
  text-decoration: underline;
}
</style>
