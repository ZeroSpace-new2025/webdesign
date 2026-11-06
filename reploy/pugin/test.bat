@echo off
setlocal EnableExtensions
chcp 65001 >nul
rem ============================================================================
rem test.bat —— 以 dev 模式启动（H2 内存库，不需要 PostgreSQL，也不需要 .env）
rem
rem 本文件是 cmd 批处理脚本（UTF-8 无 BOM + CRLF 行尾），双击或在 cmd 里直接运行。
rem dev profile 会：
rem   * 用 H2 内存库（create-drop，每次启动重建表）
rem   * 由 DatabaseDataInitializer 写入预置角色、演示账号与当日演示菜单
rem   * 监听 8080，并开放 H2 控制台 /h2-console
rem 用法：
rem   test.bat                         默认 8080
rem   test.bat -AppArgs --server.port=8081
rem 想跑单元测试请用: .\gradlew.bat test
rem ============================================================================
cd /d "%~dp0..\.."
if errorlevel 1 (
	echo [ERROR] 无法进入仓库根目录：%~dp0..\..
	exit /b 1
)

echo ============================================================
echo  企业餐厅网络点餐系统 - dev 模式启动（H2 内存库）
echo  工作目录: %CD%
echo  提示: H2 控制台 http://localhost:8080/h2-console
echo ============================================================

if not exist "reploy\start.ps1" (
	echo [ERROR] 缺少 reploy\start.ps1，请确认部署目录完整
	exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "reploy\start.ps1" -Profile dev %*
exit /b %ERRORLEVEL%
