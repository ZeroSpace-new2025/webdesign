-- =============================================================================
-- 企业餐厅网络点餐系统 · 初始化数据（PostgreSQL）
-- 文件：reploy/sql/seed.sql
-- 前置：先执行 reploy/sql/schema.sql 建表
-- 用法：psql -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/seed.sql
--
-- 内容：
--   1) 5 个预置角色及其权限位图（位号见 common/enums/PermissionEnum）；
--   2) 8 个演示账号（密码为 BCrypt 哈希，明文见下表）；
--   3) 用户-角色关联；
--   4) 默认时间窗口（09:00 截止 / 11:30 开始配餐）。
--
-- 幂等：每段都带“不存在才插入”的判断，重复执行不会写重复数据；
--       与 config/DatabaseDataInitializer 的预置数据保持一致，两者先执行谁都不会冲突。
--
-- 演示账号（明文密码仅用于课程演示）：
--   manager  / admin123     餐厅经理（全部 13 个权限点）
--   finance  / finance123   财务（查全部订单 + 报表查看/导出 + 消费审计）
--   kitchen  / kitchen123   厨房主管（查全部订单 + 聚合总括订单 + 配送单打印 + 报表查看）
--   delivery / delivery123  配餐员（配送单打印）
--   zhangsan / lisi / wangwu / sunliu，密码均为 123456，企业员工（提交订单）
-- 上线前请删除演示账号，或全部改密。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 预置角色
-- permission_list 是 PermissionEnum 位号的逗号分隔字符串（升序），由
-- common/converter/RoleListConverter 与 common/model/PermissionList(BitSet) 互转。
--   13 menu:recipe:manage        18 operation:aggregate       23 report:view
--   14 menu:menu:manage          19 operation:delivery:print  24 report:export
--   15 order:submit              20 operation:window:manage   25 audit:view
--   16 order:invalidate          21 user:manage
--   17 order:view:all            22 role:manage
-- -----------------------------------------------------------------------------
insert into role (name, permission_list)
select m.name, m.permission_list
from (values
	('MANAGER',            '13,14,15,16,17,18,19,20,21,22,23,24,25'),
	('KITCHEN_SUPERVISOR', '17,18,19,23'),
	('DELIVERY_STAFF',     '19'),
	('FINANCE',            '17,23,24,25'),
	('EMPLOYEE',           '15')
) as m (name, permission_list)
where not exists (select 1 from role r where r.name = m.name);

-- -----------------------------------------------------------------------------
-- 2. 演示账号
-- 密码列存 BCrypt（$2a$10$…）哈希；同一密码在不同账号上使用不同盐值。
-- 如需新增账号：由餐厅经理在“用户管理”页面创建，或调用 POST /api/v1/user/users，
-- 不要在 SQL 里拼明文密码。
-- -----------------------------------------------------------------------------
insert into users (employee_no, password, name, dept_id, dept_name, workstation, phone, status)
select m.employee_no, m.password, m.name, m.dept_id, m.dept_name, m.workstation, m.phone, 'ACTIVE'
from (values
	('manager',  '$2a$10$PVtJq9QboAM4MmaMS4TLr.sC6zSEgF3MGDY056G0IwNxw0qGOuwzq', '林经理', 1, '行政部', 'A-01', '13800000001'),
	('finance',  '$2a$10$/cZ9gUwrSkB2wUsmHCpyP.JCf7b7Vomym6dlq8q0JVygVO8UIOx6a', '周财务', 2, '财务部', 'B-03', '13800000002'),
	('kitchen',  '$2a$10$FlYTmt/vzGIbEqm3VTHkee5T0o0R8.13b9Z1dIDP/fiG4IfDOLYci', '陈主管', 3, '后厨',   'K-01', '13800000003'),
	('delivery', '$2a$10$SP.daKAtcbBN7jMhs/y/YOWaGzzq0D5HyX9dJ2xadlhHB.VEFfIq6', '赵配餐', 4, '配送组', 'D-02', '13800000004'),
	('zhangsan', '$2a$10$aZAyfAe.tNo8z4BoZXpAIORwCPh4hz6bW88B44p44DPgluxVAv3pm', '张三',   5, '研发部', 'R-12', '13800000005'),
	('lisi',     '$2a$10$vfA9dzAU3uIPo3TGjY172.wJB/5CAwNl7vyYD67PpxPtPXYalt/b6', '李四',   5, '研发部', 'R-08', '13800000006'),
	('wangwu',   '$2a$10$Po.ZDNWOVyYWkLtNfU8KXOJBFig1wYNM51RMjeDeZN8/PZsvW2CpC', '王五',   6, '运营部', 'O-06', '13800000007'),
	('sunliu',   '$2a$10$8KMza7lnXrlASsKdtRTI6elPfJBKhFl00w14lfi9Gu7yGnEJo8MLq', '孙六',   6, '运营部', 'O-18', '13800000008')
) as m (employee_no, password, name, dept_id, dept_name, workstation, phone)
where not exists (select 1 from users u where u.employee_no = m.employee_no);

-- -----------------------------------------------------------------------------
-- 3. 用户-角色关联
-- -----------------------------------------------------------------------------
insert into user_role (user_id, role_id)
select u.id, r.id
from (values
	('manager',  'MANAGER'),
	('finance',  'FINANCE'),
	('kitchen',  'KITCHEN_SUPERVISOR'),
	('delivery', 'DELIVERY_STAFF'),
	('zhangsan', 'EMPLOYEE'),
	('lisi',     'EMPLOYEE'),
	('wangwu',   'EMPLOYEE'),
	('sunliu',   'EMPLOYEE')
) as m (employee_no, role_name)
join users u on u.employee_no = m.employee_no
join role r on r.name = m.role_name
where not exists (
	select 1 from user_role ur where ur.user_id = u.id and ur.role_id = r.id
);

-- -----------------------------------------------------------------------------
-- 4. 默认时间窗口（全局）
-- effective_from 取很早的日期，保证任何一天都命中；只插一条全局配置，
-- 后续调整请在“服务时间窗口”页面改，或新增 DEPT 作用域配置。
-- -----------------------------------------------------------------------------
insert into service_window (cutoff_time, delivery_start_time, effective_from, scope, dept_id, created_at, updated_at)
select time '09:00', time '11:30', date '2000-01-01', 'GLOBAL', null, now(), now()
where not exists (
	select 1 from service_window where scope = 'GLOBAL' and dept_id is null
);

-- -----------------------------------------------------------------------------
-- 演示菜品与当日菜单不在此初始化：由 config/DatabaseDataInitializer 在首次启动时写入
-- （需要同时生成 menu_item 与 menu_snapshot，走 service 层才能保证快照冻结正确）。
-- -----------------------------------------------------------------------------
