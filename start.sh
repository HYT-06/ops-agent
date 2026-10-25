#!/usr/bin/env bash
# ops-agent 构建与启动脚本
#   ./start.sh         构建并运行可执行 jar
#   ./start.sh dev     以 spring-boot:run 启动（配合 devtools 热更新）
#   ./start.sh build   只构建
#   ./start.sh test    运行全部测试
#   ./start.sh run     直接运行已构建的 jar
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
JAR="ops-orchestrator/target/ops-orchestrator-0.1.0-SNAPSHOT.jar"
CMD="${1:-start}"

case "$CMD" in
  build)
    mvn -DskipTests package
    ;;
  test)
    mvn test
    ;;
  run)
    [ -f "$JAR" ] || { echo "未找到 $JAR，请先执行 ./start.sh build" >&2; exit 1; }
    java -jar "$JAR"
    ;;
  dev)
    mvn -pl ops-orchestrator -am spring-boot:run
    ;;
  start)
    mvn -DskipTests package
    java -jar "$JAR"
    ;;
  *)
    echo "用法: $0 {start|dev|build|test|run}" >&2
    exit 1
    ;;
esac
