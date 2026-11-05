-- =============================================================================
-- 企业餐厅网络点餐系统 · 反安装脚本（PostgreSQL）
-- 文件：reploy/sql/drop.sql
--
-- ⚠ 危险：会删除本系统全部业务表与其中的数据，且不可恢复。
--    仅限本地开发、演示环境重建时使用；生产环境禁止执行。
--
-- 用法：psql -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/drop.sql
-- 重建：先执行本脚本，再依次执行 reploy/sql/schema.sql、reploy/sql/seed.sql。
-- =============================================================================

-- 子表（含外键、@ElementCollection 与关联表）在前，父表在后；
-- 即使顺序写错，cascade 也会把依赖对象一并删除。
drop table if exists monthly_report_item cascade;
drop table if exists monthly_report cascade;
drop table if exists user_role cascade;
drop table if exists users cascade;
drop table if exists role cascade;
drop table if exists delivery_task_item cascade;
drop table if exists delivery_task cascade;
drop table if exists statistics_item cascade;
drop table if exists daily_statistics cascade;
drop table if exists service_window cascade;
drop table if exists order_detail cascade;
drop table if exists order_form cascade;
drop table if exists menu_snapshot cascade;
drop table if exists menu_item cascade;
drop table if exists menu cascade;
drop table if exists recipe cascade;
