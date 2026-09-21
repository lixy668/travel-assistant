<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
      <span style="font-weight:600">系统监控（Prometheus + Grafana）</span>
      <el-button size="small" @click="t++">刷新</el-button>
    </div>
    <iframe
      :key="t"
      src="http://localhost:3000/d/travel-monitor?orgId=1&kiosk&refresh=10s"
      style="width:100%;height:calc(100vh - 170px);border:0;background:#fff"
    ></iframe>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const t = ref(0)
</script>
