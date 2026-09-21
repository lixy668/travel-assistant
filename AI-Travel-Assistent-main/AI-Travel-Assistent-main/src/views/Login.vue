<template>
  <div class="login-container">
    <div class="login-header">
      <van-icon name="travel" size="60" color="#1890ff" />
      <h2>欢迎回来</h2>
      <p>登录后解锁更多旅行功能</p>
    </div>

    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请填写用户名' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请填写密码' }]"
        />
      </van-cell-group>

      <div class="login-btn">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
        >
          登录
        </van-button>
      </div>

      <!-- 增加注册按钮 -->
      <div class="register-btn">
        <van-button round block plain type="primary" @click="handleRegister">
          没有账号？点击注册
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { showToast } from "vant";
import request from "../utils/request";

const router = useRouter();
const route = useRoute();
const loading = ref(false);

const form = reactive({
  username: "",
  password: "",
});

const handleLogin = async () => {
  loading.value = true;
  try {
    // 1. 调用后端登录接口
    const res = await request.post("/auth/login", form);

    // 2. 【必须加上这个判断！】
    if (res.code === 200) {
      const token = res.data;
      localStorage.setItem("token", token);
      localStorage.setItem("username", form.username);
      showToast("登录成功");
      const redirect = route.query.redirect || "/home";
      router.replace(redirect);
    } else {
      // 3. 如果后端返回的是 401，就直接弹出错误信息！
      showToast(res.msg || "密码错误，请重试");
    }
  } catch (error) {
    console.error("登录失败:", error);
    showToast("登录失败，请检查用户名或密码");
  } finally {
    loading.value = false;
  }
};

const handleRegister = () => {
  router.push("/register");
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-bottom: 80px;
}
.login-header {
  text-align: center;
  margin-bottom: 40px;
  margin-top: 20px;
}
.login-header h2 {
  margin: 16px 0 8px;
  color: #333;
}
.login-header p {
  color: #999;
  font-size: 14px;
}
.login-btn {
  margin: 24px 16px 0;
}
/* 注册按钮样式 */
.register-btn {
  margin: 16px 16px 0;
}
</style>
