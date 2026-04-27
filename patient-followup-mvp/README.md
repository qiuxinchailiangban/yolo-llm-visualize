# patient-followup-mvp

医院患者随访问卷管理系统 MVP，包含：

- `backend`：Spring Boot 后端
- `admin-web`：Vue 3 管理后台
- `miniapp`：微信小程序 MVP
- `db/mysql-schema.sql`：MySQL 建表脚本
- `docs/architecture.md`：架构设计说明

## 快速启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认使用内存 H2 启动，便于快速演示；切换到 MySQL 时可参考 `application-mysql.example.yml`。

### 后端切换 MySQL

1. 执行 `db/mysql-schema.sql`
2. 配置环境变量：

```bash
set DB_URL=jdbc:mysql://127.0.0.1:3306/patient_followup?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
set DB_USERNAME=root
set DB_PASSWORD=your_password
set REDIS_HOST=127.0.0.1
set REDIS_PORT=6379
```

1. 使用 MySQL profile 启动：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 本地专用配置模板

如果你希望把本机的数据库密码、微信 `AppSecret` 等私密配置放到本地文件，而不是每次手动输入环境变量，可以这样做：

1. 复制模板文件：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

1. 编辑你自己的本地配置文件：

- [application-local.example.yml](file:///e:/wxb/360sd/patient-followup-mvp/backend/src/main/resources/application-local.example.yml)
- 复制后的实际文件：`backend/src/main/resources/application-local.yml`

1. 使用 `mysql,local` 双 profile 启动：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql,local"
```

说明：

- `application-local.yml` 仅供你自己本机使用
- 该文件已经加入 [.gitignore](file:///e:/wxb/360sd/patient-followup-mvp/.gitignore)，不会默认提交
- 推荐把 `app-secret`、数据库密码、公网地址等都写在这个本地文件里
- 本地第一次连 MySQL 时，建议在 `application-local.yml` 里保留：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

- 否则如果数据库里还没有业务表，启动时会报 `missing table`

### 为什么写了 app-secret 还不行

最常见是下面 3 个原因：

1. 你启动后端时没有带 `local` profile，所以 `application-local.yml` 根本没生效

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql,local"
```

1. 小程序请求地址还是 `localhost`

- 这对你自己电脑可以，对别人手机不可以
- 别人扫码后，小程序必须请求你的公网 HTTPS 后端地址

1. 你当前配置的是 `trial`

- `trial` 版本只能指定体验成员使用
- 如果你要“别人”都能扫，通常要发 `release`
- 或先把对方加入体验成员

### 让别人真的能扫码填写

要满足下面 4 条：

1. 后端用 `mysql,local` 成功启动
2. 管理端里至少存在一个 `ACTIVE` 的 `首诊问卷`
3. 小程序请求地址不能是 `localhost`，必须是公网 `HTTPS` 后端地址
4. 小程序版本对扫码用户可用：
   - 体验版：对方必须在体验成员里
   - 正式版：使用 `release`

### 管理端

```bash
cd admin-web
npm install
npm run dev
```

- 登录页地址：`/login`
- 默认账号：`admin`
- 默认密码：`admin123456`

### 小程序

- 用微信开发者工具打开 `miniapp`
- 默认接口地址为 `http://localhost:8080`
- 真机调试、别人扫码、外部访问时，不能继续用 `localhost`
- 请复制一份小程序本地配置：

```powershell
New-Item -ItemType Directory -Force config
Copy-Item config/local.example.js config/local.js
```

- 然后把 `config/local.js` 中的 `BASE_URL` 改成你的真实后端 HTTPS 地址

## 小程序二维码联动

当前已支持两类二维码：

- 首诊模板二维码：在 `问卷模板` 页面为 `首诊问卷` 生成二维码
- 患者任务二维码：在 `问卷任务` 页面为具体任务生成二维码

开发环境下：

- 若未配置微信小程序 `appId/appSecret`，系统会生成调试二维码
- 调试二维码内容会指向后端 `/api/public/qrcode/resolve`，用于先跑通 token 解析链路

正式接入微信小程序码时，请配置后端环境变量：

```powershell
$env:WECHAT_MINIAPP_APP_ID="你的小程序AppID"
$env:WECHAT_MINIAPP_APP_SECRET="你的小程序AppSecret"
$env:WECHAT_MINIAPP_PAGE_PATH="pages/questionnaire/index"
$env:WECHAT_MINIAPP_ENV_VERSION="trial"
$env:WECHAT_MINIAPP_PUBLIC_API_BASE_URL="你的后端公网地址"
```

配置完成后，后台导出的二维码会自动切换为微信官方小程序码。

## 第二版新增能力

- 后台登录骨架与 token 鉴权
- 随访阶段管理
- 问卷任务管理
- MySQL profile 持久化配置
- 患者/手术名单 CSV 导入

## CSV 导入患者/手术名单

当前已经支持在管理端 `患者管理` 页面直接导入患者/手术名单 CSV，用于批量维护患者基础信息、自动生成随访任务，并联动首页待办。

导入入口：

- 后台 `患者管理` 页面右上角 `CSV 导入`
- 支持直接粘贴 CSV 文本
- 支持选择本地 `.csv` 文件

CSV 表头顺序：

```text
姓名,手机号,手术日期,性别,出生日期,诊断,来源
```

示例：

```text
姓名,手机号,手术日期,性别,出生日期,诊断,来源
张三,13800000001,2026-04-25,男,1980-01-10,膝关节置换,CSV_IMPORT
李四,13800000002,2026-04-28,女,1976-08-21,白内障,CSV_IMPORT
```

导入规则：

- 优先按 `姓名 + 手机号 + 手术日期` 匹配已有患者
- 若手机号为空，则退化为按 `姓名 + 手术日期` 匹配
- 匹配成功则更新患者信息，并重建未完成的随访任务
- 已完成任务会保留，不会因为重新导入被删除
- 未匹配到则新建患者，并根据启用中的随访阶段自动生成任务

首页待办联动：

- `今天要手术的患者`：读取 `patient.surgeryDate = 今天`
- `今天应填未填`：读取 `questionnaire_task.dueDate = 今天` 且状态为 `PENDING/OVERDUE`
- `可发送提醒`：读取 `dueDate <= 今天` 且状态为 `PENDING/OVERDUE`

所以只要 CSV 中导入了手术日期，且系统已配置启用中的随访阶段和对应模板，首页待办会自动反映导入后的患者和任务数据。

## 自动化任务中心 + RPA Worker

当前已把原来的“后台同步直调 RPA”升级为“后台只创建自动化任务，本地 desktop worker 领取并执行”。

现在的链路：

- 管理端在 `问卷任务` 页面点击 `发送提醒`
- 后端创建 `reminder_task` + `automation_job`
- 本地 `desktop-worker` 轮询领取 `WECHAT_RPA_SEND` 任务
- worker 本地调用 `omni_bot_sdk-1.0.6/send_once.py` 或 `send_later.py`
- 执行结果回写 `automation_job` 和 `reminder_task`
- 管理端可在 `自动化任务` 页面和 `问卷任务 -> 发送日志` 查看状态与日志

这样做的好处：

- Web 后台不再同步卡住等待微信 RPA
- 浏览器点击按钮后不会直接跟微信抢前台
- 后续接 `Agent`、`Skill` 时，只需要新增新的 `jobType` 和 worker

### 后端配置

可通过环境变量启用 RPA 和 worker 共享密钥：

```powershell
$env:RPA_INTEGRATION_ENABLED="true"
$env:OMNI_BOT_SDK_ROOT="E:/wxb/360sd/omni_bot_sdk-1.0.6"
$env:OMNI_BOT_PYTHON="python"
$env:OMNI_BOT_CONFIG_PATH="E:/wxb/360sd/omni_bot_sdk-1.0.6/config.yaml"
$env:AUTOMATION_WORKER_TOKEN="followup-worker-token"
```

也可以写入 `application-local.yml`：

```yaml
app:
  rpa-integration:
    enabled: true
    sdk-root: E:/wxb/360sd/omni_bot_sdk-1.0.6
    config-path: E:/wxb/360sd/omni_bot_sdk-1.0.6/config.yaml
    python-command: python
    send-script: send_once.py
    delayed-send-script: send_later.py
    send-mode: enter
    wait-seconds: 8
    timeout-seconds: 90
    default-countdown-seconds: 5
  automation-worker:
    token: followup-worker-token
```

### 启动 desktop worker

1. 复制 worker 配置模板：

```powershell
cd desktop-worker
Copy-Item config.example.json config.json
```

1. 按你的本机环境修改 `config.json`：

- `backend_base_url`：后端地址，默认 `http://localhost:8080`
- `worker_token`：要与后端 `app.automation-worker.token` 一致
- `sdk_root`：`omni_bot_sdk-1.0.6` 路径
- `config_path`：`omni_bot_sdk-1.0.6/config.yaml` 路径

1. 启动 worker（命令行模式）：

```powershell
cd desktop-worker
python worker.py
```

启动后会持续轮询后端的自动化任务。

1. 或启动带置顶日志面板的小窗口：

```powershell
cd desktop-worker
python worker_gui.py
```

图形模式特点：

- 小窗口始终置顶，可手动取消
- 可选择配置文件
- 可开始 / 停止 worker
- 实时查看 RPA 执行日志
- 适合放在微信窗口旁边观察执行过程

### Electron 桌面壳

如果你希望把当前的 worker 小窗升级成更像正式产品的桌面软件，现在已经提供了一版 Electron 外壳，位置在：

- `desktop-worker/electron-shell`

设计方式：

- Electron 负责桌面窗口、任务列表、统计、日志和置顶控制
- Python `worker.py` 继续负责领取自动化任务、调用 `omni_bot_sdk` 和回写结果
- 中间通过 `desktop-worker/electron_bridge.py` 做标准输出事件桥接

首次启动：

```powershell
cd desktop-worker/electron-shell
npm install
npm start
```

当前这版 Electron 壳已支持：

- `管理后台` 标签：直接在 Electron 里打开 `admin-web`，可进入患者管理、问卷任务、随访阶段、问卷模板等页面
- `执行中心` 标签：选择 `config.json`、启动 / 停止 Python worker
- 本机 `backend` 一键启动 / 停止
- 显示最近 20 条任务
- 成功 / 失败计数
- 当前倒计时状态
- 实时日志面板
- 根据配置自动应用“始终置顶”

说明：

- Electron 只是外壳，不替代 Python worker
- 这样后续你继续接 `RPA / Agent / Skill` 时，不需要推翻执行层，只要扩 UI 和 jobType 即可
- 当前 Electron 内嵌后台页面来自 `admin-web/dist`，因此如果你改了网页前端代码，记得先重新执行一次：

```powershell
cd admin-web
npm run build
```

### 发送提醒怎么用

- 后台 `问卷任务` 页面点击 `发送提醒`
- 填 `目标会话名`、`文案`
- 可设置 `发送前倒计时`
- 点击后任务会先入队
- 你需要在倒计时结束前切回微信窗口
- 实际执行由 `desktop-worker` 完成

### 状态查看

- `自动化任务` 页面：查看最近自动化任务、执行器、错误、完整日志
- `问卷任务 -> 发送日志`：查看当前任务对应的提醒发送历史

### 使用前提

- 微信桌面端已登录
- 微信窗口可以被本机 RPA 正常切换和操作
- `omni_bot_sdk-1.0.6/config.yaml` 配置正确
- `desktop-worker` 正在运行
- 目标会话名必须与微信里的实际会话名一致
- 管理端 `问卷任务` 页面
- 每条任务右侧 `发送提醒` 按钮

下一步建议：

- 给患者表补一个 `wechatContactName` 或 `wechatRemarkName` 字段
- CSV 导入时一起导入这个字段
- 首页 `可发送提醒` 列表直接一键发送
- 再进一步改成定时批量发送，而不是人工逐条确认
