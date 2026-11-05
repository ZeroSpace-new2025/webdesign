<#
.SYNOPSIS
	读取 reploy/.env 里的环境变量并启动应用（Windows / PowerShell）。

.DESCRIPTION
	配置分两层：
	  * 结构在 src/main/resources/application.yml（公共 + dev/prod/postgres 三段）
	  * 密钥值在 reploy/.env（本脚本负责载入），进程环境里已有的变量优先，便于 CI/容器覆盖
	默认 prod profile；本脚本只负责启动，打包请先执行 .\gradlew.bat build。

.PARAMETER Profile
	要激活的 Spring profile：prod（默认）/ dev / postgres。

.PARAMETER Jar
	显式指定可执行 jar；不传则取 build\libs 下最新的非 -plain.jar。

.PARAMETER AppArgs
	透传给应用的额外参数，例如 -AppArgs '--server.port=8081','--logging.level.root=DEBUG'。

.EXAMPLE
	.\reploy\start.ps1

.EXAMPLE
	.\reploy\start.ps1 -Profile postgres -AppArgs '--server.port=8081'
#>
[CmdletBinding()]
param(
	[ValidateSet('prod', 'dev', 'postgres')]
	[string]$Profile = 'prod',

	[string]$Jar,

	[string[]]$AppArgs = @()
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $PSScriptRoot '.env'

# ---------------------------------------------------------------- 1. 载入 .env
if (Test-Path $envFile) {
	$loaded = @()
	foreach ($raw in (Get-Content $envFile -Encoding utf8)) {
		$line = $raw.Trim()
		if (-not $line -or $line.StartsWith('#')) { continue }
		$line = $line -replace '^export\s+', ''
		$index = $line.IndexOf('=')
		if ($index -lt 1) { continue }
		$key = $line.Substring(0, $index).Trim()
		$value = $line.Substring($index + 1).Trim()
		# 去掉成对引号，允许 DB_PASSWORD="a b"
		if ($value.Length -ge 2 -and
				(($value.StartsWith('"') -and $value.EndsWith('"')) -or
				 ($value.StartsWith("'") -and $value.EndsWith("'")))) {
			$value = $value.Substring(1, $value.Length - 2)
		}
		if (Test-Path "Env:$key") {
			Write-Host "[env] $key 已由进程环境提供，跳过 .env 中的值" -ForegroundColor DarkYellow
			continue
		}
		Set-Item -Path "Env:$key" -Value $value
		$loaded += $key
	}
	Write-Host ("[env] 已从 reploy\.env 载入 " + $loaded.Count + " 个变量：" + ($loaded -join ', ')) -ForegroundColor Green
} else {
	Write-Warning "未找到 reploy\.env，将使用 application.yml 里的默认值。首次部署请：Copy-Item reploy\.env.example reploy\.env"
}

# ---------------------------------------------------------------- 2. 定位 jar / java
if (-not $Jar) {
	$Jar = Get-ChildItem (Join-Path $root 'build\libs') -Filter '*.jar' -ErrorAction SilentlyContinue |
		Where-Object { $_.Name -notlike '*-plain.jar' } |
		Sort-Object LastWriteTime -Descending |
		Select-Object -First 1 -ExpandProperty FullName
}
if (-not $Jar -or -not (Test-Path $Jar)) {
	throw '找不到可执行 jar，请先打包：.\gradlew.bat build（产物在 build\libs\*.jar）'
}

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
	throw '未找到 java，请先安装 JDK 21 并加入 PATH'
}

# ---------------------------------------------------------------- 3. 基本检查
if ($Profile -ne 'dev' -and [string]::IsNullOrEmpty($env:DB_PASSWORD)) {
	Write-Warning 'DB_PASSWORD 为空：连 PostgreSQL 会认证失败，请在 reploy\.env 里填写。'
}
if (-not $env:DB_URL) {
	Write-Warning ("DB_URL 未设置，将使用 application.yml 的默认值 " +
		'(jdbc:postgresql://localhost:5432/webdesign)')
}

# ---------------------------------------------------------------- 4. 启动
$javaArgs = @('-jar', $Jar, "--spring.profiles.active=$Profile") + $AppArgs
Write-Host ("[run] profile=" + $Profile + "  jar=" + (Split-Path -Leaf $Jar) +
	"  额外参数=" + (($AppArgs -join ' ') -replace '^\s*$', '（无）')) -ForegroundColor Cyan
Write-Host ('[run] 首次部署请确认已执行 reploy\sql\schema.sql 与 seed.sql（见 reploy\sql\README.md）')
& $java.Source @javaArgs
exit $LASTEXITCODE
