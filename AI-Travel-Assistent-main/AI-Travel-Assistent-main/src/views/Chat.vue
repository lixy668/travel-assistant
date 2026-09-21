<script setup>
import { ref, nextTick, onMounted } from "vue";
import { useRouter } from "vue-router";
import { showToast } from "vant";
import { fetchStream } from "@/utils/request.js";

const router = useRouter();
const inputValue = ref("");
const messages = ref([
  {
    role: "ai",
    content:
      "你好！我是智能旅游助手，有什么可以帮你的吗？\n我可以帮你推荐景点、规划行程、介绍美食等。",
  },
]);

const currentAiMessage = ref("");
const chatContentRef = ref(null);

// 页面加载时检查登录状态
onMounted(() => {
  const token = localStorage.getItem("token");
  if (!token || token === "null") {
    showToast("请先登录");
    router.push("/login");
    return;
  }
});

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContentRef.value) {
      chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight;
    }
  });
};

const handleSend = async () => {
  const text = inputValue.value.trim();
  if (!text) {
    showToast("请输入内容");
    return;
  }

  // 把用户发的消息加入列表
  messages.value.push({ role: "user", content: text });
  inputValue.value = "";
  scrollToBottom();

  // 预先加入一条空白的 AI 消息占位
  messages.value.push({ role: "ai", content: "" });
  const lastAiMsgIndex = messages.value.length - 1;
  currentAiMessage.value = "";

  try {
    await fetchStream(
      "chat",
      { message: text },
      (chunk) => {
        const content = chunk.content || "";
        currentAiMessage.value += content;
        messages.value[lastAiMsgIndex].content = currentAiMessage.value;
        scrollToBottom();
      },
      () => {
        console.log("对话结束");
      },
      (error) => {
        console.error("流式请求失败:", error);
        showToast("回答生成失败，请重试");
        messages.value[lastAiMsgIndex].content = "【生成失败，请重试】";
      },
    );
  } catch (e) {
    console.error(e);
  }
};
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="AI对话" />

    <!-- 消息列表区域 -->
    <div class="chat-content" ref="chatContentRef">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-item"
        :class="msg.role"
      >
        <!-- 左侧 AI 头像 -->
        <van-icon
          v-if="msg.role === 'ai'"
          name="bot"
          size="40"
          color="#1890ff"
        />

        <!-- 气泡 -->
        <div class="message-bubble">
          <p style="white-space: pre-wrap">{{ msg.content }}</p>
        </div>
      </div>
    </div>

    <!-- 底部输入框区域 -->
    <div class="chat-input">
      <van-field
        v-model="inputValue"
        placeholder="输入你想咨询的问题"
        @keyup.enter="handleSend"
      />
      <van-button
        type="primary"
        round
        size="small"
        style="margin-left: 10px"
        @click="handleSend"
      >
        发送
      </van-button>
    </div>
    <div class="bottom-spacer"></div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}
.chat-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.message-item.ai {
  flex-direction: row;
}
.message-item.user {
  justify-content: flex-end;
}
.message-bubble {
  max-width: 75%;
  background-color: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}
.message-item.user .message-bubble {
  background-color: #1890ff;
  color: #fff;
}
.message-bubble p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.chat-input {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background-color: #fff;
  border-top: 1px solid #eee;
}
.bottom-spacer {
  height: 50px;
}
</style>
