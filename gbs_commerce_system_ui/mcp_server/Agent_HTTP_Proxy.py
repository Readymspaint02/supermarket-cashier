import os
import time
from typing import Dict, Any, Optional

import requests
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel


class AgentQuery(BaseModel):
    query: str


app = FastAPI(title="Huawei Agent HTTP Proxy")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 调试阶段放开，正式可按需收紧
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Agent URL 从环境变量读取，避免写死在前端
AGENT_URL = os.getenv("HUAWEI_AGENT_URL", "")

# ===== IAM Token 简单内存缓存 =====
_IAM_TOKEN: Optional[str] = None
_IAM_TOKEN_EXPIRE_TS: float = 0.0  # UNIX 时间戳，过期后需重新获取


def _set_token_cache(token: str, ttl_seconds: int = 600) -> None:
    """设置内存中的 Token 缓存。

    为简化实现，这里默认缓存 10 分钟；如需更精细控制，可根据实际返回设置。
    """

    global _IAM_TOKEN, _IAM_TOKEN_EXPIRE_TS
    _IAM_TOKEN = token
    _IAM_TOKEN_EXPIRE_TS = time.time() + ttl_seconds


def get_iam_token() -> str:
    """通过用户名密码向 IAM 获取 X-Auth-Token。

    逻辑参考 test07_huawei_token.py，敏感信息从环境变量中读取：
    - HUAWEI_USERNAME
    - HUAWEI_PASSWORD
    - HUAWEI_DOMAIN_NAME
    - HUAWEI_PROJECT_NAME (如 cn-north-4)
    """
    global _IAM_TOKEN, _IAM_TOKEN_EXPIRE_TS

    # 如果缓存中已有且未过期，直接复用
    now = time.time()
    if _IAM_TOKEN and now < _IAM_TOKEN_EXPIRE_TS:
        return _IAM_TOKEN

    username = os.getenv("HUAWEI_USERNAME", "")
    password = os.getenv("HUAWEI_PASSWORD", "")
    domain_name = os.getenv("HUAWEI_DOMAIN_NAME", "")
    project_name = os.getenv("HUAWEI_PROJECT_NAME", "cn-north-4")

    if not all([username, password, domain_name]):
        raise RuntimeError(
            "缺少 HUAWEI_USERNAME / HUAWEI_PASSWORD / HUAWEI_DOMAIN_NAME 环境变量，无法获取 IAM Token"
        )

    url = "https://iam.cn-north-4.myhuaweicloud.com/v3/auth/tokens"
    payload = {
        "auth": {
            "identity": {
                "methods": ["password"],
                "password": {
                    "user": {
                        "name": username,
                        "password": password,
                        "domain": {"name": domain_name},
                    }
                },
            },
            "scope": {"project": {"name": project_name}},
        }
    }
    headers = {"Content-Type": "application/json"}

    resp = requests.post(url, json=payload, headers=headers, timeout=30, verify=True)
    if resp.status_code not in (200, 201):
        raise RuntimeError(f"获取 IAM Token 失败，状态码 {resp.status_code}，响应: {resp.text}")

    token = resp.headers.get("X-Subject-Token")
    if not token:
        raise RuntimeError("响应中没有 X-Subject-Token 头，无法获取 Token")

    # 简单缓存 Token，一般有效期远大于 10 分钟，这里保守缓存 10 分钟
    _set_token_cache(token, ttl_seconds=600)
    return token


def parse_sse_summary(raw_text: str) -> Dict[str, Any]:
    """解析 SSE 文本，提取中文回复及统计耗时。

    优先使用 event=summary_response 的 content；
    如无 summary_response，则将所有 event=message 的 content 依次拼接。

    同时尝试从 event=statistic_data 中提取 latency.overall 作为总耗时。

    返回:
    - {"summary": "...", "events": [...], "latency_overall": float|None}。
    """
    summary_from_event = ""
    messages: list[str] = []
    events: list[Dict[str, Any]] = []
    latency_overall: Optional[float] = None
    if not raw_text:
        return {"summary": "", "events": events}

    for line in raw_text.splitlines():
        line = line.strip()
        if not line:
            continue
        # 既兼容以 data: 开头的 SSE 行，也兼容纯 JSON 行
        if line.startswith("data:"):
            payload = line[len("data:") :].strip()
        else:
            payload = line
        # 先尝试按 JSON 解析
        try:
            obj = requests.utils.json.loads(payload)
            events.append(obj)
            ev = obj.get("event")
            content = obj.get("content", "")
            if ev == "summary_response" and content:
                summary_from_event = content
            elif ev == "message" and content:
                messages.append(content)
            elif ev == "statistic_data":
                try:
                    latency = obj.get("latency") or {}
                    overall = latency.get("overall")
                    if overall is not None:
                        latency_overall = float(overall)
                except Exception:
                    pass
            continue
        except Exception:
            # 不是 JSON，就当作纯文本 content
            if payload:
                events.append({"raw": payload})
                messages.append(payload)
            continue

    if summary_from_event:
        summary = summary_from_event
    else:
        summary = "".join(messages)

    return {"summary": summary, "events": events, "latency_overall": latency_overall}


@app.post("/agent_query")
async def agent_query(req: AgentQuery) -> Dict[str, Any]:
    """前端调用的代理接口：将 query 转发给华为智能体。

    请求体：{"query": "..."}
    返回：转发智能体返回的 JSON；如出错返回 {status: "error", message: "..."}
    """
    if not AGENT_URL:
        return {"status": "error", "message": "后端未配置 HUAWEI_AGENT_URL 环境变量"}

    try:
        token = get_iam_token()
    except Exception as e:
        return {"status": "error", "message": f"获取 IAM Token 失败: {e}"}

    try:
        # 这里禁用证书校验以解决 IP 证书不匹配问题，仅限本赛题 Demo 环境
        resp = requests.post(
            AGENT_URL,
            headers={
                "Content-Type": "application/json",
                "X-Auth-Token": token,
            },
            # 新版 Agent API 要求请求体为 {"inputs": {"query": "..."}}
            json={
                "inputs": {
                    "query": req.query,
                }
            },
            verify=False,
            timeout=30,
        )

        # 无论 Content-Type 为何，统一按 UTF-8 解析并从中提取中文 summary
        try:
            text = resp.content.decode("utf-8", errors="ignore")
        except Exception:
            text = resp.text

        parsed = parse_sse_summary(text)
        data: Dict[str, Any] = {
            "summary": parsed.get("summary", ""),
            "raw_text": text,
            "latency_overall": parsed.get("latency_overall"),
        }

        return {"status": "success", "http_status": resp.status_code, "data": data}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@app.get("/health")
async def health() -> Dict[str, str]:
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    host = "0.0.0.0"
    port = int(os.getenv("AGENT_PROXY_PORT", "9000"))
    print(f"Agent HTTP Proxy 启动在 http://{host}:{port}")
    uvicorn.run(app, host=host, port=port)
