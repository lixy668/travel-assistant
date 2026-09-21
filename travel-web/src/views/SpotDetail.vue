<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🎒 景点详情</div>
      <div class="links">
        <span @click="toggleFav">{{ favorited ? '★ 已收藏' : '☆ 收藏' }}</span>
        <span @click="router.back()">返回</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/travel" />

      <main class="content">
        <div class="card">
          <!-- 名字 -->
          <div class="hd-name">{{ name }}</div>

          <!-- 相册：1 张大图 + 4 张小图（高德实景图，点击可放大） -->
          <div class="gallery">
            <div class="big">
              <el-image
                v-if="hero"
                class="big-img"
                :src="hero"
                :preview-src-list="photos.length ? photos : [hero]"
                fit="cover"
              />
              <div v-else class="big grad">🎒</div>
            </div>
            <div class="thumbs">
              <div v-for="(p, i) in thumbs" :key="i" class="thumb" @click="hero = p">
                <img :src="p" alt="" />
              </div>
              <div v-for="i in emptyThumbs" :key="'e' + i" class="thumb ph">🎒</div>
            </div>
          </div>
        </div>

        <!-- 往下：介绍和其他信息 -->
        <div class="card">
          <div class="sec-title">介绍</div>
          <div class="info" v-if="price"><span class="lb">参考票价</span><span class="vl">¥{{ price }}（本页为景点介绍，购票请以景区官方渠道为准）</span></div>
          <div class="info" v-if="address"><span class="lb">地址</span><span class="vl">{{ address }}</span></div>
          <div class="info" v-if="type"><span class="lb">分类</span><span class="vl">{{ type }}</span></div>
          <div class="intro" v-if="intro">{{ intro }}</div>
          <div class="intro muted" v-else>{{ introLoading ? '正在生成介绍…' : '暂无介绍' }}</div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const route = useRoute()
const router = useRouter()

const name = route.query.name || ''
const city = route.query.city || ''
const image = route.query.image || ''
const price = route.query.price || ''
const address = route.query.address || ''
const type = route.query.type || ''
const placeId = route.query.id || ''
const intro = ref('')
const introLoading = ref(false)

// 收藏 / 取消收藏
const favorited = ref(false)
const loadFavState = async () => {
  try {
    const qs = new URLSearchParams()
    qs.append('type', 'SPOT')
    qs.append('name', name)
    const resp = await fetch('/api/favorite/check?' + qs.toString(), {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    if (res.code === 200) favorited.value = !!(res.data && res.data.favorited)
  } catch (e) {
    /* 忽略 */
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
      body: JSON.stringify({ type: 'SPOT', name, city, address, image, price: Number(price) || 0 }),
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

// 高德实景图相册
const photos = ref([])
const hero = ref(image)
const thumbs = computed(() => photos.value.slice(1, 5))
const emptyThumbs = computed(() => (hero.value ? 0 : 4))

const loadPhotos = async () => {
  try {
    const qs = new URLSearchParams()
    qs.append('name', name)
    if (city) qs.append('city', city)
    const resp = await fetch('/api/travel/photos?' + qs.toString(), {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const res = await resp.json()
    const arr = res.code === 200 ? res.data || [] : []
    photos.value = arr
    if (arr.length) hero.value = arr[0]
  } catch (e) {
    photos.value = []
  }
}

onMounted(() => {
  if (!name) {
    ElMessage.warning('缺少景点信息')
    router.push('/travel')
    return
  }
  loadIntro()
  loadFavState()
  loadPhotos()
})

// AI 生成景点简介
const loadIntro = async () => {
  introLoading.value = true
  try {
    const resp = await fetch(
      '/api/travel/intro?name=' + encodeURIComponent(name) + '&city=' + encodeURIComponent(city),
      { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } },
    )
    const res = await resp.json()
    if (res.code === 200 && res.data) intro.value = res.data
  } catch (e) {
    /* 忽略 */
  } finally {
    introLoading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; }
.card { background: #fff; border-radius: 8px; padding: 20px; }
.hd-name { font-size: 22px; font-weight: 700; color: #222; margin-bottom: 14px; }
.gallery { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); gap: 8px; }
.big { height: 300px; border-radius: 8px; overflow: hidden; background: #eef1f5; }
.big-img { width: 100%; height: 100%; display: block; }
.big.grad { display: flex; align-items: center; justify-content: center; font-size: 56px; background: linear-gradient(135deg, #4a90d9, #2c6fb5); }
.thumbs { display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 8px; height: 300px; }
.thumb { border-radius: 8px; overflow: hidden; background: #eef1f5; cursor: pointer; height: 100%; }
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .25s; }
.thumb img:hover { transform: scale(1.06); }
.thumb.ph { display: flex; align-items: center; justify-content: center; font-size: 26px; }
.hd-img { width: 100%; max-height: 360px; object-fit: cover; border-radius: 8px; display: block; }
.hd-img.grad { height: 320px; background: linear-gradient(135deg, #5aa87a, #3d8a5f); display: flex; align-items: center; justify-content: center; font-size: 64px; }
.book-row { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; margin-top: 18px; }
.field { flex: 1; min-width: 150px; }
.field label { display: block; font-size: 12px; color: #888; margin-bottom: 6px; }
.go { border: 0; background: #ff8a1f; color: #fff; border-radius: 6px; padding: 10px 30px; font-size: 16px; font-weight: 600; cursor: pointer; white-space: nowrap; }
.go:hover { background: #f57c0b; }
.go:disabled { opacity: .6; cursor: not-allowed; }
.sec-title { font-size: 17px; font-weight: 700; color: #222; margin-bottom: 12px; }
.info { display: flex; gap: 12px; padding: 8px 0; font-size: 14px; }
.info .lb { width: 56px; color: #999; flex-shrink: 0; }
.info .vl { color: #333; }
.intro { margin-top: 12px; font-size: 14px; color: #444; line-height: 1.9; }
.intro.muted { color: #aaa; }
</style>
