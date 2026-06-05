# Issue #1 前端 Token 存储与刷新接入方案（草案）

## 1. 文档目的

本文是 `docs/issue-1-minimal-auth-contract.md` 第 10 节“前端存储约定”的落地草案，供后续前端页面真正开始实现时直接参考。

当前前端界面尚未建设，因此本文档先回答三个问题：

1. 第一阶段前端应把 Token 存在哪里。
2. 前端应如何接入 `Authorization: Bearer <accessToken>`。
3. accessToken 过期后，前端应如何刷新、重试、退出登录。

> 本文是“后续前端实现指导文档”，不是当前后端开发阻塞项。

---

## 2. 适用范围

本文默认适用于后续的 **Vite + Vue3** 前端实现。

与后端合约对应的接口如下：

- `POST /api/auth/wechat/qrcode`
- `GET /api/auth/wechat/poll?qrCodeId=...`
- `POST /api/auth/token/refresh`
- `POST /api/auth/logout`

本文只覆盖第一阶段最小方案，不覆盖：

- HttpOnly Cookie 方案
- XSS / CSRF 的生产级加固
- 多端会话管理
- Redis access-token 黑名单
- 单点登录 / 多账号体系

---

## 3. 第一阶段推荐方案

### 3.1 结论

第一阶段建议采用：

- **持久化存储：`localStorage`**
- **运行时读取：内存态缓存一份当前 Token**
- **请求头：`Authorization: Bearer <accessToken>`**
- **刷新方式：业务请求遇到鉴权失败后，调用 refresh 接口**
- **刷新成功后：覆盖本地双 Token，并重试原请求一次**
- **刷新失败后：清空本地 Token，并跳转/引导重新扫码登录**

### 3.2 为什么先选 `localStorage`

第一阶段优先选择 `localStorage`，原因是：

1. 页面刷新后 Token 仍可保留，便于开发调试。
2. 对接成本最低，适合当前“先打通最小闭环”的目标。
3. 与当前后端双 Token 合约匹配简单，不需要额外 Cookie 机制。

### 3.3 为什么不是 `sessionStorage`

`sessionStorage` 也可用，但它更适合“关闭标签页即退出”的场景。第一阶段如果想降低前端重复登录频率，`localStorage` 更稳妥。

### 3.4 为什么不是“只放内存”

只放内存的优点是关闭刷新即失效，但页面一刷新就丢失登录态，不利于第一阶段联调。因此更适合后续增强，而不是第一阶段默认方案。

---

## 4. 前端本地保存内容

### 4.1 最小必需字段

前端至少保存：

- `accessToken`
- `refreshToken`

### 4.2 建议的本地 Key

```text
qgb.accessToken
qgb.refreshToken
```

### 4.3 可选派生字段

如果前端想更平滑地控制刷新时机，也可以额外记录：

- `qgb.accessTokenExpireAt`
- `qgb.refreshTokenExpireAt`

注意：这两个字段属于**前端派生数据**，本质上可由 `expiresIn` / `refreshExpiresIn` 计算得到，不是后端额外契约。

---

## 5. 前端认证流程

## 5.1 登录成功后的保存动作

当轮询接口返回：

```json
{
  "code": "0000",
  "data": {
    "status": "CONFIRMED",
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "refreshExpiresIn": 2592000
  }
}
```

前端应立即：

1. 保存 `accessToken`
2. 保存 `refreshToken`
3. （可选）计算并保存绝对过期时间
4. 更新内存中的登录态
5. 跳转到登录后的页面

---

## 5.2 请求受保护接口

每次请求受保护接口时：

1. 从内存态读取 `accessToken`
2. 若内存为空，则从 `localStorage` 恢复
3. 设置请求头：

```http
Authorization: Bearer <accessToken>
```

如果当前没有 `accessToken`，则不应假装已登录，应该直接走未登录态。

---

## 5.3 accessToken 过期后的刷新流程

当业务接口返回“未登录 / accessToken 无效 / accessToken 已过期”语义时，前端执行：

1. 读取本地 `refreshToken`
2. 调用：

```http
POST /api/auth/token/refresh
```

请求体：

```json
{
  "refreshToken": "current-refresh-token"
}
```

3. 如果刷新成功：
   - 覆盖本地 `accessToken`
   - 覆盖本地 `refreshToken`
   - 更新内存态
   - 原业务请求 **仅重试一次**

4. 如果刷新失败：
   - 清空本地 `accessToken`
   - 清空本地 `refreshToken`
   - 清空内存态
   - 跳转/引导重新扫码登录

### 5.4 为什么“原请求只重试一次”

这是为了避免：

- accessToken 失效 → 触发 refresh
- refresh 又失败 / 新 token 仍不可用
- 业务请求无限循环重试

第一阶段建议：**一次请求最多自动 refresh 一次，随后最多重试原请求一次。**

---

## 5.5 Logout 流程

用户主动退出登录时，前端执行：

1. 读取当前 `refreshToken`
2. 调用：

```http
POST /api/auth/logout
```

请求体：

```json
{
  "refreshToken": "current-refresh-token"
}
```

3. 无论后端响应成功还是失败，前端都应结束本地登录态：
   - 删除本地 `accessToken`
   - 删除本地 `refreshToken`
   - 清空内存态
   - 返回未登录页面

说明：

- 后端 logout 的核心语义，是吊销后续 refresh 能力。
- 第一阶段不保证已签发 accessToken 立即失效。
- 因此前端退出时，**本地清理动作不能依赖服务端成功返回后才执行**。

---

## 6. 前端模块拆分建议

后续前端真正实现时，建议至少拆成 3 个模块：

### 6.1 `token-storage`

职责：

- 读写 `localStorage`
- 提供 `getAccessToken()` / `getRefreshToken()`
- 提供 `saveTokens()` / `clearTokens()`

### 6.2 `auth-service`

职责：

- 处理登录成功后的 Token 保存
- 调用 refresh 接口
- 调用 logout 接口
- 管理“当前是否正在 refresh”状态

### 6.3 `http-client`

职责：

- 给业务请求统一加 `Authorization` 请求头
- 识别鉴权失败响应
- 触发 refresh
- refresh 成功后重试原请求

这样做的好处是：

- Token 存储策略和接口调用策略分离
- 后续从 `localStorage` 迁移到 Cookie 或更强方案时，改动集中
- 业务页面无需关心 refresh 细节

---

## 7. 推荐状态机

```text
未登录
  -> 扫码成功并拿到双 Token
已登录
  -> accessToken 过期
刷新中
  -> refresh 成功
已登录
  -> refresh 失败 / logout
未登录
```

第一阶段不建议让业务页面自己拼接这些状态判断，最好收敛在统一的认证模块中。

---

## 8. 前后端联调时的最小检查清单

当前端开始实现时，建议按以下清单联调：

### 8.1 登录链路

- 轮询返回 `CONFIRMED` 后，前端能保存双 Token
- 页面刷新后，前端仍能从 `localStorage` 恢复登录态

### 8.2 业务请求链路

- 请求受保护接口时能自动带上 `Authorization` 头
- accessToken 正常时，请求成功

### 8.3 刷新链路

- accessToken 失效时能自动调用 refresh 接口
- refresh 成功后能覆盖本地双 Token
- refresh 成功后原请求能自动重试一次
- refresh 失败后能清理本地 Token 并跳回登录流程

### 8.4 退出链路

- logout 调用后，本地双 Token 被清理
- 退出后前端重新进入未登录态

---

## 9. 第一阶段明确不做

前端第一阶段明确不做：

- HttpOnly Cookie 登录态托管
- Token 加密存储
- 跨标签页复杂会话同步
- 多设备登录态列表
- accessToken 到期前的定时预刷新

> **关于刷新并发（single-flight）：** 后端 Issuse #3 实现了严格复用检测机制——如果一个 refresh token 被并发使用（例如多个业务请求同时触发 refresh），后端会按旧 token 复用处理，吊销整个 `token_family_id`，导致当前会话需要重新扫码登录。因此 **刷新并发风暴控制（single-flight queue）建议第一阶段就做**，至少确保同一时间只有一个 refresh 请求在途。可参考以下最小实现：在 `auth-service` 模块中用一个 Promise 锁，第一个 refresh 请求完成后 resolve 所有等待者，从而把 N 次并发 refresh 合并为一次。

这些能力可以等前端页面真正上线后，再作为第二阶段增强。

---

## 10. 后续落地建议

当前建议先把本文档保留在 `docs/` 下，作为“前端尚未实现前的设计输入”。

等后续开始建设前端页面时，再据此补齐：

1. 具体前端目录结构
2. API 调用封装
3. 登录页 / 扫码页交互
4. 刷新失败后的跳转策略
5. 页面级未登录态处理

届时如果前端技术栈、路由结构、状态管理方案（如 Pinia）已经明确，可以再把本文档升级成一份更具体的实现说明。
