# 智能旅游助手 · 管理后台（独立前端）

独立的第三个前端，和 App、Web 并列，专门给管理员看使用数据。后端**不需要改**。

## 运行
```bash
cd travel-admin
npm install
npm run dev
```
浏览器打开：http://localhost:5175

## 登录
- 默认管理员：`admin / admin123`（后端 `AdminSeeder` 启动时自动创建）

## 依赖的后端接口
- `POST /admin/login`  → 返回管理员 JWT
- `GET  /admin/stats`  → 返回使用统计（需 `Authorization: Bearer <token>`）

前端通过 Vite 代理把 `/api/*` 转发到 `http://127.0.0.1:3200`，并去掉 `/api` 前缀。

## 架构
三个前端 + 一个后端：
- `AI-Travel-Assistent-main`（App，Vant，端口 5173）
- `travel-web`（网页版，Element Plus，端口 5174）
- `travel-admin`（管理后台，Element Plus，端口 5175）← 本工程
- `travel-assistant-main`（Spring Boot 后端，端口 3200）
