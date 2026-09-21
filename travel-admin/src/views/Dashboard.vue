<template>
  <div>
    <!-- 工具条：最后更新时间 + 自动刷新 -->
    <div class="toolbar">
      <span class="updated">最后更新：{{ lastUpdated || '—' }}</span>
      <div>
        <el-switch v-model="autoRefresh" active-text="自动刷新(30s)" />
        <el-button size="small" style="margin-left:12px" @click="loadStats">立即刷新</el-button>
      </div>
    </div>

    <!-- 累计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in totalCards" :key="c.key">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" :style="{ background: c.color }">{{ c.icon }}</div>
          <div>
            <div class="stat-title">{{ c.title }}</div>
            <div class="stat-value">{{ stats[c.key] ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日卡片（带环比） -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="6" v-for="c in todayCards" :key="c.key">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" :style="{ background: c.color }">{{ c.icon }}</div>
          <div>
            <div class="stat-title">{{ c.title }}</div>
            <div class="stat-value">
              {{ stats[c.key] ?? 0 }}
              <span class="delta" :class="deltaClass(c)">{{ deltaText(c) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover" style="justify-content:center">
          <el-button type="primary" @click="loadStats">刷新数据</el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近 7 天趋势 -->
    <el-card style="margin-top:16px">
      <template #header><b>近 7 天趋势</b></template>
      <div ref="chartEl" style="width:100%;height:320px"></div>
    </el-card>

    <!-- 最近操作记录 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
          <b>最近操作记录</b>
          <div>
            <el-select v-model="filterAction" placeholder="全部类型" clearable size="small" style="width:150px;margin-right:8px">
              <el-option label="登录 LOGIN" value="LOGIN" />
              <el-option label="推荐 RECOMMEND" value="RECOMMEND" />
              <el-option label="对话 CHAT" value="CHAT" />
            </el-select>
            <el-input v-model="filterUser" placeholder="按用户筛选" size="small" clearable style="width:170px" />
          </div>
        </div>
      </template>

      <el-table :data="pagedLogs" stripe>
        <el-table-column label="时间" width="200">
          <template #default="{ row }">{{ fmtTime(row.time) }}</template>
        </el-table-column>
        <el-table-column prop="user" label="用户" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-tag :type="tagType(row.action)" effect="light">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top:12px;justify-content:flex-end"
        layout="total, prev, pager, next"
        :total="filteredLogs.length"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPageChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const token = localStorage.getItem('adminToken') || ''
const stats = ref({})
const lastUpdated = ref('')
const autoRefresh = ref(true)
let timer = null
let chart = null
const chartEl = ref(null)

const totalCards = [
  { key: 'totalUsers', title: '注册用户', icon: '👤', color: '#409eff' },
  { key: 'totalLogins', title: '累计登录', icon: '🔑', color: '#67c23a' },
  { key: 'totalRecommends', title: '行程推荐', icon: '🗺️', color: '#e6a23c' },
  { key: 'totalChats', title: 'AI 对话', icon: '💬', color: '#9c27b0' },
]
const todayCards = [
  { key: 'todayLogins', yKey: 'yesterdayLogins', title: '今日登录', icon: '🔑', color: '#67c23a' },
  { key: 'todayRecommends', yKey: 'yesterdayRecommends', title: '今日推荐', icon: '🗺️', color: '#e6a23c' },
  { key: 'todayChats', yKey: 'yesterdayChats', title: '今日对话', icon: '💬', color: '#9c27b0' },
]

const pct = (c) => {
  const today = stats.value[c.key] ?? 0
  const y = stats.value[c.yKey] ?? 0
  if (y === 0) return today > 0 ? 100 : 0
  return Math.round(((today - y) / y) * 100)
}
const deltaText = (c) => {
  const v = pct(c)
  if (v > 0) return `▲ +${v}%`
  if (v < 0) return `▼ ${v}%`
  return '— 0%'
}
const deltaClass = (c) => (pct(c) > 0 ? 'up' : pct(c) < 0 ? 'down' : '')

// 表格筛选 + 分页
const filterAction = ref('')
const filterUser = ref('')
const page = ref(1)
const pageSize = 8
const filteredLogs = computed(() => {
  const logs = stats.value.recentLogs || []
  return logs.filter(
    (l) =>
      (!filterAction.value || l.action === filterAction.value) &&
      (!filterUser.value || (l.user || '').includes(filterUser.value)),
  )
})
const pagedLogs = computed(() => {
  const s = (page.value - 1) * pageSize
  return filteredLogs.value.slice(s, s + pageSize)
})
const onPageChange = (p) => { page.value = p }
watch([filterAction, filterUser], () => { page.value = 1 })

const tagType = (a) => (a === 'LOGIN' ? 'primary' : a === 'RECOMMEND' ? 'success' : 'warning')
const fmtTime = (t) => (t ? t.replace('T', ' ').slice(0, 19) : t)

const renderChart = () => {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)
  const days = stats.value.last7Days || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['登录', '推荐', '对话'], top: 0 },
    grid: { left: 44, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: days.map((d) => (d.date || '').slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '登录', type: 'line', smooth: true, data: days.map((d) => d.logins) },
      { name: '推荐', type: 'line', smooth: true, data: days.map((d) => d.recommends) },
      { name: '对话', type: 'line', smooth: true, data: days.map((d) => d.chats) },
    ],
  })
}

const loadStats = async () => {
  try {
    const resp = await fetch('/api/admin/stats', {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (resp.status === 401 || resp.status === 403) {
      ElMessage.error('登录过期或无权限')
      return
    }
    const res = await resp.json()
    if (res.code === 200) {
      stats.value = res.data
      lastUpdated.value = new Date().toLocaleTimeString()
      renderChart()
    }
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  }
}

const onResize = () => chart && chart.resize()

onMounted(() => {
  loadStats()
  timer = setInterval(() => {
    if (autoRefresh.value) loadStats()
  }, 30000)
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('resize', onResize)
  if (chart) { chart.dispose(); chart = null }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.updated {
  color: #909399;
  font-size: 13px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}
.stat-title {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.delta {
  font-size: 13px;
  font-weight: 400;
  margin-left: 6px;
  color: #909399;
}
.delta.up { color: #67c23a; }
.delta.down { color: #f56c6c; }
</style>
