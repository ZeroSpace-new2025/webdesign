# AGENTS.md

企业餐厅网络点餐系统（课程小组项目）。本文件是本仓库的**唯一协作约定入口**，所有人和 AI 代理在改代码前都要先读这里，再读：

1. `demand.md` —— 业务需求原文（权威来源）
2. `对外方法表.md` —— 模块对外方法表 v2.0（66 个 api 接口 + 72 个 service 方法的签名与归属）
3. `REFACTOR_SPEC.md` —— 本次重构的实施规范（包结构、表结构、字段命名、跨模块依赖方向）

三者冲突时的优先级：**用户明确指示 > `demand.md` > `对外方法表.md` > `REFACTOR_SPEC.md` > 本文件**。

## 1. 项目概览

- 技术栈：Java 21 + Spring Boot 4.0.8（Gradle 插件 `org.springframework.boot`）+ Gradle Wrapper 9.7.1
- 依赖：Spring Web MVC、Spring Data JPA、Spring Security、**Jakarta Bean Validation**、Thymeleaf（+ `thymeleaf-extras-springsecurity6`）、Lombok、PostgreSQL 驱动
- 数据库：**PostgreSQL**（生产）/ **H2**（`dev` 与测试，PostgreSQL 兼容模式）
- 构建工具：**只用 `gradlew`**（不要用系统 gradle，版本可能不一致）
- 包根：`com.university.webdesign`，启动类 `Init.java`
- 部署形态：**单体应用**（一个进程、一个数据库、一次部署）。模块之间**不走 HTTP**，只通过 service 接口直调 + 进程内事件协作。

## 2. 目录与模块边界（最重要）

项目按 `demand.md` 的 DDD 方案拆成 4 个模块，但**代码组织已从“一个模块一个顶层包”改为《对外方法表》1.1 的分层结构**：
四个模块作为 `api` / `service` / `domain` / `repository` 下的子包存在。

```
com.university.webdesign
├── Init.java                          启动类
├── common/                            统一响应体、错误码、异常、分页、登录上下文、导出工具
├── config/                            安全、认证拦截器、Web、定时任务、数据初始化、配置属性
├── event/                             领域事件 + listener/ 监听器
├── api/                               对外网络层：Controller + 该 Controller 专属 DTO
│   ├── menu/        ApiRecipeController, ApiMenuController
│   ├── order/       ApiOrderController, ApiOrderHistoryController
│   ├── operation/   ApiBlanketOrderController, ApiDeliveryController, ApiConfigController
│   └── user/        ApiUserController, ApiRoleController, ApiAuthController, ApiReportController
├── service/                           程序内部业务层：接口 + dto/
│   ├── menu/        RecipeService, MenuService, MenuSnapshotService
│   ├── order/       OrderService, OrderQueryService, OrderStatisticsService
│   ├── operation/   BlanketOrderService, DeliveryService, ServiceWindowService
│   ├── user/        UserService, RoleService, AuthService
│   ├── report/      ReportService, ConsumptionAuditService
│   └── impl/        上述接口的实现，按同名子包分组（impl/menu, impl/order, ...）
├── domain/                            实体、值对象、枚举（menu / order / operation / user / report）
├── repository/                        Spring Data JPA 仓储（menu / order / operation / user / report）
└── web/                               Thymeleaf 页面控制器（menu / order / ...）
```

### 2.1 模块归属

| 模块 | 职责 | 代码位置 | 负责人 |
| --- | --- | --- | --- |
| M1 菜品与菜单中心 | 食谱 CRUD、图片上传、菜单生成/版本/发布下架、菜单调价、菜单发布快照冻结 | `api/menu`、`service/{menu,impl/menu}`、`domain/menu`、`repository/menu` | 方家乐 |
| M2 订单与交易核心 | 点餐时间窗口、下单与改单、一人一天一单、个人历史与月度消费 | `api/order`、`service/{order,impl/order}`、`domain/order`、`repository/order` | 雷伟舜 |
| M3 运营与履约系统 | 截止后聚合总括订单、生产单打印、配送时间触发与批量配送单 | `api/operation`、`service/{operation,impl/operation}`、`domain/operation`、`repository/operation` | 张颖茵 |
| M4 用户与报表中心 | 登录认证、角色权限、员工维护与批量导入、月度报表与消费审计 | `api/user`、`service/{user,report,impl/user,impl/report}`、`domain/{user,report}`、`repository/{user,report}` | 王家豪 |
| 公共设施 | `Result`、`ErrorCode`、`BusinessException`、分页、`UserContextHolder`、导出工具 | `common` | 全员 |

### 2.2 分层红线

只允许 **`api → service → repository`** 与 **`service → service`**：

1. 禁止 `api → api`、`service → api`、`api → repository`；
2. `api` 层只做协议转换与 `Result` 组装，**不写业务判断、不开事务、不碰 repository**；
3. **不要跨模块直接读写对方的表。** 需要别人的数据就调对方的 Service 接口；
4. 数据所有权单一：员工基础数据只在 M4 改，菜品标准信息只在 M1 改，订单状态只在 M2 改；
5. `service` 层不得依赖 `HttpServletRequest`、`Result` 等 Web 对象。

跨模块允许的调用关系**只有 `REFACTOR_SPEC.md` 第 6 节列出的那些**，新增调用前先在那里登记。

### 2.3 跨模块能力契约

| 接口 | 位置 | 被谁使用 |
| --- | --- | --- |
| `MenuService.getCurrent(LocalDate)` | `service/menu` | M2 下单取当日已发布菜单与快照价（唯一取价入口） |
| `RecipeService.listCategories()` | `service/menu` | M3 生产单按分类聚合 |
| `ServiceWindowService.get(date, scope, deptId)` | `service/operation` | M2 下单校验截止时间、M3 校验配送时间（时间窗口唯一数据来源） |
| `OrderQueryService.listValidByDate(date)` / `getDetail` / `page` | `service/order` | M3 聚合与派单、M4 报表与消费审计 |
| `UserService.listByIds(ids)` / `getBrief(userId)` | `service/user` | M2 补员工姓名、M3 补工位电话、M4 补部门归属 |
| `UserService.hasAnyRole(userId, roleCodes...)` | `service/user` | M2 越权校验 |
| `AuthService.verifyToken(token)` / `checkPermission(userId, permCode)` | `service/user` | 认证拦截器与各模块的权限复核 |

**禁止** `AuthService` / `UserService` 反向依赖 M2/M3 的类型（会造成环形依赖）。

## 3. 模块内分层约定

```
<模块>/
├── api/          Controller + 仅该 Controller 使用的请求/响应 DTO
├── service/      接口 + dto/（跨层入参出参：Cmd / Query / VO）
├── service/impl/ 实现（@Service），一个接口一个实现类
├── domain/       @Entity、枚举、值对象
└── repository/   Spring Data JPA 接口
```

约定细节：

- **Controller 一律返回 `com.university.webdesign.common.Result<T>`**：`Result.success(data)` / `Result.ok()`；
  分页用 `PageQuery` + `PageResult<T>`。文件流接口返回 `ResponseEntity<Resource>`，成功时不包 `Result`。
- **业务失败一律抛 `BusinessException(ErrorCode.XXX, "中文提示")`**，不要返回裸 500，也不要再用
  `IllegalArgumentException` 表达业务错误。错误码见《对外方法表》1.5 与 `ErrorCode` 枚举。
- **身份传递**：service **不接收 `operatorId` 参数**，统一用 `UserContextHolder.require()` /
  `currentUserId()` 取当前登录用户；定时任务等无请求上下文的场景显式传参。禁止接受客户端自报身份。
- **权限**：api 层用 `@RequiresPerm({PermCodes.XXX})` 声明式拦截（`AuthInterceptor` 统一校验）；
  service 层仍要做**数据行级归属校验**（本人 or 经理/财务），越权抛 40300。
- **参数校验**：api 层 DTO 用 Jakarta Bean Validation（`@NotBlank`、`@NotEmpty`、`@NotNull`、`@Min`），
  失败由 `common.GlobalExceptionHandler` 统一转 40001。
- **事务**：全部落在 service 层；**查询也不要加 `readOnly = true`**——Hibernate 在只读事务下会把 flush
  模式切成 MANUAL，同一事务内“先改后查”会读到旧数据（曾因此出现取消订单仍被算作有效订单的缺陷）。
- **实体**：`@Entity` + `@Getter@Setter`（或 `@Data`）+ `@NoArgsConstructor`，显式 `@Table(name = "...")`
  与 `@Column(name = "...")`（下划线命名），不用隐式命名策略。乐观锁用 `@Version`。
- **依赖注入用构造器注入**，禁止字段注入 `@Autowired`。
- 类名、方法名用英文；注释与用户可见文案用中文；缩进用 **Tab**。
- **页面**：Thymeleaf 模板放 `templates/<模块目录>/`，静态资源放 `static/`；页面控制器放 `web/`，
  与 REST 控制器分开写（如 `web/order/OrderPageController` 走 `/order/**` 返回视图名，
  `api/order/ApiOrderController` 走 `/api/v1/order/**` 返回 JSON）。页面里不要硬编码业务规则，
  一律调用服务层判定后展示结果。

## 4. 业务不变量（改代码时不能破坏）

- **时间窗口**：当日订单必须在“订餐截止时间”（默认 9:00）前提交；截止后禁止新增/修改/取消。
  截止时间**唯一数据来源**是 `ServiceWindowService.get(date, scope, deptId)`，不得再散落
  `order.cutoff-time` 这类配置（`app.service-window.*` 只是数据库无配置时的兜底默认值）。
- **一人一天一单**：同一员工同一天只能有一张有效订单（`PENDING`/`VALID`），服务层校验 + `order_form`
  唯一约束兜底；并发下不能绕过。取消/作废后不再占用名额。
- **数据快照**：`order_detail` 冗余存放下单时的菜名、分类、单位、单价、数量；`menu_item`/`menu_snapshot`
  存放菜单侧快照。菜谱或菜单的修改、下架、删除**只影响未来菜单，不影响历史菜单与历史订单**。
- **菜单发布冻结**：`MenuService.publish` 必须先写 `menu_snapshot`（只插不改）再置 `PUBLISHED`，
  发布后菜单锁定（不可改不可删，只能下架）。
- **删单留痕**：经理作废违规订单走 `status=INVALID` + 审计字段（作废人/原因/时间），**不做物理删除**。
- **配送时间触发**：到达“配餐开始时间”（默认 11:30）后才开放配送单生成与打印。
- **聚合时机**：总括订单只在截止时间之后汇总当日**有效订单**。
- **下单幂等**：`Idempotency-Key` 请求头必填，命中既有键时直接返回既有订单。
- **报表取数**：M4 报表与消费审计必须走 `OrderQueryService`，不得直接查订单表。
- 截止时间、配餐时间属于可配置项，不要在代码里散落硬编码常量。

## 5. 常用命令

```bash
./gradlew build            # 编译 + 测试（Windows: .\gradlew.bat build）
./gradlew test             # 只跑测试
./gradlew test --tests "*OrderServiceIntegrationTests" --console=plain   # 只跑订单模块集成测试
./gradlew compileJava      # 快速语法检查
./gradlew bootRun          # 本地启动（默认 dev profile，用 H2，无需安装数据库）
./gradlew bootRun --args="--spring.profiles.active=prod"   # 连 PostgreSQL
```

提交前**至少跑一次 `./gradlew build`**（或 `compileJava` + `test`），确认能编过再提交。

测试约定：`src/test/resources/application.properties` 用 **H2 内存库**替代 PostgreSQL。
主链路测试（`service/order/OrderServiceIntegrationTests`）用 `@SpringBootTest` 走真实上下文与真实
角色/权限数据；模块内部逻辑用 Mockito 或 `@DataJpaTest` 直测实现类。**没有数据库的机器也能跑通。**

## 6. 当前状态与缺口（接手时请注意）

已经落地：

1. **四个模块的 service 与 api 全部按《对外方法表》实现**，共 66 个 REST 接口、72 个 service 方法。
2. **统一基础设施**：`Result`（`code=0` 成功，附 `traceId`）、`ErrorCode` 11 个错误码、
   `BusinessException`、`PageQuery`/`PageResult`、`DateRange`、`UserContext`/`UserContextHolder`、
   全局异常处理器（全项目唯一）、`TabularExport`（CSV / SpreadsheetML 导出，不引入 POI）。
3. **认证鉴权已接入**：JWT（HS256，`app.jwt.*` 可配）+ `AuthInterceptor` + `UserContextHolder`；
   页面侧 token 同时写 Cookie，保证“页面跳页面”也能带上登录态。开发期的临时放行配置已删除，
   `SecurityConfig` 为唯一安全配置。
4. **数据库表按需求原文命名**：`recipe` / `menu` / `menu_item` / `menu_snapshot` /
   `order_form` / `order_detail` / `service_window` / `daily_statistics` / `delivery_task` /
   `users` / `roles` / `permissions` / `monthly_report`。
5. **定时任务与事件**：`OrderWindowCloseJob`（09:00 发 `OrderWindowClosedEvent`）、
   `DeliveryOpenJob`（11:30 预生成配送任务）+ `event/listener` 下的监听器。
6. **`DatabaseDataInitializer`** 在 `app.storage=database`（默认）时写入权限点、5 个预置角色与演示账号。

仍待补齐 / 需要确认：

1. **前端**：控制台（`console.html` + `app.js`）与菜品/菜单/订单页面均已适配新接口 `/api/v1/**` 与新字段，
   并接入 JWT（`localStorage.dsh_token` + 同名 Cookie）。页面出错时统一渲染 `templates/error.html`，
   `/api/**` 仍返回 `Result` JSON。
2. **PDF 导出未实现**：`ExportFormat.PDF` / `PrintFormat.PDF` 会按参数校验失败(40001)返回，
   需要真正 PDF 时再统一引入生成库并替换 `common.TabularExport`。
3. **JWT 黑名单与改密失效时间戳是进程内 Map**，单实例够用，多实例部署必须换 Redis。
4. **分类字典由 `recipe.category` 去重派生**，`CategoryVO.categoryId` 是分类名的稳定哈希；
   若将来要独立建分类表，需要迁移 `RecipeService.listCategories` 与相关页面。
5. **`order_form` 的 `(employee_id, order_date)` 唯一约束对有效订单生效**：当前用服务层校验 +
   `Idempotency-Key` 唯一索引兜底；取消/作废不占名额，如需数据库级强约束要配合部分索引。
6. **`order_no` 发号**为 `yyyyMMdd + 4 位当日流水`，取当日最大单号 +1；极高并发下仍可能撞号，
   唯一约束会拒绝，需要重试或改用数据库序列/Redis 发号。
7. **事件监听器目前只覆盖 M3 的聚合与重算**；M4 报表缓存失效、菜单缓存预热尚未接监听器
   （`ReportService.getMonthly` 是“读缓存、缺失即同步生成”，因此暂无正确性风险，只是少了主动失效）。
8. **页面覆盖**：订单（`/order/**`）、菜品（`/recipes/**`）、菜单（`/menus/**`）与控制台（`/console`）已可用；
   M3 的聚合/配送/时间窗口目前只有 REST 接口，尚无专门的 Thymeleaf 页面。
9. **`items` 在列表类响应里**由 Jackson 正常序列化为数组；用 PowerShell `ConvertTo-Json` 查看时
   空集合会显示成空字符串，这是查看工具的假象，不要据此判断接口有问题。

## 7. 工作流约定

- **每次修改后立即提交**。小步提交，提交信息用中文简述做了什么。
- 不要在一个提交里混合多个模块的重构。
- 不确定业务规则时，以 `demand.md` 为准；`demand.md` 没写清楚的，在代码里留 `//todo:` 并向模块负责人确认，
  不要自己臆造规则。
- 新增跨模块调用后，同步更新 `REFACTOR_SPEC.md` 第 6 节的依赖表与 `对外方法表.md`。
- 不要提交 `.idea/`、`build/`、`.gradle/`（已在 `.gitignore`）；`HELP.md` 也被忽略。
- 禁止把数据库真实密码写进配置文件：用 `${DB_PASSWORD:}` 从环境变量注入。
