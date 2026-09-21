<template>
  <aside class="side">
    <div
      v-for="m in menus"
      :key="m.path"
      class="side-item"
      :class="{ active: m.path === active }"
      @click="go(m.path)"
    >
      <span class="ico">{{ m.icon }}</span>
      <span class="txt">{{ m.label }}</span>
      <span v-if="m.tag" class="tag">{{ m.tag }}</span>
      <span v-if="m.path === '/notices' && unread" class="tag">{{ unread }}</span>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

defineProps({ active: { type: String, default: '' } })
const router = useRouter()
const unread = ref(0)

const menus = [
  { icon: '🏠', label: '首页', path: '/home' },
  { icon: '🏨', label: '酒店', path: '/hotel' },
  { icon: '✈️', label: '机票', path: '/flight' },
  { icon: '🚄', label: '火车票', path: '/train' },
  { icon: '🎒', label: '旅游', path: '/travel' },
  { icon: '🗺️', label: '智能行程规划', path: '/plan' },
  { icon: '📁', label: '我的行程', path: '/my-trips' },
  { icon: '🤖', label: 'AI 行程助手', path: '/chat' },
  { icon: '📋', label: '我的订单', path: '/orders' },
  { icon: '🔔', label: '消息中心', path: '/notices' },
]

const go = (p) => router.push(p)

// 未读消息数（消息中心右边的红点）
onMounted(async () => {
  try {
    const resp = await fetch('/api/notice/unread', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    if (resp.status === 401) return
    const res = await resp.json()
    if (res.code === 200) unread.value = Number(res.data && res.data.unread) || 0
  } catch (e) {
    /* 拿不到未读数就不显示红点 */
  }
})
</script>

<style scoped>
.side {
  flex: 0 0 168px;
  width: 168px;
  box-sizing: border-box;
  background: #fff;
  border-radius: 8px;
  padding: 6px 0;
}
.side-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 14px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}
.side-item:hover {
  background: #eaf5ff;
  color: #0086f6;
}
.side-item.active {
  background: #0086f6;
  color: #fff;
}
.side-item .txt {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-item .tag {
  margin-left: auto;
  background: #ff8a1f;
  color: #fff;
  font-size: 10px;
  border-radius: 3px;
  padding: 1px 4px;
}
</style>
