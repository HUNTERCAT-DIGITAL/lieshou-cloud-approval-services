# lieshou-cloud-approval-services · 开源后端服务:approval

> 自 lieshou-cloud-core/services/approval 拆分(2026-08 细粒度)。
> **上游全部走内网 Nexus**:本仓无 submodule;`framework-parent` / `framework-*` 由
> `https://nexus.ts.huntercat.cn/repository/maven-public/` 解析(framework 版本线 0.3.3)。

```bash
# 在仓根执行(不要 cd services)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -ntp -DskipTests -f pom.xml -pl services/approval package
```

镜像:`docker build -f services/approval/Dockerfile .`(context = 仓根;builder 阶段导入 `services/.ci/caddy-root.crt`)
