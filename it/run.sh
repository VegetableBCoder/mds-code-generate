#!/usr/bin/env bash
# ============================================================================
# codegen-it 一键链路验证
#
#   ./run.sh              # 完整链路（建表 → MBG → 编译 → 生成 → 编译 → 集成测试）
#   ./run.sh --no-db      # 跳过建表（库已存在）
#
# 依赖：本机 MySQL/MariaDB 在 127.0.0.1:3306，账号见 pom.xml 的 db.* 属性。
# ============================================================================
set -euo pipefail

cd "$(dirname "$0")"
ROOT=".."
MVN="$ROOT/mvnw"
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
# 口令不从仓库读：环境变量 DB_PASSWORD 优先，其次交互式询问
DB_PASS="${DB_PASSWORD:-}"
SKIP_DB=0

[[ "${1:-}" == "--no-db" ]] && SKIP_DB=1

# MBG 连库要口令（--no-db 只是跳过建表，不跳过连库）
if [[ -z "$DB_PASS" ]]; then
  read -rsp "MySQL($DB_USER@$DB_HOST:$DB_PORT) 口令: " DB_PASS
  echo
fi

echo "==> 0/6 安装插件本体"
"$MVN" -q -f "$ROOT/pom.xml" install -DskipTests

if [[ $SKIP_DB -eq 0 ]]; then
  echo "==> 1/6 建库建表"
  MYSQL_CLIENT="${MYSQL_CLIENT:-mysql}"
  if ! command -v "$MYSQL_CLIENT" >/dev/null 2>&1; then
    echo "    找不到 mysql 客户端；请自行建表，或用 --no-db 跳过"
    exit 1
  fi
  "$MYSQL_CLIENT" -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" < db/schema.sql
else
  echo "==> 1/6 跳过建表"
fi

echo "==> 2/6 MBG 生成 DAO（连库）"
# 口令必须同时给 process-resources（它把 ${db.password} 过滤进 generatorConfig.xml）
"$MVN" -q -f pom.xml -pl demo-dao -Ddb.password="$DB_PASS" process-resources
"$MVN" -q -f pom.xml -pl demo-dao -Ddb.password="$DB_PASS" \
    org.mybatis.generator:mybatis-generator-maven-plugin:1.4.2:generate

echo "==> 3/6 第一次编译（让 DAO 的 class 存在）"
"$MVN" -q -f pom.xml -pl demo-dao compile

echo "==> 4/6 插件生成（实体清单来自 codegen.yaml）"
"$MVN" -q -f pom.xml -pl demo-dao \
    com.mds.codegen:codegen-maven-plugin:1.0.0-SNAPSHOT:generate

echo "==> 5/6 第二次编译（把生成物编进来）"
"$MVN" -q -f pom.xml -pl demo-dao compile

echo "==> 6/6 集成测试（真实 MySQL）"
DB_PASSWORD="$DB_PASS" "$MVN" -f pom.xml -pl demo-dao test -Pit -Dtest=CodegenIntegrationTest

echo
echo "全部通过 ✅"