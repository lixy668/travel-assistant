<template>
  <div>
    <div class="toolbar">
      <span class="title">待处理的申请</span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width:100%">
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ fmt(row.changeApplyTime) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="110" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'FLIGHT' ? 'primary' : row.type === 'HOTEL' ? 'warning' : row.type === 'SPOT' ? 'danger' : 'success'" effect="light">
            {{ row.type === 'FLIGHT' ? '机票' : row.type === 'HOTEL' ? '酒店' : row.type === 'SPOT' ? '景点' : '车票' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.requestType === 'CANCEL' ? 'danger' : 'warning'" effect="dark">
            {{ row.requestType === 'CANCEL' ? '退订' : '改签' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="订单号" width="175" />
      <el-table-column label="原信息" min-width="230">
        <template #default="{ row }">
          <template v-if="row.type === 'HOTEL'">
            <div class="line">入住 {{ row.travelDate }} ／ 退房 {{ row.departTime }}</div>
            <div class="sub">{{ row.fromCity }} ｜ {{ row.seat }}</div>
          </template>
          <template v-else-if="row.type === 'SPOT'">
            <div class="line">游玩 {{ row.travelDate }}</div>
            <div class="sub">{{ row.fromCity }} ｜ {{ row.seat }}</div>
          </template>
          <template v-else>
            <div class="line">{{ row.fromCity }} → {{ row.toCity }}</div>
            <div class="sub">{{ row.travelDate }} {{ row.departTime }} - {{ row.arriveTime }}</div>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="申请内容" min-width="230">
        <template #default="{ row }">
          <template v-if="row.requestType === 'CANCEL'">
            <div class="line cancel">申请退订该订单</div>
            <div class="sub">通过后订单取消，已支付的钱走沙箱退款并写退款流水</div>
          </template>
          <template v-else-if="row.type === 'HOTEL'">
            <div class="line new">入住 {{ row.reqTravelDate }} ／ 退房 {{ row.reqDepartTime }}</div>
          </template>
          <template v-else-if="row.type === 'SPOT'">
            <div class="line new">游玩日期 {{ row.reqTravelDate }}</div>
          </template>
          <template v-else>
            <div class="line new">{{ row.reqFromCity }} → {{ row.reqToCity }}</div>
            <div class="sub">{{ row.reqTravelDate }} {{ row.reqDepartTime }} - {{ row.reqArriveTime }}</div>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="approve(row)">通过</el-button>
          <el-button size="small" type="danger" @click="reject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !list.length" description="暂无待处理申请" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const token = () => localStorage.getItem('adminToken') || ''
const list = ref([])
const loading = ref(false)

const fmt = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t).replace('T', ' ').slice(0, 19)
  const p = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
}

const load = async () => {
  loading.value = true
  try {
    const resp = await fetch('/api/admin/order-changes', {
      headers: { Authorization: `Bearer ${token()}` },
    })
    if (resp.status === 401 || resp.status === 403) {
      ElMessage.error('登录过期或无权限')
      return
    }
    const res = await resp.json()
    if (res.code === 200) list.value = res.data || []
    else ElMessage.error(res.msg || '获取失败')
  } catch (e) {
    ElMessage.error('获取申请失败')
  } finally {
    loading.value = false
  }
}

const act = async (row, action) => {
  const resp = await fetch(`/api/admin/order-changes/${row.id}/${action}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token()}` },
  })
  const res = await resp.json()
  if (res.code === 200) {
    ElMessage.success(action === 'approve' ? '已通过，订单已更新' : '已驳回')
    load()
  } else {
    ElMessage.error(res.msg || '操作失败')
  }
}

const approve = (row) => {
  const msg = row.requestType === 'CANCEL'
    ? `通过该退订申请？订单 ${row.orderNo} 将被取消。`
    : `通过该改签申请？订单 ${row.orderNo} 将按申请内容更新。`
  ElMessageBox.confirm(msg, '确认', { type: 'warning' })
    .then(() => act(row, 'approve'))
    .catch(() => {})
}

const reject = (row) => {
  ElMessageBox.confirm(`驳回该${row.requestType === 'CANCEL' ? '退订' : '改签'}申请？`, '确认', { type: 'warning' })
    .then(() => act(row, 'reject'))
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.title { font-size: 17px; font-weight: 700; color: #222; }
.line { font-size: 14px; color: #333; }
.line.new { color: #0086f6; font-weight: 600; }
.line.cancel { color: #f56c6c; font-weight: 600; }
.sub { font-size: 12px; color: #999; margin-top: 2px; }
</style>
