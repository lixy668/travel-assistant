<template>
  <div>
    <div class="toolbar">
      <span class="title">管理员操作日志</span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="时间" width="165">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column prop="adminUsername" label="操作人" width="110" />
      <el-table-column label="动作" width="130">
        <template #default="{ row }">
          <el-tag :type="tagType(row.action)" effect="light">{{ actionText(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="target" label="对象" width="180" />
      <el-table-column prop="detail" label="详情" min-width="260" />
    </el-table>

    <el-empty v-if="!loading && !list.length" description="暂无操作记录" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const token = () => localStorage.getItem('adminToken') || ''
const list = ref([])
const loading = ref(false)

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')
const actionText = (a) =>
  ({
    LOGIN: '登录',
    APPROVE: '审批通过',
    REJECT: '审批驳回',
    UPDATE_ORDER: '修改订单',
    DELETE_ORDER: '删除订单',
    ISSUE_INVOICE: '开具发票',
    REJECT_INVOICE: '驳回发票',
  }[a] || a)
const tagType = (a) =>
  a === 'LOGIN' ? 'info' : a.indexOf('REJECT') >= 0 || a === 'DELETE_ORDER' ? 'danger' : a === 'APPROVE' || a === 'ISSUE_INVOICE' ? 'success' : 'primary'

const load = async () => {
  loading.value = true
  try {
    const resp = await fetch('/api/admin/logs', {
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

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.title { font-size: 17px; font-weight: 700; color: #222; }
</style>
