<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🏨 酒店详情</div>
      <div class="links">
        <span @click="toggleFav">{{ favorited ? '★ 已收藏' : '☆ 收藏' }}</span>
        <span @click="go('/hotel')">返回酒店列表</span>
        <span @click="go('/orders')">我的订单</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/hotel" />

      <main class="content">
        <!-- ① 相册：1 张大图 + 4 张小图 -->
        <section class="card album" v-loading="loading">
          <div class="big">
            <el-image v-if="hero" class="big-img" :src="hero" :preview-src-list="previewList" fit="cover" />
            <div v-else class="big ph">🏨</div>
            <span v-if="photos.length" class="badge">共 {{ photos.length }} 张图片</span>
          </div>
          <div class="thumbs">
            <div v-for="(p, i) in thumbs" :key="i" class="thumb" @click="hero = p">
              <img :src="p" alt="" />
            </div>
            <div v-for="i in emptyThumbs" :key="'e' + i" class="thumb ph">🏨</div>
          </div>
        </section>

        <!-- ② 名称 / 地址 / 评分 / 简介 -->
        <section class="card info">
          <div class="i-head">
            <div class="i-left">
              <h1 class="name">{{ name }} <span class="star">{{ starText }}</span></h1>
              <div class="addr">📍 {{ address || city || '地址待补充' }}</div>
              <div class="tags">
                <span v-for="t in tagList" :key="t" class="tag">{{ t }}</span>
              </div>
            </div>
            <div class="i-right">
              <div class="score"><b>{{ score.toFixed(1) }}</b><span>/5</span></div>
              <div class="score-lb">很好</div>
              <div class="cmt">{{ cmtCount }} 条点评</div>
            </div>
          </div>

          <div class="bars">
            <div v-for="b in bars" :key="b.label" class="bar-row">
              <span class="bl">{{ b.label }}</span>
              <span class="bar"><i :style="{ width: (b.v / 5 * 100) + '%' }"></i></span>
              <span class="bv">{{ b.v.toFixed(1) }}</span>
            </div>
          </div>

          <p v-if="intro" class="intro">{{ intro }}</p>
        </section>

        <!-- ③ 选择日期 -->
        <section class="card book-bar">
          <div class="field">
            <label>入住</label>
            <el-date-picker v-model="checkIn" type="date" value-format="YYYY-MM-DD" :disabled-date="disBefore" style="width:100%" />
          </div>
          <div class="nights">{{ nights }} 晚</div>
          <div class="field">
            <label>退房</label>
            <el-date-picker v-model="checkOut" type="date" value-format="YYYY-MM-DD" :disabled-date="disCheckOut" style="width:100%" />
          </div>
          <div class="tip">入住 14:00 后 ｜ 退房 12:00 前</div>
        </section>

        <!-- ④ 房型列表 -->
        <section class="card rooms">
          <div class="sec-title">选择房型</div>
          <div class="room" v-for="r in rooms" :key="r.name">
            <img v-if="r.img" class="r-img" :src="r.img" alt="" />
            <div v-else class="r-img ph">🛏️</div>
            <div class="r-info">
              <div class="r-name">{{ r.name }}</div>
              <div class="r-tags">
                <span v-for="t in r.tags" :key="t" class="rt">{{ t }}</span>
              </div>
              <div class="r-meta">{{ r.bed }} ｜ {{ r.area }}㎡ ｜ 可住 {{ r.guests }} 人</div>
              <div
                v-if="stockMap[r.name] !== undefined"
                class="r-stock"
                :class="{ out: stockMap[r.name] <= 0 }"
              >
                {{ stockMap[r.name] > 0 ? '仅剩 ' + stockMap[r.name] + ' 间' : '该日期已售完' }}
              </div>
            </div>
            <div class="r-price">
              <div class="p1">¥{{ r.price }}<span>/晚</span></div>
              <div class="p2">{{ nights }} 晚合计 ¥{{ r.price * nights }}</div>
            </div>
            <button class="r-btn" :disabled="booking === r.name || stockMap[r.name] === 0" @click="book(r)">
              {{ stockMap[r.name] === 0 ? '已售完' : booking === r.name ? '提交中…' : '预订' }}
            </button>
          </div>
        </section>

        <!-- ⑤ 服务及设施 -->
        <section class="card fac">
          <div class="sec-title">服务及设施</div>
          <div class="fac-grid">
            <div v-for="f in facilities" :key="f.t" class="fac-item">
              <span class="fi">{{ f.i }}</span><span>{{ f.t }}</span>
            </div>
          </div>
          <div class="policy">
            <div><b>入住时间</b>14:00 以后</div>
            <div><b>退房时间</b>12:00 以前</div>
            <div><b>宠物</b>不可携带宠物</div>
            <div><b>吸烟</b>全区域禁烟</div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
/* hotel-detail v5 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const route = useRoute()
const router = useRouter()

const name = route.query.name || ''
const city = route.query.city || ''
const address = route.query.address || ''
const imageParam = route.query.image || ''
const placeId = route.query.id || ''      // 来自推荐库的酒店才有 id，高德实时搜索的没有 → 不卡库存

const rawPrice = String(route.query.price || '').replace(/[^\d]/g, '')
const basePrice = ref(Number(rawPrice) || 328)

const loading = ref(false)
const booking = ref('')
// 幂等键：同一次下单重复提交（连点 / 网络超时重发）后端只会创建一张订单
const newIdem = () => 'idem-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10)
let idemKey = newIdem()
const photos = ref([])
const hero = ref(imageParam)
const intro = ref(route.query.desc || '')
const stockMap = ref({})   // 房型 -> 剩余数量

const previewList = computed(() => (photos.value.length ? photos.value : imageParam ? [imageParam] : []))
const thumbs = computed(() => photos.value.slice(1, 5))
const emptyThumbs = computed(() => Math.max(0, 4 - thumbs.value.length))

const score = computed(() => {
  const s = Number(route.query.rating)
  return s > 0 && s <= 5 ? s : 4.7
})
const seed = (name || '').length % 3
const bars = computed(() => {
  const s = score.value
  const clamp = (v) => Math.min(5, Math.max(3.5, v))
  return [
    { label: '卫生', v: clamp(s + (seed === 0 ? 0.1 : 0.05)) },
    { label: '设施', v: clamp(s - 0.1) },
    { label: '环境', v: clamp(s - 0.05) },
    { label: '服务', v: clamp(s + (seed === 2 ? 0.1 : 0)) },
  ]
})
const cmtCount = computed(() => 168 + (name || '').length * 37)
const starText = computed(() => {
  const r = score.value
  return r >= 4.8 ? '豪华型 ★★★★★' : r >= 4.5 ? '高档型 ★★★★' : '舒适型 ★★★'
})
const tagList = computed(() => {
  const raw = String(route.query.tags || '').trim()
  const arr = raw ? raw.split(/[，,、|/]/).map((s) => s.trim()).filter(Boolean).slice(0, 6) : []
  const def = ['近地铁', '免费WiFi', '行李寄存', '24小时前台', '可开发票', '免费停车']
  return (arr.length ? arr : def).slice(0, 6)
})

const fmt = (d) => {
  const p = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
}
const checkIn = ref(fmt(new Date(Date.now() + 86400000)))
const checkOut = ref(fmt(new Date(Date.now() + 2 * 86400000)))
const nights = computed(() => {
  const a = new Date(checkIn.value).getTime()
  const b = new Date(checkOut.value).getTime()
  const d = Math.round((b - a) / 86400000)
  return d > 0 ? d : 1
})
const disBefore = (d) => d.getTime() < Date.now() - 86400000
const disCheckOut = (d) => d.getTime() <= new Date(checkIn.value).getTime()

/** 查某天各房型剩余库存；没配库存的酒店不显示也不限制 */
const loadStock = async () => {
  if (!placeId) {
    stockMap.value = {}
    return
  }
  try {
    const qs = new URLSearchParams()
    qs.append('placeId', placeId)
    qs.append('date', checkIn.value)
    const resp = await fetch('/api/travel/stock?' + qs.toString(), {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    const m = {}
    if (res.code === 200) {
      ;(res.data || []).forEach((s) => {
        m[s.roomType] = Number(s.left) || 0
      })
    }
    stockMap.value = m
  } catch (e) {
    stockMap.value = {}
  }
}
watch(checkIn, loadStock)

const ROOM_DEFS = [
  { name: '标准大床房', bed: '1张1.8米大床', area: 22, guests: 2, add: 0, tags: ['含早餐', '免费取消', '即时确认'] },
  { name: '高级大床房', bed: '1张2.0米大床', area: 28, guests: 2, add: 80, tags: ['含早餐', '免费取消', '即时确认'] },
  { name: '双床房', bed: '2张1.2米单床', area: 26, guests: 2, add: 40, tags: ['含早餐', '免费取消'] },
  { name: '家庭房', bed: '大床+单床', area: 35, guests: 3, add: 160, tags: ['含早餐', '免费取消', '可加床'] },
]
const rooms = computed(() =>
  ROOM_DEFS.map((r, i) => ({
    ...r,
    price: basePrice.value + r.add,
    img: photos.value[i + 1] || photos.value[0] || imageParam,
  }))
)

const facilities = [
  { i: '📶', t: '免费WiFi' },
  { i: '🅿️', t: '停车场' },
  { i: '🍽️', t: '中餐厅' },
  { i: '🏊', t: '游泳池' },
  { i: '🏋️', t: '健身房' },
  { i: '🧳', t: '行李寄存' },
  { i: '🕐', t: '24小时前台' },
  { i: '🧺', t: '洗衣服务' },
  { i: '🚕', t: '叫车服务' },
]

const go = (p) => router.push(p)

// 收藏 / 取消收藏
const favorited = ref(false)
const loadFavState = async () => {
  try {
    const qs = new URLSearchParams()
    qs.append('type', 'HOTEL')
    qs.append('name', name)
    const resp = await fetch('/api/favorite/check?' + qs.toString(), {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    if (res.code === 200) favorited.value = !!(res.data && res.data.favorited)
  } catch (e) {
    /* 收藏状态拿不到不影响预订 */
  }
}

const toggleFav = async () => {
  try {
    const resp = await fetch('/api/favorite/toggle', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({
        type: 'HOTEL',
        name,
        city,
        address,
        image: hero.value || imageParam,
        price: basePrice.value,
      }),
    })
    const res = await resp.json()
    if (res.code === 200) {
      favorited.value = !!(res.data && res.data.favorited)
      ElMessage.success(favorited.value ? '已加入收藏' : '已取消收藏')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const loadPhotos = async () => {
  if (!name) return
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const qs = new URLSearchParams()
    qs.append('name', name)
    if (city) qs.append('city', city)
    const resp = await fetch('/api/travel/photos?' + qs.toString(), {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    const arr = res.code === 200 ? res.data || [] : []
    photos.value = arr
    if (arr.length) hero.value = arr[0]
  } catch (e) {
    photos.value = []
  } finally {
    loading.value = false
  }
}

const loadIntro = async () => {
  if (intro.value || !name) return
  try {
    const qs = new URLSearchParams()
    qs.append('name', name)
    if (city) qs.append('city', city)
    const resp = await fetch('/api/travel/intro?' + qs.toString(), {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    if (res.code === 200 && res.data) intro.value = res.data
  } catch (e) {
    /* 简介获取失败不影响预订 */
  }
}

const book = async (room) => {
  if (!checkIn.value || !checkOut.value) {
    ElMessage.warning('请选择入住和退房日期')
    return
  }
  if (checkOut.value <= checkIn.value) {
    ElMessage.warning('退房日期要晚于入住日期')
    return
  }
  booking.value = room.name
  const total = room.price * nights.value
  try {
    const resp = await fetch('/api/order/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
        'Idempotency-Key': idemKey,
      },
      body: JSON.stringify({
        type: 'HOTEL',
        fromCity: name,
        toCity: city || '',
        travelDate: checkIn.value,
        departTime: checkOut.value,
        arriveTime: '',
        seat: room.name + ' × ' + nights.value + '晚',
        price: total,
        passenger: localStorage.getItem('username') || '',
        phone: '',
        placeId: placeId ? Number(placeId) : null,
        bizDate: checkIn.value,
        roomType: room.name,
        quantity: 1,
      }),
    })
    const res = await resp.json()
    if (res.code === 200) {
      idemKey = newIdem()
      router.push({
        path: '/pay',
        query: {
          orderId: res.data.id,
          orderNo: res.data.orderNo,
          amount: res.data.price || total,
          type: 'HOTEL',
          info: name + ' ｜ ' + room.name + ' ｜ ' + checkIn.value + ' 入住 · ' + checkOut.value + ' 离店 ｜ ' + nights.value + '晚',
        },
      })
    } else {
      ElMessage.error(res.msg || '预订失败')
    }
  } catch (e) {
    ElMessage.error('预订失败，请检查网络')
  } finally {
    booking.value = ''
  }
}

onMounted(() => {
  if (!name) {
    ElMessage.warning('缺少酒店信息')
    router.push('/hotel')
    return
  }
  loadPhotos()
  loadIntro()
  loadFavState()
  loadStock()
})
</script>

<style scoped>
/* hotel-detail v5 styles */
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links { display: flex; gap: 18px; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.links span:hover { color: #0086f6; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.sec-title { font-size: 17px; font-weight: 700; color: #222; margin-bottom: 12px; }

/* 相册 */
.album { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); gap: 8px; }
.big { position: relative; height: 320px; border-radius: 8px; overflow: hidden; background: #eef1f5; }
.big-img { width: 100%; height: 100%; display: block; }
.big.ph { display: flex; align-items: center; justify-content: center; font-size: 56px; }
.badge { position: absolute; right: 10px; bottom: 10px; background: rgba(0,0,0,.55); color: #fff; font-size: 12px; border-radius: 12px; padding: 3px 10px; }
.thumbs { display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 8px; height: 320px; }
.thumb { border-radius: 8px; overflow: hidden; background: #eef1f5; cursor: pointer; height: 100%; }
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .25s; }
.thumb img:hover { transform: scale(1.06); }
.thumb.ph { display: flex; align-items: center; justify-content: center; font-size: 26px; }

/* 信息 */
.i-head { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; }
.i-left { min-width: 0; }
.name { font-size: 24px; font-weight: 700; color: #1b1b1b; margin: 0 0 8px; }
.star { font-size: 13px; color: #f5a623; font-weight: 500; margin-left: 8px; }
.addr { font-size: 13px; color: #666; margin-bottom: 10px; word-break: break-all; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { font-size: 12px; color: #0086f6; background: #eaf5ff; border-radius: 4px; padding: 2px 8px; }
.i-right { text-align: right; flex-shrink: 0; }
.score b { font-size: 26px; color: #0086f6; }
.score span { font-size: 12px; color: #999; }
.score-lb { font-size: 13px; color: #0086f6; font-weight: 600; }
.cmt { font-size: 12px; color: #999; margin-top: 2px; }
.bars { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 24px; margin-top: 14px; padding-top: 12px; border-top: 1px dashed #eee; }
.bar-row { display: flex; align-items: center; gap: 8px; }
.bl { font-size: 12px; color: #888; width: 30px; }
.bar { flex: 1; height: 6px; background: #eef1f5; border-radius: 3px; overflow: hidden; }
.bar i { display: block; height: 100%; background: linear-gradient(90deg, #29b6f6, #0086f6); border-radius: 3px; }
.bv { font-size: 12px; color: #0086f6; width: 26px; text-align: right; }
.intro { margin: 12px 0 0; font-size: 13px; color: #555; line-height: 1.8; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }

/* 日期 */
.book-bar { display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.field { flex: 1; min-width: 160px; }
.field label { display: block; font-size: 12px; color: #888; margin-bottom: 6px; }
.nights { font-size: 14px; color: #0086f6; font-weight: 600; padding-bottom: 10px; white-space: nowrap; }
.tip { font-size: 12px; color: #999; padding-bottom: 10px; white-space: nowrap; }

/* 房型 */
.room { display: flex; align-items: center; gap: 14px; border: 1px solid #eef0f3; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.room:last-child { margin-bottom: 0; }
.room:hover { border-color: #0086f6; }
.r-img { width: 96px; height: 72px; border-radius: 6px; object-fit: cover; display: block; flex-shrink: 0; }
.r-img.ph { display: flex; align-items: center; justify-content: center; background: #eef1f5; font-size: 26px; }
.r-info { flex: 1; min-width: 0; }
.r-name { font-size: 16px; font-weight: 600; color: #222; }
.r-tags { display: flex; gap: 6px; flex-wrap: wrap; margin: 5px 0; }
.rt { font-size: 12px; color: #0f9d58; background: #eaf7ef; border-radius: 3px; padding: 1px 6px; }
.r-meta { font-size: 12px; color: #888; }
.r-stock { font-size: 12px; color: #e6702b; margin-top: 4px; font-weight: 600; }
.r-stock.out { color: #d93025; }
.r-price { text-align: right; flex-shrink: 0; }
.p1 { font-size: 20px; color: #ff6a00; font-weight: 700; }
.p1 span { font-size: 12px; color: #999; font-weight: 400; }
.p2 { font-size: 12px; color: #999; margin-top: 2px; }
.r-btn { border: 0; background: #ff8a1f; color: #fff; border-radius: 6px; padding: 10px 22px; font-size: 15px; font-weight: 600; cursor: pointer; flex-shrink: 0; }
.r-btn:hover { background: #f57c0b; }
.r-btn:disabled { opacity: .6; cursor: not-allowed; }

/* 设施 */
.fac-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; }
.fac-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #444; background: #f8fafc; border-radius: 6px; padding: 8px 10px; }
.fi { font-size: 15px; }
.policy { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px 16px; margin-top: 14px; padding-top: 12px; border-top: 1px dashed #eee; font-size: 13px; color: #555; }
.policy b { color: #222; margin-right: 8px; font-weight: 600; }
</style>
