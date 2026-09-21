<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🎒 旅游</div>
      <div class="search">
        <input v-model="q" placeholder="搜索景点 / 城市，如：西湖、故宫、成都" @keyup.enter="doSearch" />
        <button @click="doSearch">🔍 搜索</button>
      </div>
      <div class="links">
        <span @click="go('/home')">返回首页</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/travel" />
      <main class="content">
        <!-- 两个一级标签：城市 / 景点 -->
        <el-tabs v-model="tab" class="kind-tabs" @tab-change="onTabChange">
          <el-tab-pane label="城市" name="city" />
          <el-tab-pane label="景点" name="spot" />
        </el-tabs>

        <!-- ① 城市：全部推荐城市，点进去看这座城市的景点和酒店 -->
        <div class="card" v-if="tab === 'city'">
          <div class="head">
            <span class="h-title">推荐城市（共 {{ cities.length }} 个）</span>
            <span class="h-sub">点一座城市，看它的全部景点和酒店</span>
          </div>
          <div class="city-grid" v-loading="loadingCity">
            <div class="city" v-for="c in cities" :key="c" @click="openCity(c)">
              <span class="city-name">{{ c }}</span>
              <span class="city-go">景点 · 酒店 →</span>
            </div>
          </div>
        </div>

        <!-- ② 景点：随机推荐 + 高德图片和文字介绍 -->
        <div class="card" v-else>
          <div class="head">
            <span class="h-title">{{ q ? '「' + q + '」相关景点' : '随机推荐景点' }}</span>
            <div>
              <el-button v-if="q" size="small" @click="clearSearch">返回推荐</el-button>
              <el-button v-else size="small" type="primary" :loading="loading" @click="randomSpots">换一批</el-button>
            </div>
          </div>

          <div v-loading="loading" style="min-height: 160px">
            <div v-if="list.length" class="grid">
              <div class="pcard" v-for="(p, i) in list" :key="i" @click="openSpot(p)">
                <img v-if="p.image" class="pthumb" :src="p.image" alt="" @error="onImgError(p)" />
                <div v-else class="pthumb grad" :style="{ background: grad(i) }">
                  {{ (p.name || '?').slice(0, 1) }}
                </div>
                <div class="pname">{{ p.name }}</div>
                <div class="pmeta">
                  <span v-if="p.rating" class="t">⭐ {{ p.rating }}</span>
                  <span v-if="p.city" class="t">{{ p.city }}</span>
                  <span v-if="p.distance" class="t">🚶 {{ p.distance }}</span>
                </div>
                <div class="paddr">📍 {{ p.address || p.city || '地址未知' }}</div>
                <div class="plink">点击查看景点介绍 →</div>
              </div>
            </div>
            <el-empty v-else-if="!loading" description="暂无数据：请确认已配置高德 Key（AMAP_KEY）" />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const router = useRouter()
const route = useRoute()
const q = ref(route.query.kw || '')
const tab = ref('city')
const cities = ref([])
const list = ref([])
const loading = ref(false)
const loadingCity = ref(false)

// 接口拿不到时的兜底城市（正常情况下后端会返回 40 个常见城市 + 推荐库里的城市）
const FALLBACK_CITIES = [
  '北京', '上海', '广州', '深圳', '成都', '杭州', '西安', '重庆', '南京', '苏州',
  '厦门', '三亚', '青岛', '大连', '昆明', '丽江', '大理', '桂林', '张家界', '黄山',
  '武汉', '长沙', '天津', '哈尔滨', '沈阳', '郑州', '洛阳', '拉萨', '西宁', '兰州',
  '海口', '南宁', '福州', '贵阳', '乌鲁木齐', '呼和浩特', '银川', '南昌', '太原', '石家庄',
]

const colors = ['#4a90d9,#2c6fb5', '#e8875a,#d3643a', '#5aa87a,#3d8a5f', '#7a6fd9,#5b4fc0', '#d95a8a,#b53a6b', '#4aa8b8,#2c8a99']
const grad = (i) => 'linear-gradient(135deg,' + colors[i % colors.length] + ')'
const token = () => localStorage.getItem('token')
const go = (p) => router.push(p)
const shuffle = (arr) => arr.map((v) => [Math.random(), v]).sort((a, b) => a[0] - b[0]).map((v) => v[1])

/** 图片加载失败就退回渐变占位，避免出现空白或裂图 */
const onImgError = (p) => {
  p.image = ''
}

/** 没有配图的景点，用「景点名 + 风景」去高德补一张风景图（后端有缓存） */
const fillMissingImages = async (arr) => {
  const need = arr.filter((p) => !p.image).slice(0, 12)
  if (!need.length) return
  await Promise.all(
    need.map(async (p) => {
      try {
        const qs = new URLSearchParams()
        qs.append('keyword', (p.name || '') + ' 风景')
        if (p.city) qs.append('city', p.city)
        const resp = await fetch('/api/travel/image?' + qs.toString(), {
          headers: { Authorization: `Bearer ${token()}` },
        })
        const res = await resp.json()
        if (res.code === 200 && res.data) p.image = res.data
      } catch (e) {
        /* 补图失败就保持占位 */
      }
    }),
  )
}

// ============ 城市 ============
const loadCities = async () => {
  loadingCity.value = true
  try {
    const resp = await fetch('/api/travel/cities', { headers: { Authorization: `Bearer ${token()}` } })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    cities.value = res.code === 200 && res.data && res.data.length ? res.data : FALLBACK_CITIES
  } catch (e) {
    cities.value = FALLBACK_CITIES
  } finally {
    loadingCity.value = false
  }
}

const openCity = (c) => router.push({ path: '/city', query: { city: c } })

// ============ 景点 ============
/** 随机推荐：先从推荐库里随机取，再随机挑两座城市拉高德真实景点，混合后打乱 */
const randomSpots = async () => {
  loading.value = true
  try {
    let arr = []
    // 1. 推荐库（管理端维护，带图片和简介）
    const r1 = await fetch('/api/travel/places?type=SPOT', { headers: { Authorization: `Bearer ${token()}` } })
    const j1 = await r1.json()
    if (j1.code === 200) arr = j1.data || []

    // 2. 随机两座城市的真实景点（高德）
    const cityPool = shuffle(cities.value.length ? cities.value : FALLBACK_CITIES).slice(0, 2)
    for (const c of cityPool) {
      const params = new URLSearchParams()
      params.append('city', c)
      params.append('keyword', '景点')
      const resp = await fetch('/api/travel/spots?' + params.toString(), {
        headers: { Authorization: `Bearer ${token()}` },
      })
      if (resp.status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
        return
      }
      const res = await resp.json()
      if (res.code === 200) arr = arr.concat(res.data || [])
    }

    // 3. 去重 + 打乱 + 只要 12 个
    const seen = new Set()
    const merged = []
    for (const p of shuffle(arr)) {
      const key = (p.name || '') + '|' + (p.city || '')
      if (!p.name || seen.has(key)) continue
      seen.add(key)
      merged.push(p)
    }
    list.value = merged.slice(0, 12)
    fillMissingImages(list.value)
  } catch (e) {
    ElMessage.error('获取景点失败')
  } finally {
    loading.value = false
  }
}

/** 搜索景点（关键词可以是城市或具体景点名） */
const doSearch = async () => {
  const k = q.value.trim()
  if (!k) {
    randomSpots()
    return
  }
  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('city', k)
    params.append('keyword', k)
    const resp = await fetch('/api/travel/spots?' + params.toString(), {
      headers: { Authorization: `Bearer ${token()}` },
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    list.value = res.code === 200 ? res.data || [] : []
    fillMissingImages(list.value)
  } catch (e) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const clearSearch = () => {
  q.value = ''
  randomSpots()
}

const openSpot = (p) => {
  router.push({
    path: '/spot-detail',
    query: {
      id: p.id,
      name: p.name,
      city: p.city,
      address: p.address,
      image: p.image,
      price: p.price || p.cost,
      rating: p.rating,
    },
  })
}

const onTabChange = () => {
  if (tab.value === 'spot' && !list.value.length) randomSpots()
}

onMounted(async () => {
  await loadCities()
  if (q.value) doSearch()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; gap: 20px; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; white-space: nowrap; }
.search { flex: 1; display: flex; border: 2px solid #0086f6; border-radius: 6px; overflow: hidden; height: 40px; }
.search input { flex: 1; border: 0; outline: none; padding: 0 14px; font-size: 14px; }
.search button { border: 0; background: #0086f6; color: #fff; padding: 0 22px; cursor: pointer; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.links span:hover { color: #0086f6; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; }
.kind-tabs { background: #fff; border-radius: 8px; padding: 6px 16px 0; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.h-title { font-size: 17px; font-weight: 700; color: #222; }
.h-sub { font-size: 12px; color: #999; }

/* 城市卡片 */
.city-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.city { border: 1px solid #eef0f3; border-radius: 8px; padding: 14px 12px; cursor: pointer; display: flex; flex-direction: column; gap: 6px; transition: all .18s; }
.city:hover { border-color: #0086f6; background: #f5faff; box-shadow: 0 4px 14px rgba(0,134,246,.12); }
.city-name { font-size: 16px; font-weight: 600; color: #222; }
.city-go { font-size: 12px; color: #0086f6; }

/* 景点卡片 */
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px; }
.pcard { border: 1px solid #eef0f3; border-radius: 8px; padding: 12px; cursor: pointer; transition: box-shadow .2s; }
.pcard:hover { box-shadow: 0 4px 14px rgba(0,134,246,.15); border-color: #0086f6; }
.pthumb { height: 120px; width: 100%; border-radius: 6px; object-fit: cover; display: block; margin-bottom: 8px; }
.pthumb.grad { display: flex; align-items: center; justify-content: center; color: #fff; font-size: 32px; font-weight: 700; }
.pname { font-size: 15px; font-weight: 600; color: #222; }
.pmeta { margin: 6px 0; display: flex; flex-wrap: wrap; gap: 6px; }
.pmeta .t { font-size: 12px; color: #e6702b; background: #fff3ec; border-radius: 4px; padding: 1px 6px; }
.paddr { font-size: 12px; color: #999; }
.plink { margin-top: 8px; font-size: 12px; color: #0086f6; }
</style>
