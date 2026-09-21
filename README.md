# 智能旅游助手（AI Travel Assistant）

一个基于 **Spring Boot + Vue3** 的智能旅游订票平台，包含**用户端网页**、**独立管理后台**、**AI 行程规划**、**真实酒店/景点数据**、**订单与审批流**、**库存防超卖**，并配了完整的 Docker 部署方案和 Prometheus + Grafana 监控。

## 一、功能概览

### 用户端（travel-web）

| 模块 | 功能 |
| --- | --- |
| 账号 | 注册、登录、退出登录（token 拉黑立即失效）、修改密码 |
| 首页 | 搜索、热门目的地随机推荐、推荐酒店/景点 |
| 酒店 | 按设备/城市推荐、关键词搜索（高德真实 POI）、详情页（相册、评分、房型、设施）、选日期按晚数下单、**显示实时余量、售完置灰** |
| 旅游 | **城市 / 景点**两个一级标签；城市列表（41 个，点进去看该城市全部景点和酒店，带二级标签）；景点随机推荐 + 高德图文介绍 |
| 机票 / 火车票 | 出发地目的地选择 → 结果列表 → 下单（演示数据） |
| 智能行程规划 | 填城市/天数/预算 → AI 生成每日行程（含景点配图、推荐酒店、车票）→ 可保存到「我的行程」回看 |
| AI 行程助手 | 流式打字机对话（SSE） |
| 订单 | 列表、状态/支付/退款/申请状态、**收银台沙箱支付**（4 种方式、30 分钟倒计时）、申请改签、申请退订、开发票 |
| 个人 | 收藏酒店/景点、我的行程、消息中心（未读红点）、修改密码 |

### 管理端（travel-admin，独立页面、独立账号库）

数据统计（近 7 天趋势图）｜ 系统监控（Grafana 看板）｜ 车票/机票/酒店/景点订单管理（改、删）｜ **待处理申请审批**（改签/退订，通过退订自动退款）｜ 发票申请管理 ｜ 酒店/景点管理（增删改查、排序、上下架、高德收录导入）｜ **库存管理** ｜ **管理员操作审计日志**

### 技术亮点

- **AI 熔断降级**：连续失败 3 次熔断 60 秒，期间返回本地兜底行程，不会把用户卡死
- **接口限流**：Redis 计数分档限流（AI 20/分、订单 30/分、登录 10/分、默认 120/分），Redis 挂了自动降级为内存计数；Nginx 层再限一道
- **下单幂等**：`Idempotency-Key` 10 分钟内重复提交只生成一张订单，连点/超时重发都不会重复下单
- **库存防超卖**：`update ... where total - sold >= n` 原子扣减 + 订单超时/退订/改期自动回补，配并发抢库存测试脚本验证
- **订单状态机**：所有写操作带事务 + 前置校验，重复审批、越权操作直接拦下

## 二、技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.4、Spring Data JPA、MySQL 8、Redis 7、JWT（jjwt）、OkHttp、BCrypt、Actuator + Micrometer |
| AI / 数据 | DeepSeek 大模型（对话、行程规划、简介生成）、高德地图 Web 服务（POI 搜索、周边、相册、地理编码） |
| 前端 | Vue 3、Vite 5、Vue Router、Element Plus、ECharts |
| 部署 | Docker、Docker Compose、Nginx（反向代理 + 限流 + 静态托管）、Prometheus、Grafana |

## 三、目录结构

```
旅游app与网页简历/
├── travel-web/             # 用户端网页（Vue3 + Vite，开发端口 5174）
├── travel-admin/           # 管理后台（Vue3 + Vite，开发端口 5175）
├── travel-assistant-main/  # 后端（Spring Boot，端口 3200）
├── deploy/                 # 上线部署（Docker Compose + Nginx + 自检/备份/测试脚本）
│   └── 测试/               # 接口冒烟测试、压测脚本、并发抢库存脚本
├── monitor/                # 监控（Prometheus + Grafana 看板）
├── 接口文档.md             # 完整接口文档（54 个接口）
└── README.md               # 本文件
```

## 四、快速开始

### 4.1 环境要求

JDK 17 ｜ Maven 3.9+ ｜ Node 18+ ｜ MySQL 8 ｜ Redis 7 ｜（可选）Docker Desktop

### 4.2 配置环境变量

密钥一律走环境变量，不写在代码里。Windows 可以在「系统属性 → 环境变量」里加，或在 IDEA 的运行配置里填：

| 变量 | 说明 | 示例 |
| --- | --- | --- |
| `DEEPSEEK_API_KEY` | DeepSeek 的 key（AI 对话/行程规划） | `sk-xxxxxxxx` |
| `AMAP_KEY` | 高德 Web 服务 key（酒店/景点真实数据） | `一串 32 位字符` |
| `JWT_SECRET` | JWT 签名密钥（32 位以上随机串） | `随便一串足够长的随机字符` |
| `MYSQL_PASSWORD` | 本机 MySQL root 密码 | `2003` |

数据库：建一个名为 `travel` 的库即可，表结构由 Hibernate 自动创建（`ddl-auto=update`），管理员和推荐地点会在首次启动时自动初始化。

### 4.3 启动后端

```bash
cd travel-assistant-main
mvn spring-boot:run          # 或直接在 IDEA 里运行 TravelassistantApplication
```

启动后：接口 <http://localhost:3200>，健康检查 <http://localhost:3200/actuator/health>

### 4.4 启动前端

```bash
# 用户端
cd travel-web
npm install
npm run dev                  # http://localhost:5174

# 管理后台（另开一个终端）
cd travel-admin
npm install
npm run dev                  # http://localhost:5175
```

前端通过 Vite 代理把 `/api/xxx` 转发到 `http://127.0.0.1:3200/xxx`，开发和上线都不用改代码。

### 4.5 默认账号

| 端 | 账号 | 密码 |
| --- | --- | --- |
| 管理后台 | `admin` | `admin123` |
| 用户端 | 自己注册 | — |

### 4.6 一键上线（Docker）

```bash
cd deploy
cp .env.example .env         # Windows: Copy-Item .env.example .env，然后填好密钥
docker compose up -d --build
```

访问：用户端 <http://localhost/> ｜ 管理后台 <http://localhost/admin/> ｜ 监控 <http://localhost:3000/d/travel-monitor>

详细的部署、数据迁移、HTTPS（可选）、备份、常见问题都在 **[deploy/README-上线部署.md](deploy/README-上线部署.md)**。

## 五、接口文档

完整接口说明见 **[接口文档.md](接口文档.md)**，包含统一响应格式、状态码、鉴权方式、限流规则、幂等说明，以及认证、AI 与内容、订单支付、站内消息、个人中心、管理端共 54 个接口的请求/响应示例。

## 六、测试

```bash
# 1. 单元测试（订单状态机 13 例、限流、JWT、库存；需本机 MySQL + Redis）
cd travel-assistant-main && mvn test

# 2. 接口冒烟测试：deploy/测试/接口冒烟测试.http（VS Code REST Client 直接点发送）
# 3. 压测：输出 QPS / 平均延迟 / 429 数量
powershell -ExecutionPolicy Bypass -File deploy\测试\压测脚本.ps1

# 4. 并发抢库存：库存设 5、并发 50 请求，断言成功数正好等于库存数
powershell -ExecutionPolicy Bypass -File deploy\测试\并发抢库存.ps1 -AdminToken "管理员token" -Token "用户token"
```

## 七、说明与边界

- **支付是沙箱模拟**：收银台点「立即支付」即成功，不产生真实扣款，也没有接支付渠道回调/对账
- **机票、火车票是演示数据**：真实票务接口需要企业资质，未接入
- 酒店/景点的**图文信息、评分来自高德真实数据**，但房价、库存为平台自行维护的演示数据
- 景点页提供图文介绍，**未开放门票下单入口**（后端票务与库存逻辑仍保留）

## 八、License

本项目为个人学习 / 毕业设计作品，代码可自由参考学习。
