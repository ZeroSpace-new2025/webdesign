@echo off
setlocal EnableExtensions
chcp 65001 >nul
rem ============================================================================
rem start.bat —— 一键部署：执行建库脚本，然后拉起服务（prod / PostgreSQL）
rem
rem 本文件是 cmd 批处理脚本（UTF-8 无 BOM + CRLF 行尾），双击或在 cmd 里直接运行。
rem 流程：
rem   1) 读取 reploy\.env 里的数据库连接
rem   2) 用 psql 执行 reploy\sql\schema.sql 与 seed.sql（两个脚本都可重复执行）
rem   3) 调用 reploy\start.ps1，以 prod profile 启动
rem 找不到 psql 时只跳过建库、继续启动（库可能已经建好）。
rem 换 profile 请用 run.dat（本脚本固定 prod）。
rem ============================================================================
cd /d "%~dp0..\.."
if errorlevel 1 (
	echo [ERROR] 无法进入仓库根目录：%~dp0..\..
	exit /b 1
)

echo ============================================================
echo  企业餐厅网络点餐系统 - 一键部署（建库 + 启动）
echo  工作目录: %CD%
echo ============================================================

if not exist "reploy\start.ps1" (
	echo [ERROR] 缺少 reploy\start.ps1，请确认部署目录完整
	exit /b 1
)

if not exist "reploy\.env" (
	echo [ERROR] 缺少 reploy\.env
	echo         请先执行: copy reploy\.env.example reploy\.env
	echo         然后编辑 reploy\.env 填写 DB_PASSWORD
	exit /b 1
)

rem ---- 读取 .env：只取 KEY=VALUE，忽略 # 注释；统一加 ENV_ 前缀，避免污染进程环境 ----
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("reploy\.env") do (
	if not "%%~A"=="" set "ENV_%%~A=%%B"
)

if not defined ENV_DB_URL (
	echo [ERROR] reploy\.env 里没有 DB_URL
	exit /b 1
)

rem ---- 从 jdbc:postgresql://host:port/db 解析 psql 需要的参数 ----
set "JDBC=%ENV_DB_URL%"
set "JDBC=%JDBC:jdbc:postgresql://=%"
for /f "tokens=1,2 delims=/" %%A in ("%JDBC%") do (
	set "HOSTPORT=%%A"
	set "DBNAME=%%B"
)
for /f "tokens=1,2 delims=:" %%A in ("%HOSTPORT%") do (
	set "PGHOST=%%A"
	set "PGPORT=%%B"
)
if not defined PGHOST set "PGHOST=localhost"
if not defined PGPORT set "PGPORT=5432"
if not defined DBNAME set "DBNAME=webdesign"
set "PGUSER=%ENV_DB_USERNAME%"
set "PGPASSWORD=%ENV_DB_PASSWORD%"

echo [sql] 目标库: %PGHOST%:%PGPORT%/%DBNAME%    用户: %PGUSER%

where psql >nul 2>nul
if errorlevel 1 (
	echo [WARN] 未找到 psql，跳过建库；若库还没建好，应用启动会报 Schema-validation
	echo [WARN] 可手工执行：
	echo        psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DBNAME% -v ON_ERROR_STOP=1 -f reploy\sql\schema.sql
	echo        psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DBNAME% -v ON_ERROR_STOP=1 -f reploy\sql\seed.sql
	goto :start_service
)

echo [sql] 执行 reploy\sql\schema.sql ...
rem 用 call：psql 若是 .bat/.cmd 包装脚本，直接调用会夺走控制权
call psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DBNAME% -v ON_ERROR_STOP=1 -f "reploy\sql\schema.sql"
if errorlevel 1 (
	echo [ERROR] schema.sql 执行失败，已中止
	exit /b 1
)

echo [sql] 执行 reploy\sql\seed.sql ...
call psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DBNAME% -v ON_ERROR_STOP=1 -f "reploy\sql\seed.sql"
if errorlevel 1 (
	echo [ERROR] seed.sql 执行失败，已中止
	exit /b 1
)

echo [sql] 建表与初始化数据完成

:start_service
echo [run] 拉起服务（profile=prod）...
powershell -NoProfile -ExecutionPolicy Bypass -File "reploy\start.ps1" -Profile prod %*
exit /b %ERRORLEVEL%
