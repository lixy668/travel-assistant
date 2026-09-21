<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🗺️ {{ city }} 行程计划</div>
      <div class="links">
        <span @click="savePlan">保存行程</span>
        <span @click="go('/plan')">重新规划</span>
        <span @click="router.back()">返回</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/plan" />

      <main class="content">
        <div v-loading="loading">
          <!-- 概览 -->
          <div class="card overview">
            <div class="ov-item"><span class="ov-label">目的地</span><span class="ov-value">📍 {{ city }}</span></div>
            <div class="ov-item"><span class="ov-label">天数</span><span class="ov-value">📅 {{ days }} 天</span></div>
            <div class="ov-item"><span class="ov-label">预算</span><span class="ov-value">💰 {{ budget }} 元</span></div>
            <div class="ov-item" v-if="data"><span class="ov-label">状态</span>
              <span class="ov-value" :class="data.degraded ? 'warn-text' : 'ok-text'">
                {{ data.degraded ? '离线降级内容' : 'AI 生成' }}
              </span>
            </div>
          </div>

          <el-alert
            v-if="data && data.degraded"
            type="warning"
            :closable="false"
            title="AI 服务暂时不可用，以下为离线行程，稍后可刷新获取更详细规划"
            style="margin-bottom:14px"
          />

          <!-- 每日行程 -->
          <div class="card" v-if="data">
            <div class="sec-title">每日行程</div>
            <el-empty v-if="data.error" :description="data.error" />
            <template v-else>
              <div class="day" v-for="d in data.dailyItinerary || []" :key="d.day">
                <div class="day-head">
                  <span class="day-badge">第 {{ d.day }} 天</span>
                  <span class="day-date">{{ d.date || '' }}</span>
                </div>
                <div class="slots">
                  <div class="slot" v-if="d.morning">
                    <div class="slot-time morning">上午</div>
                    <div class="slot-body">
                      <div class="slot-head">
                        <img v-if="d.morning.image" class="slot-img" :src="d.morning.image" alt="" />
                        <div class="slot-title">{{ d.morning.spot }}</div>
                      </div>
                      <div class="slot-desc">{{ d.morning.description }}</div>
                      <div class="slot-tags">
                        <span class="tag">🎫 {{ d.morning.ticket || '免费' }}</span>
                        <span class="tag green">🚌 {{ d.morning.transportation || '步行/公交' }}</span>
                        <span class="tag gray" v-if="d.morning.duration">⏱ {{ d.morning.duration }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="slot" v-if="d.afternoon">
                    <div class="slot-time afternoon">下午</div>
                    <div class="slot-body">
                      <div class="slot-head">
                        <img v-if="d.afternoon.image" class="slot-img" :src="d.afternoon.image" alt="" />
                        <div class="slot-title">{{ d.afternoon.spot }}</div>
                      </div>
                      <div class="slot-desc">{{ d.afternoon.description }}</div>
                      <div class="slot-tags">
                        <span class="tag">🎫 {{ d.afternoon.ticket || '免费' }}</span>
                        <span class="tag green">🚌 {{ d.afternoon.transportation || '步行/公交' }}</span>
                        <span class="tag gray" v-if="d.afternoon.duration">⏱ {{ d.afternoon.duration }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="slot" v-if="d.evening">
                    <div class="slot-time evening">晚上</div>
                    <div class="slot-body">
                      <div class="slot-head">
                        <img v-if="d.evening.image" class="slot-img" :src="d.evening.image" alt="" />
                        <div class="slot-title">{{ d.evening.spot }}</div>
                      </div>
                      <div class="slot-desc">{{ d.evening.description }}</div>
                      <div class="slot-tags">
                        <span class="tag">🎫 {{ d.evening.ticket || '免费' }}</span>
                        <span class="tag green">🚌 {{ d.evening.transportation || '步行/公交' }}</span>
                        <span class="tag gray" v-if="d.evening.duration">⏱ {{ d.evening.duration }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- 预算明细 -->
          <div class="card" v-if="data && data.budgetBreakdown">
            <div class="sec-title">预算明细</div>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="🏨 住宿">{{ data.budgetBreakdown.accommodation }} 元</el-descriptions-item>
              <el-descriptions-item label="🍜 餐饮">{{ data.budgetBreakdown.food }} 元</el-descriptions-item>
              <el-descriptions-item label="🚗 交通">{{ data.budgetBreakdown.transportation }} 元</el-descriptions-item>
              <el-descriptions-item label="🎫 门票">{{ data.budgetBreakdown.tickets }} 元</el-descriptions-item>
              <el-descriptions-item label="📦 其他">{{ data.budgetBreakdown.other }} 元</el-descriptions-item>
              <el-descriptions-item label="💰 总预算">
                <b style="color:#ff6a00">{{ data.totalBudget }} 元</b>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 推荐酒店（高德真实数据） -->
          <div class="card">
            <div class="sec-head">
              <span class="sec-title">推荐酒店</span>
              <span class="sec-sub">高德地图真实数据</span>
            </div>
            <div v-loading="hotelLoading" style="min-height:110px">
              <div v-if="hotels.length" class="hotel-grid">
                <div class="hotel-card" v-for="(h, i) in hotels" :key="i">
                  <img v-if="h.image" class="h-img" :src="h.image" alt="" />
                  <div v-else class="h-img" :style="{ background: grad(i) }">🏨</div>
                  <div class="h-body">
                    <div class="h-name">{{ h.name }}</div>
                    <div class="h-meta">
                      <span v-if="h.rating" class="score">{{ h.rating }}</span>
                      <span class="great">超棒</span>
                      <span v-if="h.distance" class="dist">🚶 {{ h.distance }}</span>
                    </div>
                    <div class="h-addr">📍 {{ h.address || h.city || '地址未知' }}</div>
                    <div class="h-price" v-if="h.cost">¥{{ h.cost }}<span>起</span></div>
                  </div>
                </div>
              </div>
              <el-empty v-else-if="!hotelLoading" description="暂无酒店数据（需配置高德 Key）" />
            </div>
          </div>

          <!-- 推荐车票（示例） -->
          <div class="card">
            <div class="sec-head">
              <span class="sec-title">推荐车票</span>
              <span class="sec-sub">示例数据</span>
            </div>
            <div class="ticket-list">
              <div class="ticket" v-for="(t, i) in tickets" :key="i">
                <span class="t-no">{{ t.no }}</span>
                <span class="t-route">{{ fromCity }} → {{ city }}</span>
                <span class="t-time">{{ t.dep }} - {{ t.arrive }}</span>
                <span class="t-seat">{{ t.seat }} ¥{{ t.price }}</span>
              </div>
            </div>
          </div>

          <!-- 出行提示 -->
          <div class="card" v-if="data && ((data.tips && data.tips.length) || (data.warnings && data.warnings.length))">
            <div class="sec-title">出行提示</div>
            <ul class="tips">
              <li v-for="(t, i) in (data.tips || [])" :key="'t' + i">💡 {{ t }}</li>
              <li v-for="(w, i) in (data.warnings || [])" :key="'w' + i">⚠️ {{ w }}</li>
            </ul>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const route = useRoute()
const router = useRouter()
const city = route.query.city
const budget = route.query.budget
const days = route.query.days

const data = ref(null)
const loading = ref(true)
const hotels = ref([])
const hotelLoading = ref(false)
const fromCity = ref(city === '上海' ? '北京' : '上海')

const colors = ['#4a90d9,#2c6fb5', '#e8875a,#d3643a', '#5aa87a,#3d8a5f', '#7a6fd9,#5b4fc0', '#d95a8a,#b53a6b', '#4aa8b8,#2c8a99']
const grad = (i) => 'linear-gradient(135deg,' + colors[i % colors.length] + ')'

const go = (p) => router.push(p)

// 把 AI 生成的行程存进「我的行程」
const saving = ref(false)
const savePlan = async () => {
  if (!data.value) {
    ElMessage.warning('行程还在生成中，稍等一下')
    return
  }
  saving.value = true
  try {
    const resp = await fetch('/api/plan/save', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({
        city,
        days: Number(days),
        budget: Number(budget),
        resultJson: JSON.stringify(data.value),
      }),
    })
    const res = await resp.json()
    if (res.code === 200) ElMessage.success('行程已保存，可在「我的行程」里查看')
    else ElMessage.error(res.msg || '保存失败')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 示例车票
const tickets = [
  { no: 'G102', dep: '07:00', arrive: '09:28', seat: '二等座', price: 553 },
  { no: 'G214', dep: '09:12', arrive: '11:50', seat: '二等座', price: 553 },
  { no: 'D311', dep: '13:30', arrive: '17:20', seat: '二等座', price: 396 },
  { no: 'G520', dep: '18:05', arrive: '20:35', seat: '二等座', price: 553 },
]

const loadHotels = async () => {
  if (!city) return
  hotelLoading.value = true
  try {
    const params = new URLSearchParams()
    params.append('city', city)
    params.append('keyword', '酒店')
    params.append('machineId', localStorage.getItem('machineId') || '')
    const resp = await fetch('/api/travel/hotels?' + params.toString(), {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    if (res.code === 200) hotels.value = (res.data || []).slice(0, 3)
  } catch (e) {
    console.error(e)
  } finally {
    hotelLoading.value = false
  }
}

onMounted(async () => {
  if (!city) {
    ElMessage.warning('缺少目的地参数')
    router.back()
    return
  }
  try {
    const resp = await fetch('/api/travel/recommend', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ city, budget: Number(budget), days: Number(days) }),
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    data.value = res.data
    fillSlotImages()
  } catch (e) {
    ElMessage.error('获取行程失败')
  } finally {
    loading.value = false
  }
  // 行程出来后，再拉推荐酒店
  loadHotels()
})

// 给每日行程景点配图（按景点名查高德，接口带缓存）
const fillSlotImages = () => {
  const days = data.value && data.value.dailyItinerary
  if (!days) return
  const token = localStorage.getItem('token')
  days.forEach((d) => {
    ;['morning', 'afternoon', 'evening'].forEach((k) => {
      const s = d[k]
      if (s && s.spot && !s.image) {
        fetch('/api/travel/image?city=' + encodeURIComponent(city) + '&keyword=' + encodeURIComponent(s.spot), {
          headers: { Authorization: `Bearer ${token}` },
        })
          .then((r) => r.json())
          .then((res) => {
            if (res.code === 200 && res.data) s.image = res.data
          })
          .catch(() => {})
      }
    })
  })
}
</script>

<style scoped>
/* refresh v2 */
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links { display: flex; gap: 16px; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.links span:hover { color: #0086f6; }

.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.sec-title { font-size: 17px; font-weight: 700; color: #222; }
.sec-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; }
.sec-sub { font-size: 12px; color: #999; }

/* 概览 */
.overview { display: flex; gap: 40px; flex-wrap: wrap; }
.ov-item { display: flex; flex-direction: column; gap: 4px; }
.ov-label { font-size: 12px; color: #999; }
.ov-value { font-size: 16px; font-weight: 600; color: #222; }
.ok-text { color: #52c41a; }
.warn-text { color: #e6a23c; }

/* 每日行程 */
.day { border: 1px solid #eef0f3; border-radius: 10px; padding: 14px; margin-bottom: 14px; }
.day:last-child { margin-bottom: 0; }
.day-head { display: flex; align-items: center; gap: 10px; padding-bottom: 10px; border-bottom: 1px dashed #e8eaee; margin-bottom: 12px; }
.day-badge { background: #0086f6; color: #fff; border-radius: 14px; padding: 3px 12px; font-size: 13px; font-weight: 600; }
.day-date { font-size: 13px; color: #999; }
.slots { display: flex; flex-direction: column; gap: 12px; }
.slot { display: flex; gap: 12px; }
.slot-time { width: 52px; flex-shrink: 0; text-align: center; font-size: 13px; font-weight: 600; color: #fff; border-radius: 6px; padding: 4px 0; height: 26px; }
.slot-time.morning { background: #e6a23c; }
.slot-time.afternoon { background: #f56c6c; }
.slot-time.evening { background: #409eff; }
  .slot-body { flex: 1; min-width: 0; }
  .slot-head { display: flex; align-items: center; gap: 10px; }
  .slot-img { width: 64px; height: 48px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
  img.h-img { width: 100%; height: 110px; object-fit: cover; display: block; }
.slot-title { font-size: 15px; font-weight: 600; color: #222; }
.slot-desc { font-size: 13px; color: #666; line-height: 1.6; margin: 4px 0 6px; }
.slot-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { font-size: 12px; color: #409eff; background: #ecf5ff; border-radius: 4px; padding: 2px 8px; }
.tag.green { color: #52c41a; background: #f0f9eb; }
.tag.gray { color: #888; background: #f4f4f5; }

/* 酒店卡片 */
.hotel-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 14px; }
.hotel-card { border: 1px solid #eef0f3; border-radius: 8px; overflow: hidden; }
.h-img { height: 110px; display: flex; align-items: center; justify-content: center; font-size: 32px; color: #fff; }
.h-body { padding: 10px 12px 12px; }
.h-name { font-size: 15px; font-weight: 600; color: #222; }
.h-meta { display: flex; align-items: center; gap: 8px; margin: 6px 0; font-size: 12px; }
.score { background: #0086f6; color: #fff; border-radius: 4px; padding: 1px 6px; font-weight: 600; }
.great { color: #0086f6; font-weight: 600; }
.dist { color: #999; }
.h-addr { font-size: 12px; color: #999; line-height: 1.4; min-height: 32px; }
.h-price { margin-top: 6px; color: #ff6a00; font-size: 18px; font-weight: 700; text-align: right; }
.h-price span { font-size: 12px; font-weight: 400; color: #999; }

/* 车票 */
.ticket-list { display: flex; flex-direction: column; }
.ticket { display: flex; align-items: center; gap: 20px; padding: 12px 6px; border-bottom: 1px solid #f2f2f2; font-size: 14px; }
.ticket:last-child { border-bottom: 0; }
.t-no { width: 70px; font-weight: 700; color: #0086f6; }
.t-route { flex: 1; color: #333; }
.t-time { color: #666; }
.t-seat { width: 130px; text-align: right; color: #ff6a00; font-weight: 600; }

/* 提示 */
.tips { margin: 0; padding-left: 18px; color: #666; font-size: 13px; line-height: 1.9; }
</style>
