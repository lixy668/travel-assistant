<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">💳 收银台</div>
      <div class="links">
        <span @click="go('/orders')">我的订单</span>
        <span @click="go('/home')">返回首页</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/orders" />

      <main class="content">
        <!-- 步骤条 -->
        <div class="card steps">
          <div class="step" :class="{ on: true }"><i>1</i><span>提交订单</span></div>
          <div class="line" :class="{ on: stage !== 'pay' }"></div>
          <div class="step" :class="{ on: stage !== 'pay' }"><i>2</i><span>支付订单</span></div>
          <div class="line" :class="{ on: stage === 'success' }"></div>
          <div class="step" :class="{ on: stage === 'success' }"><i>3</i><span>支付成功</span></div>
        </div>

        <!-- 支付成功 -->
        <div v-if="stage === 'success'" class="card done">
          <div class="ok">✓</div>
          <div class="ok-t">支付成功</div>
          <div class="ok-amt">¥{{ money(amount) }}</div>
          <div class="ok-rows">
            <div class="row"><span class="lb">订单类型</span><span class="vl">{{ typeText }}</span></div>
            <div class="row"><span class="lb">订单号</span><span class="vl">{{ orderNo }}</span></div>
            <div class="row"><span class="lb">交易流水号</span><span class="vl">{{ order.payNo || '—' }}</span></div>
            <div class="row"><span class="lb">支付方式</span><span class="vl">{{ methodName(order.payMethod) }}</span></div>
            <div class="row"><span class="lb">支付时间</span><span class="vl">{{ fmt(order.payTime) }}</span></div>
          </div>
          <div class="ok-tip">本次为毕设演示的<strong>沙箱支付</strong>，未产生真实扣款。</div>
          <div class="ok-btns">
            <el-button type="primary" size="large" @click="go('/orders')">查看我的订单</el-button>
            <el-button size="large" @click="go('/home')">返回首页</el-button>
          </div>
        </div>

        <!-- 收银台 -->
        <div v-else class="pay-grid">
          <div class="card left">
            <div class="sec-title">订单信息</div>
            <div class="row">
              <span class="lb">订单类型</span>
              <span class="vl"><em class="badge">{{ typeText }}</em></span>
            </div>
            <div class="row" v-if="info"><span class="lb">订单内容</span><span class="vl">{{ info }}</span></div>
            <div class="row"><span class="lb">订单号</span><span class="vl">{{ orderNo }}</span></div>
            <div class="row"><span class="lb">下单时间</span><span class="vl">{{ fmt(order.createTime) || '—' }}</span></div>
            <div class="row"><span class="lb">出行日期</span><span class="vl">{{ order.travelDate || '—' }}</span></div>
            <div class="row"><span class="lb">{{ order.type === 'HOTEL' ? '退房日期' : '到达时间' }}</span><span class="vl">{{ order.departTime || order.arriveTime || '—' }}</span></div>
            <div class="row"><span class="lb">房型/座位</span><span class="vl">{{ order.seat || '—' }}</span></div>
            <div class="row"><span class="lb">联系人</span><span class="vl">{{ order.passenger || '—' }}</span></div>

            <div class="split"></div>
            <div class="row"><span class="lb">商品金额</span><span class="vl">¥{{ money(amount) }}</span></div>
            <div class="row"><span class="lb">优惠</span><span class="vl">- ¥0.00</span></div>
            <div class="sum">
              <span>应付金额</span>
              <b>¥{{ money(amount) }}</b>
            </div>
          </div>

          <div class="card right">
            <div class="sec-title">选择支付方式</div>
            <div class="methods">
              <div
                v-for="m in METHODS"
                :key="m.key"
                class="m"
                :class="{ on: method === m.key }"
                @click="method = m.key"
              >
                <span class="m-ico" :style="{ background: m.color }">{{ m.icon }}</span>
                <span class="m-name">{{ m.name }}</span>
                <span class="m-radio"></span>
              </div>
            </div>

            <div class="cd" :class="{ warn: remainMs > 0 && remainMs < 300000, over: expired }">
              <template v-if="expired">订单已超时（30 分钟未支付），请返回订单列表重新下单</template>
              <template v-else>请在 <b>{{ remainText }}</b> 内完成支付，超时订单将自动关闭</template>
            </div>

            <el-button
              type="primary"
              class="pay-btn"
              :loading="stage === 'paying'"
              :disabled="expired"
              @click="doPay"
            >
              立即支付 ¥{{ money(amount) }}
            </el-button>

            <div class="tip">
              <p>🔒 支付安全由平台保障，本页为<strong>沙箱（模拟）支付</strong>：点击支付即成功，不会产生真实扣款。</p>
              <p>完成支付后可在「我的订单」查看支付状态，或申请改期 / 退订（需管理员审核）。</p>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 支付中 -->
    <div v-if="stage === 'paying'" class="mask">
      <div class="spinner"></div>
      <div class="mtxt">正在调起 {{ methodName(method) }}…</div>
      <div class="msub">沙箱环境 · 请勿关闭页面</div>
    </div>
  </div>
</template>

<script setup>
/* pay-cashier v3 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const route = useRoute()
const router = useRouter()

const METHODS = [
  { key: 'ALIPAY', name: '支付宝', icon: '支', color: '#1677ff' },
  { key: 'WECHAT', name: '微信支付', icon: '微', color: '#07c160' },
  { key: 'UNIONPAY', name: '云闪付', icon: '云', color: '#e60012' },
  { key: 'CARD', name: '银行卡', icon: '卡', color: '#7a5cff' },
]
const EXPIRE_MINUTES = 30

const orderId = route.query.orderId || ''
const orderNo = ref(route.query.orderNo || '')
const amount = ref(Number(route.query.amount) || 0)
const type = ref(route.query.type || '')
const info = route.query.info || ''

const order = ref({})
const method = ref('ALIPAY')
const stage = ref('pay') // pay | paying | success
const now = ref(Date.now())
let timer = null

const typeText = computed(() => {
  const t = type.value
  return t === 'FLIGHT' ? '机票' : t === 'TRAIN' ? '火车票' : t === 'HOTEL' ? '酒店' : t === 'SPOT' ? '景点门票' : '订单'
})
const methodName = (k) => (METHODS.find((m) => m.key === k) || METHODS[0]).name
const money = (v) => Number(v || 0).toFixed(2)
const fmt = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t).replace('T', ' ').slice(0, 19)
  const p = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
}
const expireAt = computed(() => {
  const c = order.value.createTime ? new Date(order.value.createTime).getTime() : Date.now()
  return c + EXPIRE_MINUTES * 60 * 1000
})
const remainMs = computed(() => Math.max(0, expireAt.value - now.value))
const expired = computed(() => remainMs.value <= 0)
const remainText = computed(() => {
  const s = Math.floor(remainMs.value / 1000)
  const m = Math.floor(s / 60)
  const ss = s % 60
  return String(m).padStart(2, '0') + ':' + String(ss).padStart(2, '0')
})

const go = (p) => router.push(p)

const load = async () => {
  if (!orderId) return
  try {
    const resp = await fetch('/api/order/' + orderId, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    if (res.code === 200 && res.data) {
      order.value = res.data
      orderNo.value = res.data.orderNo || orderNo.value
      amount.value = Number(res.data.price) || amount.value
      type.value = res.data.type || type.value
      if (res.data.payStatus === 'PAID') stage.value = 'success'
    }
  } catch (e) {
    /* 拉取失败时用 URL 上的参数兜底 */
  }
}

const doPay = () => {
  if (!orderId) {
    ElMessage.warning('缺少订单信息')
    router.push('/orders')
    return
  }
  if (expired.value) {
    ElMessage.warning('订单已超时，请返回订单列表重新下单')
    return
  }
  stage.value = 'paying'
  setTimeout(sendPay, 1000)
}

const sendPay = async () => {
  try {
    const resp = await fetch('/api/order/pay/' + orderId, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ method: method.value }),
    })
    const res = await resp.json()
    if (res.code === 200 && res.data) {
      order.value = res.data
      amount.value = Number(res.data.price) || amount.value
      stage.value = 'success'
    } else {
      ElMessage.error(res.msg || '支付失败')
      stage.value = 'pay'
    }
  } catch (e) {
    ElMessage.error('支付失败，请重试')
    stage.value = 'pay'
  }
}

onMounted(() => {
  if (!orderId) {
    ElMessage.warning('缺少订单信息')
    router.push('/orders')
    return
  }
  load()
  timer = setInterval(() => (now.value = Date.now()), 1000)
})
onUnmounted(() => timer && clearInterval(timer))
</script>

<style scoped>
/* pay-cashier v3 styles */
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links { display: flex; gap: 18px; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.links span:hover { color: #0086f6; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.card { background: #fff; border-radius: 8px; padding: 18px; }
.sec-title { font-size: 16px; font-weight: 700; color: #222; margin-bottom: 14px; }

/* 步骤条 */
.steps { display: flex; align-items: center; gap: 8px; padding: 14px 18px; }
.step { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #999; }
.step i { width: 22px; height: 22px; border-radius: 50%; background: #e6e9ee; color: #999; font-style: normal; font-size: 12px; display: flex; align-items: center; justify-content: center; }
.step.on { color: #0086f6; font-weight: 600; }
.step.on i { background: #0086f6; color: #fff; }
.line { flex: 1; height: 2px; background: #e6e9ee; border-radius: 1px; }
.line.on { background: #0086f6; }

/* 主体两栏 */
.pay-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 14px; align-items: start; }
.row { display: flex; gap: 12px; padding: 9px 0; font-size: 14px; border-bottom: 1px dashed #f0f2f5; }
.row:last-of-type { border-bottom: 0; }
.lb { width: 88px; color: #999; flex-shrink: 0; }
.vl { color: #222; min-width: 0; word-break: break-all; }
.badge { font-style: normal; font-size: 12px; color: #0086f6; background: #eaf5ff; border-radius: 4px; padding: 2px 8px; }
.split { height: 1px; background: #f0f2f5; margin: 10px 0; }
.sum { display: flex; justify-content: space-between; align-items: baseline; margin-top: 10px; padding-top: 12px; border-top: 1px solid #f0f2f5; font-size: 14px; color: #333; }
.sum b { font-size: 26px; color: #ff6a00; }

/* 支付方式 */
.methods { display: flex; flex-direction: column; gap: 10px; }
.m { display: flex; align-items: center; gap: 12px; border: 1px solid #e6e9ee; border-radius: 8px; padding: 12px 14px; cursor: pointer; transition: all .15s; }
.m:hover { border-color: #0086f6; }
.m.on { border-color: #0086f6; background: #f5faff; box-shadow: 0 0 0 2px rgba(0,134,246,.08) inset; }
.m-ico { width: 30px; height: 30px; border-radius: 6px; color: #fff; font-size: 14px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.m-name { font-size: 14px; color: #333; flex: 1; }
.m-radio { width: 16px; height: 16px; border-radius: 50%; border: 1px solid #cfd6e0; position: relative; flex-shrink: 0; }
.m.on .m-radio { border-color: #0086f6; }
.m.on .m-radio::after { content: ''; position: absolute; inset: 3px; border-radius: 50%; background: #0086f6; }

.cd { margin: 16px 0 12px; font-size: 13px; color: #666; background: #f7f9fc; border-radius: 6px; padding: 10px 12px; }
.cd b { color: #ff6a00; font-size: 15px; }
.cd.warn { background: #fff5ec; color: #d2691e; }
.cd.warn b { color: #e64a19; }
.cd.over { background: #fdecec; color: #d93025; }
.pay-btn { width: 100%; height: 46px; font-size: 16px; }
.tip { margin-top: 14px; font-size: 12px; color: #999; line-height: 1.8; }
.tip p { margin: 0 0 6px; }

/* 支付成功 */
.done { text-align: center; padding: 40px 20px; }
.ok { width: 72px; height: 72px; border-radius: 50%; background: #eaf7ef; color: #0f9d58; font-size: 40px; line-height: 72px; margin: 0 auto 14px; }
.ok-t { font-size: 22px; font-weight: 700; color: #222; }
.ok-amt { font-size: 30px; color: #ff6a00; font-weight: 700; margin: 10px 0 22px; }
.ok-rows { max-width: 460px; margin: 0 auto; text-align: left; }
.ok-rows .row { border-bottom: 1px dashed #f0f2f5; }
.ok-tip { margin: 18px 0; font-size: 12px; color: #999; }
.ok-btns { display: flex; gap: 12px; justify-content: center; }

/* 支付中遮罩 */
.mask { position: fixed; inset: 0; background: rgba(255,255,255,.88); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; z-index: 99; }
.spinner { width: 44px; height: 44px; border: 4px solid #e6e9ee; border-top-color: #0086f6; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.mtxt { font-size: 15px; color: #333; font-weight: 600; }
.msub { font-size: 12px; color: #999; }
</style>
