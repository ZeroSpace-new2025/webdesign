@echo off
setlocal EnableExtensions
chcp 65001 >nul
rem ============================================================================
rem run.bat —— 仅拉起服务（默认 prod / PostgreSQL，不做建库）
rem
rem 本文件是 cmd 批处理脚本（UTF-8 无 BOM + CRLF 行尾），双击或在 cmd 里直接运行。
rem 前置：已执行 reploy\sql\schema.sql，并且 reploy\.env 里填好数据库密码。
rem 用法：
rem   run.bat                                    仅启动（prod）
rem   run.bat -Profile postgres                  换 profile
rem   run.bat -AppArgs --server.port=8081        透传给应用
rem 首次部署请用 start.dat（会先建库）。
rem ============================================================================
cd /d "%~dp0..\.."
if errorlevel 1 (
	echo [ERROR] 无法进入仓库根目录：%~dp0..\..
	exit /b 1
)

echo ============================================================
echo  企业餐厅网络点餐系统 - 启动服务
echo  工作目录: %CD%
echo ============================================================

if not exist "reploy\start.ps1" (
	echo [ERROR] 缺少 reploy\start.ps1，请确认部署目录完整
	exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "reploy\start.ps1" %*
exit /b %ERRORLEVEL%
