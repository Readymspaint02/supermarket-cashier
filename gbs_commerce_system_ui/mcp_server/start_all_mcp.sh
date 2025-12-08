#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$SCRIPT_DIR"

LOG_DIR="${SCRIPT_DIR}/logs"
mkdir -p "$LOG_DIR"

SERVICES=(
  "Baidu_TTS_HTTP_test.py"
  "Member_Service_HTTP_test.py"
  "Order_Service_HTTP_test.py"
  "Baidu_TTS_MCP.py"
  "Member_Service_MCP.py"
  "Order_Service_MCP.py"
  "Huawei_ASR_MCP.py"
  "Agent_HTTP_Proxy.py"
)

start_service() {
  local script_name="$1"
  local log_file="$LOG_DIR/${script_name%.py}.log"
  echo "启动 ${script_name} ..."
  nohup uv run "$script_name" >"$log_file" 2>&1 &
}

for svc in "${SERVICES[@]}"; do
  start_service "$svc"
done

echo
echo "所有 MCP / HTTP 服务已后台启动，日志位于 ${LOG_DIR}"
