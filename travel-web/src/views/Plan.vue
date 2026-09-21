<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🗺️ 智能行程规划</div>
      <div class="links"><span @click="go('/home')">返回首页</span></div>
    </header>

    <div class="wrap">
      <SideNav active="/plan" />
      <main class="content">
        <div class="hero">
          <div class="hero-title">智能行程规划</div>
          <div class="hero-sub">选择目的地 · 预算 · 天数，AI 一键生成专属行程</div>
          <div class="hero-form">
            <div class="field">
              <label>目的地</label>
              <input v-model="form.city" placeholder="如：北京 / 成都 / 三亚" />
            </div>
            <div class="field">
              <label>预算(元)</label>
              <input v-model="form.budget" type="number" />
            </div>
            <div class="field">
              <label>天数(天)</label>
              <input v-model="form.days" type="number" />
            </div>
            <button class="go" @click="handlePlan">开始规划</button>
          </div>
        </div>

        <div class="card">
          <div class="section-head">
            <span class="section-title">热门目的地</span>
            <el-button size="small" @click="refreshCities">换一批</el-button>
          </div>
          <div class="cards">
            <div class="city-card" v-for="(c, i) in hotCities" :key="c" @click="quickPlan(c)">
              <div
                class="thumb"
                :style="cityImg[c] ? { backgroundImage: 'url(' + cityImg[c] + ')' } : { background: grad(i) }"
              >
                <span v-if="!cityImg[c]">{{ c }}</span>
              </div>
              <div class="c-name">{{ c }}</div>
              <div class="c-sub">点击生成行程</div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const router = useRouter()
const form = reactive({ city: '', budget: 3000, days: 3 })
// 城市池（随机推荐用）
const ALL_CITIES = [
  '北京', '上海', '广州', '深圳', '成都', '杭州', '西安', '重庆', '南京', '武汉',
  '苏州', '长沙', '天津', '郑州', '昆明', '三亚', '丽江', '大理', '厦门', '青岛',
  '大连', '沈阳', '哈尔滨', '福州', '南昌', '合肥', '南宁', '桂林', '贵阳', '兰州',
  '西宁', '银川', '乌鲁木齐', '拉萨', '呼和浩特', '太原', '石家庄', '济南',
  '珠海', '洛阳', '开封', '敦煌', '张家界', '黄山', '威海', '秦皇岛',
]
const hotCities = ref([])
const cityImg = ref({})

const pickRandomCities = (n) => {
  const arr = [...ALL_CITIES]
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr.slice(0, n)
}

const refreshCities = () => {
  hotCities.value = pickRandomCities(8)
  cityImg.value = {}
  loadCityImages()
}

const go = (p) => router.push(p)

const colors = ['#4a90d9,#2c6fb5', '#e8875a,#d3643a', '#5aa87a,#3d8a5f', '#7a6fd9,#5b4fc0', '#d95a8a,#b53a6b', '#4aa8b8,#2c8a99']
const grad = (i) => 'linear-gradient(135deg,' + colors[i % colors.length] + ')'

const handlePlan = () => {
  if (!form.city || !String(form.city).trim()) {
    ElMessage.warning('请输入目的地')
    return
  }
  router.push({ path: '/detail', query: { city: form.city, budget: form.budget, days: form.days } })
}

const quickPlan = (city) => {
  form.city = city
  router.push({ path: '/detail', query: { city, budget: form.budget, days: form.days } })
}

// 给热门目的地城市卡片配图（走缓存的高德图片接口）
const loadCityImages = () => {
  const token = localStorage.getItem('token')
  hotCities.value.forEach((c) => {
    fetch('/api/travel/image?city=' + encodeURIComponent(c) + '&keyword=' + encodeURIComponent('景点'), {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((res) => {
        if (res.code === 200 && res.data) cityImg.value = { ...cityImg.value, [c]: res.data }
      })
      .catch(() => {})
  })
}

onMounted(refreshCities)
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.hero { background: linear-gradient(135deg, #4aa3f0, #1f7fd0); border-radius: 10px; padding: 22px; color: #fff; }
.hero-title { font-size: 22px; font-weight: 700; }
.hero-sub { font-size: 13px; opacity: .92; margin: 6px 0 16px; }
.hero-form { display: flex; gap: 10px; background: rgba(255,255,255,.18); padding: 12px; border-radius: 8px; }
.field { flex: 1; background: #fff; border-radius: 6px; padding: 6px 12px; min-width: 0; }
.field label { display: block; font-size: 12px; color: #8a8a8a; }
.field input { border: 0; outline: none; width: 100%; font-size: 15px; color: #222; padding: 2px 0; }
.go { border: 0; background: #ff8a1f; color: #fff; border-radius: 6px; padding: 0 30px; font-size: 16px; font-weight: 600; cursor: pointer; }
.go:hover { background: #f57c0b; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.section-title { font-size: 17px; font-weight: 700; color: #222; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.city-card { cursor: pointer; }
.thumb { height: 92px; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: 600; }
.thumb { background-size: cover; background-position: center; }
.c-name { font-size: 14px; color: #222; margin-top: 6px; }
.c-sub { font-size: 12px; color: #999; }
</style>
