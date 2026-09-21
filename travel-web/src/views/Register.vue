<template>
  <div class="login-page">
    <!-- 顶部：只有 logo + 右上角首页（和登录页保持一致） -->
    <header class="lp-header">
      <div class="brand">🧭 智能旅游助手</div>
      <div class="home" @click="goHome">
        <span class="home-ico">🏠</span> 首页
      </div>
    </header>

    <!-- 主体：中间一张卡 -->
    <div class="lp-body">
      <div class="login-card">
        <div class="card-title">注册账号</div>

        <div class="row">
          <label>用户名</label>
          <div class="ipt">
            <el-input v-model="form.username" placeholder="请输入用户名" @keyup.enter="handleRegister" />
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
              @keyup.enter="handleRegister"
            />
          </div>
        </div>

        <div class="row">
          <label>确认密码</label>
          <div class="ipt">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入密码"
              @keyup.enter="handleRegister"
            />
          </div>
        </div>

        <el-button class="login-btn" type="primary" :loading="loading" @click="handleRegister">注册</el-button>

        <div class="to-register" @click="router.push('/login')">已有账号？返回登录</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '' })

const goHome = () => router.push('/home')

const handleRegister = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/auth/register', {
      username: form.username,
      password: form.password,
      email: `${form.username}@example.com`,
    })
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.replace('/login')
    } else {
      ElMessage.error(res.msg || '注册失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* register v2：标签定宽，三行输入框严格对齐 */
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eaf4ff 0%, #dbeafe 100%);
  display: flex;
  flex-direction: column;
}

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

/* 三行：标签定宽 64px + 输入框撑满，保证上下严格对齐 */
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.row label {
  width: 64px;
  flex: 0 0 64px;
  text-align: right;
  font-size: 14px;
  color: #666;
  white-space: nowrap;
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
