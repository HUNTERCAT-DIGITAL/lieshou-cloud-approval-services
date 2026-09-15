# Changelog

本仓（`lieshoucloud-approval`）的版本变更。版本号 = `services/approval` 的 Maven 版本，默认继承上游
`framework-parent`；制品发到内网 Nexus `maven-releases`（见 `AGENTS.md`）。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## 0.3.3（2026-09-15）

上游引入方式统一（submodule → 内网 Nexus）+ 文档补齐，无业务代码行为变更。

### 变更

- **移除 `framework` submodule**：`framework-parent` / `framework-*` 一律从内网 Nexus
  `maven-public` 解析；新增 `services/.m2-settings.xml`（`mirrorOf=*`）供本机与 Dockerfile 复用
- `services/approval` parent `framework-parent` 0.3.1 → **0.3.3**（本仓版本随之 = 0.3.3）
- `services/approval/Dockerfile`：builder 阶段导入 `services/.ci/caddy-root.crt`（内部 CA）后走仓内 settings
- CI：`build-image` 新增（tag → 版本号自动，`main` → `:dev` 手动）；Harbor 登录用户名改单引号字面量
  （`robot$lieshou-cloud+gitlab-ci` 的 `$` 会被 GitLab 变量展开吃掉）；`before_script` 去掉 ASCII `": "`
  （YAML 会把它解析成 mapping → pipeline 零 job）；PG 集成测试由 postgres service 提供真库

### 新增

- 文档：`CHANGELOG.md` · `AGENTS.md`（项目记忆）· `README.md` 补齐构建/运行/端点/制品/CI

## 0.3.1（2026-09-14）

制品首次入内网 Nexus：`cn.huntercat:lieshoucloud-approval:0.3.1` → `maven-releases`
（CI `deploy` stage，仅 `v*` tag 触发；校验 tag == `project.version`；`-pl services/approval` 只发本仓模块）。

- `services/approval` parent `framework-parent` 0.3.0 → 0.3.1（0.3.1 起自带 `distributionManagement`）
- 该 tag 早于 `build-image` stage 上线，故无对应 Harbor 镜像
