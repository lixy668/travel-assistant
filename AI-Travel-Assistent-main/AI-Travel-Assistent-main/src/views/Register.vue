<template>
  <div class="register-container">
    <div class="register-header">
      <van-icon name="friends-o" size="60" color="#1890ff" />
      <h2>创建账号</h2>
      <p>注册后开启你的智能旅行之旅</p>
    </div>

    <van-form @submit="handleRegister">
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
        <van-field
          v-model="form.confirmPassword"
          type="password"
          name="confirmPassword"
          label="确认密码"
          placeholder="请再次输入密码"
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: validatePassword, message: '两次输入的密码不一致' },
          ]"
        />
      </van-cell-group>

      <div class="register-btn">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
        >
          注册
        </van-button>
      </div>

      <div class="login-link" @click="handleBackToLogin">
        已有账号？返回登录
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { showToast } from "vant";
import request from "../utils/request";

const router = useRouter();
const loading = ref(false);

const form = reactive({
  username: "",
  password: "",
  confirmPassword: "",
});

// 自定义校验：确认密码是否一致
const validatePassword = (val) => {
  return form.password === val;
};

const handleRegister = async () => {
  loading.value = true;
  try {
    // 注意：注册接口只传 username 和 password，不传 confirmPassword
    const { confirmPassword, ...registerData } = form;

    // 【关键修改】不要传空字符串，把 email 设置为用户名+域名
    registerData.email = `${registerData.username}@example.com`;

    // 调用后端注册接口
    const res = await request.post("/auth/register", registerData);

    // 【关键】判断后端返回的 code 是否为 200
    if (res.code === 200) {
      showToast("注册成功，请登录");
      // 注册成功后，自动跳回登录页
      router.replace("/login");
    } else {
      showToast(res.msg || "注册失败");
    }
  } catch (error) {
    console.error("注册失败:", error);
    showToast(error.message || "注册失败，请检查输入或联系管理员");
  } finally {
    loading.value = false;
  }
};

const handleBackToLogin = () => {
  router.push("/login");
};
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-bottom: 80px;
}
.register-header {
  text-align: center;
  margin-bottom: 40px;
  margin-top: 20px;
}
.register-header h2 {
  margin: 16px 0 8px;
  color: #333;
}
.register-header p {
  color: #999;
  font-size: 14px;
}
.register-btn {
  margin: 24px 16px 0;
}
.login-link {
  margin-top: 16px;
  text-align: center;
  color: #1890ff;
  font-size: 14px;
  cursor: pointer;
}
</style>
