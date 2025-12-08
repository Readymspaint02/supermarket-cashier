#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$SCRIPT_DIR"

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

stop_service() {
  local keyword="$1"
  local pids
  pids=$(ps aux | grep "$keyword" | grep -v grep | awk '{print $2}')
  if [[ -z "$pids" ]]; then
    echo "未找到进程: $keyword"
    return
  fi
  echo "停止 ${keyword} -> ${pids}"
  # shellcheck disable=SC2086
  kill $pids || true
}

for svc in "${SERVICES[@]}"; do
  stop_service "uv run ${svc}"
done

echo
echo "已发送终止信号，可使用 ps/grep 再次确认进程状态。"
