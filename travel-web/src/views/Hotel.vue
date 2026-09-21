<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🏨 酒店</div>
      <div class="search">
        <input v-model="q" placeholder="搜索地点 / 酒店名称，如：上海外滩、希尔顿" @keyup.enter="doSearch" />
        <button @click="doSearch">🔍 搜索</button>
      </div>
      <div class="links">
        <span @click="go('/home')">返回首页</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/hotel" />
      <main class="content">
        <div class="card">
          <div class="head">
            <span class="h-title">推荐酒店</span>
            <span class="h-sub">{{ q ? '搜索：' + q : '按设备自动推荐城市' }}</span>
          </div>

          <div v-loading="loading" style="min-height:120px">
            <div v-if="list.length" class="hlist">
              <div class="hitem" v-for="(h, i) in list" :key="i" @click="goHotel(h)">
                <img v-if="h.image" class="hthumb" :src="h.image" alt="" />
                <div v-else class="hthumb" :style="{ background: grad(i) }">🏨</div>
                <div class="hinfo">
                  <div class="hname">{{ h.name }}</div>
                  <div class="haddr">📍 {{ h.address || h.city || '地址未知' }}</div>
                  <div class="htags">
                    <span v-if="h.distance" class="t">🚶 {{ h.distance }}</span>
                    <span v-if="h.rating" class="t">⭐ {{ h.rating }}</span>
                    <span v-if="h.price" class="t">💰 ¥{{ h.price }}起</span>
                    <span v-if="h.tags" class="t plain">{{ h.tags }}</span>
                    <span v-if="h.tel" class="t plain">☎ {{ h.tel }}</span>
                  </div>
                </div>
              </div>
            </div>
            <el-empty
              v-else-if="!loading"
              description="暂无数据：请确认已配置高德 Key（AMAP_KEY），或换个关键词"
            />
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
const list = ref([])
const loading = ref(false)

let machineId = localStorage.getItem('machineId')
if (!machineId) {
  machineId = 'dev-' + Math.random().toString(36).slice(2) + Date.now()
  localStorage.setItem('machineId', machineId)
}

const grad = (i) => {
  const colors = ['#4a90d9,#2c6fb5', '#e8875a,#d3643a', '#5aa87a,#3d8a5f', '#7a6fd9,#5b4fc0', '#d95a8a,#b53a6b', '#4aa8b8,#2c8a99']
  return 'linear-gradient(135deg,' + colors[i % colors.length] + ')'
}

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
      price: h.price || h.cost,
      rating: h.rating,
      image: h.image,
      tags: h.tags,
      desc: h.description,
    },
  })
}

const load = async () => {
  loading.value = true
  try {
    const k = q.value.trim()
    const token = localStorage.getItem('token')
    let arr = []

    if (k) {
      // 搜索：走高德实时数据
      const params = new URLSearchParams()
      params.append('city', k)
      params.append('keyword', k)
      const resp = await fetch('/api/travel/hotels?' + params.toString(), {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (resp.status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
        return
      }
      const res = await resp.json()
      arr = res.code === 200 ? res.data || [] : []
    } else {
      // 推荐：读管理端维护的推荐库；库里没有则回退高德
      const resp = await fetch('/api/travel/places?type=HOTEL', {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (resp.status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
        return
      }
      const res = await resp.json()
      arr = res.code === 200 ? res.data || [] : []
      // 库里没有、或库里都没图片 → 回退高德（带图）
      if (!arr.length || !arr.some((x) => x.image)) {
        const p2 = new URLSearchParams()
        p2.append('machineId', machineId)
        p2.append('keyword', '酒店')
        const r2 = await fetch('/api/travel/hotels?' + p2.toString(), {
          headers: { Authorization: `Bearer ${token}` },
        })
        const j2 = await r2.json()
        arr = j2.code === 200 ? j2.data || [] : []
      }
    }
    list.value = arr
  } catch (e) {
    ElMessage.error('获取酒店失败')
  } finally {
    loading.value = false
  }
}

const doSearch = () => load()

onMounted(load)
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
  gap: 20px;
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
  white-space: nowrap;
}
.search {
  flex: 1;
  display: flex;
  border: 2px solid #0086f6;
  border-radius: 6px;
  overflow: hidden;
  height: 40px;
}
.search input {
  flex: 1;
  border: 0;
  outline: none;
  padding: 0 14px;
  font-size: 14px;
}
.search button {
  border: 0;
  background: #0086f6;
  color: #fff;
  padding: 0 22px;
  cursor: pointer;
}
.links span {
  color: #333;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}
.links span:hover {
  color: #0086f6;
}
.wrap {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr);
  gap: 16px;
  padding: 16px 24px 40px;
  align-items: start;
  box-sizing: border-box;
}
.content {
  min-width: 0;
}
.card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}
.h-title {
  font-size: 17px;
  font-weight: 700;
  color: #222;
}
.h-sub {
  font-size: 12px;
  color: #999;
}
.hlist {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}
.hitem {
  display: flex;
  gap: 12px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  padding: 12px;
}
.hitem { cursor: pointer; }
.hitem:hover { border-color: #0086f6; }
.hthumb {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}
img.hthumb {
  object-fit: cover;
  display: block;
}
.hname {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}
.haddr {
  font-size: 13px;
  color: #666;
  margin: 4px 0;
}
.htags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.htags .t {
  font-size: 12px;
  color: #e6702b;
  background: #fff3ec;
  border-radius: 4px;
  padding: 1px 6px;
}
.htags .t.plain {
  color: #666;
  background: #f4f4f4;
}
</style>
