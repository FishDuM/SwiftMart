# SwiftMart Frontend

SwiftMart 秒杀商城前端，使用 Vue 3、Vite、TypeScript、Pinia、TanStack Query、Naive UI 构建。

## 启动

```bash
npm install
npm run dev
```

默认通过 Vite 代理访问后端：

```text
/api -> http://localhost:8911
```

如需指定后端地址，可设置：

```bash
VITE_API_BASE_URL=http://localhost:8911
```

## 模块边界

```text
src/shared       # 请求层、路由、存储、通用工具
src/entities     # 用户、商品、订单等后端资源模型
src/features     # 验证码、秒杀状态机、倒计时等业务能力
src/pages        # 页面组合，不承载复杂业务规则
src/app          # 应用壳层
```

## 后端对接约定

- 登录后使用 `Authorization: Bearer <token>` 请求受保护接口。
- 普通业务接口统一解包后端 `Response<T>`。
- 验证码接口返回的是第三方 `ApiResponse`，暂时独立处理。