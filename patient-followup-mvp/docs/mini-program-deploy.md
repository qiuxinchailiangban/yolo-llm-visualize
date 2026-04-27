# 阶段一：让别人的微信扫码就能填问卷（cpolar + 体验版）

> 目标：从"只有自己电脑能用"升级到"最多 19 位体验成员，用自己手机扫码就能进入小程序填问卷"。
>
> 不需要买服务器、不需要域名、不需要 ICP 备案，1~3 天内可完成。

---

## 0. 总览：要做的 7 件事

```text
[1] 启动后端 (mysql,local)        ─┐
[2] 用 cpolar 把 8080 映射成 HTTPS │  ── 后端公网化
[3] 改后端 public-api-base-url    ─┘

[4] 改小程序 BASE_URL              ─┐
[5] 微信公众平台配 request 域名     │  ── 小程序绑定后端
[6] 上传小程序为体验版 + 加体验成员 ─┘

[7] 管理后台「环境自检」一键确认  ── 收尾验证
```

---

## 1. 启动后端（mysql,local 双 profile）

```powershell
cd E:\wxb\360sd\patient-followup-mvp\backend
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql,local"
```

启动成功的标志：终端打出 `Tomcat started on port 8080`。

确认本机能访问：浏览器开 [http://localhost:8080/api/public/intake-template](http://localhost:8080/api/public/intake-template) 应当返回 `{"success":true,...}`。

---

## 2. 安装 & 启动 cpolar，把 8080 映射成 HTTPS

### 2.1 注册并下载

1. 打开 [https://www.cpolar.com](https://www.cpolar.com)，用手机号注册（免费）
2. 登录控制台 → 左侧「**验证**」→ 拿到自己的 `authtoken`
3. 控制台「**下载**」→ 选 Windows 64 位版本
4. 解压到任意目录，比如 `D:\tools\cpolar`

### 2.2 配置 authtoken

打开 PowerShell，执行：

```powershell
cd D:\tools\cpolar
.\cpolar.exe authtoken 你的authtoken字符串
```

### 2.3 启动 HTTP 隧道

```powershell
.\cpolar.exe http 8080
```

启动后会看到类似这样的输出：

```text
Forwarding   https://abcd1234.r5.cpolar.top -> http://localhost:8080
Forwarding   http://abcd1234.r5.cpolar.top  -> http://localhost:8080
```

**记住那个 `https://abcd1234.r5.cpolar.top`**，后面所有地方填的都是它。

> ⚠️ **免费版隧道每 1~2 小时会变域名**。
>
> 长期用建议升级"专业版"（约 ¥10~30/月）拿固定二级域名，不然每次重启都要改 4 个地方。

### 2.4 验证隧道通

浏览器访问 `https://abcd1234.r5.cpolar.top/api/public/intake-template`，能拿到 JSON 就说明 OK。

---

## 3. 修改后端 `public-api-base-url`

打开 [`backend/src/main/resources/application-local.yml`](../backend/src/main/resources/application-local.yml)，找到这一行：

```yaml
    public-api-base-url: http://localhost:8080
```

改成：

```yaml
    public-api-base-url: https://abcd1234.r5.cpolar.top
```

**保存后重启后端**（停掉 mvn，再 `mvn spring-boot:run "-Dspring-boot.run.profiles=mysql,local"`）。

---

## 4. 修改小程序 `BASE_URL`

打开 [`config/local.js`](../config/local.js)（这个文件已经为你创建好，且加进 .gitignore），把：

```javascript
module.exports = {
  BASE_URL: "https://your-public-https-domain.com",
};
```

改成：

```javascript
module.exports = {
  BASE_URL: "https://abcd1234.r5.cpolar.top",
};
```

> 注意：BASE_URL 后面**不要带尾斜杠**，更不要带 `/api`。`utils/request.js` 里会自动拼 `/api/...`。

---

## 5. 微信公众平台配置「服务器域名」

> 这一步是给"上传后的小程序"看的，开发者工具里如果你勾了"不校验合法域名"可以先跳过，但**上传前必须配好**，否则真机扫码后 wx.request 会被拦截报错。

1. 打开 [https://mp.weixin.qq.com](https://mp.weixin.qq.com)，用小程序管理员微信扫码登录
2. 左侧菜单：**开发管理 → 开发设置 → 服务器域名**
3. 点击「**修改**」（每月最多 5 次！修改后微信扫码确认）
4. 在 **request 合法域名** 框里加上：
   ```text
   https://abcd1234.r5.cpolar.top
   ```
5. 保存

> ⚠️ cpolar 的 `cpolar.top` / `cpolar.cn` 域名都是经过 ICP 备案的（cpolar 公司的备案），所以微信会接受。这也是这个阶段方案的关键。

---

## 6. 微信开发者工具：上传 + 设为体验版

### 6.1 打开项目

1. 微信开发者工具 → 导入项目 → 项目目录选 `E:\wxb\360sd\patient-followup-mvp`
2. AppID 自动识别为 `wx992a7b0583a26115`
3. 不勾选"使用云开发"

### 6.2 本地先验证一次

1. 工具右上角「**详情** → **本地设置**」：
   - ✅ 不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书
   （仅自己用，方便 debug）
2. 「**编译**」→ 看看小程序能不能正常加载首诊问卷
3. 如果加载失败，先回到第 4 步检查 `BASE_URL`

### 6.3 上传

1. 工具右上角「**上传**」按钮
2. 版本号：`1.0.0`
3. 项目备注：`首版体验`
4. 点上传

### 6.4 在 mp 后台设为体验版

1. mp 后台 → **版本管理 → 开发版本**
2. 找到刚上传的 `1.0.0` → 点「**选为体验版**」
3. 体验版生效后，「体验版」一栏就有这个版本了

### 6.5 添加体验成员

1. mp 后台 → **管理 → 成员管理 → 体验成员**
2. 添加微信号（最多 19 个）
3. 被加的人会收到模板消息确认

---

## 7. 收尾验证：管理后台一键自检

1. 启动管理后台：

   ```powershell
   cd E:\wxb\360sd\patient-followup-mvp\admin-web
   npm install
   npm run dev
   ```

2. 浏览器开 [http://localhost:5173](http://localhost:5173)
3. 用 `admin / admin123456` 登录
4. 首页右上角点 **「环境自检」**

期望结果（绿色全 OK）：

| 检查项 | 期望 |
|---|---|
| 微信小程序 appId / appSecret | OK |
| 微信 access_token 联通性 | OK，提示"成功拿到 access_token" |
| 对外公网域名 (public-api-base-url) | OK，是 https:// 开头的 cpolar 域名 |
| 管理端 CORS 允许来源 | OK |
| RPA 集成 | OK 或 提醒（取决于你是否要发提醒） |
| desktop-worker 共享 token | OK |

如果某一项是红色「阻塞」，按它给的「立即修复」提示去改。

---

## 8. 真正生成二维码并扫码测试

1. 后台 → **问卷模板** 页面
2. 找一个状态为 `ACTIVE` 的「首诊问卷」
3. 点「**生成二维码**」→ 点「**预览二维码**」→ 下载 PNG
4. 把 PNG 发到自己微信收藏 / 朋友圈 / 任意设备
5. **用体验成员的微信**长按图片 → 「识别小程序码」
6. 应该能直接进入"问卷填写"页

期间在后端终端，你应该能看到这样的日志：

```text
[qrcode] 调用微信官方 getwxacodeunlimit 接口，token=ABcdef12... type=TEMPLATE
[scan]   收到小程序扫码请求 token=ABcdef12...
[scan]   token=ABcdef12... 类型=TEMPLATE 第 1 次扫码命中
```

看到 `[scan]` 这条日志，就证明**整条链路彻底跑通了**。

---

## 9. 常见报错速查

| 现象 | 原因 | 解决 |
|---|---|---|
| 自检"access_token 联通性"失败：`invalid appid` | appId 拼错 | 改 `application-local.yml` |
| 自检"access_token 联通性"失败：`invalid appsecret` | secret 错或被重置 | 在 mp 后台「开发设置」重置 secret 后改回来 |
| 自检"access_token 联通性"失败：`40164 invalid ip xxx not in whitelist` | 微信 IP 白名单开了，你公网 IP 不在内 | mp 后台 → 开发设置 → IP 白名单 → 加上 cpolar 出口 IP，或暂时关闭白名单 |
| 体验版扫码后白屏 | `BASE_URL` 还是 localhost；或没设服务器域名；或没把人加到体验成员 | 回到第 4、5、6 步逐项排查 |
| 真机扫码报"该小程序未发布" | 上传后没设为体验版，或扫码人不是体验成员 | 回到第 6.4 / 6.5 |
| 后端 `[scan]` 报 `二维码不存在或已失效` | 体验版扫的是上一次启动时生成的二维码，token 已过期 | 重新生成二维码 |
| `Forbidden` / 跨域报错 | CORS 没加管理端正式域名 | `application.yml` → `app.cors.allowed-origins` |

---

## 10. 切到正式生产时要做的事（阶段二）

阶段一的 cpolar 方案适合**自己 + 19 个体验成员**。要给医院全院/真实患者用，必须升级：

- [ ] 把小程序主体改为**企业主体**（医院或挂靠公司），并申请医疗类目
- [ ] 买云服务器（腾讯云/阿里云轻量 2C2G ≈ ¥70/月起）
- [ ] 买域名（.com ≈ ¥55/年）
- [ ] 提交 **ICP 备案**（10~20 工作日，云厂商免费代办）
- [ ] 用 **宝塔面板 / Docker** 把后端、MySQL、Redis 部署到云上
- [ ] 申请免费 HTTPS 证书（Let's Encrypt 或云厂商）
- [ ] 把 `application-local.yml` 的 `public-api-base-url` 改为正式域名
- [ ] mp 后台把正式域名加入 request 合法域名
- [ ] 小程序版本提交审核 → 发布为正式版
- [ ] 数据合规：补充《用户隐私协议》《知情同意》，并在 mp 后台做隐私接口声明

需要哪一步细化，单独提就行。
