<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">🚄 火车票</div>
      <div class="links"><span @click="go('/home')">返回首页</span></div>
    </header>

    <div class="wrap">
      <SideNav active="/train" />
      <main class="content">
        <div class="card">
          <div class="form-row">
            <div class="fld">
              <label>出发城市</label>
              <el-select v-model="from" filterable placeholder="出发城市" style="width:100%">
                <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
              </el-select>
            </div>
            <div class="swap" @click="swap">⇄</div>
            <div class="fld">
              <label>到达城市</label>
              <el-select v-model="to" filterable placeholder="到达城市" style="width:100%">
                <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
              </el-select>
            </div>
            <div class="fld">
              <label>出发日期</label>
              <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </div>
            <button class="go" @click="handleGo">查询车次</button>
          </div>
          <div class="tip">
            提示：12306 未开放免费接口，此处为<strong>示例车次数据</strong>（仅演示交互）。若要真实车次需接入付费/授权数据源。
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideNav from '../components/SideNav.vue'

const router = useRouter()
const cities = ['北京', '上海', '广州', '深圳', '成都', '杭州', '西安', '重庆', '南京', '武汉', '苏州', '长沙', '天津', '郑州', '济南']
const from = ref('上海')
const to = ref('北京')
const date = ref(new Date().toISOString().slice(0, 10))

const go = (p) => router.push(p)
const swap = () => {
  const t = from.value
  from.value = to.value
  to.value = t
}
const handleGo = () => {
  if (!from.value || !to.value) {
    ElMessage.warning('请选择出发和到达城市')
    return
  }
  if (from.value === to.value) {
    ElMessage.warning('出发和到达城市不能相同')
    return
  }
  router.push({ path: '/train-result', query: { from: from.value, to: to.value, date: date.value } })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f4f7; }
.topbar { height: 64px; background: #fff; display: flex; align-items: center; gap: 20px; padding: 0 24px; box-shadow: 0 1px 6px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 20; }
.brand { font-size: 20px; font-weight: 700; color: #0086f6; }
.links span { color: #333; font-size: 14px; cursor: pointer; }
.wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; padding: 16px 24px 40px; align-items: start; box-sizing: border-box; }
.content { min-width: 0; }
.card { background: #fff; border-radius: 8px; padding: 20px; }
.form-row { display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.fld { flex: 1; min-width: 160px; }
.fld label { display: block; font-size: 12px; color: #888; margin-bottom: 6px; }
.swap { font-size: 20px; color: #0086f6; cursor: pointer; padding-bottom: 8px; }
.go { border: 0; background: #0086f6; color: #fff; border-radius: 6px; padding: 10px 30px; font-size: 16px; font-weight: 600; cursor: pointer; }
.go:hover { background: #0072d6; }
.tip { margin-top: 14px; font-size: 13px; color: #999; }
</style>
