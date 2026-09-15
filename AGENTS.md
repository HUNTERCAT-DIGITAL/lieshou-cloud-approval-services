# AGENTS.md — 给 AI 协作者的仓库指引

> 给"未来在这个仓库工作的 AI agent"读的指引。新会话按顺序 `cat` 本文件 → `README.md` → `CHANGELOG.md`。

## 我是谁

你是 **lieshou-cloud-approval-services**（审批流服务 · `cn.huntercat:lieshoucloud-approval`）的 AI 协作伙伴。
技术栈：Java 21 · Spring Boot 3.5 · Spring Cloud Alibaba（Nacos）· Maven 聚合构建
（parent = `cn.huntercat:framework-parent:0.3.3`，来自内网 Nexus，**本仓无 submodule**）·
PostgreSQL + Flyway · JUnit 5 + JaCoCo + Spotless。

## 架构速览

```
pom.xml（聚合器，只有一个模块）
└── services/approval/          Spring Boot 薄壳（业务核心在 framework-approval）
    ├── web/                    ApprovalController(/api/approvals) · ApprovalTypeController(/api/approval-types)
    ├── feign/                  UserQueryClient(选审批人) · EmployeeStatusClient(改员工状态) · ProjectCreateClient(建项目)
    ├── service/                ApprovalNotifier（待办/结果邮件；未配 spring.mail.* 自动旁路 · ADR-0032）
    ├── domain/                 ApprovalTypeConfig(+Repository)
    └── resources/              application*.yml · db/migration V1..V5（独立 history 表 flyway_schema_history_approval）
```

- **多租户**：`X-Tenant-Id` 强制过滤（缺失/非数字 → `TenantContextRequiredException`，ADR-0025 模式）。
- **审批链**：`approval_requests.approver_ids`（逗号分隔用户 ID，空 = 单步）+ `current_step`（0-based，多级）；
  `ApprovalTypeConfig` 管类型（code/name/category/表单 fields schema），不管审批人。
- **跨服务动作**：审批通过后经 Feign 改员工状态（`EmployeeStatusClient`）、建项目（`ProjectCreateClient`）；审批人挑选读 `user-service`。
- 端口 `8087`（`APPROVAL_PORT`）。

## 关键约定

- **Commit**：Conventional Commits（`feat:` / `fix:` / `ci:` / `docs:` / `chore:` / `refactor:`），中文描述。
- **分支**：`main` 已保护（`push_access_level=0`）→ 只能特性分支 + MR；改动 push 后等 MR pipeline 绿再合并。
- **版本号**：`services/approval` 的 `project.version` **继承 `framework-parent`**（framework 升版 → 本仓版本跟着走）。
  tag `vX.Y.Z` 必须等于 `project.version`，否则 CI `deploy` 拒发；版本号不是自选的，先看 framework 版本线。
- **发布点只增不改**：改 `.gitlab-ci.yml` / 打 tag / 触发 Nexus 发布前先说明影响。
- **只发自己**：`framework-*` 归 `lieshou-framework` 仓发布（同坐标重复发布会撞 Nexus 重发策略），CI 用 `-pl services/approval`。
- **格式**：google-java-format（GOOGLE 风格，`mvn spotless:apply`）；公开类/方法写中文 Javadoc。
- **声称"完成/通过"前要有实测输出**：`mvn -B -ntp -s services/.m2-settings.xml verify` 的输出、pipeline 链接、
  或 Nexus / Harbor 里可见的制品。

## 当前阶段

- 2026-09-15：上游引入方式统一为内网 Nexus（framework submodule 已裁）、CI 三段（verify / deploy / image）跑通，
  版本线 0.3.3；**0.3.1** 的 JAR 在 Nexus，**0.3.3** 起开始同时出 Harbor 镜像（见 `CHANGELOG.md`）。

## 关键决策

- **2026-09-14 · 制品仓库分工**：Maven 制品走 Nexus `maven-releases`（`nexus.ts.huntercat.cn`），
  docker/OCI 镜像走 Harbor（`harbor.ts.huntercat.cn/lieshou-cloud/approval`）。**不在 Nexus 开 docker 仓**。
- **2026-09-15 · 上游只走 Nexus**：移除 `framework` submodule，全部按坐标从 `maven-public` 解析
  （同一份上游只有一种引入方式；代价：Dockerfile / 新机器必须先信任 Caddy 内部 CA）。
