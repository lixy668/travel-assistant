<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">✈️ {{ from }} → {{ to }}</div>
      <div class="links"><span @click="router.back()">返回</span></div>
    </header>

    <div class="wrap">
      <SideNav active="/flight" />
      <main class="content">
        <div class="card">
          <div class="head">
            <span class="title">{{ from }} → {{ to }} 的航班（{{ date }}）</span>
            <el-tag type="warning" effect="light">示例班次</el-tag>
          </div>

          <div class="frow" v-for="(f, i) in flights" :key="i">
            <div class="airline">
              <div class="a-name">{{ f.airline }}</div>
              <div class="a-no">{{ f.no }}</div>
            </div>
            <div class="time">
              <div class="t-dep">{{ f.dep }}</div>
              <div class="t-from">{{ from }}</div>
            </div>
            <div class="dur">
              <div class="d-line">—— {{ f.duration }} ——</div>
              <div class="d-stop">直飞</div>
            </div>
            <div class="time">
              <div class="t-dep">{{ f.arrive }}</div>
              <div class="t-from">{{ to }}</div>
            </div>
            <div class="price">
              <div class="p-num">¥{{ f.price }}</div>
              <div class="p-sub">起</div>
            </div>
            <el-button type="primary" @click="openOrder(f)">预订</el-button>
          </div>
        </div>
      </main>
    </div>

    <!-- 下单弹窗 -->
    <el-dialog v-model="dialog" title="填写订单信息" width="430px">
      <el-form label-width="80px">
        <el-form-item label="航班">{{ current.airline }} {{ current.no }}</el-form-item>
        <el-form-item label="行程">{{ from }} → {{ to }}</el-form-item>
        <el-form-item label="日期">{{ date }} {{ current.dep }} - {{ current.arrive }}</el-form-item>
        <el-form-item label="票价">¥{{ current.price }}</el-form-item>
        <el-form-item label="乘客"><el-input v-model="form.passenger" placeholder="乘客姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="接收订单信息的手机号" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">确认预订</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const route = useRoute()
const router = useRouter()
const from = route.query.from || '上海'
const to = route.query.to || '北京'
const date = route.query.date || new Date().toISOString().slice(0, 10)

const flights = computed(() => {
  const airlines = ['中国国航 CA', '东方航空 MU', '南方航空 CZ', '海南航空 HU', '四川航空 3U', '吉祥航空 HO', '春秋航空 9C', '深圳航空 ZH']
  const deps = ['07:15', '08:40', '10:05', '11:30', '13:20', '15:45', '17:10', '19:35']
  const durMin = [125, 140, 110, 150, 135, 120, 160, 130]
  return deps.map((d, i) => {
    const [h, m] = d.split(':').map(Number)
    const total = h * 60 + m + durMin[i % durMin.length]
    const ah = String(Math.floor(total / 60) % 24).padStart(2, '0')
    const am = String(total % 60).padStart(2, '0')
    return {
      airline: airlines[i % airlines.length],
      no: airlines[i % airlines.length].split(' ')[1] + (1000 + i * 137),
      dep: d,
      arrive: ah + ':' + am,
      duration: Math.floor(durMin[i % durMin.length] / 60) + 'h' + (durMin[i % durMin.length] % 60) + 'm',
      price: 420 + i * 130,
    }
  })
})

const dialog = ref(false)
const submitting = ref(false)
// 幂等键：同一次下单重复提交（连点 / 网络超时重发）后端只会创建一张订单
const newIdem = () => 'idem-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10)
let idemKey = newIdem()
const current = ref({})
const form = reactive({ passenger: '', phone: '' })

const openOrder = (f) => {
  current.value = f
  form.passenger = localStorage.getItem('username') || ''
  form.phone = ''
  dialog.value = true
}

const submitOrder = async () => {
  if (!form.passenger || !form.phone) {
    ElMessage.warning('请填写乘客和手机号')
    return
  }
  submitting.value = true
  try {
    const resp = await fetch('/api/order/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
        'Idempotency-Key': idemKey,
      },
      body: JSON.stringify({
        type: 'FLIGHT',
        fromCity: from,
        toCity: to,
        travelDate: date,
        departTime: current.value.dep,
        arriveTime: current.value.arrive,
        seat: (current.value.airline || '') + ' 经济舱',
        price: current.value.price,
        passenger: form.passenger,
        phone: form.phone,
      }),
    })
    const res = await resp.json()
    if (res.code === 200) {
      idemKey = newIdem()
      dialog.value = false
      router.push({
        path: '/pay',
        query: {
          orderId: res.data.id,
          orderNo: res.data.orderNo,
          amount: res.data.price || 0,
          type: 'FLIGHT',
          info: from + ' → ' + to + ' ｜ ' + date + ' ' + current.value.dep + '-' + current.value.arrive,
        },
      })
    } else {
      ElMessage.error(res.msg || '预订失败')
    }
  } catch (e) {
    ElMessage.error('预订失败')
  } finally {
    submitting.value = false
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
.card { background: #fff; border-radius: 8px; padding: 16px; }
.head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.title { font-size: 16px; font-weight: 700; color: #222; }
.frow { display: flex; align-items: center; gap: 22px; padding: 14px 8px; border-bottom: 1px solid #f2f2f2; }
.frow:last-child { border-bottom: 0; }
.airline { width: 140px; }
.a-name { font-size: 14px; color: #222; }
.a-no { font-size: 12px; color: #999; }
.time { text-align: center; width: 80px; }
.t-dep { font-size: 19px; font-weight: 700; color: #222; }
.t-from { font-size: 12px; color: #999; }
.dur { text-align: center; color: #bbb; font-size: 12px; }
.price { margin-left: auto; text-align: right; }
.p-num { font-size: 19px; font-weight: 700; color: #ff6a00; }
.p-sub { font-size: 12px; color: #999; }
</style>
