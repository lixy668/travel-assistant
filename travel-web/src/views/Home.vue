<template>
  <div class="page">
    <!-- 顶部 -->
    <header class="topbar">
      <div class="brand">🧭 智能旅游助手</div>
      <div class="search">
        <input v-model="kw" placeholder="搜索任何旅游相关" @keyup.enter="topSearch" />
        <button @click="topSearch">🔍</button>
      </div>
      <div class="links">
        <span @click="go('/orders')">我的订单 ▾</span>
        <span class="sep"></span>
        <span class="user">👤 {{ username }}</span>
        <span @click="logout">退出</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/home" />

      <main class="content">
        <!-- 预订酒店 -->
        <div class="hero">
          <div class="hero-title">预订酒店</div>
          <div class="hero-form">
            <div class="field big">
              <label>目的地 / 酒店名称</label>
              <input v-model="hotel.city" placeholder="如：上海 / 外滩 / 希尔顿" />
            </div>
            <div class="field">
              <label>入住</label>
              <input v-model="hotel.checkIn" type="date" />
            </div>
            <div class="field">
              <label>退房</label>
              <input v-model="hotel.checkOut" type="date" />
            </div>
          </div>
          <div class="hero-form second">
            <div class="field">
              <label>房间及住客</label>
              <el-select v-model="hotel.guests" size="small" style="width:100%">
                <el-option label="1间, 1位" value="1间,1位" />
                <el-option label="1间, 2位" value="1间,2位" />
                <el-option label="2间, 2位" value="2间,2位" />
                <el-option label="2间, 4位" value="2间,4位" />
                <el-option label="3间, 6位" value="3间,6位" />
              </el-select>
            </div>
            <div class="field">
              <label>酒店级别</label>
              <el-select v-model="hotel.level" size="small" style="width:100%">
                <el-option label="不限" value="不限" />
                <el-option label="经济型" value="经济型" />
                <el-option label="三星级/舒适型" value="三星级" />
                <el-option label="四星级/高档型" value="四星级" />
                <el-option label="五星级/豪华型" value="五星级" />
              </el-select>
            </div>
            <div class="field big">
              <label>关键词（选填）</label>
              <input v-model="hotel.keyword" placeholder="机场 / 火车站 / 酒店名称…" />
            </div>
            <button class="go" @click="searchHotel">🔍 搜索</button>
          </div>
        </div>

        <!-- 公告 banner -->
        <div class="banner">🤖 AI 智能规划 · 一键生成专属行程 · 支持预算分配与每日安排</div>

        <!-- 功能入口 -->
        <div class="features">
          <div class="feature" @click="go('/chat')">
            <div class="f-ico">🤖</div>
            <div>
              <div class="f-title">AI 行程助手</div>
              <div class="f-sub">一站式打造完美旅程</div>
            </div>
          </div>
          <div class="feature" @click="go('/travel')">
            <div class="f-ico">🎒</div>
            <div>
              <div class="f-title">旅游</div>
              <div class="f-sub">探索 · 好去处</div>
            </div>
          </div>
          <div class="feature" @click="go('/plan')">
            <div class="f-ico">🗺️</div>
            <div>
              <div class="f-title">智能行程规划</div>
              <div class="f-sub">按预算天数定制行程</div>
            </div>
          </div>
        </div>

        <!-- 酒店推荐 -->
        <div class="card">
          <div class="sec-head">
            <span class="sec-title">酒店推荐</span>
            <div class="tabs">
              <span
                v-for="c in hotCities"
                :key="c"
                class="tab"
                :class="{ active: c === city }"
                @click="switchCity(c)"
              >{{ c }}</span>
            </div>
          </div>

          <div v-loading="hotelLoading" style="min-height:120px">
            <div v-if="hotels.length" class="hotel-grid">
              <div class="hotel-card" v-for="(h, i) in hotels" :key="i" @click="goHotel(h)">
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
                  <div class="h-price" v-if="h.cost || h.price">¥{{ h.cost || h.price }}<span>起</span></div>
                  <div class="h-price none" v-else>查看详情</div>
                </div>
              </div>
            </div>
            <el-empty
              v-else-if="!hotelLoading"
              description="暂无酒店数据：请确认配置了高德 Key（AMAP_KEY），或换个城市"
            />
          </div>
        </div>

        <!-- 当季热推 -->
        <div class="card">
          <div class="sec-head">
            <span class="sec-title">当季热推</span>
            <span class="sec-sub">出发地
              <input v-model="depCity" class="dep-input" />
            </span>
          </div>
          <div class="season">
            <div class="s-col">
              <div class="s-col-title">热门景点推荐</div>
              <div class="s-item" v-for="(s, i) in spots" :key="i" @click="goSpot(s)">
                <div class="rank" :class="'r' + (i + 1)">{{ i + 1 }}</div>
                <img v-if="s.image" class="s-thumb" :src="s.image" alt="" />
                <div v-else class="s-thumb" :style="{ background: grad(i + 2) }"></div>
                <div class="s-info">
                  <div class="s-name">{{ s.name }}</div>
                  <div class="s-meta">
                    <span v-if="s.rating">⭐ {{ s.rating }}</span>
                    <span v-if="s.distance"> · 🚶 {{ s.distance }}</span>
                    <span v-else-if="s.city"> · {{ s.city }}</span>
                  </div>
                </div>
                <div class="s-price" v-if="s.price || s.cost">¥{{ s.price || s.cost }}<span>起</span></div>
              </div>
              <el-empty v-if="!spots.length" description="暂无景点数据" :image-size="60" />
            </div>
            <div class="s-col">
              <div class="s-col-title">周末畅游 · 特价机票</div>
              <div class="s-item" v-for="(f, i) in cheapFlights" :key="i">
                <div class="rank" :class="'r' + (i + 1)">{{ i + 1 }}</div>
                <div class="s-thumb" :style="{ background: grad(i + 5) }"></div>
                <div class="s-info">
                  <div class="s-name">{{ depCity }} ⇄ {{ f.city }}</div>
                  <div class="s-tag">精选</div>
                  <div class="s-meta">{{ f.date }} · {{ f.discount }}折</div>
                </div>
                <div class="s-price">¥{{ f.price }}<span>起</span></div>
              </div>
            </div>
          </div>
          <div class="season-tip">提示：跟团游 / 特价机票为<strong>示例数据</strong>，用于演示；酒店推荐为高德地图真实数据。</div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '游客')
const kw = ref('')
const depCity = ref('上海')

const today = new Date()
const tomorrow = new Date(Date.now() + 86400000)
const fmt = (d) => d.toISOString().slice(0, 10)

const hotel = reactive({
  city: '',
  checkIn: fmt(today),
  checkOut: fmt(tomorrow),
  guests: '1间,1位',
  level: '不限',
  keyword: '',
})

let machineId = localStorage.getItem('machineId')
if (!machineId) {
  machineId = 'dev-' + Math.random().toString(36).slice(2) + Date.now()
  localStorage.setItem('machineId', machineId)
}

const hotCities = ['上海', '北京', '广州', '三亚', '成都', '杭州']
const city = ref('上海')
const hotels = ref([])
const hotelLoading = ref(false)

const spots = ref([])
const cheapFlights = [
  { city: '大连', date: '10-10 去 10-11 回', price: 460, discount: 1.5 },
  { city: '郑州', date: '09-19 去 09-21 回', price: 460, discount: 1.8 },
  { city: '广州', date: '10-09 去 10-12 回', price: 509, discount: 1.2 },
  { city: '合肥', date: '09-18 去 09-21 回', price: 520, discount: 2.5 },
  { city: '杭州', date: '09-18 去 09-20 回', price: 580, discount: 2.5 },
]

const go = (p) => router.push(p)

// 点击酒店 → 酒店详情/预订页
const goHotel = (h) => {
  router.push({
    path: '/hotel-detail',
    query: {
      id: h.id,
      name: h.name,
      city: h.city,
      address: h.address,
      price: h.cost || h.price,
      rating: h.rating,
      image: h.image,
      tags: h.tags,
      desc: h.description,
    },
  })
}

// 点击景点 → 景点详情/预订页
const goSpot = (s) => {
  router.push({
    path: '/spot-detail',
    query: {
      id: s.id,
      name: s.name,
      city: s.city,
      image: s.image,
      price: s.price || s.cost,
      rating: s.rating,
    },
  })
}

const colors = ['#4a90d9,#2c6fb5', '#e8875a,#d3643a', '#5aa87a,#3d8a5f', '#7a6fd9,#5b4fc0', '#d95a8a,#b53a6b', '#4aa8b8,#2c8a99']
const grad = (i) => 'linear-gradient(135deg,' + colors[i % colors.length] + ')'

const loadHotels = async () => {
  hotelLoading.value = true
  try {
    const token = localStorage.getItem('token')
    // 1) 优先读「管理端维护的推荐库」
    let resp = await fetch('/api/travel/places?type=HOTEL&city=' + encodeURIComponent(city.value), {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    let res = await resp.json()
    let list = res.code === 200 ? res.data || [] : []

    // 2) 库里没有、或库里都没图片 → 回退到高德实时数据（高德带图片）
    if (!list.length || !list.some((x) => x.image)) {
      const params = new URLSearchParams()
      params.append('city', city.value)
      params.append('keyword', '酒店')
      params.append('machineId', machineId)
      resp = await fetch('/api/travel/hotels?' + params.toString(), {
        headers: { Authorization: `Bearer ${token}` },
      })
      res = await resp.json()
      list = res.code === 200 ? res.data || [] : []
    }
    hotels.value = list.slice(0, 6)
  } catch (e) {
    ElMessage.error('获取酒店失败')
  } finally {
    hotelLoading.value = false
  }
}

// 景点推荐：优先读库，库空则回退高德
const loadSpots = async () => {
  try {
    const token = localStorage.getItem('token')
    let arr = []
    const resp = await fetch('/api/travel/places?type=SPOT', {
      headers: { Authorization: `Bearer ${token}` },
    })
    const res = await resp.json()
    arr = res.code === 200 ? res.data || [] : []
    if (!arr.length || !arr.some((x) => x.image)) {
      const r2 = await fetch(
        '/api/travel/spots?city=' + encodeURIComponent(city.value) + '&keyword=' + encodeURIComponent('景点'),
        { headers: { Authorization: `Bearer ${token}` } },
      )
      const j2 = await r2.json()
      arr = j2.code === 200 ? j2.data || [] : []
    }
    spots.value = arr.slice(0, 5)
  } catch (e) {
    console.error(e)
  }
}

const switchCity = (c) => {
  city.value = c
  loadHotels()
}

const searchHotel = () => {
  const k = (hotel.keyword || hotel.city || '').trim()
  router.push({ path: '/hotel', query: k ? { kw: k } : {} })
}

const topSearch = () => {
  const k = kw.value.trim()
  if (!k) {
    ElMessage.warning('请输入要搜索的内容')
    return
  }
  // 搜索地点 → 城市目的地页（城市图片 + 景点/酒店）
  router.push({ path: '/city', query: { city: k } })
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}

onMounted(() => {
  loadHotels()
  loadSpots()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }

/* 顶部 */
.topbar {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 20;
}
.brand { font-size: 20px; font-weight: 700; color: #0086f6; white-space: nowrap; }
.search {
  width: 360px;              /* 缩短搜索框 */
  max-width: 40%;
  display: flex;
  border: 2px solid #0086f6;
  border-radius: 6px;
  overflow: hidden;
  height: 38px;
}
.search input { flex: 1; border: 0; outline: none; padding: 0 12px; font-size: 14px; min-width: 0; }
.search button { border: 0; background: #0086f6; color: #fff; padding: 0 16px; cursor: pointer; }
.links { margin-left: auto; display: flex; align-items: center; gap: 14px; font-size: 14px; color: #333; }
.links span { cursor: pointer; white-space: nowrap; }
.links span:hover { color: #0086f6; }
.links .sep { width: 1px; height: 18px; background: #e0e0e0; cursor: default; }
.links .user { background: #eef6ff; border-radius: 16px; padding: 5px 14px; }

.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; display: flex; flex-direction: column; gap: 14px; }

/* 预订酒店 */
.hero { background: linear-gradient(135deg, #5aaef5, #2382d6); border-radius: 10px; padding: 20px; color: #fff; }
.hero-title { font-size: 22px; font-weight: 700; margin-bottom: 14px; }
.hero-form { display: flex; gap: 10px; margin-bottom: 10px; }
.hero-form .field { flex: 1; background: #fff; border-radius: 6px; padding: 6px 12px; min-width: 0; }
.hero-form .field.big { flex: 2; }
.field label { display: block; font-size: 12px; color: #8a8a8a; }
.field input { border: 0; outline: none; width: 100%; font-size: 15px; color: #222; padding: 2px 0; }
.hero-form.second { align-items: stretch; }
.go { border: 0; background: #0086f6; color: #fff; border-radius: 6px; padding: 0 30px; font-size: 16px; font-weight: 600; cursor: pointer; }
.go:hover { background: #0072d6; }

.banner { background: #fff; border-radius: 8px; padding: 12px 16px; color: #666; font-size: 14px; }

.features { display: flex; gap: 14px; }
.feature { flex: 1; background: #fff; border-radius: 8px; padding: 14px 16px; display: flex; gap: 12px; align-items: center; cursor: pointer; }
.feature:hover { box-shadow: 0 2px 10px rgba(0, 134, 246, 0.15); }
.f-ico { font-size: 24px; }
.f-title { font-size: 15px; font-weight: 600; color: #222; }
.f-sub { font-size: 12px; color: #888; margin-top: 2px; }

/* 通用卡片 */
.card { background: #fff; border-radius: 8px; padding: 16px; }
.sec-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }
.sec-title { font-size: 18px; font-weight: 700; color: #222; }
.sec-sub { font-size: 13px; color: #666; display: flex; align-items: center; gap: 8px; }
.dep-input { width: 100px; border: 1px solid #ddd; border-radius: 4px; padding: 4px 8px; outline: none; }

.tabs .tab { display: inline-block; margin-left: 10px; font-size: 14px; color: #333; padding: 4px 16px; border: 1px solid #e0e0e0; border-radius: 4px; cursor: pointer; }
.tabs .tab.active { border-color: #ff8a1f; color: #ff8a1f; font-weight: 600; }

/* 酒店推荐卡片 */
.hotel-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
.hotel-card { border: 1px solid #eef0f3; border-radius: 8px; overflow: hidden; }
.hotel-card { cursor: pointer; transition: box-shadow .2s; }
.hotel-card:hover { box-shadow: 0 4px 14px rgba(0, 134, 246, .15); }
img.h-img { width: 100%; height: 120px; object-fit: cover; display: block; }
.h-img { height: 120px; display: flex; align-items: center; justify-content: center; font-size: 34px; color: #fff; }
.h-body { padding: 10px 12px 12px; }
.h-name { font-size: 15px; font-weight: 600; color: #222; }
.h-meta { display: flex; align-items: center; gap: 8px; margin: 6px 0; font-size: 12px; }
.score { background: #0086f6; color: #fff; border-radius: 4px; padding: 1px 6px; font-weight: 600; }
.great { color: #0086f6; font-weight: 600; }
.dist { color: #999; }
.h-addr { font-size: 12px; color: #999; line-height: 1.4; min-height: 32px; }
.h-price { margin-top: 8px; color: #ff6a00; font-size: 20px; font-weight: 700; text-align: right; }
.h-price span { font-size: 12px; font-weight: 400; color: #999; margin-left: 2px; }
.h-price.none { font-size: 13px; color: #999; font-weight: 400; }

/* 当季热推 */
.season { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.s-col { background: #f8fafc; border-radius: 8px; padding: 12px; }
.s-col-title { font-size: 15px; font-weight: 700; color: #222; margin-bottom: 10px; }
.s-item { display: flex; align-items: center; gap: 10px; background: #fff; border-radius: 8px; padding: 8px 10px; margin-bottom: 8px; }
.s-item { cursor: pointer; }
.s-item:hover { background: #f5faff; }
.s-item:last-child { margin-bottom: 0; }
.rank { width: 18px; height: 18px; border-radius: 4px; background: #ccc; color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.rank.r1 { background: #ff4d4f; }
.rank.r2 { background: #ff8a1f; }
.rank.r3 { background: #f7b500; }
img.s-thumb { width: 52px; height: 40px; border-radius: 5px; flex-shrink: 0; object-fit: cover; }
.s-thumb { width: 52px; height: 40px; border-radius: 5px; flex-shrink: 0; }
.s-info { flex: 1; min-width: 0; }
.s-name {
  display: inline-block;
  max-width: 100%;
  font-size: 13px;
  color: #222;
  border: 1px solid #dfe3e8;
  background: #fafbfc;
  border-radius: 4px;
  padding: 2px 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.s-tag { display: inline-block; font-size: 11px; color: #00a870; border: 1px solid #00a870; border-radius: 3px; padding: 0 4px; margin-top: 2px; }
.s-meta { font-size: 12px; color: #999; margin-top: 2px; }
.s-price { color: #ff6a00; font-size: 16px; font-weight: 700; white-space: nowrap; }
.s-price span { font-size: 11px; font-weight: 400; color: #999; }
.season-tip { margin-top: 10px; font-size: 12px; color: #aaa; }
</style>
