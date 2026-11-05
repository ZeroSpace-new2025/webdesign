#!/usr/bin/env bash
# =============================================================================
# 读取 reploy/.env 并启动应用（Linux / macOS）
#
# 用法：
#   ./reploy/start.sh                     # 默认 prod
#   ./reploy/start.sh postgres            # 指定 profile
#   ./reploy/start.sh prod --server.port=8081   # 额外参数透传给应用
#
# 配置分两层：结构在 src/main/resources/application.yml，密钥值在 reploy/.env；
# 进程环境里已有的变量优先（便于 CI / 容器覆盖）。打包请先执行 ./gradlew build。
# =============================================================================
set -euo pipefail

PROFILE="${1:-prod}"
case "$PROFILE" in
	prod | dev | postgres) shift || true ;;
	*)
		# 第一个参数不是 profile：按默认 prod，并把该参数原样透传给应用
		PROFILE='prod'
		;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# ---------------------------------------------------------------- 1. 载入 .env
if [ -f "$ENV_FILE" ]; then
	loaded=0
	while IFS= read -r raw || [ -n "$raw" ]; do
		line="${raw%$'\r'}"
		line="$(printf '%s' "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
		if [ -z "$line" ]; then continue; fi
		case "$line" in \#*) continue ;; esac
		line="${line#export }"
		case "$line" in *=*) ;; *) continue ;; esac
		key="$(printf '%s' "${line%%=*}" | tr -d '[:space:]')"
		value="${line#*=}"
		if [ -z "$key" ]; then continue; fi
		case "$value" in
			\"*\") value="${value#\"}"; value="${value%\"}" ;;
			\'*\') value="${value#\'}"; value="${value%\'}" ;;
		esac
		# 用 printenv 判断而不是 ${!key}：macOS 自带 bash 3.2 不支持间接展开加默认值
		if ! printenv "$key" >/dev/null 2>&1; then
			export "$key=$value"
			loaded=$((loaded + 1))
		else
			echo "[env] $key 已由进程环境提供，跳过 .env 中的值"
		fi
	done <"$ENV_FILE"
	echo "[env] 已从 reploy/.env 载入 $loaded 个变量"
else
	echo "[env] 未找到 reploy/.env，将使用 application.yml 里的默认值。" >&2
	echo "[env] 首次部署请：cp reploy/.env.example reploy/.env" >&2
fi

# ---------------------------------------------------------------- 2. 定位 jar
JAR="${JAR_PATH:-}"
if [ -z "$JAR" ]; then
	JAR="$(ls -1t "$ROOT"/build/libs/*.jar 2>/dev/null | grep -v -- '-plain\.jar' | head -n 1 || true)"
fi
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
	echo "找不到可执行 jar，请先打包：./gradlew build（产物在 build/libs/*.jar）" >&2
	exit 1
fi

# ---------------------------------------------------------------- 3. 基本检查
if [ "$PROFILE" != "dev" ] && [ -z "${DB_PASSWORD:-}" ]; then
	echo "[warn] DB_PASSWORD 为空：连 PostgreSQL 会认证失败，请在 reploy/.env 里填写。" >&2
fi
if [ -z "${DB_URL:-}" ]; then
	echo "[warn] DB_URL 未设置，将使用 application.yml 的默认值（jdbc:postgresql://localhost:5432/webdesign）" >&2
fi
if ! command -v java >/dev/null 2>&1; then
	echo "未找到 java，请先安装 JDK 21 并加入 PATH" >&2
	exit 1
fi

# ---------------------------------------------------------------- 4. 启动
echo "[run] profile=$PROFILE  jar=$(basename "$JAR")  额外参数=${*:-（无）}"
echo "[run] 首次部署请确认已执行 reploy/sql/schema.sql 与 seed.sql（见 reploy/sql/README.md）"
exec java -jar "$JAR" "--spring.profiles.active=$PROFILE" "$@"
