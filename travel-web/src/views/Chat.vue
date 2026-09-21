<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🤖 AI 行程助手</div>
      <div class="links">
        <span @click="go('/home')">返回首页</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/chat" />

      <main class="content">
        <div class="chat-card">
          <div class="chat-head">
            <span>AI 对话</span>
            <span class="chat-sub">可以咨询景点、美食、交通、行程安排等</span>
          </div>

          <div ref="box" class="chat-box">
            <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
              <div v-if="m.role === 'ai'" class="avatar">🤖</div>
              <div class="bubble">{{ m.content }}</div>
            </div>
          </div>

          <div class="input-row">
            <el-input
              v-model="input"
              placeholder="输入你想咨询的问题（回车发送）"
              @keyup.enter="handleSend"
            />
            <el-button type="primary" @click="handleSend">发送</el-button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStream } from '../api'
import SideNav from '../components/SideNav.vue'

const router = useRouter()

onMounted(() => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
  }
})

const go = (p) => router.push(p)
const input = ref('')
const box = ref(null)
const messages = ref([
  { role: 'ai', content: '你好！我是智能旅游助手，有什么可以帮你的吗？\n我可以帮你推荐景点、规划行程、介绍美食等。' },
])

const scroll = () =>
  nextTick(() => {
    if (box.value) box.value.scrollTop = box.value.scrollHeight
  })

const handleSend = async () => {
  const text = input.value.trim()
  if (!text) {
    ElMessage.warning('请输入内容')
    return
  }
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  messages.value.push({ role: 'ai', content: '' })
  const idx = messages.value.length - 1
  scroll()

  await fetchStream(
    'chat',
    { message: text },
    (chunk) => {
      messages.value[idx].content += chunk.content || ''
      scroll()
    },
    () => console.log('done'),
    (e) => {
      messages.value[idx].content = '【生成失败，请重试】'
      console.error(e)
    },
  )
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f2f4f7;
}
.topbar {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 20;
}
.brand {
  font-size: 20px;
  font-weight: 700;
  color: #0086f6;
}
.links span {
  color: #333;
  font-size: 14px;
  cursor: pointer;
}
.links span:hover {
  color: #0086f6;
}

.wrap {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr);
  gap: 16px;
  padding: 16px 24px 24px;
  align-items: start;
  box-sizing: border-box;
}
.content {
  min-width: 0;
}

.chat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 140px);
}
.chat-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
  margin-bottom: 12px;
}
.chat-head > span:first-child {
  font-size: 17px;
  font-weight: 700;
  color: #222;
}
.chat-sub {
  font-size: 12px;
  color: #999;
}

.chat-box {
  flex: 1;
  min-height: 320px;
  overflow-y: auto;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  padding: 16px;
  background: #fafbfc;
}
.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.msg.user {
  justify-content: flex-end;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #eaf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e8eaee;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.user .bubble {
  background: #1890ff;
  border-color: #1890ff;
  color: #fff;
}

.input-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}
</style>
