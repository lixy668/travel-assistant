<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

// 1. 动态读取登录状态和用户名（只要 localStorage 有值，页面就会自动更新）
const isLoggedIn = computed(() => !!localStorage.getItem("token"));
const username = computed(() => localStorage.getItem("username") || "用户昵称");

// 2. 点击顶部卡片时的逻辑
const handleUserCardClick = () => {
  if (isLoggedIn.value) {
    // 已登录，跳转到我的行程（或首页）
    router.push("/home");
  } else {
    // 未登录，跳转到登录页
    router.push("/login");
  }
};

// 3. 退出登录
const handleLogout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  router.push("/login");
};
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="我的" />

    <!-- 绑定点击事件，变成可点击跳转的卡片 -->
    <div class="user-card" @click="handleUserCardClick">
      <!-- 改成白色，不然在蓝色背景上看不清 -->
      <van-icon name="user-o" size="80" color="#fff" />
      <div class="user-info">
        <div class="user-name">{{ isLoggedIn ? username : "用户昵称" }}</div>
        <div class="user-desc">{{ isLoggedIn ? "欢迎回来" : "点击登录" }}</div>
      </div>
      <van-icon name="arrow" size="20" color="#ccc" />
    </div>

    <!-- 只有登录了才显示退出登录按钮 -->
    <div v-if="isLoggedIn" class="logout-btn-wrapper">
      <van-button block type="danger" @click="handleLogout"
        >退出登录</van-button
      >
    </div>

    <div class="page-card">
      <van-cell-group inset>
        <van-cell title="我的收藏" icon="star-o" is-link />
        <van-cell title="我的足迹" icon="location-o" is-link />
        <van-cell title="浏览历史" icon="history" is-link />
        <van-cell title="行程订单" icon="ticket" is-link />
      </van-cell-group>
    </div>
    <div class="page-card">
      <van-cell-group inset>
        <van-cell title="设置" icon="setting-o" is-link />
        <van-cell title="帮助中心" icon="help-o" is-link />
        <van-cell title="关于我们" icon="info-o" is-link />
      </van-cell-group>
    </div>
    <div class="bottom-spacer"></div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}
.user-card {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  margin: 0 16px 12px;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer; /* 改成手型，提示可点击 */
}
.user-info {
  flex: 1;
}
.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}
.user-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}
.page-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  margin: 0 16px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.logout-btn-wrapper {
  margin: 0 16px 16px;
}
.bottom-spacer {
  height: 50px;
}
</style>
