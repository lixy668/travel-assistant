<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">✈️ 机票</div>
      <div class="links"><span @click="go('/home')">返回首页</span></div>
    </header>

    <div class="wrap">
      <SideNav active="/flight" />
      <main class="content">
        <div class="card">
          <div class="form-row">
            <div class="fld">
              <label>出发地</label>
              <el-select v-model="from" filterable placeholder="选择出发城市" style="width:100%">
                <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
              </el-select>
            </div>
            <div class="swap" @click="swap">⇄</div>
            <div class="fld">
              <label>目的地</label>
              <el-select v-model="to" filterable placeholder="选择到达城市" style="width:100%">
                <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
              </el-select>
            </div>
            <div class="fld">
              <label>出发日期</label>
              <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </div>
            <button class="go" @click="handleGo">出发</button>
          </div>
          <div class="tip">提示：机票为<strong>示例数据</strong>（无免费真实航班接口），用于演示交互流程。</div>
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
const cities = ['北京', '上海', '广州', '深圳', '成都', '杭州', '西安', '重庆', '南京', '武汉', '三亚', '厦门', '青岛', '昆明', '大连']
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
    ElMessage.warning('请选择出发地和目的地')
    return
  }
  if (from.value === to.value) {
    ElMessage.warning('出发地和目的地不能相同')
    return
  }
  router.push({ path: '/flight-result', query: { from: from.value, to: to.value, date: date.value } })
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
.go { border: 0; background: #ff8a1f; color: #fff; border-radius: 6px; padding: 10px 34px; font-size: 16px; font-weight: 600; cursor: pointer; }
.go:hover { background: #f57c0b; }
.tip { margin-top: 14px; font-size: 13px; color: #999; }
</style>
