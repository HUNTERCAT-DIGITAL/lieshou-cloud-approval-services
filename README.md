# lieshou-cloud-approval-services · 开源后端服务:approval

> 自 `lieshou-cloud-core/services/approval` 拆分（2026-08 细粒度）。
> 审批业务核心在 **`framework-approval`**（上游 `lieshou-framework`），本仓只留 Controller + 装配 + 启动。
> **上游全部走内网 Nexus**：本仓无 submodule；`framework-parent` / `framework-*` 由
> `https://nexus.ts.huntercat.cn/repository/maven-public/` 解析（framework 版本线 0.3.3，本仓版本继承它）。
> 多租户按 `X-Tenant-Id` 强制过滤（ADR-0025 模式）。

## 构建

```bash
# 前提：JDK 21 + Maven 3.8+；依赖从内网 Nexus 解析（仓内自带 settings，无需改 ~/.m2）
# 在仓根执行（不要 cd services）
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -ntp -s services/.m2-settings.xml \
  -DskipTests -f pom.xml -pl services/approval package
# 全量门（spotless:check + 单测 + jacoco，由 framework-parent 统一绑定）
mvn -B -ntp -s services/.m2-settings.xml verify
```

镜像：`docker build -f services/approval/Dockerfile .`（context = 仓根；builder 阶段导入
`services/.ci/caddy-root.crt` 后走同一份 `services/.m2-settings.xml`）。

## 运行

```bash
java -jar services/approval/target/lieshoucloud-approval.jar     # 默认 8087（APPROVAL_PORT）
```

关键环境变量：`APPROVAL_PORT` · `DB_HOST`/`DB_PORT`/`POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD`
（PostgreSQL，schema 由 Flyway 维护，独立 history 表 `flyway_schema_history_approval`）·
`NACOS_DISCOVERY_ENABLED`/`NACOS_SERVER_ADDR`（默认开）· `NACOS_CONFIG_ENABLED`（默认关，ADR-0029）·
`EMAIL_FROM_ADDR`（审批待办/结果邮件，未配 `spring.mail.*` 自动旁路，ADR-0032）。

## 对外接口

| 端点 | 说明 |
| --- | --- |
| `GET /api/approvals` | 审批列表（可选 `role` / `type` / `status` 过滤） |
| `GET /api/approvals/counts` | 各状态计数（前端角标） |
| `GET /api/approvals/{id}` | 审批详情 |
| `POST /api/approvals` | 发起审批（可传审批人链 `approverIds`，空 = 单步；多级进度见 `currentStep`） |
| `POST /api/approvals/{id}/approve` · `/reject` · `/cancel` | 通过 / 驳回 / 撤销 |
| `GET /api/approvals/audit-logs` | 审批审计日志（`limit` 默认 100） |
| `GET` `POST /api/approval-types` · `PUT` `DELETE /api/approval-types/{id}` | 审批类型配置：类别 + 表单字段 schema（R5.2） |

调用方身份经网关注入 `X-Tenant-Id` / `X-User-Id`（缺失或非数字 → `TenantContextRequiredException`，由框架统一映射）。
契约见运行时 SpringDoc：`/v3/api-docs`、`/swagger-ui.html`。健康检查 `/actuator/health`。

## 制品

| 制品 | 坐标 / 位置 | 触发 |
| --- | --- | --- |
| Maven JAR | `cn.huntercat:lieshoucloud-approval:<version>` → Nexus `maven-releases` | tag `vX.Y.Z`（CI `deploy`；tag 必须 == `project.version`，否则拒发） |
| 容器镜像 | `harbor.ts.huntercat.cn/lieshou-cloud/approval:<version>` | tag → 版本号自动；`main` → `:dev` 手动（CI `image`） |

版本号 = `services/approval` 的 `project.version`，**继承 `framework-parent`**（升 framework 即升本仓版本）。
`framework-*` 由 `lieshou-framework` 仓自己发布，本仓只发自己这一个模块。

## CI（`.gitlab-ci.yml`）

`verify`（MR / main / tag）→ `deploy`（仅 tag）→ `image`（tag 自动 / main 手动）；
runner tag `java,wqc`，PG 集成测试在 CI 由 postgres service 提供真库（本地开发走 Testcontainers）。
凭证：CI 变量 `NEXUS_DEPLOY_USER/PASS`、`HARBOR_PASS`（protected）。

## 文档导航

- `AGENTS.md` —— 给 AI 协作者的仓库指引 + 项目记忆（架构 / 约定 / 决策）
- `CHANGELOG.md` —— 版本变更
