<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">📋 我的订单</div>
      <div class="links">
        <span @click="openPwd">修改密码</span>
        <span @click="go('/home')">返回首页</span>
      </div>
    </header>

    <div class="wrap">
      <SideNav active="/orders" />
      <main class="content">
        <div class="card">
          <div class="sec-head">
            <span class="sec-title">我的订单</span>
            <el-button size="small" @click="load">刷新</el-button>
          </div>

          <el-table :data="list" v-loading="loading" stripe style="width:100%">
            <el-table-column label="下单时间" width="165">
              <template #default="{ row }">{{ fmt(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="85">
              <template #default="{ row }">
                <el-tag :type="row.type === 'FLIGHT' ? 'primary' : row.type === 'HOTEL' ? 'warning' : row.type === 'SPOT' ? 'danger' : 'success'" effect="light">
                  {{ row.type === 'FLIGHT' ? '机票' : row.type === 'HOTEL' ? '酒店' : row.type === 'SPOT' ? '景点' : '车票' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="orderNo" label="订单号" width="175" />
            <el-table-column label="行程" width="150">
              <template #default="{ row }">
                {{ row.type === 'HOTEL' || row.type === 'SPOT' ? (row.city || row.fromCity || row.toCity) : row.fromCity + ' → ' + row.toCity }}
              </template>
            </el-table-column>
            <el-table-column prop="travelDate" label="日期" width="110" />
            <el-table-column label="时间" width="125">
              <template #default="{ row }">
                {{ row.type === 'HOTEL' ? '退房 ' + (row.departTime || '—') : row.type === 'SPOT' ? '—' : row.departTime + ' - ' + row.arriveTime }}
              </template>
            </el-table-column>
            <el-table-column prop="seat" label="座位/房型" min-width="130" />
            <el-table-column label="价格" width="90">
              <template #default="{ row }"><span class="price">¥{{ row.price }}</span></template>
            </el-table-column>
            <el-table-column label="订单状态" width="95">
              <template #default="{ row }">
                <el-tag :type="row.status === '已取消' ? 'info' : 'success'" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="支付状态" width="130">
              <template #default="{ row }">
                <el-tag :type="row.payStatus === 'PAID' ? 'success' : 'danger'" effect="light">
                  {{ row.payStatus === 'PAID' ? '已支付' : '未支付' }}
                </el-tag>
                <div v-if="row.refundStatus === 'REFUNDED'" class="p-refund">已退款 {{ fmtShort(row.refundTime) }}</div>
                <div v-else-if="row.payStatus === 'PAID'" class="p-time">{{ fmtShort(row.payTime) }}</div>
                <div v-else-if="row.status !== '已取消'" class="p-cd" :class="{ over: isExpired(row) }">
                  {{ isExpired(row) ? '支付已超时' : '剩 ' + remainText(row) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="申请状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.changeStatus === 'PENDING'" type="warning" effect="light">
                  {{ row.requestType === 'CANCEL' ? '退订审核中' : '改签审核中' }}
                </el-tag>
                <el-tag v-else-if="row.changeStatus === 'APPROVED'" type="success" effect="light">已通过</el-tag>
                <el-tag v-else-if="row.changeStatus === 'REJECTED'" type="danger" effect="light">已驳回</el-tag>
                <span v-else class="none">—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="370" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.payStatus !== 'PAID' && row.status !== '已取消'"
                  size="small"
                  type="success"
                  @click="goPay(row)"
                >去支付</el-button>
                <el-button
                  v-else-if="row.payStatus === 'PAID'"
                  size="small"
                  @click="goPay(row)"
                >详情</el-button>
                <el-button
                  v-if="row.payStatus === 'PAID' && row.refundStatus !== 'REFUNDED'"
                  size="small"
                  @click="openInvoice(row)"
                >开发票</el-button>
                <el-button
                  size="small"
                  type="primary"
                  :disabled="row.changeStatus === 'PENDING' || row.payStatus !== 'PAID'"
                  :title="row.payStatus !== 'PAID' ? '未支付的订单请先支付，或直接申请退订' : ''"
                  @click="openChange(row)"
                >{{ row.type === 'HOTEL' || row.type === 'SPOT' ? '申请改期' : '申请改签' }}</el-button>
                <el-button
                  size="small"
                  type="danger"
                  :disabled="row.status === '已取消' || row.changeStatus === 'PENDING'"
                  @click="cancelOrder(row)"
                >申请退订</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && !list.length" description="暂无订单，去「机票 / 火车票」预订吧" />
        </div>
      </main>
    </div>

    <!-- 申请改签 -->
    <el-dialog v-model="dialog" :title="(form.type === 'HOTEL' || form.type === 'SPOT' ? '申请改期' : '申请改签') + '（提交后等待管理员审核）'" width="450px">
      <el-form label-width="90px">
        <el-form-item label="订单号">{{ form.orderNo }}</el-form-item>
        <el-form-item label="现信息">
          <span v-if="form.type === 'HOTEL'">入住 {{ form.travelDate }} ／ 退房 {{ form.departTime }}</span>
          <span v-else-if="form.type === 'SPOT'">游玩日期 {{ form.travelDate }} ｜ {{ form.seat }}</span>
          <span v-else>{{ form.fromCity }} → {{ form.toCity }} ｜ {{ form.travelDate }} {{ form.departTime }}</span>
        </el-form-item>

        <template v-if="form.type === 'HOTEL'">
          <el-form-item label="新入住日期"><el-input v-model="form.travelDate" placeholder="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="新退房日期"><el-input v-model="form.departTime" placeholder="YYYY-MM-DD" /></el-form-item>
        </template>
        <template v-else-if="form.type === 'SPOT'">
          <el-form-item label="新游玩日期"><el-input v-model="form.travelDate" placeholder="YYYY-MM-DD" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="新出发地"><el-input v-model="form.fromCity" /></el-form-item>
          <el-form-item label="新目的地"><el-input v-model="form.toCity" /></el-form-item>
          <el-form-item label="新出行日期"><el-input v-model="form.travelDate" placeholder="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="新出发时间"><el-input v-model="form.departTime" placeholder="HH:mm" /></el-form-item>
          <el-form-item label="新到达时间"><el-input v-model="form.arriveTime" placeholder="HH:mm" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitChange">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdDialog" title="修改密码（改完需要重新登录）" width="420px">
      <el-form label-width="90px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再输一遍新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="submitPwd">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 申请发票 -->
    <el-dialog v-model="invDialog" title="申请电子发票（管理员开具后通知你）" width="460px">
      <el-form label-width="90px">
        <el-form-item label="订单号">{{ invForm.orderNo }}</el-form-item>
        <el-form-item label="开票金额">¥{{ invForm.amount }}</el-form-item>
        <el-form-item label="发票抬头">
          <el-input v-model="invForm.title" placeholder="个人姓名或公司名称" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="invForm.taxNo" placeholder="公司抬头才需要填" />
        </el-form-item>
        <el-form-item label="接收邮箱">
          <el-input v-model="invForm.email" placeholder="用于接收电子发票" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invDialog = false">取消</el-button>
        <el-button type="primary" :loading="invSubmitting" @click="submitInvoice">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const dialog = ref(false)
const submitting = ref(false)
const now = ref(Date.now())
const EXPIRE_MIN = 30
let timer = null

const pwdDialog = ref(false)
const pwdSubmitting = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const invDialog = ref(false)
const invSubmitting = ref(false)
const invForm = reactive({ orderId: null, orderNo: '', amount: 0, title: '', taxNo: '', email: '' })

const form = reactive({
  id: null, orderNo: '', type: '', oldFrom: '', oldTo: '',
  fromCity: '', toCity: '', travelDate: '', departTime: '', arriveTime: '', seat: '',
})

const go = (p) => router.push(p)

const fmt = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t).replace('T', ' ').slice(0, 19)
  const p = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
}

const fmtShort = (t) => {
  const s = fmt(t)
  return s ? s.slice(5, 16) : ''
}

// 与后端口径一致：30 分钟未支付自动关单
const remainMs = (row) => {
  const c = row.createTime ? new Date(row.createTime).getTime() : Date.now()
  return Math.max(0, c + EXPIRE_MIN * 60000 - now.value)
}
const isExpired = (row) => remainMs(row) <= 0
const remainText = (row) => {
  const s = Math.floor(remainMs(row) / 1000)
  return String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0')
}

const load = async () => {
  loading.value = true
  try {
    const resp = await fetch('/api/order/my', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return
    }
    const res = await resp.json()
    if (res.code === 200) list.value = res.data || []
    else ElMessage.error(res.msg || '获取订单失败')
  } catch (e) {
    ElMessage.error('获取订单失败')
  } finally {
    loading.value = false
  }
}

const openChange = (row) => {
  form.id = row.id
  form.orderNo = row.orderNo
  form.type = row.type
  form.oldFrom = row.fromCity
  form.oldTo = row.toCity
  form.fromCity = row.fromCity
  form.toCity = row.toCity
  form.travelDate = row.travelDate
  form.departTime = row.departTime
  form.arriveTime = row.arriveTime
  form.seat = row.seat
  dialog.value = true
}

const cancelOrder = (row) => {
  ElMessageBox.confirm(`确认提交退订申请？订单 ${row.orderNo}（${row.fromCity} → ${row.toCity}）将等待管理员审核。`, '退订申请', {
    type: 'warning',
  })
    .then(async () => {
      try {
        const resp = await fetch('/api/order/cancel/' + row.id, {
          method: 'POST',
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
        })
        const res = await resp.json()
        if (res.code === 200) {
          ElMessage.success('已提交退订申请，等待管理员审核')
          load()
        } else {
          ElMessage.error(res.msg || '提交失败')
        }
      } catch (e) {
        ElMessage.error('提交失败')
      }
    })
    .catch(() => {})
}

// 去支付
const goPay = (row) => {
  router.push({
    path: '/pay',
    query: {
      orderId: row.id,
      orderNo: row.orderNo,
      amount: row.price,
      type: row.type,
      info:
        row.type === 'HOTEL'
          ? row.fromCity + ' ｜ ' + row.seat + ' ｜ ' + row.travelDate + ' ~ ' + row.departTime
          : row.fromCity + ' → ' + row.toCity + ' ｜ ' + row.travelDate + ' ' + row.departTime,
    },
  })
}

const openPwd = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
  pwdDialog.value = true
}

const openInvoice = (row) => {
  invForm.orderId = row.id
  invForm.orderNo = row.orderNo
  invForm.amount = row.price
  invForm.title = localStorage.getItem('username') || ''
  invForm.taxNo = ''
  invForm.email = ''
  invDialog.value = true
}

const submitInvoice = async () => {
  if (!invForm.title) {
    ElMessage.warning('请填写发票抬头')
    return
  }
  invSubmitting.value = true
  try {
    const resp = await fetch('/api/invoice/apply', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({
        orderId: invForm.orderId,
        title: invForm.title,
        taxNo: invForm.taxNo,
        email: invForm.email,
      }),
    })
    const res = await resp.json()
    if (res.code === 200) {
      invDialog.value = false
      ElMessage.success('发票申请已提交，可在「消息中心」查看进度')
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    invSubmitting.value = false
  }
}

const submitPwd = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdSubmitting.value = true
  try {
    const resp = await fetch('/api/auth/change-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword }),
    })
    const res = await resp.json()
    if (res.code === 200) {
      pwdDialog.value = false
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (e) {
    ElMessage.error('修改失败，请检查网络')
  } finally {
    pwdSubmitting.value = false
  }
}

const submitChange = async () => {
  submitting.value = true
  try {
    const resp = await fetch('/api/order/change/' + form.id, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({
        fromCity: form.fromCity,
        toCity: form.toCity,
        travelDate: form.travelDate,
        departTime: form.departTime,
        arriveTime: form.arriveTime,
      }),
    })
    const res = await resp.json()
    if (res.code === 200) {
      ElMessage.success('已提交改签申请，等待管理员审核')
      dialog.value = false
      load()
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  load()
  timer = setInterval(() => (now.value = Date.now()), 1000)
})
onUnmounted(() => timer && clearInterval(timer))
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.sec-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.sec-title { font-size: 17px; font-weight: 700; color: #222; }
.price { color: #ff6a00; font-weight: 600; }
.none { color: #bbb; }
.p-time { font-size: 11px; color: #999; margin-top: 3px; }
.p-refund { font-size: 11px; color: #0f9d58; margin-top: 3px; }
.p-cd { font-size: 11px; color: #e6702b; margin-top: 3px; }
.p-cd.over { color: #d93025; }
</style>
