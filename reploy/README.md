# 部署目录（reploy）

> 目录名沿用仓库现有拼写 `reploy`（= deploy）。这里放**部署期**用到的东西：
> 建库脚本、环境变量模板、启动脚本。应用本身的配置结构在
> `src/main/resources/application.yml`，密钥值只在本目录的 `.env` 里。

```
reploy/
├── .env.example   环境变量模板（复制成 .env 后填写，.env 不进版本库）
├── start.ps1      Windows 启动脚本（读 .env → 校验 → java -jar）
├── start.sh       Linux / macOS 启动脚本
├── pugin/         三个一键脚本：start.bat / run.bat / test.bat
└── sql/           建库 / 初始化 / 反安装脚本（详见 sql/README.md）
```

## 0. 三个 .bat 一键脚本

`pugin/` 下的 `.bat` 是 **cmd 批处理脚本**，三者都只是 `start.ps1` 的薄封装
（环境变量与 jar 解析只维护在 `start.ps1` 一处）：

| 文件 | 作用 |
| :--- | :--- |
| `start.bat` | **一键部署**：读 `.env` → `psql` 建表 + 初始化数据 → 以 `prod` 启动 |
| `run.bat` | **仅启动**服务（默认 `prod`，不做建库；可 `-Profile postgres` / `-AppArgs ...`） |
| `test.bat` | 以 **dev 模式启动**（H2 内存库 + 演示数据，不读 `.env`、不需要 PostgreSQL） |

```bat
reploy\pugin\start.bat                   :: 首次部署
reploy\pugin\run.bat                     :: 日常启动
reploy\pugin\run.bat -Profile postgres   :: 换 profile
reploy\pugin\test.bat                    :: 本地 dev（http://localhost:8080）
```

注意：

- `start.bat` 找不到 `psql` 时只打印手工命令并继续启动（库可能已经建好）；`schema.sql` / `seed.sql` 都可重复执行。
- 编码约定：`.bat` 必须 **CRLF + UTF-8 无 BOM**（`.gitattributes` 已固定 `*.bat eol=crlf`）；
  `start.ps1` 反过来必须 **UTF-8 带 BOM** —— 否则 Windows PowerShell 5.1 会按 ANSI 解码中文注释，
  报 `Missing closing '}'` 之类看起来完全不相干的语法错误。

## 1. 三步部署

```powershell
# ① 填环境变量（只需一次）
Copy-Item reploy\.env.example reploy\.env
notepad reploy\.env        # 填 DB_PASSWORD 等

# ② 建库（只需一次；库与表由脚本负责，应用侧 ddl-auto=validate）
psql -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/schema.sql
psql -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/seed.sql

# ③ 打包并启动
.\gradlew.bat build
.\reploy\start.ps1                    # 默认 prod
.\reploy\start.ps1 -Profile postgres  # 等价 profile
```

Linux：

```bash
cp reploy/.env.example reploy/.env
vi reploy/.env
psql -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/schema.sql
psql -U postgres -d webdesign -v ON_ERROR_STOP=1 -f reploy/sql/seed.sql
./gradlew build
./reploy/start.sh
```

## 2. 环境变量

配置分两层，别混在一起：

| 层 | 放哪 | 放什么 |
| :--- | :--- | :--- |
| 结构（键、默认值、profile 差异） | `src/main/resources/application.yml` | 数据源 URL 模板、JPA、上传、JWT 键名、时间窗口兜底 |
| 密钥值（每个环境不同） | `reploy/.env` | 数据库密码、JWT 密钥、端口等 |

| 变量 | 必填 | 说明 |
| :--- | :--- | :--- |
| `DB_URL` | 是 | `jdbc:postgresql://host:5432/webdesign` |
| `DB_USERNAME` | 是 | 默认 `postgres` |
| `DB_PASSWORD` | 是 | 留空会认证失败（PG 要求 SCRAM 密码） |
| `APP_JWT_SECRET` | 建议 | 覆盖 `app.jwt.secret`，正式环境务必改掉演示密钥 |
| `SERVER_PORT` | 否 | 覆盖 `server.port`，默认 8080 |
| `APP_UPLOAD_DIR` | 否 | 菜品图片目录，默认 `uploads` |

**优先级：进程环境变量 > `reploy/.env` > `application.yml` 默认值。**
脚本只补「当前进程里没有」的变量，所以 CI / Docker 里直接给环境变量即可，无需 `.env`。

变量名规则：`application.yml` 里写死的 `${DB_URL:...}` 用同名变量；其它配置用
「大写 + 下划线」即可覆盖（Spring Boot 宽松绑定会自动转换，如
`APP_JWT_SECRET` → `app.jwt.secret`）。

## 3. profile 怎么选

| profile | 数据库 | `ddl-auto` | 用途 |
| :--- | :--- | :--- | :--- |
| `dev`（默认） | H2 内存库 | `create-drop` | 本地开发，无需装数据库 |
| `prod` | PostgreSQL | `validate` | 生产部署（脚本建表） |
| `postgres` | PostgreSQL | `validate` | `prod` 的别名，兼容老文档/旧脚本 |

启动脚本默认 `prod`；本地想验证脚本本身可以跑 `.\reploy\start.ps1 -Profile dev`（`dev` 不读 `DB_*`）。

## 4. 常见问题

| 现象 | 处理 |
| :--- | :--- |
| `找不到可执行 jar` | 先 `.\gradlew.bat build`，或 `-Jar <路径>` 指定 |
| 启动报 `SCRAM-based authentication, but no password was provided` | `.env` 里 `DB_PASSWORD` 为空 |
| 启动报 `Schema-validation: missing table [xxx]` | 没执行 `reploy/sql/schema.sql`，或执行到了别的库 |
| 想临时让 Hibernate 自动建表 | 把 `application.yml` 里 prod 段的 `ddl-auto` 改回 `update`（仅限临时） |
| `[env] XXX 已由进程环境提供` | 正常提示：进程环境变量优先，`.env` 里的同名值被跳过 |

## 5. 安全约定

- `.env` 已被 `.gitignore` 忽略，**禁止提交真实密码**；仓库里只保留 `.env.example`。
- 上线前删除演示账号，或把 `reploy/sql/seed.sql` 里的演示密码全部改掉。
  硬编码的临时管理员（`admin` / `admin123`）**已从代码中删除**，系统只认 `users` 表里的真实账号。
