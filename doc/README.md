# 实现路径文档（按模块）

本目录按 **四个业务模块** 分别描述“一个功能从前端入口到数据库落库”的完整实现路径，
用于代码走读、答辩讲解和后续接手。所有描述都以当前代码为准（最近一次同步对应提交 `e232a97`，
`./gradlew build` 通过、四个模块页面与接口均已验证）。**代码改动后请同步更新本目录。**

| 文档 | 模块 | 负责人（AGENTS.md 2.1） |
| --- | --- | --- |
| [01-菜品与菜单中心.md](01-菜品与菜单中心.md) | M1 菜品与菜单中心 | 方家乐 |
| [02-订单与交易核心.md](02-订单与交易核心.md) | M2 订单与交易核心 | 雷伟舜 |
| [03-运营与履约系统.md](03-运营与履约系统.md) | M3 运营与履约系统 | 张颖茵 |
| [04-用户与报表中心.md](04-用户与报表中心.md) | M4 用户与报表中心 | 王家豪 |

需求来源：业务原文 `root_demend.md`（功能 1~8 + 数据需求 1），模块拆分与页面设计见 `demand.md`，
对外接口/方法签名与归属见 `对外方法表.md`，分层与表结构规范见 `REFACTOR_SPEC.md`。

---

## 一、统一的请求链路（所有模块共用）

```
浏览器
  │
  ├─ TraceIdFilter（最外层，写入/复用 X-Trace-Id，供 Result.traceId 串联日志）
  ├─ Spring Security 过滤链（SecurityConfig：CSRF 关闭、全部放行，认证交给下面的拦截器）
  ├─ UserContextCleanupFilter（finally 兜底清理 UserContextHolder，避免线程复用串号）
  │
  └─ DispatcherServlet
       ├─ AuthInterceptor.preHandle
       │    · 解析 token：Authorization: Bearer → token 请求头 → Cookie dsh_token
       │    · AuthService.verifyToken → UserContext 写入 UserContextHolder
       │      （校验签名/有效期/黑名单/改密时间戳，并回查库里的账号状态与角色权限；
       │        令牌里的 roles/permCodes 只是签发时快照，不作授权依据）
       │    · 页面请求未登录 → 302 /login；/api/** 保持 JSON 语义
       │    · @RequiresPerm 校验权限点/角色，越权抛 40300
       ├─ Controller（api/** 返回 Result<T>；web/** 返回视图名，页面控制器同样声明 @RequiresPerm）
       ├─ Service 接口 → Impl（@Transactional 一律在 service 层）
       ├─ Repository（Spring Data JPA）→ 数据库表
       ├─ （事务提交后，同一线程同步执行）event/listener 的 @TransactionalEventListener：
       │    重算 daily_statistics 等派生数据，监听方法一律 @Transactional(REQUIRES_NEW)
       ├─ NavPermissionInterceptor.postHandle（仅视图：注入 navPermCodes / navRoleCodes 给导航栏）
       └─ GlobalExceptionHandler（业务异常 → ErrorCode + 中文提示；页面请求渲染 error.html）
```

约定要点（详见 `AGENTS.md` 第 3 节）：

- **分层方向**：`api → service → repository`、`service → service`；api 不碰 repository，service 不用 Web 对象。
- **身份**：service 不接收 `operatorId`，统一 `UserContextHolder.require()`；权限点编码只来自
  `common/enums/PermissionEnum`，落库为 `role.permission_list` 位图。
- **失败**：业务错误抛 `BusinessException(ErrorCode, "中文")`，由 `GlobalExceptionHandler` 统一转换。
- **快照**：菜品/菜单/订单三类快照字段只增不改，历史数据不随主数据变化。
- **监听器**：跨模块协作只走进程内事件。`@TransactionalEventListener` 的监听方法必须
  `@Transactional(propagation = REQUIRES_NEW)`；由定时任务发布（无事务）的事件还要
  `fallbackExecution = true`，否则监听器会被静默跳过（两个坑的成因见 `AGENTS.md` 第 3 节）。

## 二、公共设施（被四个模块共用）

| 设施 | 位置 | 作用 |
| --- | --- | --- |
| `Result<T>` / `ErrorCode` / `BusinessException` | `common` | 统一响应体（`code=0` 成功 + `traceId`）与 11 个错误码 |
| `PageQuery` / `PageResult<T>` / `DateRange` | `common` | 统一分页与日期区间参数 |
| `UserContext` / `UserContextHolder` / `RoleCodes` | `common` | JWT 解析出的登录上下文、角色名称常量 |
| `NavContext` / `NavPermissionInterceptor` | `common` / `config` | 导航栏按权限渲染所需的模型属性 |
| `TabularExport` | `common` | CSV（UTF-8 BOM）/ SpreadsheetML 导出，不引入 POI |
| `TraceIdFilter` / `Result.traceId` | `config` / `common` | 请求追踪号，串联一次请求的所有日志 |
| `AuthInterceptor` / `RequiresPerm` / `SecurityConfig` / `WebConfig` | `config` | 认证、权限注解、白名单与静态资源映射 |
| `UserContextCleanupFilter` | `config` | 身份 ThreadLocal 兜底清理 |
| `GlobalExceptionHandler` | `common` | 唯一异常出口：`/api/**` 返回 JSON，页面返回 `error.html` |
| `PermissionEnum` / `PermissionList` / `RoleListConverter` | `common` | 权限字典、位图模型与「逗号位号字符串 ↔ BitSet」转换 |

## 三、16 张表与模块归属

| 模块 | 表 | 说明 |
| --- | --- | --- |
| M1 | `recipe` / `menu` / `menu_item` / `menu_snapshot` | 菜品库、菜单头、菜单-菜品关联、发布快照（只插不改） |
| M2 | `order_form` / `order_detail` | 订单主表（一人一天一单部分唯一索引）、下单时刻明细快照 |
| M3 | `service_window` / `daily_statistics` / `statistics_item` / `delivery_task` / `delivery_task_item` | 时间窗口、每日总括订单快照、配送任务与明细 |
| M4 | `users` / `role` / `user_role` / `monthly_report` / `monthly_report_item` | 员工、角色（权限位图）、用户-角色、月度报表缓存 |

建表与种子数据只认 `reploy/sql/schema.sql` / `seed.sql`（`prod`/`postgres` 的 `ddl-auto=validate`）。

## 四、页面总览（Thymeleaf）

| 页面 | 路由 | 控制器 / 视图 | 需要权限点 |
| --- | --- | --- | --- |
| 登录 | `/login` | `PageController.login` → `login.html` | 公开（白名单） |
| 总门户 | `/portal` | `PageController.portal` → `portal.html` | 登录即可 |
| 今日点餐 | `/order/create` | `OrderPageController.createPage` → `ordertransaction/order-create.html` | `order:submit` |
| 我的订单 | `/order/mine` | `OrderPageController.myOrdersPage` → `ordertransaction/my-orders.html` | `order:submit` |
| 个人月度汇总（可打印） | `/order/monthly` | `OrderPageController.monthlyReport` → `ordertransaction/monthly-summary.html` | `order:submit` |
| 订单管理（经理） | `/order/manage` | `OrderManagePageController` → `ordertransaction/order-manage.html` | `order:invalidate` |
| 菜品管理 / 历史菜品 | `/recipes`、`/recipes/history` | `RecipePageController` → `recipe/*.html` | `menu:recipe:manage` |
| 菜单管理 / 历史菜单 | `/menus`、`/menus/history` | `MenuPageController` → `menu/*.html` | `menu:menu:manage` |
| 厨房备料 / 历史快照 | `/operation/blanket-orders`、`/history` | `BlanketOrderPageController` → `operation/blanket-orders*.html` | `operation:aggregate` |
| 配送管理 / 详情 | `/operation/deliveries`、`/{id}` | `DeliveryPageController` → `operation/deliveries*.html` | `operation:delivery:print` |
| 时间配置 | `/operation/configs/service-window` | `ServiceWindowPageController` → `operation/service-window.html` | `operation:window:manage` |
| 员工月度消费报表 | `/reports/monthly`、`/employees/{id}` | `MonthlyConsumptionPageController` → `report/*.html` | `report:view` / `audit:view`（或 FINANCE / MANAGER） |
| 控制台 | `/console` | `PageController.console` → `console.html`（JS 调 `/api/v1/**`） | `user:manage` / `role:manage` / `report:view` / `audit:view`（或 FINANCE） |
| 打印页 | `/operation/blanket-orders/print/preview`、配送批量打印、`/order/monthly` | 独立纸质排版（内联样式 / `@media print`） | 随所属功能 |

页面统一复用 `templates/fragments/layout.html` 的 `head` + `navbar` 片段与 `static/css/style.css`，
导航栏与总门户入口按权限点显隐（见 `AGENTS.md` 第 3 节）。

## 五、常用验证命令

```bash
./gradlew build                                  # 编译 + 测试（提交前必跑）
./gradlew bootRun                                # dev（H2，自动执行 seed.sql）
.\gradlew.bat test --tests "*OrderAggregationRefreshTests" --console=plain   # 只跑某一组用例
psql -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/schema.sql
```

现有测试（`src/test/java`，全部 `@SpringBootTest` + H2 + seed）：

| 测试类 | 覆盖 |
| --- | --- |
| `WebdesignApplicationTests` | 上下文装配 |
| `impl/order/OrderAggregationRefreshTests` | 下单/改单后 `daily_statistics` 必须重算 |
| `impl/operation/WindowClosedAggregationTests` | 定时聚合（无事务入口）执行且覆盖重算 |
| `impl/user/AuthTokenStatusTests` | 停用账号 / 撤销权限 / 同秒重新登录的令牌语义 |
| `impl/user/JwtCodecTests` | 令牌唯一性（jti）与篡改拒绝 |

演示账号（`reploy/sql/seed.sql`）：`manager/admin123`、`finance/finance123`、`kitchen/kitchen123`、
`delivery/delivery123`、`zhangsan|lisi|wangwu|sunliu` 密码 `123456`。
