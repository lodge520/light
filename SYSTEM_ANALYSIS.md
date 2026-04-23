# 智能灯控系统完整代码分析（smart-light-front / smart-light-backend / 8266.cpp）

## 1. 总体架构

该项目是一个“前端管理平台 + 后端业务服务 + ESP8266 终端固件”的三层架构：

1. **smart-light-front（Web 前端）**：用于账号登录、店铺配置、设备管理、灯光控制、人流/光照可视化。
2. **smart-light-backend（后端服务）**：提供认证、店铺、设备、光照、停留时长、AI识别、WebSocket 推送等 API。
3. **8266.cpp（设备固件）**：运行在 ESP8266，接入 Wi-Fi，采集传感器数据，执行灯光 PWM 控制，与后端通过 HTTP+WebSocket 交互，支持 OTA。

---

## 2. smart-light-front 完整功能分析

### 2.1 启动与路由

- 入口 `main.ts` 启动 Vue 应用并挂载路由。
- 路由 `router/index.ts` 包含：
  - `/login` 登录页
  - `/register` 注册页
  - `/store-setup` 店铺初始化页（需登录）
  - `/smartlightdashboard` 主控制台（需登录）
- 路由守卫能力：
  - 无 token 强制回登录；
  - 已登录但未完成店铺配置，强制跳转店铺配置页；
  - 已配置完成则进入主控制台。

### 2.2 认证与会话

- 登录页通过 `request('/api/auth/login')` 请求后端；
- 支持“记住我”：token 存 `localStorage` 或 `sessionStorage`；
- 401 统一处理：清除 token 并跳转登录页（`utils/request.ts` 与 `api/http.ts` 双通道实现）。

### 2.3 主控制台（SmartLightDashboard）

主界面以 tab 形式分为：

1. **main（设备控制）**
   - 顶部状态栏（时钟/日期/天气文本）；
   - 实时环境区（温度、人流、面积、光照展示）；
   - 扫描设备（监听 WebSocket announce）、手动添加设备；
   - 设备网格 `DeviceGrid`（刷新、实时更新、删除）；
   - 调用设备相关 API：创建设备、更新设备、在线列表、删除、状态同步等。

2. **flow（数据分析）**
   - `FlowOverview` 聚合多个图表卡片：
     - 停留时长热力/汇总
     - 多设备光照趋势
     - 温度-人流趋势（预留）
     - 策略对比（预留）
     - 设备分布图
   - 统计卡显示在线设备数、平均亮度、当前光照、店铺面积。

3. **settings（系统设置）**
   - 店铺参数设置面板（夜间模式、登出）；
   - 停留时长查询面板；
   - 云台控制面板（对指定设备发方向指令）；
   - 人流监控面板（摄像头灯设备筛选与状态展示）。

### 2.4 数据层与 API 封装

前端对后端接口做了模块化：

- `api/device.ts`：设备 CRUD、在线列表、云台控制、人流上传开关
- `api/lux.ts`：最新光照、光照列表、多设备趋势
- `api/duration.ts`：日期范围设备停留汇总
- `api/ai.ts`：面料识别上传
- `api/analytics.ts`：温度/人流趋势、策略对比（当前后端未见对应 controller，属于预留/可降级接口）
- `api/store.ts` / `api/auth.ts`：店铺与认证

### 2.5 实时通信

- `composables/useWebSocket.ts` 封装浏览器 WS 客户端：
  - 自动重连（3s）
  - JSON 自动解析
  - URL 变化自动重连
  - 提供 send/connect/close/reconnect

### 2.6 可视化能力

- 使用 Chart.js 系列图表组件（趋势、对比、分布）
- 流程统计信息由后端聚合接口驱动

---

## 3. smart-light-backend 完整功能分析

### 3.1 基础框架与配置

- Spring Boot 主程序开启：
  - `@MapperScan`（MyBatis-Plus Mapper）
  - `@EnableScheduling`（在线状态定时扫描）
- `application.yaml` 配置：
  - 3000 端口
  - MySQL 数据源
  - AI 两个外部服务地址（fabric/flow）
  - springdoc/openapi 文档路径

### 3.2 鉴权与安全

- Spring Security + JWT：
  - `/api/auth/login`、`/api/auth/register`、swagger 等放行；
  - 设备主动上报接口放行（announce/state-report/lux/duration）；
  - `/ws/device` 放行（设备 WS）；
  - `/ws` 浏览器 WS 需认证；
  - 其他接口默认需认证。
- `JwtAuthenticationFilter` + `JwtTokenService` 完成 token 解析与上下文注入。

### 3.3 业务模块（Controller + Service）

#### A. 账号模块

- `AuthController`
  - 注册：用户名唯一性校验、密码确认、BCrypt 存储
  - 登录：密码校验、生成 JWT、返回用户+店铺配置状态

#### B. 店铺模块

- `StoreController`
  - 获取当前店铺 `/api/store/current`
  - 配置/更新店铺 `/api/store/setup`
- `StoreServiceImpl` 负责当前登录用户与店铺绑定逻辑。

#### C. 设备模块

- `DeviceController`
  - 设备增删改查
  - 当前店铺设备列表
  - 绑定到当前店铺
- `DeviceServiceImpl`
  - 支持“已存在但未绑定”的设备直接绑定
  - 防止跨店铺绑定冲突
  - 设备变更后广播 WebSocket state

#### D. 设备网关与控制模块

- `DeviceGatewayController`
  - `/announce`：设备上线通告
  - `/arm/{chipId}`：云台方向控制
  - `/flow-upload/{chipId}`：人流上传开关
  - `/state-sync/{chipId}`：将状态下发给设备并写库
- `DeviceControlServiceImpl`
  - 校验设备在线
  - 更新设备状态字段
  - 通过 `DeviceSessionManager` 下发 WS state 指令

#### E. 设备状态上报与在线状态模块

- `DeviceReportController`：设备主动上报当前状态
- `DeviceReportServiceImpl`：写库 + 刷新 lastSeen + 广播
- `DeviceOnlineController`：在线状态查询
- `DeviceOnlineStatusScheduler`：每 10s 扫描超时离线并推送

#### F. 光照模块

- `LuxController`
  - 新增光照记录
  - 查询最新记录
  - 查询列表
  - 多设备趋势 `/multi-trend`
- `LuxServiceImpl`：写入时强校验“设备存在且已绑定店铺”
- `MultiLuxServiceImpl`：按设备取最近 12 条并对齐时间标签输出 datasets

#### G. 停留时长模块

- `DurationController`
  - 单日查询、范围查询、总和统计、设备汇总
- `DurationServiceImpl`
  - `createOrIncrease` 支持同日累计写入
  - 范围统计与设备级汇总
  - 每次写入后 WS 推送 `durationUpdate`

#### H. AI 模块

- `AiController`
  - 面料识别（multipart）
  - 人流检测（multipart）
- `AiServiceImpl`
  - 调用外部 AI 客户端
  - 识别结果通过 WS 广播给前端
- `FabricAiClient` / `PersonDetectClient`
  - 通过 `RestTemplate` 调外部推理服务

### 3.4 WebSocket 实时链路

- 浏览器端：`/ws` -> `AppWebSocketHandler`
  - 连接确认、ping/pong
  - 广播 state/lux/duration/announce/onlineStatus/AI结果 等
- 设备端：`/ws/device` -> `DeviceWebSocketHandler`
  - register 注册 chipId 与 session
  - ping 更新在线心跳
  - 断开时移除并触发在线状态变化推送

### 3.5 数据访问层

- MyBatis-Plus：
  - DO：`DeviceDO/LuxRecordDO/DurationRecordDO/StoreDO/UserAccountDO`
  - Mapper：对应 CRUD 与自定义 SQL（如 duration 的插入或累加）
- Convert：DO/VO 映射层（device/lux/duration）

---

## 4. 8266.cpp 完整功能分析

### 4.1 硬件与外设

- 控制对象：冷光 LED、暖光 LED（PWM）
- 传感器：
  - BH1750 光照传感器
  - VL53L0X ToF 测距传感器
- 其他 IO：BLUR 引脚（与面料策略联动）

### 4.2 本地配置管理

- 使用 `LittleFS` 存储 `/config.json`：
  - Wi-Fi ssid/password
  - serverHost/httpPort/wsPort
- 支持：加载配置、保存配置、清除配置
- 设备 ID 自动生成：`lamp-<ChipIdHex>`

### 4.3 配网流程（鲁棒性较高）

启动后优先级：

1. 读取本地配置并直连 Wi-Fi
2. 失败则进入 SmartConfig 批量配网
3. 仍失败则进入 AP 配网页面（内置 HTTP 表单）

网页配网模式支持：

- 保存 Wi-Fi + 服务器参数并重启
- 清除配置并重启

### 4.4 设备 HTTP 能力（本地服务）

内置 `ESP8266WebServer`，关键接口：

- `/status`：设备配网状态
- `/setLight`：接收亮度/色温/自动模式/推荐值/面料参数
- `/stopBroadcast`、`/resumeBroadcast`：控制 UDP 广播
- `/stopAnnounce`：控制向后端 announce 上报
- `/resetWifi`：清空配置并重启

### 4.5 云端通信能力

#### A. WebSocket（到后端）

- 连接地址：`ws://<serverHost>:<wsPort>/ws/device`
- 首包 register：包含 id/chipId/fwType/fwVersion/ip/mac
- 心跳：定时发送 `ping`
- 接收消息：
  - `state`：更新 brightness/temp/autoMode/recommend/fabric
  - `ota:update`：判断 fwType/版本号后执行 OTA

#### B. HTTP（到后端）

- 设备 announce：`/admin/device/announce`
- 上传光照：`/admin/lux/create`
- 上传停留时长：`/admin/duration/create`

### 4.6 OTA 升级

- 接收 WS `ota:update`
- 版本比较 `x.y.z`，仅目标版本更高时升级
- `ESPhttpUpdate` 执行下载与刷写
- 失败会重连 WebSocket，成功后设备重启

### 4.7 灯光控制策略

#### A. 基础 PWM 计算

- 将色温 2700~6500 映射冷暖通道比例
- 将亮度 0~100 映射 PWM 占空比
- 最终分别写入冷暖 LED 引脚

#### B. 自动模式 + ToF 联动

- 若 ToF 不可用：直接使用基础/推荐值
- 若 ToF 可用：
  - 根据距离判断“有人靠近/离开”
  - 使用过渡比例（约 2s）平滑切换亮度与色温
  - 离开后计算停留时长并上报
- 面料策略：当识别为 `polyester` 时控制 BLUR 引脚状态

### 4.8 发现与上线策略

- UDP 局域网广播 announce 包（可停用）
- HTTP announce 上报后台（后端返回 added=true 后停止广播/上报）
- 具备断网重连与回退配网逻辑（`ensureWiFiReady`）

---

## 5. 三端职责对照（你要的“各部分功能完整列表”）

### 前端（smart-light-front）

- 登录/注册与 token 管理
- 店铺初始化与配置维护
- 设备扫描、添加、删除、状态控制
- 云台控制/人流上传控制
- 光照/停留时长/人流可视化
- WebSocket 实时接收设备与分析结果

### 后端（smart-light-backend）

- 用户认证与权限控制（JWT）
- 店铺与设备管理
- 设备在线状态管理（心跳+定时离线扫描）
- 设备状态同步下发（WebSocket）
- 光照与停留时长存储、查询、汇总
- AI服务代理调用与结果广播
- OpenAPI 文档与统一异常响应

### 设备固件（8266.cpp）

- Wi-Fi 配网（保存配置 + SmartConfig + AP Portal）
- 传感器采集（BH1750/ToF）
- 灯光执行（PWM 冷暖光混光）
- 自动模式策略（距离/面料联动）
- 与后端双通道通信（HTTP + WS）
- OTA 在线升级

---

## 6. 技术栈总结

### 6.1 前端技术栈

- **Vue 3**（Composition API + SFC）
- **TypeScript**
- **Vite** 构建工具
- **Vue Router** 路由
- **Pinia**（已依赖，项目内状态管理能力预留）
- **Axios + Fetch**（双请求封装）
- **Chart.js** 图表可视化
- 浏览器 **WebSocket API**

### 6.2 后端技术栈

- **Java 17**
- **Spring Boot 4**
- **Spring Web MVC**
- **Spring WebSocket**
- **Spring Security**
- **JWT（java-jwt）**
- **MyBatis-Plus**
- **MySQL**
- **Lombok**
- **Hibernate Validator/Jakarta Validation**
- **Springdoc OpenAPI (Swagger UI)**
- **RestTemplate**（AI 外部服务调用）
- **Jackson**（JSON 序列化）

### 6.3 设备端技术栈

- **Arduino C++（ESP8266 SDK 生态）**
- `ESP8266WiFi`, `ESP8266WebServer`, `WiFiUdp`
- `WebSocketsClient`
- `ESP8266HTTPClient`, `ESP8266httpUpdate`
- `LittleFS`
- `ArduinoJson`
- `Adafruit_VL53L0X`, `BH1750`
- I2C（`Wire`）与 PWM 引脚控制

---

## 7. 关键结论（简要）

1. 这是一个完整的 IoT 灯控闭环系统，具备“采集-决策-控制-回传-可视化”。
2. 后端与固件之间通过 `/ws/device` + HTTP 上报构成稳定控制链路。
3. 前端功能较完整，分析页中 `analytics` 两个接口存在“前端预留、后端未实现”迹象。
4. 系统已经具备企业化要素：鉴权、数据分店铺隔离、实时推送、可扩展 AI 接入、OTA 升级。
