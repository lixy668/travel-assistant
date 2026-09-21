# 上线部署说明（Docker + Nginx + 限流 + 幂等）

这套 compose 把整个项目按「上线形态」跑起来：MySQL、Redis、Spring Boot、Nginx 网关各自独立容器，
网关同时负责静态页面、反向代理和限流，后端再做一层接口限流和下单幂等。

## 一、都有哪些服务

| 服务 | 容器名 | 端口 | 作用 |
| --- | --- | --- | --- |
| mysql | travel-mysql | 宿主机 3307 → 容器 3306 | 数据库（数据存在 docker 卷里，不碰你本机 3306 那个 MySQL） |
| redis | travel-redis | 仅内网 | 缓存 + 限流计数 + 幂等键 |
| backend | travel-backend | 宿主机 3200 → 容器 3200 | Spring Boot 接口（对外只走网关，3200 留着给 Prometheus 和调试） |
| gateway | travel-gateway | 80（443 可选） | 用户端网页、管理后台、反向代理、Nginx 限流 |

访问入口：

- 用户端网页：<http://localhost/>
- 管理后台：<http://localhost/admin/>（默认账号 `admin / admin123`，上线后请改密码）
- 接口调试：<http://localhost:3200>

## 二、第一次启动

```powershell
# 1. 先启动 Docker Desktop，等托盘图标变绿、docker ps 能出结果

# 2. 进 deploy 目录
cd C:\Users\26859\Desktop\旅游app与网页简历\deploy

# 3. 复制环境变量模板，填上自己的 key（.env 里有密钥，别提交到 git）
Copy-Item .env.example .env
notepad .env

# 4. 构建并后台启动（第一次要下镜像 + 装依赖，5~15 分钟）
docker compose up -d --build

# 5. 看状态和日志
docker compose ps
docker compose logs -f backend
```

浏览器打开 <http://localhost/> 就能用了（默认走 HTTP，不需要证书）。

常用命令：

```powershell
docker compose restart backend     # 只重启后端
docker compose down                # 停掉（数据保留在卷里）
docker compose up -d --build       # 改完代码重新构建启动
docker compose down -v             # 连数据库数据一起删（慎用）
```

## 三、上线前必须改的 3 个地方

1. `.env` 里的 `JWT_SECRET`、`DEEPSEEK_API_KEY`、`AMAP_KEY`、`MYSQL_PASSWORD` 换成自己的。
2. `.env` 里 `CORS_ALLOWED_ORIGINS` 改成你的访问地址（本地或演示直接保持 `*` 即可）。
3. 管理后台「系统监控」页面里的 Grafana 地址目前写死 `http://localhost:3000`，在
   `travel-admin/src/views/Monitor.vue` 里换成你的域名再重新构建。

## 四、以后想上 HTTPS / 绑域名（可选，现在不用管）

默认就是 HTTP，够本地和答辩演示用。等你以后有服务器和域名了，按 `nginx/conf.d/travel.conf` 文件末尾那段注释操作即可（四步：放证书到 `deploy/certs/`、compose 打开 443 映射和 certs 挂载、把 `listen 80` 那个 server 块复制一份改成 `listen 443 ssl`、重新构建网关）。

域名相关要提前准备：国内服务器需要 **ICP 备案**（80/443 都要），境外服务器不需要；证书用阿里云/腾讯云免费证书或 certbot 签发都行。

## 五、把本机数据搬进容器（可选）

容器里是全新的 `travel` 库，管理员和推荐地点会自动初始化（AdminSeeder / PlaceSeeder）。
想把本机 MySQL 里已有的账号、订单搬过去：

```powershell
# 导出本机库
mysqldump -uroot -p2003 --databases travel > D:\travel-backup.sql

# 导入容器
docker exec -i travel-mysql mysql -uroot -p2003 < D:\travel-backup.sql
```

## 六、怎么验证「防刷」和「防重复下单」

**1）接口限流**：AI 类接口每个 IP 每分钟 20 次，订单类 30 次，登录 10 次，其余 120 次；
超了返回 429 +「请求过于频繁，请稍后再试」。

```powershell
# 拿一个登录后的 token 填进去，然后连打 25 次 chat 接口
1..25 | ForEach-Object {
  curl.exe -s -o NUL -w "%{http_code} " -X POST "http://localhost/api/travel/chat" `
    -H "Content-Type: application/json" -H "Authorization: Bearer <你的token>" `
    -d "{\"message\":\"你好\"}"
}
# 预期：前 20 次 200，后面全是 429
```

**2）下单幂等**：同一个 `Idempotency-Key` 在 10 分钟内重复提交，只会生成一张订单，第二次直接返回原订单。

```powershell
$body = '{"type":"HOTEL","fromCity":"测试酒店","travelDate":"2026-10-01","departTime":"2026-10-02","seat":"标准大床房 × 1晚","price":328}'
1..2 | ForEach-Object {
  curl.exe -s -X POST "http://localhost/api/order/create" `
    -H "Content-Type: application/json" -H "Authorization: Bearer <你的token>" `
    -H "Idempotency-Key: demo-key-001" -d $body
}
# 预期：两次返回的 orderNo 完全一样，数据库里只有一条订单
```

前端已经自动带 `Idempotency-Key`（酒店 / 景点 / 机票 / 火车票下单都带），正常点按钮不会重复下单。

## 七、常见问题

| 现象 | 处理 |
| --- | --- |
| `failed to connect to the docker API ... dockerDesktopLinuxEngine` | Docker Desktop 没启动，先启动它 |
| 80 端口被占用 | 关掉本机 IIS 或其他 nginx，或改 compose 里的端口映射 |
| 3306 想连容器 | 用 **3307**（容器里还是 3306），避免和本机 MySQL 冲突 |
| 页面能开但接口 502 | `docker compose logs -f backend` 看后端有没有起来，通常是 MySQL/Redis 还没就绪 |
| 想用 HTTPS | 见第四节，默认走 HTTP 不影响使用 |
| 想回开发模式 | 停掉 compose，继续用 IDEA 跑后端 + `npm run dev`（两者都占 3200，别同时开） |

## 八、和 monitor 监控目录配合

`monitor` 里的 Prometheus 抓的是 `host.docker.internal:3200/actuator/prometheus`，
后端已经把 3200 映射到宿主机，所以照旧启动即可：

```powershell
cd C:\Users\26859\Desktop\旅游app与网页简历\monitor
docker compose up -d
```

Grafana 在 <http://localhost:3000>，管理后台「系统监控」页用 iframe 嵌入它。

## 九、上线检查清单（照着打勾）

| 项目 | 位置 | 状态 |
| --- | --- | --- |
| 密钥全部走环境变量（不写在代码里） | `.env` | ✅ |
| 密码 BCrypt 加密存储 | UserService | ✅ |
| 登录接口限流（防撞库） | RateLimitInterceptor | ✅ |
| 接口限流（AI 20/分、订单 30/分、默认 120/分） | RateLimitInterceptor + Nginx | ✅ |
| 下单幂等（防连点重复下单） | IdempotencyService + 前端 Idempotency-Key | ✅ |
| 订单超时自动关单（30 分钟） | OrderService 定时任务 | ✅ |
| 改签/退订走审批 + 事务 + 状态校验 | OrderService | ✅ |
| 退订通过后自动退款并写流水 | OrderService.refundIfPaid | ✅ |
| 退出登录真实失效（JWT 黑名单） | UserService + Redis | ✅ |
| 修改密码 | /auth/change-password | ✅ |
| 网关反向代理 + 限流（HTTP） | deploy/nginx | ✅ |
| 监控看板（9 个面板：在线状态/QPS/P95/5xx/内存/CPU/连接池/TOP接口） | monitor/grafana | ✅ |
| 数据库定时备份 | deploy/backup-mysql.ps1 | ✅ |
| 单元测试 | src/test | ✅ 需本地跑一次 |
| 压力测试报告 | deploy/测试/压测脚本.ps1 | ✅ 需本地跑一次 |
| 站内消息通知（下单/支付/审批/退款） | t_notice + NoticeService | ✅ |
| 收藏 + AI 行程历史回放 | t_favorite / t_trip_plan | ✅ |
| 发票申请与开具 | t_invoice | ✅ |
| 管理员操作审计日志 | t_admin_log | ✅ |

## 十、业务功能补充说明（本次新增）

**用户端新增**

| 功能 | 入口 | 说明 |
| --- | --- | --- |
| 消息中心 | 左侧导航「消息中心」 | 下单、支付、改签/退订申请、审批结果、退款到账、发票开具都会推一条，有未读红点 |
| 我的行程 | 左侧导航「我的行程」 | 保存 AI 生成的行程并随时回看；同一页签里有「我的收藏」 |
| 收藏 | 酒店/景点详情页「☆ 收藏」 | 再点一次取消，收藏可在「我的行程 → 我的收藏」里打开 |
| 保存行程 | 行程结果页「保存行程」 | 把 AI 结果落库，最多 50 条 |
| 开发票 | 我的订单 → 已支付订单「开发票」 | 填抬头/税号/邮箱提交，管理员开具后收到消息 |
| 会员积分 | 我的订单页右上角 | 支付成功按 1 元 = 1 积分累计，按累计消费算会员等级 |

**管理端新增**

| 功能 | 入口 | 说明 |
| --- | --- | --- |
| 发票申请管理 | 导航「发票申请」 | 按状态筛选、开票、驳回，操作会通知用户 |
| 操作日志 | 导航「操作日志」 | 记录管理员登录、审批、改单、删单、开票，谁在什么时候改了什么 |

**新增数据表**（`ddl-auto=update` 会自动建表，不用手动执行 SQL）

`t_notice`（站内消息）、`t_favorite`（收藏）、`t_trip_plan`（行程历史）、`t_invoice`（发票）、`t_admin_log`（管理员审计）；`t_user` 增加 `points`、`total_spend` 两列。

**新增接口**

```
GET  /notice/my              消息列表
GET  /notice/unread          未读数
POST /notice/read/{id}       标记已读
POST /notice/read-all        全部已读
GET  /favorite/my            我的收藏
GET  /favorite/check         是否已收藏
POST /favorite/toggle        收藏 / 取消收藏
DELETE /favorite/{id}        删除收藏
GET  /plan/my                行程历史
POST /plan/save              保存行程
GET  /plan/{id}              行程详情
DELETE /plan/{id}            删除行程
POST /invoice/apply          申请发票
GET  /invoice/my             我的发票
GET  /admin/invoices         发票列表（管理员）
POST /admin/invoices/{id}/issue    开票
POST /admin/invoices/{id}/reject   驳回
GET  /admin/logs             管理员操作日志
GET  /auth/me                个人中心（积分/等级/累计消费）
```

## 十点五、库存与超卖控制（重点，答辩可演示）

**做了什么**

1. 新建 `t_stock` 表：一行 = 某地点 + 某天 + 某房型/票种的库存（`total` 总量、`sold` 已占用、`price` 当天价），唯一索引锁死重复配置。
2. **原子扣减**防超卖（
   `update t_stock set sold = sold + n where ... and total - sold >= n`），靠 InnoDB 行锁，返回受影响行数 = 0 就说明库存不够，直接抛「库存不足」业务异常，订单不创建。
3. 占用与回补的完整闭环：
   - 下单 → 占库存
   - 30 分钟未支付自动关单 → 回补
   - 未支付订单直接取消 → 回补
   - 退订审批通过 → 回补
   - 管理员改期/改房型 → 先占新的再还旧的
   - 管理员删单 → 回补
4. 兼容性：某个日期没配库存 = 不限量，老数据和高德实时搜索的酒店不受影响。
5. 前端：酒店详情页选好日期后显示「仅剩 N 间」，售完的房型按钮变「已售完」且点不动；景点票按张数占用库存。

**怎么验证不超卖**

```powershell
cd C:\Users\26859\Desktop\旅游app与网页简历\deploy\测试
# 库存设 5，并发 50 个下单请求，成功数应该正好是 5
powershell -ExecutionPolicy Bypass -File .\并发抢库存.ps1 -AdminToken "管理员token" -Token "用户token" -PlaceId 1 -Stock 5 -Requests 50
```

脚本会打印「下单成功 N 个」，并自动断言 N 是否等于库存数；数据库复核语句：

```sql
select room_type, total, sold from t_stock where place_id = 1 and biz_date = '2026-10-01';
```

**相关接口**：`GET /travel/stock?placeId=&date=`（查剩余）、`GET /admin/stocks?placeId=`（库存列表）、`POST /admin/stocks`（维护库存）。

**还没做的业务功能**（答辩时说明即可）：优惠券、多管理员角色细分、短信/邮件通知（目前是站内信）。

## 十一、单元测试怎么跑

```powershell
cd C:\Users\26859\Downloads\travel-assistant-main\travel-assistant-main
mvn test
```

覆盖的内容：

- `OrderServiceTest`：支付幂等、超时关单、退订直接取消 / 走审批、审批通过自动退款、重复审批拦截、越权操作拦截、金额校验
- `RateLimiterServiceTest`：限流阈值生效、Redis 挂掉自动降级、不同 IP 互不影响
- `JwtUtilTest`：签发/解析/篡改校验/剩余有效期
- `TravelassistantApplicationTests`：启动上下文测试，需要本机 MySQL + Redis + 环境变量（没配会自动跳过，不会让 `mvn test` 变红）

跑测试前记得先启动 MySQL 和 Redis。IDEA 里直接右键测试类 Run 也可以。

## 十二、监控怎么看

```powershell
cd C:\Users\26859\Desktop\旅游app与网页简历\monitor
docker compose up -d
```

- Prometheus：<http://localhost:9090>（指标原始数据，随便查哪个指标）
- Grafana：<http://localhost:3000>（看板界面，默认匿名只读）

看板地址 <http://localhost:3000/d/travel-monitor>，也就是管理后台「系统监控」里嵌的那个页面，9 个面板：

后端在线状态 / 运行时长 / JVM 堆内存 / CPU 使用率 / 每秒请求数 QPS / 接口响应 P95 与 P50 / 5xx 错误率 / 数据库连接池 / 调用最多的接口 TOP5。

看板是**代码化配置**的：文件在 `monitor/grafana/provisioning/dashboards/travel-monitor.json`，改完 Grafana 30 秒内自动重载；Grafana 还挂了 `grafana-data` 数据卷，重建容器也不会丢。改了 prometheus 配置想热加载可以执行 `curl.exe -X POST http://localhost:9090/-/reload`。

提示：面板数据来自后端指标，**后端没启动时「后端服务」那格会显示离线、曲线是空的**。

## 十三、数据库备份

```powershell
# 手动备份一次
powershell -ExecutionPolicy Bypass -File .\backup-mysql.ps1

# 看备份文件
dir .\backups
```

备份文件放在 `deploy/backups`，默认只保留最近 7 份。要做每天自动备份，用「任务计划程序」新建一个基本任务，每天凌晨 3 点执行：

```
powershell.exe -ExecutionPolicy Bypass -File "C:\Users\26859\Desktop\旅游app与网页简历\deploy\backup-mysql.ps1"
```

恢复备份：

```powershell
docker exec -i travel-mysql mysql -uroot -p2003 < .\backups\travel-20260921-030000.sql
```
