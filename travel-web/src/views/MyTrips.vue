<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🎒 我的行程</div>
      <div class="links">
        <span @click="go('/plan')">去规划</span>
        <span @click="go('/home')">返回首页</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/my-trips" />
      <main class="content">
        <div class="card">
          <el-tabs v-model="tab">
            <!-- 行程历史 -->
            <el-tab-pane label="行程历史" name="plans">
              <div v-loading="loadingPlans">
                <div v-for="p in plans" :key="p.id" class="plan">
                  <div class="plan-head">
                    <div>
                      <b>{{ p.city }}</b>
                      <span class="meta">{{ p.days }} 天 ｜ 预算 {{ p.budget }} 元 ｜ {{ fmt(p.createTime) }}</span>
                    </div>
                    <div>
                      <el-button size="small" @click="toggle(p)">{{ expanded === p.id ? '收起' : '查看行程' }}</el-button>
                      <el-button size="small" type="danger" @click="removePlan(p)">删除</el-button>
                    </div>
                  </div>
                  <div v-if="expanded === p.id" class="plan-body">
                    <div v-for="d in parse(p.resultJson).dailyItinerary || []" :key="d.day" class="day">
                      <div class="day-title">第 {{ d.day }} 天</div>
                      <div class="slot" v-for="k in ['morning', 'afternoon', 'evening']" :key="k">
                        <template v-if="d[k] && d[k].spot">
                          <span class="slot-name">{{ slotName(k) }}：{{ d[k].spot }}</span>
                          <span class="slot-desc">{{ d[k].desc || d[k].note || '' }}</span>
                        </template>
                      </div>
                    </div>
                    <div v-if="parse(p.resultJson).hotels && parse(p.resultJson).hotels.length" class="hotels">
                      <div class="day-title">推荐酒店</div>
                      <span v-for="(h, i) in parse(p.resultJson).hotels" :key="i" class="hotel">{{ h.name }}</span>
                    </div>
                  </div>
                </div>
                <el-empty v-if="!loadingPlans && !plans.length" description="还没有保存的行程，去「智能行程规划」生成一个吧" />
              </div>
            </el-tab-pane>

            <!-- 收藏 -->
            <el-tab-pane label="我的收藏" name="favorites">
              <div v-loading="loadingFav">
                <div v-for="f in favorites" :key="f.id" class="fav" @click="openFav(f)">
                  <img v-if="f.image" class="fav-img" :src="f.image" alt="" />
                  <div v-else class="fav-img ph">{{ f.type === 'SPOT' ? '🎫' : '🏨' }}</div>
                  <div class="fav-info">
                    <div class="fav-name">{{ f.name }}</div>
                    <div class="fav-sub">{{ f.address || f.city || '' }}</div>
                    <div class="fav-price">{{ f.price ? '¥' + f.price + ' 起' : '' }}</div>
                  </div>
                  <el-button size="small" type="danger" @click.stop="removeFav(f)">取消收藏</el-button>
                </div>
                <el-empty v-if="!loadingFav && !favorites.length" description="还没有收藏，在酒店/景点详情页点「收藏」试试" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const router = useRouter()
const tab = ref('plans')
const plans = ref([])
const favorites = ref([])
const loadingPlans = ref(false)
const loadingFav = ref(false)
const expanded = ref(null)

const go = (p) => router.push(p)
const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')
const slotName = (k) => (k === 'morning' ? '上午' : k === 'afternoon' ? '下午' : '晚上')
const parse = (json) => {
  try {
    return JSON.parse(json || '{}')
  } catch (e) {
    return {}
  }
}
const toggle = (p) => (expanded.value = expanded.value === p.id ? null : p.id)

const loadPlans = async () => {
  loadingPlans.value = true
  try {
    const resp = await fetch('/api/plan/my', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    if (res.code === 200) plans.value = res.data || []
  } catch (e) {
    ElMessage.error('加载行程失败')
  } finally {
    loadingPlans.value = false
  }
}

const loadFavorites = async () => {
  loadingFav.value = true
  try {
    const resp = await fetch('/api/favorite/my', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    if (res.code === 200) favorites.value = res.data || []
  } catch (e) {
    ElMessage.error('加载收藏失败')
  } finally {
    loadingFav.value = false
  }
}

const removePlan = (p) => {
  ElMessageBox.confirm('确认删除这条行程记录？', '删除行程', { type: 'warning' })
    .then(async () => {
      const resp = await fetch('/api/plan/' + p.id, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      })
      const res = await resp.json()
      if (res.code === 200) {
        ElMessage.success('已删除')
        loadPlans()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
    .catch(() => {})
}

const removeFav = (f) => {
  ElMessageBox.confirm('取消收藏「' + f.name + '」？', '取消收藏', { type: 'warning' })
    .then(async () => {
      const resp = await fetch('/api/favorite/' + f.id, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      })
      const res = await resp.json()
      if (res.code === 200) {
        ElMessage.success('已取消收藏')
        loadFavorites()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    })
    .catch(() => {})
}

const openFav = (f) => {
  if (f.type === 'SPOT') {
    router.push({
      path: '/spot-detail',
      query: { name: f.name, city: f.city, address: f.address, price: f.price, image: f.image },
    })
  } else {
    router.push({
      path: '/hotel-detail',
      query: { name: f.name, city: f.city, address: f.address, price: f.price, image: f.image },
    })
  }
}

onMounted(() => {
  loadPlans()
  loadFavorites()
})
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
.plan { border: 1px solid #eef0f3; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.plan-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.plan-head b { font-size: 16px; color: #222; }
.meta { font-size: 12px; color: #999; margin-left: 10px; }
.plan-body { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #eee; }
.day { margin-bottom: 10px; }
.day-title { font-size: 13px; font-weight: 700; color: #0086f6; margin-bottom: 4px; }
.slot { font-size: 13px; color: #444; line-height: 1.9; }
.slot-name { color: #222; font-weight: 600; margin-right: 8px; }
.slot-desc { color: #888; }
.hotels { margin-top: 8px; }
.hotel { display: inline-block; font-size: 12px; color: #e6702b; background: #fff3ec; border-radius: 4px; padding: 2px 8px; margin: 0 6px 6px 0; }
.fav { display: flex; align-items: center; gap: 12px; border: 1px solid #eef0f3; border-radius: 8px; padding: 10px; margin-bottom: 10px; cursor: pointer; }
.fav:hover { border-color: #0086f6; }
.fav-img { width: 72px; height: 56px; border-radius: 6px; object-fit: cover; display: block; flex-shrink: 0; }
.fav-img.ph { display: flex; align-items: center; justify-content: center; background: #eef1f5; font-size: 24px; }
.fav-info { flex: 1; min-width: 0; }
.fav-name { font-size: 15px; font-weight: 600; color: #222; }
.fav-sub { font-size: 12px; color: #888; margin: 3px 0; }
.fav-price { font-size: 13px; color: #ff6a00; font-weight: 600; }
</style>
