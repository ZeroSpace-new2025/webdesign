# 重构实施规范（对外方法表 v2.0 落地）

> 本文件是本次重构的**唯一技术契约**。`对外方法表.md` 定义“有哪些方法、什么签名”，
> 本文件定义“代码放在哪、共用什么类型、表和字段叫什么”。
> 任何模块实现与本文冲突时，以本文为准；本文与 `对外方法表.md` 冲突时，先改需求口径再改代码。
>
> 版本：2026-01　对应 `对外方法表.md` v2.0

---

## 1. 包结构（硬性）

包根保持 `com.university.webdesign`，**四个模块从顶层包下沉为 `api` / `service` / `domain` / `repository` 下的子包**，
完全对齐《对外方法表》1.1 的目录：

```
com.university.webdesign
├── Init.java                          启动类
├── common/                            统一响应体、错误码、异常、分页、登录上下文
├── config/                            安全、Web、初始化、配置属性（不分模块）
├── api/
│   ├── menu/        ApiRecipeController, ApiMenuController
│   ├── order/       ApiOrderController, ApiOrderHistoryController
│   ├── operation/   ApiBlanketOrderController, ApiDeliveryController, ApiConfigController
│   └── user/        ApiUserController, ApiRoleController, ApiAuthController, ApiReportController
├── service/
│   ├── menu/        RecipeService, MenuService, MenuSnapshotService
│   ├── order/       OrderService, OrderQueryService, OrderStatisticsService
│   ├── operation/   BlanketOrderService, DeliveryService, ServiceWindowService
│   ├── user/        UserService, RoleService, AuthService
│   └── report/      ReportService, ConsumptionAuditService
├── service/impl/    上述全部接口的实现（*Impl），按同名子包分组：
│                    impl/menu, impl/order, impl/operation, impl/user, impl/report
├── domain/          实体、值对象、枚举、领域事件，按模块子包分组：
│                    domain/menu, domain/order, domain/operation, domain/user, domain/report
├── repository/      数据访问接口，按模块子包分组：repository/{menu,order,operation,user,report}
└── event/           领域事件与监听器（listener 子包）
```

分层红线（沿用《对外方法表》第七章）：

- 只允许 `api → service → repository` 与 `service → service`；
- 禁止 `api → api`、`service → api`、`api → repository`；
- **`api` 包只放 Controller（+ 该 Controller 专属的请求/响应 DTO），不写任何业务判断**；
- DTO/VO/Query/Cmd 一律放在**使用它的那一层**：只被 api 层使用的放 `api/<模块>`，
  跨层契约（service 入参出参）放 `service/<模块>/dto`，实体与枚举放 `domain/<模块>`。

### 1.1 页面控制器

Thymeleaf 页面控制器**继续保留**，统一放在 `web/` 包：

```
web/
├── PageController.java            登录页/控制台入口
├── menu/RecipePageController, MenuPageController     → templates/menu, templates/recipe
├── order/OrderPageController                          → templates/ordertransaction
└── operation/…, user/…                                （按需新增）
```

页面控制器只允许调用 `service` 包的接口，**不得**依赖 `impl` / `repository` / `domain` 的写操作，
也不得自己拼业务规则（截止时间、状态文案等一律由 service 判定后返回）。

---

## 2. common 包（已落地，直接使用）

| 类型 | 用途 |
|---|---|
| `Result<T>` | 统一响应体：`{code, success, message, data, traceId}`，`code=0` 成功 |
| `ErrorCode` | 统一错误码枚举（0/40001/40100/40300/40400/40901/40902/42201/42202/42203/50000） |
| `BusinessException` | service 层业务异常，带 `ErrorCode`，静态工厂：`paramInvalid/notFound/forbidden/stateConflict/outOfWindow/ruleViolation` |
| `PageQuery` | 分页入参 `{pageNum, pageSize}`，`normalizedPageNum()/normalizedPageSize()/toSpringPageNumber()` |
| `PageResult<T>` | 分页出参 `{total, list}`，`empty()/of(list)/from(Page, fn)/ofPage(list, num, size)` |
| `DateRange` | 闭区间日期范围 `{from, to}`，`startDateTime()/endDateTimeExclusive()` |
| `UserContext` | 登录用户 `{userId, employeeNo, name, deptId, workstation, phone, roles, permCodes}` |
| `UserContextHolder` | 上述的 ThreadLocal：`get()/require()/currentUserId()/set()/clear()` |
| `GlobalExceptionHandler` | **全模块唯一**的 `@RestControllerAdvice`，勿再新建 |

**身份传递规则**：service 不再接收 `operatorId` 参数，一律通过
`UserContextHolder.require()` 取当前登录用户。定时任务/初始化等无请求上下文场景显式传参。

### 2.1 角色与权限编码（全项目统一常量）

角色：`MANAGER`(餐厅经理)、`KITCHEN_SUPERVISOR`(厨房主管)、`DELIVERY_STAFF`(配餐员)、
`FINANCE`(财务管理)、`EMPLOYEE`(企业员工)。

权限点：`menu:recipe:manage`、`menu:menu:manage`、`order:submit`、`order:invalidate`、
`order:view:all`、`operation:aggregate`、`operation:delivery:print`、`operation:window:manage`、
`user:manage`、`role:manage`、`report:view`、`report:export`、`audit:view`。

常量类：`com.university.webdesign.common.RoleCodes`、`com.university.webdesign.common.PermCodes`。

---

## 3. 数据库与实体命名（原文表名，显式 @Table/@Column）

沿用 `AGENTS.md` 约定：显式 `@Table(name="...")` 与 `@Column(name="...")`（下划线命名），
`@Entity + @Data/@Getter@Setter + @NoArgsConstructor`，依赖构造器注入，缩进用 Tab。

| 模块 | 表 | 实体 | 归属包 |
|---|---|---|---|
| M1 | `recipe` | `Recipe` | `domain/menu` |
| M1 | `menu` | `Menu` | `domain/menu` |
| M1 | `menu_item` | `MenuItem` | `domain/menu` |
| M1 | `menu_snapshot` | `MenuSnapshot` | `domain/menu` |
| M2 | `order_form` | `OrderForm` | `domain/order` |
| M2 | `order_detail` | `OrderDetail` | `domain/order` |
| M3 | `service_window` | `ServiceWindow` | `domain/operation` |
| M3 | `daily_statistics` | `DailyStatistics` | `domain/operation` |
| M3 | `delivery_task` | `DeliveryTask` | `domain/operation` |
| M4 | `users` | `User` | `domain/user` |
| M4 | `roles` | `Role` | `domain/user` |
| M4 | `permissions` | `Permission` | `domain/user` |
| M4 | `monthly_report` | `MonthlyReport`（+`MonthlyReportItem`） | `domain/report` |

> 历史遗留名说明：`my_order`/`order_item` 是重构前的表名，重构后统一为需求原文的
> `order_form`/`order_detail`。`user` 是 PostgreSQL 保留字，用户表名继续用 `users`。

关键字段（其余按各模块章节）：

- `OrderForm`：`employee_id`、`order_date`(LocalDate)、`total_amount`、`status`、
  `idempotency_key`(unique)、`version`(乐观锁 `@Version`)、`remark`、
  `invalidated_by`、`invalidate_reason`、`invalidate_time`、`created_at`、`updated_at`。
- `OrderDetail`：`order_id`、`recipe_id`、`recipe_name`、`category`、`unit`、
  `unit_price`、`quantity`、`amount`（全部为**下单时刻快照**）。
- `MenuItem`：`menu_id`、`recipe_id`、`recipe_name`、`category`、`unit`、`image_url`、`menu_price`。
- `MenuSnapshot`：`menu_id`、`recipe_id`、`recipe_name`、`category`、`unit`、`unit_price`、
  `menu_price`、`frozen_at`（**只允许插入，不允许更新/删除**）。
- `ServiceWindow`：`cutoff_time`、`delivery_start_time`、`effective_from`、`scope`(GLOBAL/DEPT)、`dept_id`。
- `DailyStatistics`：`statistics_date`(unique)、`total_orders`、`total_amount`、`status`、
  `items`（`@ElementCollection` 存 `statistics_item`：`category`/`recipe_id`/`recipe_name`/`unit`/`quantity`/`amount`）。
- `DeliveryTask`：`statistics_date`、`employee_id`、`workstation`、`phone`、`status`、
  `print_count`、`printed_at`、`receiver`、`remark`、`delivered_at`、`items`（`delivery_task_item`：`recipe_name`/`quantity`/`unit`）。
- `User`：`employee_no`(unique)、`password`(BCrypt)、`name`、`dept_id`、`dept_name`、`workstation`、
  `phone`、`status`(ACTIVE/DISABLED/LOCKED)、`roles`(ManyToMany `user_role`)。
- `Role`：`role_code`(unique)、`role_name`、`description`、`permissions`(ManyToMany `role_permission`)。
- `Permission`：`perm_code`(unique)、`perm_name`、`module`、`description`。
- `MonthlyReport`：`report_month`(unique, `yyyy-MM`)、`total_quantity`、`total_amount`、`order_count`、
  `generated_at`、`items`（`monthly_report_item`：`recipe_name`/`category`/`quantity`/`amount`）。

---

## 4. 贯穿全模块的既有业务不变量（不得破坏）

1. **时间窗口**：当日订单必须在“订餐截止时间”（默认 09:00）前提交；截止后禁止新增/修改。
   截止时间**唯一数据来源**是 `ServiceWindowService.get(date, scope, deptId)`，
   不得再出现 `order.cutoff-time` 这类散落配置。
2. **一人一天一单**：`(employee_id, order_date)` 对**有效订单**唯一，服务层校验 + 数据库唯一索引兜底。
3. **数据快照**：`order_detail` 冗余存放下单时刻菜名/分类/单位/单价/数量；
   菜谱或菜单的修改、删除、下架**只影响未来菜单**，不影响历史订单与历史菜单。
4. **菜单发布冻结**：`MenuService.publish` 必须调用 `MenuSnapshotService.freeze(menuId)` 写入 `menu_snapshot`，
   发布后 `menu` 锁定（`locked=true`），不可再编辑/删除，只能下架。
5. **删单留痕**：经理作废违规订单走 `status=INVALID` + 审计字段（作废人/原因/时间），**不做物理删除**。
6. **配送时间触发**：到达“配餐开始时间”（默认 11:30）后才开放配送单生成/打印。
7. **聚合时机**：总括订单只在截止时间之后汇总当日**有效订单**（`PENDING`/`VALID`）。
8. **事务**：统一用普通 `@Transactional`，**不要为了查询加 `readOnly = true`**。
9. **错误码**：业务失败一律抛 `BusinessException(ErrorCode.XXX, "中文提示")`，不返回裸 500。

---

## 5. 各模块方法清单与落点

### M1 菜品与菜单中心 `api/menu` + `service/menu` + `domain/menu` + `repository/menu`

Controller（基础路径 `/api/v1/menu`）：

| 类 | 方法 | HTTP |
|---|---|---|
| `ApiRecipeController` | `create` | `POST /recipes` |
| | `update` | `PUT /recipes/{recipeId}` |
| | `disable` | `DELETE /recipes/{recipeId}` |
| | `detail` | `GET /recipes/{recipeId}` |
| | `page` | `GET /recipes` |
| | `uploadImage` | `POST /recipes/images` |
| | `categories` | `GET /recipes/categories` |
| `ApiMenuController` | `createDraft` | `POST /menus` |
| | `update` | `PUT /menus/{menuId}` |
| | `publish` | `POST /menus/{menuId}/publish` |
| | `unpublish` | `POST /menus/{menuId}/unpublish` |
| | `current` | `GET /menus/current` |
| | `page` | `GET /menus` |

Service 方法（严格按《对外方法表》2.2）：

```java
// RecipeService
Long create(RecipeCreateCmd cmd);
void update(Long recipeId, RecipeUpdateCmd cmd);
int disable(Long recipeId, String reason);
RecipeVO getById(Long recipeId);
PageResult<RecipeVO> page(RecipeQuery q);
String uploadImage(MultipartFile file);
List<CategoryVO> listCategories();

// MenuService
Long createDraft(MenuCreateCmd cmd);
int update(Long menuId, MenuUpdateCmd cmd);          // 返回新版本号
PublishResultVO publish(Long menuId);
void unpublish(Long menuId, String reason);
MenuVO getCurrent(LocalDate date);                    // 跨模块主入口（M2 下单取价）
PageResult<MenuVO> page(MenuQuery q);                 // withItems=true 带快照供复用

// MenuSnapshotService
int freeze(Long menuId);                              // 只插不改
```

- `MenuVO` 必须含 `menuId`、`name`、`status`、`version`、`effectiveDate`、
  `publishedAt`、`offlineAt`、`items: List<MenuItemVO>`；
  `MenuItemVO{itemId(菜谱ID), recipeId, recipeName, category, unit, unitPrice, menuPrice, imageUrl}`。
- `MenuVO.items[].menuPrice` 是**下单取价字段**（菜单级调价优先，为空回退 `unitPrice`）。
- `RecipeStatus`：`ACTIVE` / `DISABLED`；`MenuStatus`：`DRAFT` / `PUBLISHED` / `OFFLINE`。
- `RecipeService.disable`：被“已发布菜单”引用时抛 `ErrorCode.DATA_PROTECTED`(42203)，返回受影响菜单数。
- 分类当前由 `recipe.category` 去重聚合，`CategoryVO{categoryId, categoryName, recipeCount}`，
  `categoryId` 用分类名哈希或序号，文档标注“未来可独立建分类表”。

### M2 订单与交易核心 `api/order` + `service/order` + `domain/order` + `repository/order`

Controller（基础路径 `/api/v1/order`）：

| 类 | 方法 | HTTP |
|---|---|---|
| `ApiOrderController` | `window` | `GET /windows` |
| | `submit` | `POST /orders`（请求头 `Idempotency-Key` 必填） |
| | `modify` | `PUT /orders/{orderId}` |
| | `cancel` | `POST /orders/{orderId}/cancel` |
| | `invalidate` | `DELETE /orders/{orderId}` |
| | `detail` | `GET /orders/{orderId}` |
| | `page` | `GET /orders` |
| `ApiOrderHistoryController` | `history` | `GET /orders/history` |
| | `monthlySummary` | `GET /orders/history/monthly-summary` |
| | `export` | `GET /orders/history/export` |

Service 方法（严格按《对外方法表》3.2，另补 `getWindow` 供 M2-01 调用）：

```java
ServiceWindowVO getWindow(LocalDate date);
OrderVO submit(OrderSubmitCmd cmd);
void modify(Long orderId, OrderModifyCmd cmd);
void cancel(Long orderId, String reason);
String invalidate(Long orderId, String reason);
int countDailyOrders(Long employeeId, LocalDate date);

OrderVO getDetail(Long orderId);
PageResult<OrderVO> page(OrderQuery q);
PageResult<OrderVO> pageHistory(Long employeeId, DateRange r, PageQuery p);
List<OrderBriefVO> listValidByDate(LocalDate date);   // 供 M3 聚合/配送

MonthlySummaryVO monthlySummary(Long employeeId, YearMonth month);
Resource exportHistory(Long employeeId, YearMonth month, ExportFormat fmt);
```

- `OrderStatus`：`PENDING`(待确认/有效) / `VALID`(已确认/有效) / `INVALID`(经理作废) / `CANCELLED`(本人取消)。
  `isActive()` 为 `PENDING|VALID`；`isEditable()` 为 `PENDING`。
  中文展示：待确认 / 已确认 / 已作废 / 已取消。
- 下单校验链顺序：① 当前用户 → ② `ServiceWindowService.get` 时间窗口（超时 42201）
  → ③ `(employeeId, orderDate)` 唯一（42202）→ ④ `MenuService.getCurrent` 取菜单并校验菜品在菜单内
  （不在菜单内 40001，菜单未发布 40902）→ ⑤ 写明细快照 → ⑥ 算总价 → ⑦ 写 `order_form` → ⑧ 发 `OrderCreatedEvent`。
- 幂等：`Idempotency-Key` 为空抛 40001；命中已存在 key 时**直接返回既有订单**（不重复下单）。
- 越权：`getDetail`/`modify`/`cancel` 只允许本人；经理或财务可 `page` 全量与他人历史，否则 40300。
- `invalidate` 仅 `MANAGER`（权限点 `order:invalidate`），返回 `auditId`，写审计字段，不物理删除。
- 订单号：沿用 `yyMMdd + 5 位当日流水`，落在 `order_form.order_no`？——**否**：本版使用
  `order_no` 字符串（`LOCALDATE + 4 位流水`），保留 `Long orderNumber` 兼容字段的旧逻辑删除；
  `OrderVO.orderNo` 为 `String`。
- `exportHistory` 用 `XLSXWriter`/简单 CSV 实现，`PDF` 若无库则抛 `ErrorCode.PARAM_INVALID` 并提示仅支持 XLSX/CSV。

### M3 运营与履约系统 `api/operation` + `service/operation` + `domain/operation` + `repository/operation`

Controller（基础路径 `/api/v1/operation`）：

| 类 | 方法 | HTTP |
|---|---|---|
| `ApiBlanketOrderController` | `aggregate` | `POST /blanket-orders/aggregate` |
| | `list` | `GET /blanket-orders` |
| | `print` | `GET /blanket-orders/print` |
| | `byCategory` | `GET /blanket-orders/by-category` |
| | `dailyStat` | `GET /statistics/daily` |
| | `refresh` | `POST /statistics/daily/refresh` |
| `ApiDeliveryController` | `generate` | `POST /deliveries/generate` |
| | `page` | `GET /deliveries` |
| | `detail` | `GET /deliveries/{taskId}` |
| | `batchPrint` | `POST /deliveries/batch-print` |
| | `updateStatus` | `PUT /deliveries/{taskId}/status` |
| | `export` | `GET /deliveries/export` |
| `ApiConfigController` | `create` | `POST /configs/service-window` |
| | `update` | `PUT /configs/service-window/{configId}` |
| | `get` | `GET /configs/service-window` |

Service 方法（严格按《对外方法表》4.2）：

```java
// BlanketOrderService
DailyStatVO aggregate(LocalDate date, boolean force);
List<CategoryStatVO> listAggregated(LocalDate date, Long categoryId);
Resource printProductionOrder(LocalDate date, List<Long> categoryIds, PrintFormat fmt);
List<CategorySumVO> sumByCategory(LocalDate date);
PageResult<DailyStatVO> pageDailyStat(DateRange r, PageQuery p);
void refreshDailyStat(LocalDate date);

// DeliveryService
List<Long> generateTasks(LocalDate date, Long deptId, String workstation);
PageResult<DeliveryTaskVO> page(DeliveryQuery q);
DeliveryTaskVO getDetail(Long taskId);
PrintBatchVO batchPrint(List<Long> taskIds, String templateId);
void updateStatus(Long taskId, TaskStatus status, String receiver, String remark);
Resource export(LocalDate date, Long deptId, ExportFormat fmt);

// ServiceWindowService
Long create(ServiceWindowCmd cmd);
void update(Long configId, ServiceWindowCmd cmd);
ServiceWindowVO get(LocalDate date, String scope, Long deptId);   // 无配置回退 09:00 / 11:30
```

- `TaskStatus`：`PENDING → DELIVERING → DELIVERED | EXCEPTION`，非法流转抛 40902。
- 定时任务：`OrderWindowCloseJob`（`0 0 9 * * ?` 发 `OrderWindowClosedEvent`）、
  `DeliveryOpenJob`（`0 30 11 * * ?` 预生成配送任务）；监听器订阅事件触发聚合/重算。
- `printProductionOrder` 返回纯文本生产单（`MediaType.TEXT_PLAIN`）或 XLSX；校验已过截止时间。

### M4 用户与报表中心 `api/user` + `service/{user,report}` + `domain/{user,report}` + `repository/{user,report}`

Controller（基础路径 `/api/v1/user`）：

| 类 | 方法 | HTTP | 说明 |
|---|---|---|---|
| `ApiAuthController` | `login` | `POST /auth/login` | 唯一免鉴权接口 |
| | `logout` | `POST /auth/logout` | |
| | `me` | `GET /auth/me` | |
| | `changePassword` | `PUT /auth/password` | |
| `ApiUserController` | `create` `page` `detail` `update` `disable` `importEmployees` `importTemplate` `changeStatus` `assignRoles` | `/users…` | 9 个 |
| `ApiRoleController` | `create` `page` `update` `delete` `permissions` `updatePermissions` `getPermissions` | `/roles…`、`/permissions` | 7 个 |
| `ApiReportController` | `generateMonthly` `getMonthly` `exportMonthly` `refreshMonthly` `employeeConsumption` `deptConsumption` | `/reports…` | 6 个 |

Service 方法（严格按《对外方法表》5.2）：`AuthService` 6 个、`UserService` 10 个、
`RoleService` 7 个、`ReportService` 4 个、`ConsumptionAuditService` 2 个。

- 认证采用 **JWT（HS256，`demo-secret` 可配置）+ `AuthInterceptor`**：
  `AuthService.verifyToken(token)` 解析出 `UserContext`，`AuthInterceptor` 写入
  `UserContextHolder` 并清理；登录态来源**不再接受前端自报 `operatorId`**。
- `logout` 用内存黑名单（`Map<String, Instant>`，TTL = 剩余有效期），文档标注“生产建议换 Redis”。
- 密码统一 BCrypt；`UserService.create` 初始密码缺省 `123456`。
- 角色/权限变更发 `RolePermissionChangedEvent`；用户信息变更发 `UserUpdatedEvent`。
- `ReportService.getMonthly` 优先读 `monthly_report` 缓存，缺失则同步生成。
- `ConsumptionAuditService` 取数走 `OrderQueryService`，**不得**直接访问订单表。

---

## 6. 跨模块依赖注入方向（只允许这些）

| 调用方 | 被调方 | 方法 |
|---|---|---|
| `OrderService` | `MenuService` | `getCurrent(date)` |
| `OrderService` | `ServiceWindowService` | `get(date, scope, deptId)` |
| `OrderService` | `AuthService` | `checkPermission(userId, permCode)` |
| `OrderQueryService` | `UserService` | `listByIds(userIds)` |
| `BlanketOrderService` | `OrderQueryService` | `listValidByDate(date)` |
| `BlanketOrderService` | `RecipeService` | `listCategories()` |
| `DeliveryService` | `OrderQueryService` | `listValidByDate(date)` |
| `DeliveryService` | `UserService` | `listByIds(userIds)` |
| `DeliveryService` | `ServiceWindowService` | `get(date, ...)` |
| `ReportService` | `OrderQueryService` | `page` / `listValidByDate` |
| `ConsumptionAuditService` | `OrderQueryService` | `getDetail` / `page` |
| `ConsumptionAuditService` | `UserService` | `listByIds(userIds)` |

**环形依赖风险**：`OrderService` 依赖 `AuthService`，而 `AuthService` 属于 M4。
为避免循环，`AuthService.checkPermission` 与 `UserService.hasAnyRole` 的实现
**不得反向依赖 M2/M3 的任何类型**。

---

## 7. 前端适配

- 现有 Thymeleaf 页面全部保留，页面控制器迁到 `web/` 包并改用新 service 契约。
- 现有 `static/js/app.js`、`login.js` 依赖 `result.success` 判定成败，`Result` 已保留该字段（见 2 节）。
- 页面里引用旧字段名的地方同步更新（如 `itemIds` → `items[].recipeId`、
  `OrderStatus` 中文文案 → 新状态文案）。
- `static/js` 里的接口路径统一改为 `/api/v1/...`。

---

## 8. 实施顺序与验收

1. `common` + `config`（安全、配置属性、数据初始化）
2. M1 → M4 → M2 → M3（M1/M4 是 M2/M3 的依赖）
3. `api` 层 Controller 全量补齐
4. `web/` 页面控制器适配
5. 清理：删除所有 `*page-info.java` 占位、重复实现类、旧包结构残留
6. `./gradlew compileJava` → `./gradlew test` → `./gradlew build` 全绿
7. 同步更新 `AGENTS.md` 第 2/3/6 节为新结构
