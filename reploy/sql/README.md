# 数据库脚本与部署说明

本目录是企业餐厅网络点餐系统的 **PostgreSQL 建表与初始化脚本**，也是部署到生产库的唯一建表依据。
应用侧已改为 `ddl-auto=validate`：**表结构由脚本负责，Hibernate 只校验、不改表**。

```
reploy/sql/
├── schema.sql   建表脚本（16 张表 + 索引 + CHECK 约束 + 表/列注释）
├── seed.sql     初始化数据（5 个预置角色、8 个演示账号、默认时间窗口；幂等可重复执行）
├── drop.sql     反安装（删除全部业务表，仅限本地/演示环境）
└── README.md    本文件
```

## 1. 表清单

| 模块 | 表 | 说明 |
| :--- | :--- | :--- |
| M1 菜品与菜单中心 | `recipe` | 菜品（食谱）标准信息，下架为逻辑删除 |
| | `menu` | 菜单头：状态、生效日期、版本链、发布/下架时间 |
| | `menu_item` | 菜单与菜品的关联 + 菜单级调价 |
| | `menu_snapshot` | 菜单发布快照，**只插不改**，历史菜单的数据保护核心 |
| M2 订单与交易核心 | `order_form` | 订单主表，一人一天一单、幂等下单、作废留痕 |
| | `order_detail` | 订单明细，冗余存放下单时刻的菜名/分类/单位/单价 |
| M3 运营与履约系统 | `service_window` | 订餐截止时间与配餐开始时间的**唯一数据来源** |
| | `daily_statistics` | 每日汇总快照头（一天一条） |
| | `statistics_item` | 每日汇总明细（按菜品聚合，`@ElementCollection`） |
| | `delivery_task` | 配送任务，`(statistics_date, employee_id)` 唯一 |
| | `delivery_task_item` | 配送任务明细（`@ElementCollection`） |
| M4 用户与报表中心 | `users` | 员工/账号（`user` 是 PostgreSQL 保留字，故用复数） |
| | `role` | 角色 + `permission_list` 权限位图（权限点不建表） |
| | `user_role` | 用户-角色关联（多对多） |
| | `monthly_report` | 月度报表缓存（一个月一条） |
| | `monthly_report_item` | 月度报表明细（按菜品聚合） |

## 2. 部署步骤（PostgreSQL）

```bash
# 1) 建库（连接信息用环境变量，不要把密码写进配置文件）
createdb -h localhost -U postgres webdesign

# 2) 建表 + 初始化数据（ON_ERROR_STOP 保证出错即停，不会留下半套表）
psql -h localhost -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/schema.sql
psql -h localhost -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/seed.sql

# 3) 打包
./gradlew build            # Windows: .\gradlew.bat build

# 4) 启动（prod profile；密码从环境变量注入）
export DB_URL=jdbc:postgresql://localhost:5432/webdesign
export DB_USERNAME=postgres
export DB_PASSWORD='你的密码'
java -jar build/libs/webdesign-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

首次启动时 `config.DatabaseDataInitializer` 还会补两类数据（都已做幂等判断）：

- 演示菜品 8 道 + 当日已发布菜单（需要走 service 层才能正确冻结 `menu_snapshot`，因此放在 Java 里）；
- 若 `seed.sql` 没执行，它也会补上 5 个角色、8 个演示账号和默认时间窗口。

也就是说：**只执行 `schema.sql` 也能跑起来**，`seed.sql` 的作用是让 DBA 在不启动应用时就能预置账号。

## 3. 演示账号

| 工号 | 密码 | 姓名 | 角色 | 权限点 |
| :--- | :--- | :--- | :--- | :--- |
| `manager` | `admin123` | 林经理 | `MANAGER` | 全部 13 个 |
| `finance` | `finance123` | 周财务 | `FINANCE` | `order:view:all`、`report:view`、`report:export`、`audit:view` |
| `kitchen` | `kitchen123` | 陈主管 | `KITCHEN_SUPERVISOR` | `order:view:all`、`operation:aggregate`、`operation:delivery:print`、`report:view` |
| `delivery` | `delivery123` | 赵配餐 | `DELIVERY_STAFF` | `operation:delivery:print` |
| `zhangsan`/`lisi`/`wangwu`/`sunliu` | `123456` | 张三/李四/王五/孙六 | `EMPLOYEE` | `order:submit` |

密码在库里是 BCrypt 哈希（`$2a$10$…`），**上线前请删除演示账号或全部改密**。
系统只认本表里的账号登录（硬编码的临时管理员 `admin` / `admin123` 已从代码中删除），
因此**空库无法登录**：要么执行本脚本的演示账号，要么先由经理在“用户管理”里建账号。

## 4. 设计要点（与实体/业务的对应关系）

1. **`status` 用 VARCHAR + CHECK，不用数据库 ENUM**：枚举值以 Java 枚举为准，改枚举只需改 CHECK，
   不会出现 PG 自有类型无法回退的问题。CHECK 取值与 `OrderStatus`、`MenuStatus`、`RecipeStatus`、
   `TaskStatus`、`WindowScope`、`UserStatus` 一一对应。
2. **一人一天一单落到数据库层**：`schema.sql` 末尾用**部分唯一索引**
   `uk_order_form_employee_date_active (employee_id, order_date) WHERE status IN ('PENDING','VALID')`。
   普通唯一约束会误杀“取消后再下单”，部分索引只约束有效订单，既能兜住并发，又不占用已取消/已作废的名额。
3. **权限用位图，不建权限点表**：`role.permission_list` 存 `PermissionEnum` 位号的逗号分隔字符串
   （如 `"17,23,24,25"`），由 `common/converter/RoleListConverter` 与 `PermissionList`(BitSet) 转换。
   因此库里没有 `permission` / `role_permission` 表，权限字典即枚举中 `permCode` 非空的 13 项。
4. **快照字段不许被“优化”掉**：`order_detail` 的菜名/分类/单位/单价、`menu_snapshot` 的冻结价、
   `statistics_item` 与 `monthly_report_item` 的字面菜名，都是需求要求的数据保护机制。
   菜谱改名、调价、下架只影响未来菜单，不影响这些历史数据。
5. **`@ElementCollection` 的子表没有实体**：`statistics_item`、`delivery_task_item` 由父表级联维护，
   因此没有对应的 Repository，也不要在别处单独写入。
6. **时间精度**：`time(0)`（截止/配餐时间只到分钟）与 `timestamp(6)`（业务时间戳）由实体映射决定，
   脚本按 Hibernate 元数据导出后整理，改实体精度时请同步改脚本。

## 5. 索引

索引按 `repository/` 下的查询方法逐条对应，没有对应查询的列不建索引：

- `order_form (employee_id, order_date)`：一人一天一单校验、个人历史；
- `order_form (order_date, status)`：当日有效订单聚合/配送/报表取数；
- `order_form (created_at)`：按时间区间的个人历史与月度消费；
- `menu (status, effective_date)`：`MenuService.getCurrent` 取当日已发布菜单；
- `order_detail (order_id)`、`order_detail (recipe_id)`：订单详情与按菜品聚合；
- `service_window (scope, dept_id, effective_from)`：时间窗口取值；
- `user_role (role_id)`：角色反查关联用户（删除角色前的引用校验）；
- 其余见 `schema.sql` 的“索引”一节。

## 6. 验证与排错

```sql
-- 表数量（应为 16）
select count(*) from information_schema.tables
where table_schema = 'public' and table_type = 'BASE TABLE';

-- 预置角色与权限位图
select name, permission_list from role order by id;

-- 演示账号与角色
select u.employee_no, u.name, r.name as role
from users u join user_role ur on ur.user_id = u.id join role r on r.id = ur.role_id
order by u.id;

-- 一人一天一单的部分唯一索引是否生效（应返回 uk_order_form_employee_date_active）
select indexname from pg_indexes where tablename = 'order_form';
```

常见问题：

| 现象 | 原因与处理 |
| :--- | :--- |
| 启动报 `Schema validation: missing table [xxx]` | 没执行 `reploy/sql/schema.sql`，或执行到了别的库/别的 schema。先建表，或临时把 `ddl-auto` 改回 `update` |
| 启动报 `Schema validation: missing column [xxx]` | 库里的表是旧版本，重新执行 `reploy/sql/schema.sql` 不会补列，需手工 `alter table` 或按 `drop.sql` + `schema.sql` 重建 |
| 重复执行 `seed.sql` 报唯一键冲突 | 正常行为会被 `where not exists` 判断挡掉；若仍冲突说明手工改过角色名或工号，先清理 `role` / `users` 表 |
| 下单偶发 50000 | 并发下撞到 `uk_order_form_employee_date_active`（部分唯一索引）。这是设计内的最后防线，接口应提示“今日已有有效订单” |

## 7. 与开发环境的关系

- `dev` / 测试沿用 H2 内存库 + `ddl-auto=create-drop`，**不需要手工执行**本目录脚本；
- 但 `seed.sql` 会被自动执行：`application.yml` 的 dev 段配了
  `spring.sql.init.mode=always` + `data-locations=classpath:db/seed.sql`
  + `jpa.defer-datasource-initialization=true`（等 Hibernate 建完表再灌数据），
  测试的 `src/test/resources/application.yml` 同理。`seed.sql` 由 `build.gradle` 的
  `processResources` 从本目录拷到 `classpath:db/`，所以**内容仍然只维护在这一处**；
- 也就是说：预置角色 / 演示账号 / 时间窗口**只存在于 `seed.sql`**，代码里不再有种子数据，
  改角色或权限改这个文件即可（dev 重启生效，生产重跑脚本生效）；
- H2 的 PostgreSQL 兼容模式支持本脚本的绝大部分语法，但**不支持部分唯一索引**（`CREATE UNIQUE INDEX … WHERE`），
  因此不要在 H2 上直接跑 `schema.sql`（`seed.sql` 没有这个问题，两边都能跑）；
- 切换 PostgreSQL 用 `--spring.profiles.active=prod`（或 `postgres`），连接串与账号密码只走环境变量。

## 8. 改了实体之后怎么同步脚本

`ddl-auto=validate` 之后，实体与脚本必须保持一致。改了 `domain/` 下的实体，可以先用下面的命令
让 Hibernate 导出它认为的 DDL，再和 `schema.sql` 逐列比对（注意表名/列名要与脚本一致）：

```bash
java -jar build/libs/webdesign-0.0.1-SNAPSHOT.jar \
  --spring.main.banner-mode=off --server.port=0 --app.storage=none \
  --spring.jpa.hibernate.ddl-auto=none \
  --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  --spring.jpa.properties.jakarta.persistence.schema-generation.create-source=metadata \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/ddl-pg.sql
```

`build/ddl-pg.sql` 生成后即可停止进程，然后：

```bash
diff <(grep -o 'create table [a-z_]*' build/ddl-pg.sql | sort) \
     <(grep -o 'create table if not exists [a-z_]*' reploy/sql/schema.sql | sed 's/if not exists //' | sort)
```

本地验证的完整做法（不需要安装 PostgreSQL）：用 H2 文件库模拟持久化库，
先执行 `schema.sql`（H2 不支持末尾那条部分唯一索引，验证时去掉）+ `seed.sql`，
再用 `--spring.jpa.hibernate.ddl-auto=validate --spring.jpa.database-platform=PostgreSQLDialect`
连续启动两次应用，确认 `Started Init` 且各表数量不变。

## 9. 变更记录

- 建表脚本首次整理：表结构由实体元数据导出后加索引、CHECK 与注释，作为部署基线；
- `prod` / `postgres` 的 `ddl-auto` 从 `update` 改为 `validate`，避免 Hibernate 在线上改表；
- 修复演示数据初始化在持久化库里重启报错的问题（时间窗口重复插入、菜品重名）。
