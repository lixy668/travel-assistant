// src/utils/request.js
import axios from "axios";

//  创建普通的 Axios 实例
const request = axios.create({
  baseURL: "/api", // 让 Vite 代理转发到后端 3200
  timeout: 60000,
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 响应拦截器：处理后端统一返回格式
request.interceptors.response.use(
  (response) => {
    // 后端返回的是 { code: 200, msg: 'success', data: ... }
    return response.data;
  },
  (error) => {
    // 如果后端返回 400 或 401，这里统一提取错误信息
    const msg = error.response?.data?.msg || "网络错误";
    return Promise.reject(new Error(msg));
  },
);

// 2. 处理流式接口请求 (SSE) 用于聊天
export async function fetchStream(url, data, onChunk, onComplete, onError) {
  const controller = new AbortController();

  try {
    const response = await fetch(`/api/travel/${url}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      body: JSON.stringify(data),
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`请求失败，状态码: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        if (onComplete) onComplete();
        break;
      }

      const chunk = decoder.decode(value, { stream: true });
      const lines = chunk.split("\n").filter((line) => line.trim() !== "");

      for (const line of lines) {
        if (line.startsWith("data:")) {
          const jsonStr = line.substring(5).trim();

          if (jsonStr === "[DONE]") {
            if (onComplete) onComplete();
            return;
          }

          try {
            const parsedData = JSON.parse(jsonStr);
            if (onChunk) onChunk(parsedData);
          } catch (parseError) {
            console.warn("解析JSON片段失败:", parseError, jsonStr);
          }
        }
      }
    }
  } catch (error) {
    if (onError) onError(error);
  }
}

// 3. 默认导出普通 Axios 实例，供 Login.vue 和 Register.vue 使用
export default request;
