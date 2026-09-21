<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🔔 消息中心</div>
      <div class="links">
        <span @click="readAll">全部标为已读</span>
        <span @click="go('/home')">返回首页</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/notices" />
      <main class="content">
        <div class="card">
          <div class="head">
            <span class="title">我的消息</span>
            <span class="sub">共 {{ list.length }} 条，未读 {{ unread }} 条</span>
          </div>

          <div v-if="list.length" class="list" v-loading="loading">
            <div
              v-for="n in list"
              :key="n.id"
              class="item"
              :class="{ unread: !n.readFlag }"
              @click="open(n)"
            >
              <span class="dot" :class="n.type"></span>
              <div class="body">
                <div class="t">
                  <b>{{ n.title }}</b>
                  <span class="time">{{ fmt(n.createTime) }}</span>
                </div>
                <div class="c">{{ n.content }}</div>
              </div>
              <el-tag size="small" :type="tagType(n.type)" effect="light">{{ typeText(n.type) }}</el-tag>
            </div>
          </div>
          <el-empty v-else-if="!loading" description="还没有消息，下单、支付、审批结果都会通知到这里" />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const unread = computed(() => list.value.filter((n) => !n.readFlag).length)

const go = (p) => router.push(p)
const fmt = (t) => (t ? String(t).replace('T', ' ').slice(5, 16) : '')
const typeText = (t) =>
  t === 'PAY' ? '支付' : t === 'REFUND' ? '退款' : t === 'APPROVE' ? '审批' : t === 'ORDER' ? '订单' : '系统'
const tagType = (t) =>
  t === 'PAY' ? 'success' : t === 'REFUND' ? 'warning' : t === 'APPROVE' ? 'primary' : t === 'ORDER' ? 'info' : ''

const load = async () => {
  loading.value = true
  try {
    const resp = await fetch('/api/notice/my', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    if (res.code === 200) list.value = res.data || []
  } catch (e) {
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const open = async (n) => {
  if (n.readFlag) return
  n.readFlag = true
  try {
    await fetch('/api/notice/read/' + n.id, {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
  } catch (e) {
    /* 已读失败不提示，下次进来还能再点 */
  }
}

const readAll = async () => {
  try {
    const resp = await fetch('/api/notice/read-all', {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    if (res.code === 200) {
      list.value.forEach((n) => (n.readFlag = true))
      ElMessage.success('已全部标为已读')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links { display: flex; gap: 18px; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.links span:hover { color: #0086f6; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 12px; }
.title { font-size: 17px; font-weight: 700; color: #222; }
.sub { font-size: 12px; color: #999; }
.item { display: flex; gap: 12px; align-items: flex-start; padding: 12px; border-bottom: 1px solid #f2f4f7; cursor: pointer; }
.item:hover { background: #f8fbff; }
.item.unread { background: #f5faff; }
.dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 7px; flex-shrink: 0; background: #cfd6e0; }
.dot.PAY { background: #0f9d58; }
.dot.REFUND { background: #ff8a1f; }
.dot.APPROVE { background: #0086f6; }
.dot.ORDER { background: #7a5cff; }
.body { flex: 1; min-width: 0; }
.t { display: flex; gap: 10px; align-items: baseline; }
.t b { font-size: 14px; color: #222; }
.time { font-size: 12px; color: #aaa; }
.c { font-size: 13px; color: #666; margin-top: 4px; line-height: 1.7; word-break: break-all; }
</style>
