<template>
  <div>
    <div class="toolbar">
      <span class="title">{{ title }}</span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width:100%">
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column label="行程" width="150">
        <template #default="{ row }">{{ row.fromCity }} → {{ row.toCity }}</template>
      </el-table-column>
      <el-table-column prop="travelDate" label="出行日期" width="115" />
      <el-table-column label="时间" width="130">
        <template #default="{ row }">{{ row.departTime }} - {{ row.arriveTime }}</template>
      </el-table-column>
      <el-table-column prop="seat" label="座位/舱位" min-width="140" />
      <el-table-column label="价格" width="90">
        <template #default="{ row }"><span class="price">¥{{ row.price }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '已取消' ? 'info' : 'success'" effect="light">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付" width="120">
        <template #default="{ row }">
          <el-tag :type="row.payStatus === 'PAID' ? 'success' : 'danger'" effect="light">
            {{ row.payStatus === 'PAID' ? '已支付' : '未支付' }}
          </el-tag>
          <div v-if="row.refundStatus === 'REFUNDED'" class="refund">已退款</div>
          <div v-else-if="row.payStatus === 'PAID' && row.payMethod" class="payway">{{ row.payMethod }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !list.length" description="暂无订单" />

    <el-dialog v-model="dialog" title="修改订单" width="460px">
      <el-form label-width="90px">
        <el-form-item label="出发地"><el-input v-model="form.fromCity" /></el-form-item>
        <el-form-item label="目的地"><el-input v-model="form.toCity" /></el-form-item>
        <el-form-item label="出行日期"><el-input v-model="form.travelDate" placeholder="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="出发时间"><el-input v-model="form.departTime" placeholder="HH:mm" /></el-form-item>
        <el-form-item label="到达时间"><el-input v-model="form.arriveTime" placeholder="HH:mm" /></el-form-item>
        <el-form-item label="座位/舱位"><el-input v-model="form.seat" /></el-form-item>
        <el-form-item label="价格"><el-input v-model="form.price" type="number" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="已预订" value="已预订" />
            <el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  type: { type: String, required: true },
  title: { type: String, default: '订单管理' },
})

const token = () => localStorage.getItem('adminToken') || ''
const list = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const form = reactive({
  id: null, fromCity: '', toCity: '', travelDate: '',
  departTime: '', arriveTime: '', seat: '', price: 0, status: '已预订',
})

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
    const resp = await fetch('/api/admin/orders?type=' + props.type, {
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
    ElMessage.error('获取订单失败')
  } finally {
    loading.value = false
  }
}

const openEdit = (row) => {
  Object.assign(form, row)
  dialog.value = true
}

const save = async () => {
  saving.value = true
  try {
    const resp = await fetch('/api/admin/orders/' + form.id, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token()}` },
      body: JSON.stringify(form),
    })
    const res = await resp.json()
    if (res.code === 200) {
      ElMessage.success('修改成功')
      dialog.value = false
      load()
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (e) {
    ElMessage.error('修改失败')
  } finally {
    saving.value = false
  }
}

const remove = (row) => {
  ElMessageBox.confirm(`确认删除订单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      const resp = await fetch('/api/admin/orders/' + row.id, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token()}` },
      })
      const res = await resp.json()
      if (res.code === 200) {
        ElMessage.success('已删除')
        load()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
    .catch(() => {})
}

watch(() => props.type, load)
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.title { font-size: 17px; font-weight: 700; color: #222; }
.price { color: #ff6a00; font-weight: 600; }
.refund { font-size: 11px; color: #0f9d58; margin-top: 3px; }
.payway { font-size: 11px; color: #999; margin-top: 3px; }
</style>
