# AGENTS.md

企业餐厅网络点餐系统（课程小组项目）。本文件是本仓库的**唯一协作约定入口**，所有人和 AI 代理在改代码前都要先读这里，再读 `demand.md`（业务需求原文，权威来源）。

## 1. 项目概览

- 技术栈：Java 21 + Spring Boot 4.0.8（Gradle 插件 `org.springframework.boot`）+ Gradle Wrapper 9.7.1
- 依赖：Spring Web MVC、Spring Data JPA、Spring Security、Thymeleaf（+ `thymeleaf-extras-springsecurity6`）、Lombok、PostgreSQL 驱动
- 数据库：**PostgreSQL**（`runtimeOnly org.postgresql:postgresql`）
- 构建工具：**只用 `gradlew`**（不要用系统 gradle，版本可能不一致）
- 包根：`com.university.webdesign`，启动类 `Init.java`（不是默认的 `*Application` 名）

## 2. 目录与模块边界（最重要）

项目按 DDD 拆成 4 个模块，**一个模块一个顶层包**，各模块由指定负责人主导，改别人的模块前先沟通。

| 模块 | 顶层包 | 职责（详见 `demand.md`） | 负责人 |
| --- | --- | --- | --- |
| 菜品与菜单中心 | `menurecipe` | 食谱 CRUD、图片上传、菜单生成/版本/发布下架、菜单调价 | 方家乐 |
| 订单与交易核心 | `ordertransaction` | 点餐时间窗口、下单与改单、一人一天一单、个人历史与月度消费 | 雷伟舜 |
| 运营与履约系统 | `operationfulfillment` | 截止后聚合总括订单、生产单打印、配送时间触发与批量配送单 | 张颖茵 |
| 用户与报表中心 | `user` / `reporting` | 登录认证、角色权限、员工维护与 Excel 批量导入、月度报表与消费审计 | 王家豪 |
| 公共设施 | `common` | `Result<T>` 等跨模块共享的极小工具，**只放不偏向任何单个模块的东西** | 全员 |

### 跨模块能力契约（各模块 `service` 包中的接口）

其他模块的实现类尚未落地，为了让项目能编译、上下文能启动，各模块 `service` 包中已经放好了对外的 Service 接口骨架，**调用方只依赖这些接口**：

| 接口 | 位置 | 订单模块用到的能力 |
| --- | --- | --- |
| `MenuService` | `menurecipe/service` | `getActiveMenu(LocalDate)` 取当日已发布菜单，下单时用它校验菜品与取价 |
| `RecipeService` | `menurecipe/service` | 暂无调用 |
| `UserService` | `user/service` | `hasAnyRole(userId, roleCodes)` 已接入订单模块做经理/财务越权校验；登录态仍待接入（目前前端传 `operatorId`） |

接口里带 `//todo 确认` 的注释表示「签名是按调用方需要先约定的，需要由对应负责人确认后补齐实现」。

### 各模块的包结构（已按订单模块的结构统一建好）

```
com.university.webdesign.
├── common/                        跨模块共享的工具（Result 等）
├── ordertransaction/              ✔ 已实现：api / service / impl / repository / data
├── menurecipe/                    api / service ✔有接口 / impl / repository / data
├── operationfulfillment/          api / service / impl / repository / data
├── user/                          api / service ✔有接口 / impl / repository / data
└── reporting/                     报表模块：api / service / impl / repository / data
```

- 一个模块内五层含义见第 3 节；**跨模块调用只允许依赖对方的 `api` 与 `service` 包**。
- 报表代码属用户与报表中心，放在顶层 `reporting` 包，不再含在 `user` 里。
- 刚建好、还没有代码的包用 `package-info.java` 占位（空目录 git 不跟踪），
  该文件里写明了这个包该放什么、以及可以直接调用哪些已实现的跨模块入口，动手前先读它。


**硬性边界规则**：

1. 每个模块对外**只暴露 `api` 与 `service` 两个包**（`api` 放 Controller/DTO，`service` 放能力接口）。其他模块只能依赖这两个包，禁止直接依赖别的模块的 `impl` / `repository` / `data`。
2. 每个模块只放自己领域的接口，不要把无关接口塞进别人的 Controller（`RecipeApi` / `MenuApi` / `OrderApi` / `UserApi` 头部注释已写明）。
3. **不要跨模块直接读写对方的表。** 这是本项目最大的架构红线，破坏它就等于拆掉了微服务边界。
4. 数据所有权单一：用户基础数据只在用户中心改，菜品标准信息只在菜单中心改。

## 3. 模块内分层约定

沿用 `ordertransaction` 已有的分层，新代码照抄这套结构：

```
<module>/
├── api/         Controller（@RestController）+ 入参出参 DTO + QueryData
├── service/     服务接口（业务能力契约）
├── impl/        服务实现（@Component / @Service）
├── repository/  Spring Data JPA Repository 接口
└── data/        JPA 实体（@Entity）+ 领域枚举
```

约定细节：

- Controller 一律返回 `com.university.webdesign.common.Result<T>`，用 `Result.success(...)` / `Result.failure(code, msg)`（`code=200` 表示成功）。
- DTO / QueryData 用 Lombok `@Data` + Javadoc 注释说明每个字段，字段命名与已有代码保持一致。
- 实体：`@Entity` + `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor`，显式 `@Table(name = "...")` 和 `@Column(name = "...")`（下划线命名），不用隐式命名策略。
- 依赖注入用**构造器注入**（已有代码全部如此），禁止字段注入 `@Autowired`。
- 类名、方法名用英文；注释与用户可见文案用中文；缩进用 **Tab**（与现有文件一致）。

## 4. 业务不变量（改代码时不能破坏）

- **时间窗口**：当日订单必须在“订餐截止时间”（默认 9:00）前提交；截止后禁止新增/修改订单。订单模块已按此实现（`order.cutoff-time` 配置项，默认 `09:00`）。
- **一人一天一单**：同一员工同一天只能有一张有效订单，需在服务层做校验（并发下要防止绕过）。订单模块已实现“非取消订单占名额”的校验。
- **数据快照**：订单明细冗余存放下单时的菜名、分类、单价、数量；菜谱/菜单的修改或删除**只能影响未来菜单，不能影响历史订单**。`OrderItem` 已带 `itemName` / `category` / `unitPrice` 快照字段，改菜谱逻辑时务必保住这一点。
- **权限边界**：员工只能操作/查询自己的订单与消费；经理可删单，经理与财务可查他人历史与消费明细。订单模块通过 `UserService.hasAnyRole` 校验，角色编码暂定 `MANAGER` / `FINANCE`（见第 6 节待确认项）。
- **删单留痕**：经理删除违规订单走“置为已取消”，不做物理删除，保证财务审计可追溯。
- **配送时间触发**：到达“配餐开始时间”（默认 11:30）后才开放配送单打印权限。
- **聚合时机**：总括订单只在截止时间之后汇总当日有效订单（只统计非取消订单）。
- 截止时间、配餐时间属于可配置项，不要在代码里散落硬编码常量。
- 事务：订单模块统一用普通 `@Transactional`，**不要为了查询加 `readOnly = true`**——Hibernate 在只读事务下会把 flush 模式切成 MANUAL，同一事务内“先改后查”会读到旧数据（曾因此出现取消订单仍被算作有效订单的缺陷）。

## 5. 常用命令

```bash
./gradlew build            # 编译 + 测试（Windows: .\gradlew.bat build）
./gradlew test             # 只跑测试
./gradlew test --tests "*OrderServiceTests" --console=plain   # 只跑订单模块测试
./gradlew bootRun          # 本地启动（需先补 PostgreSQL 数据源配置）
./gradlew compileJava      # 快速语法检查
```

提交前**至少跑一次 `./gradlew build`**（或 `compileJava` + `test`），确认能编过再提交。

测试约定：`src/test/resources/application.properties` 用 **H2 内存库**替代 PostgreSQL，`StubServicesTestConfiguration` 为尚未实现的 service 提供桩实现，因此**测试在没有数据库的机器上也能跑通**。给测试加依赖时注意 `--offline` 可能取不到新依赖。

## 6. 当前状态与缺口（接手时请注意）

已经落地：

1. **订单与交易核心（`ordertransaction`）已实现**：下单、改单、支付、取消、经理删单、订单查询、个人历史、月度消费统计、经理/财务越权校验，共 14 个集成测试（`OrderServiceTests`）覆盖核心规则。对外通过 `OrderService` 暴露，`OrderApi` 提供 REST 接口。
2. 跨模块调用的 Service 接口骨架已就位（见第 2 节表格），`UserService` 已接入订单模块做角色校验，项目当前**可以编译、`gradlew build` 通过**。
3. `MenuDTO.menuItems` 已改为 `List<MenuItemData>`；`OrderItem.itemId` 的 `@ManyToOne`、`OrderStatus` 多余的 `}`、`MenuDTO` 误用 `java.awt.List` 都已修正。

仍是缺口：

1. `application.properties` 目前只有 `spring.application.name`，**没有数据源配置**。`bootRun` 之前需要补 `spring.datasource.url/username/password`（PostgreSQL），且不要提交真实密码。测试走 H2，不受影响。
2. 各模块的 Service **实现类**都还没有：`menurecipe` 的 `MenuService`、`RecipeService` 与 `user` 的 `UserService` 目前只有接口，需要负责人补齐 `impl`。
3. 订单模块中所有跨模块/待定决策点都以 `//todo 确认` 标出，例如：**角色编码取值 `MANAGER`/`FINANCE` 需与 IAM 对齐**、登录态来源（现为前端传 `operatorId`，应改为认证上下文）、`getActiveMenu` 的方法签名与菜单项字段、订单号发号规则（并发可能重复）、“一人一天一单”的唯一约束、删单审计字段、时区取值。**这些是需要跟对应负责人确认的问题清单，不要当成已定论。**
4. `OrderService` 中 `deleteOrder(Long)`、`getHistoryOrders(userId, start, end)` 等**兼容旧签名的重载不校验权限**（`deleteOrder(Long)` 仅告警），接入登录态后应删除这些重载；当前新代码请一律走带 `operatorId` 的重载。
5. `Report`（报表）功能在 `demand.md` 中属于用户与报表中心，顶层 `reporting` 包的目录结构已建好，但**还没有任何代码**。
6. 其他模块的 `api` 包中仍有 `//todo:` 占位，返回空对象/空列表；这类占位不算实现。
7. `src/main/resources/templates` 与 `static` 目前为空，Thymeleaf 页面尚未开始。
8. 未接线项：登录态（Spring Security）尚未接入，`operatorId` 仍由前端传入，**因此角色校验目前可被伪造的身份绕过**；`operationfulfillment` 尚未调用 `OrderService.query` / `findActiveOrder` 做聚合与配送。


## 7. 工作流约定

- **每次修改后立即提交**（`api` 包头部注释已明确要求）。小步提交，提交信息用中文简述做了什么。
- 不要在一个提交里混合多个模块的重构。
- 不确定业务规则时，以 `demand.md` 为准；`demand.md` 没写清楚的，在代码里留 `//todo:` 并向模块负责人确认，不要自己臆造规则。
- 不要提交 `.idea/`、`build/`、`.gradle/`（已在 `.gitignore`）；`HELP.md` 也被忽略。
