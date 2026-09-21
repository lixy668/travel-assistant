<template>
  <div class="admin">
    <header class="topbar">
      <div class="brand">智能旅游助手 · 管理后台</div>
      <nav v-if="token" class="nav">
        <a :class="{ active: view === 'stats' }" @click="view = 'stats'"
          >数据统计</a
        >
        <a :class="{ active: view === 'monitor' }" @click="view = 'monitor'"
          >系统监控</a
        >
        <a :class="{ active: view === 'trainOrders' }" @click="view = 'trainOrders'"
          >车票订单</a
        >
        <a :class="{ active: view === 'flightOrders' }" @click="view = 'flightOrders'"
          >机票订单</a
        >
        <a :class="{ active: view === 'hotelOrders' }" @click="view = 'hotelOrders'"
          >酒店订单</a
        >
        <a :class="{ active: view === 'spotOrders' }" @click="view = 'spotOrders'"
          >景点订单</a
        >
        <a :class="{ active: view === 'changes' }" @click="view = 'changes'"
          >待处理申请</a
        >
        <a :class="{ active: view === 'hotels' }" @click="view = 'hotels'"
          >酒店管理</a
        >
      <a :class="{ active: view === 'spots' }" @click="view = 'spots'"
        >景点管理</a
      >
      <a :class="{ active: view === 'invoices' }" @click="view = 'invoices'"
        >发票申请</a
      >
      <a :class="{ active: view === 'logs' }" @click="view = 'logs'"
        >操作日志</a
      >
    </nav>
      <el-button v-if="token" link @click="logout" style="color: #fff"
        >退出</el-button
      >
    </header>

    <main class="main">
      <!-- 未登录 -->
      <el-card v-if="!token" class="login-card">
        <template #header><b>管理员登录</b></template>
        <el-form label-width="70px" @submit.prevent>
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="管理员账号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="管理员密码"
            />
          </el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
            >登录</el-button
          >
        </el-form>
      </el-card>

      <!-- 已登录 -->
      <template v-else>
        <Dashboard v-if="view === 'stats'" />
        <Monitor v-show="view === 'monitor'" />
        <OrderTable v-if="view === 'trainOrders'" type="TRAIN" title="车票订单管理" />
        <OrderTable v-if="view === 'flightOrders'" type="FLIGHT" title="机票订单管理" />
        <OrderTable v-if="view === 'hotelOrders'" type="HOTEL" title="酒店订单管理" />
        <OrderTable v-if="view === 'spotOrders'" type="SPOT" title="景点订单管理" />
        <ChangeRequests v-if="view === 'changes'" />
        <PlaceManage v-if="view === 'hotels'" type="HOTEL" title="酒店" />
        <PlaceManage v-if="view === 'spots'" type="SPOT" title="景点" />
        <InvoiceManage v-if="view === 'invoices'" />
        <AdminLogs v-if="view === 'logs'" />
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import Dashboard from "./views/Dashboard.vue";
import Monitor from "./views/Monitor.vue";
import OrderTable from "./components/OrderTable.vue";
import ChangeRequests from "./components/ChangeRequests.vue";
import PlaceManage from "./components/PlaceManage.vue";
import InvoiceManage from "./components/InvoiceManage.vue";
import AdminLogs from "./components/AdminLogs.vue";

const token = ref(localStorage.getItem("adminToken") || "");
const form = reactive({ username: "", password: "" });
const loading = ref(false);
const view = ref("stats");

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入账号密码");
    return;
  }
  loading.value = true;
  try {
    const resp = await fetch("/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: form.username,
        password: form.password,
      }),
    });
    const res = await resp.json();
    if (res.code === 200) {
      token.value = res.data;
      localStorage.setItem("adminToken", res.data);
      ElMessage.success("登录成功");
    } else {
      ElMessage.error(res.msg || "登录失败");
    }
  } catch (e) {
    ElMessage.error("登录失败");
  } finally {
    loading.value = false;
  }
};

const logout = () => {
  token.value = "";
  localStorage.removeItem("adminToken");
};
</script>

<style>
body,
html {
  margin: 0;
  background: #f5f7fa;
}
.admin {
  min-height: 100vh;
}
.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #1f2d3d;
  color: #fff;
}
.brand {
  font-size: 18px;
  font-weight: 600;
}
.nav {
  display: flex;
  gap: 8px;
}
.nav a {
  color: #fff;
  opacity: 0.7;
  cursor: pointer;
  padding: 6px 14px;
  border-radius: 16px;
}
.nav a.active,
.nav a:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.15);
}
.main {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
}
.login-card {
  max-width: 420px;
  margin: 8vh auto 0;
}
</style>
