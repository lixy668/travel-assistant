<template>
  <div>
    <div class="toolbar">
      <span class="title">发票申请</span>
      <div>
        <el-select v-model="status" size="small" style="width: 130px; margin-right: 8px" @change="load">
          <el-option label="全部" value="" />
          <el-option label="待开具" value="PENDING" />
          <el-option label="已开具" value="ISSUED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="申请时间" width="165">
        <template #default="{ row }">{{ fmt(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="110" />
      <el-table-column prop="orderNo" label="订单号" width="175" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><span class="price">¥{{ row.amount }}</span></template>
      </el-table-column>
      <el-table-column prop="title" label="发票抬头" min-width="160" />
      <el-table-column prop="taxNo" label="税号" width="180" />
      <el-table-column prop="email" label="接收邮箱" width="170" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ISSUED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" effect="light">
            {{ row.status === 'ISSUED' ? '已开具' : row.status === 'REJECTED' ? '已驳回' : '待开具' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="act(row, 'issue')">开票</el-button>
            <el-button size="small" type="danger" @click="act(row, 'reject')">驳回</el-button>
          </template>
          <span v-else class="done">{{ fmt(row.issueTime) || '已处理' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !list.length" description="暂无发票申请" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const token = () => localStorage.getItem('adminToken') || ''
const list = ref([])
const loading = ref(false)
const status = ref('')

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const load = async () => {
  loading.value = true
  try {
    const resp = await fetch('/api/admin/invoices' + (status.value ? '?status=' + status.value : ''), {
      headers: { Authorization: `Bearer ${token()}` },
    })
    if (resp.status === 401 || resp.status === 403) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }
    const res = await resp.json()
    if (res.code === 200) list.value = res.data || []
    else ElMessage.error(res.msg || '加载失败')
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const act = (row, action) => {
  const label = action === 'issue' ? '开具' : '驳回'
  ElMessageBox.confirm(`确认${label}订单 ${row.orderNo} 的发票申请（¥${row.amount}）？`, label + '发票', { type: 'warning' })
    .then(async () => {
      const resp = await fetch('/api/admin/invoices/' + row.id + '/' + action, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token()}` },
      })
      const res = await resp.json()
      if (res.code === 200) {
        ElMessage.success(`${label}成功，已通知用户`)
        load()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.title { font-size: 17px; font-weight: 700; color: #222; }
.price { color: #ff6a00; font-weight: 600; }
.done { font-size: 12px; color: #999; }
</style>
