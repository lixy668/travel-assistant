<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🏙️ {{ city }}</div>
      <div class="links"><span @click="go('/home')">返回首页</span></div>
    </header>

    <div class="wrap">
      <SideNav active="/travel" />

      <main class="content">
        <!-- 城市大图 -->
        <div class="banner">
          <img v-if="cityImg" class="banner-img" :src="cityImg" alt="" />
          <div v-else class="banner-img grad">{{ city }}</div>
          <div class="banner-name">{{ city }}</div>
        </div>

        <!-- 景点 / 酒店 -->
        <div class="card">
          <el-tabs v-model="tab">
            <el-tab-pane :label="'景点（' + spots.length + '）'" name="spot" />
            <el-tab-pane :label="'酒店（' + hotels.length + '）'" name="hotel" />
          </el-tabs>

          <div v-if="tab === 'spot'" v-loading="loading" style="min-height:120px">
            <div v-if="spots.length" class="grid">
              <div class="item" v-for="(s, i) in spots" :key="i" @click="goSpot(s)">
                <img v-if="s.image" class="item-img" :src="s.image" alt="" @error="s.image = ''" />
                <div v-else class="item-img grad">{{ (s.name || '?').slice(0, 1) }}</div>
                <div class="item-body">
                  <div class="item-name">{{ s.name }}</div>
                  <div class="item-meta">
                    <span v-if="s.rating" class="score">{{ s.rating }}分</span>
                    <span v-if="s.distance" class="dist">{{ s.distance }}</span>
                  </div>
                  <div class="item-desc">📍 {{ s.address || s.city }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else-if="!loading" description="暂无景点数据" />
          </div>

          <div v-else v-loading="loadingHotel" style="min-height:120px">
            <div v-if="hotels.length" class="grid">
              <div class="item" v-for="(h, i) in hotels" :key="i" @click="goHotel(h)">
                <img v-if="h.image" class="item-img" :src="h.image" alt="" @error="h.image = ''" />
                <div v-else class="item-img grad">{{ (h.name || '?').slice(0, 1) }}</div>
                <div class="item-body">
                  <div class="item-name">{{ h.name }}</div>
                  <div class="item-meta">
                    <span v-if="h.rating" class="score">{{ h.rating }}分</span>
                    <span v-if="h.distance" class="dist">{{ h.distance }}</span>
                    <span v-if="h.cost" class="price">¥{{ h.cost }}起</span>
                  </div>
                  <div class="item-desc">📍 {{ h.address || h.city }} ｜ 点击预订</div>
                </div>
              </div>
            </div>
            <el-empty v-else-if="!loadingHotel" description="暂无酒店数据" />
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

const city = ref(route.query.city || '北京')
const tab = ref('spot')
const cityImg = ref('')
const spots = ref([])
const hotels = ref([])
const loading = ref(false)
const loadingHotel = ref(false)

const go = (p) => router.push(p)
const token = () => localStorage.getItem('token')

const loadCityImage = async () => {
  try {
    const resp = await fetch(
      '/api/travel/image?city=' + encodeURIComponent(city.value) + '&keyword=' + encodeURIComponent(city.value + ' 风景'),
      { headers: { Authorization: `Bearer ${token()}` } },
    )
    const res = await resp.json()
    if (res.code === 200 && res.data) cityImg.value = res.data
  } catch (e) {
    /* 忽略 */
  }
}

const loadSpots = async () => {
  loading.value = true
  try {
    const resp = await fetch(
      '/api/travel/spots?city=' + encodeURIComponent(city.value) + '&keyword=' + encodeURIComponent('景点'),
      { headers: { Authorization: `Bearer ${token()}` } },
    )
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    spots.value = res.code === 200 ? res.data || [] : []
  } catch (e) {
    ElMessage.error('获取景点失败')
  } finally {
    loading.value = false
  }
}

const loadHotels = async () => {
  loadingHotel.value = true
  try {
    const resp = await fetch(
      '/api/travel/hotels?city=' + encodeURIComponent(city.value) + '&keyword=' + encodeURIComponent('酒店'),
      { headers: { Authorization: `Bearer ${token()}` } },
    )
    const res = await resp.json()
    hotels.value = res.code === 200 ? res.data || [] : []
  } catch (e) {
    ElMessage.error('获取酒店失败')
  } finally {
    loadingHotel.value = false
  }
}

// 景点 → 景点详情（可买票）
const goSpot = (s) => {
  router.push({
    path: '/spot-detail',
    query: {
      id: s.id,
      name: s.name,
      city: city.value,
      image: s.image,
      price: s.price || s.cost,
      rating: s.rating,
      address: s.address,
      type: s.type,
    },
  })
}

// 酒店 → 酒店预订页（不是景点买票）
const goHotel = (h) => {
  router.push({
    path: '/hotel-detail',
    query: {
      id: h.id,
      name: h.name,
      city: city.value,
      address: h.address,
      price: h.cost || h.price,
      rating: h.rating,
      image: h.image,
      tags: h.type,
      desc: h.address,
    },
  })
}

onMounted(() => {
  loadCityImage()
  loadSpots()
  loadHotels()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.banner { position: relative; border-radius: 10px; overflow: hidden; }
.banner-img { width: 100%; height: 260px; object-fit: cover; display: block; }
.banner-img.grad { background: linear-gradient(135deg, #4aa3f0, #1f7fd0); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 42px; font-weight: 700; }
.banner-name { position: absolute; left: 24px; bottom: 18px; color: #fff; font-size: 30px; font-weight: 700; text-shadow: 0 2px 8px rgba(0,0,0,.45); }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 16px; }
.item { border: 1px solid #eef0f3; border-radius: 8px; overflow: hidden; cursor: pointer; transition: box-shadow .2s; }
.item:hover { box-shadow: 0 4px 14px rgba(0, 134, 246, .15); }
.item-img { width: 100%; height: 150px; object-fit: cover; display: block; }
.item-img.grad { background: linear-gradient(135deg, #5aa87a, #3d8a5f); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 40px; font-weight: 700; }
.item-body { padding: 10px 12px 12px; }
.item-name { font-size: 15px; font-weight: 600; color: #222; }
.item-meta { display: flex; align-items: center; gap: 8px; margin: 6px 0; font-size: 12px; }
.score { background: #0086f6; color: #fff; border-radius: 4px; padding: 1px 6px; font-weight: 600; }
.dist { color: #999; }
.price { color: #ff6a00; font-weight: 600; }
.item-desc { font-size: 12px; color: #999; line-height: 1.5; }
</style>
