<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🚄 {{ from }} → {{ to }}</div>
      <div class="links"><span @click="router.back()">返回</span></div>
    </header>

    <div class="wrap">
      <SideNav active="/train" />
      <main class="content">
        <div class="card">
          <div class="head">
            <span class="title">{{ from }} → {{ to }} 车次（{{ date }}）</span>
            <el-tag type="warning" effect="light">示例车次</el-tag>
          </div>

          <div class="trow" v-for="(t, i) in trains" :key="i">
            <div class="no">
              <span class="c-no">{{ t.no }}</span>
              <span class="c-type">{{ t.type }}</span>
            </div>
            <div class="time">
              <div class="t-big">{{ t.dep }}</div>
              <div class="t-small">{{ from }}</div>
            </div>
            <div class="dur"><div class="d-line">—— {{ t.duration }} ——</div></div>
            <div class="time">
              <div class="t-big">{{ t.arrive }}</div>
              <div class="t-small">{{ to }}</div>
            </div>
            <div class="seats">
              <div class="seat">二等座 ¥{{ t.price2 }} <em :class="t.has2 ? 'ok' : 'no'">{{ t.has2 ? '有票' : '无票' }}</em></div>
              <div class="seat">一等座 ¥{{ t.price1 }} <em :class="t.has1 ? 'ok' : 'no'">{{ t.has1 ? '有票' : '无票' }}</em></div>
            </div>
            <el-button type="primary" :disabled="!t.has2" @click="openOrder(t, '二等座', t.price2)">预订</el-button>
          </div>
        </div>
      </main>
    </div>

    <el-dialog v-model="dialog" title="填写订单信息" width="430px">
      <el-form label-width="80px">
        <el-form-item label="车次">{{ current.no }}（{{ current.type }}）</el-form-item>
        <el-form-item label="行程">{{ from }} → {{ to }}</el-form-item>
        <el-form-item label="日期">{{ date }} {{ current.dep }} - {{ current.arrive }}</el-form-item>
        <el-form-item label="座位">{{ seat }} ¥{{ price }}</el-form-item>
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

const trains = computed(() => {
  const plan = [
    { pre: 'G', type: '高铁', dep: '07:00', min: 268, p2: 553, p1: 933 },
    { pre: 'G', type: '高铁', dep: '08:05', min: 276, p2: 553, p1: 933 },
    { pre: 'D', type: '动车', dep: '09:12', min: 345, p2: 396, p1: 634 },
    { pre: 'G', type: '高铁', dep: '10:30', min: 262, p2: 553, p1: 933 },
    { pre: 'G', type: '高铁', dep: '13:00', min: 270, p2: 553, p1: 933 },
    { pre: 'D', type: '动车', dep: '15:20', min: 350, p2: 396, p1: 634 },
    { pre: 'G', type: '高铁', dep: '18:00', min: 265, p2: 553, p1: 933 },
    { pre: 'K', type: '普快', dep: '21:15', min: 720, p2: 156, p1: 246 },
  ]
  return plan.map((p, i) => {
    const [h, m] = p.dep.split(':').map(Number)
    const total = h * 60 + m + p.min
    const ah = String(Math.floor(total / 60) % 24).padStart(2, '0')
    const am = String(total % 60).padStart(2, '0')
    return {
      no: p.pre + (200 + i * 7),
      type: p.type,
      dep: p.dep,
      arrive: ah + ':' + am,
      duration: Math.floor(p.min / 60) + 'h' + (p.min % 60) + 'm',
      price2: p.p2,
      price1: p.p1,
      has2: i % 3 !== 0,
      has1: i % 2 === 0,
    }
  })
})

const dialog = ref(false)
const submitting = ref(false)
const current = ref({})
const seat = ref('二等座')
const price = ref(0)
const form = reactive({ passenger: '', phone: '' })
// 幂等键：同一次下单重复提交（连点 / 网络超时重发）后端只会创建一张订单
const newIdem = () => 'idem-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10)
let idemKey = newIdem()

const openOrder = (t, s, p) => {
  current.value = t
  seat.value = s
  price.value = p
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
        type: 'TRAIN',
        fromCity: from,
        toCity: to,
        travelDate: date,
        departTime: current.value.dep,
        arriveTime: current.value.arrive,
        seat: current.value.no + ' ' + seat.value,
        price: price.value,
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
          type: 'TRAIN',
          info: from + ' → ' + to + ' ｜ ' + date + ' ' + current.value.no + ' ' + seat.value,
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
.trow { display: flex; align-items: center; gap: 24px; padding: 14px 8px; border-bottom: 1px solid #f2f2f2; }
.trow:last-child { border-bottom: 0; }
.no { width: 110px; }
.c-no { font-size: 18px; font-weight: 700; color: #0086f6; }
.c-type { font-size: 12px; color: #999; margin-left: 6px; }
.time { text-align: center; width: 78px; }
.t-big { font-size: 18px; font-weight: 700; color: #222; }
.t-small { font-size: 12px; color: #999; }
.dur { color: #bbb; font-size: 12px; }
.seats { margin-left: auto; display: flex; gap: 16px; }
.seat { font-size: 13px; color: #333; }
.seat em { font-style: normal; font-size: 12px; margin-left: 4px; }
.seat em.ok { color: #52c41a; }
.seat em.no { color: #bbb; }
</style>
