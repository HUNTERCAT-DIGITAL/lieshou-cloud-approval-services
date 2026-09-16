#!/usr/bin/env bash
# ============================================================
# 守卫：产物 jar 里的 Flyway 迁移版本号必须唯一
# ------------------------------------------------------------
# 为什么查**产物**而不是查源码：
#   2026-09-15 发布的 cn.huntercat:lieshoucloud-approval:0.3.5 里同时含
#     V4__seed_default_approval_types.sql
#     V4__seed_haizan_approval_types.sql
#   而本仓**任何提交**都不同时含这两份（去客户化提交是「改名」：删旧名 + 加新名，原子）。
#   → 第二份来自**构建目录残留**（上一次的 target/classes 被 `mvn package` 一起打进 jar）。
#   这类残留**源码层扫描查不出来**，只有拆开 jar 才看得见，所以守卫必须落在产物上。
#
# 症状（线上实测）：approval 启动即
#   org.flywaydb.core.api.FlywayException: Found more than one migration with version 4
#   → 容器 CrashLoopBackOff（演示栈整栈起不来）。
#
# 用法：
#   scripts/check-migration-versions.sh <jar 路径 | 含唯一 jar 的目录>
# 退出码：0 = 版本号唯一；1 = 重复/找不到 jar（发布流程应据此拒发）
# ============================================================
set -euo pipefail

target="${1:?用法: $0 <jar 或含 jar 的目录>}"

if [ -d "$target" ]; then
  # 目录形态：取唯一的 jar（排除 sources/javadoc 之类）
  jar="$(ls -1 "$target"/*.jar 2>/dev/null | grep -vE -- '-(sources|javadoc)\.jar$' | head -1 || true)"
  [ -n "$jar" ] || { echo "❌ 目录里没有 jar（先构建）：$target"; exit 1; }
else
  jar="$target"
fi
[ -f "$jar" ] || { echo "❌ jar 不存在：$jar"; exit 1; }

# 列 jar 条目：优先用 JDK 的 jar（maven 镜像必有），退回 unzip
list_entries() {
  if command -v jar >/dev/null 2>&1; then
    jar tf "$1"
  else
    unzip -Z1 "$1"
  fi
}

# 只取 db/migration 下的迁移文件（支持 V1 / V4 / V4_1 这类版本形式），保留**完整条目名**便于定位
MIG_RE='db/migration/V[0-9]+(_[0-9]+)*__[^/]*'
entries="$(list_entries "$jar" | grep -oE "$MIG_RE" || true)"
count="$(printf '%s\n' "$entries" | grep -c . || true)"

if [ "$count" = "0" ]; then
  echo "⚠️  $jar 里没有 db/migration/V*__* 迁移文件（服务不带动迁移？）—— 守卫跳过"
  exit 0
fi

# 版本号从条目名里抽出来，查重复
dups="$(printf '%s\n' "$entries" | grep -oE 'V[0-9]+(_[0-9]+)*__' | sed 's/__$//' | sort | uniq -d || true)"
if [ -n "$dups" ]; then
  echo "❌ $jar 迁移版本号重复：$(printf '%s' "$dups" | tr '\n' ' ')"
  echo "   涉及文件："
  printf '%s\n' "$entries" | sort -u | sed 's/^/     /'
  echo "   → 根因通常是上一次构建的 target/classes 残留被一起打包：用 \`mvn clean package\` 重构建"
  echo "   → 服务端症状：Flyway \"Found more than one migration with version N\"，容器起不来"
  exit 1
fi

echo "✅ $jar 迁移版本号唯一（$count 个迁移文件）"
