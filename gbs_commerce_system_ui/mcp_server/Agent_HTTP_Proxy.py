import os
import json
import httpx
from typing import Any, Dict, List, Optional
from datetime import datetime
from pathlib import Path

from dotenv import load_dotenv
env_path = Path(__file__).parent / ".env.secrets"
if env_path.exists():
    load_dotenv(env_path)
else:
    load_dotenv()

from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
import jwt as pyjwt

from repositories import MemberRepository, OrderRepository, AnalyticsRepository


class AgentQuery(BaseModel):
    query: str


app = FastAPI(title="DeepSeek Agent Proxy")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

JWT_SECRET = os.getenv("JWT_SECRET", "mySecretKeyForJWTWhichShouldBeLongerAndMoreSecure123456")
security = HTTPBearer(auto_error=False)


def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)) -> Dict[str, Any]:
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="未提供认证令牌"
        )
    
    token = credentials.credentials
    
    try:
        payload = pyjwt.decode(token, JWT_SECRET, algorithms=["HS256", "HS384", "HS512"])
        return payload
    except pyjwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="令牌已过期"
        )
    except pyjwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的令牌"
        )


DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
if not DEEPSEEK_API_KEY:
    raise ValueError("DEEPSEEK_API_KEY environment variable is required. Please set it in .env.secrets file.")
DEEPSEEK_BASE_URL = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")

member_repo = MemberRepository()
order_repo = OrderRepository()
analytics_repo = AnalyticsRepository()

TOOLS: List[Dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "member_get_profile",
            "description": "根据会员ID查询会员资料，包括积分、等级、余额等信息",
            "parameters": {
                "type": "object",
                "properties": {
                    "member_id": {"type": "string", "description": "会员ID"}
                },
                "required": ["member_id"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "member_search",
            "description": "模糊搜索会员，关键字可以是会员编号、姓名或手机号",
            "parameters": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "description": "搜索关键字"},
                    "limit": {"type": "integer", "description": "返回条数，默认10"}
                },
                "required": ["keyword"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "order_get_detail",
            "description": "查询订单详情，包括商品明细、金额、支付方式等",
            "parameters": {
                "type": "object",
                "properties": {
                    "order_no": {"type": "string", "description": "订单号"}
                },
                "required": ["order_no"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "order_recent",
            "description": "查询最近订单列表",
            "parameters": {
                "type": "object",
                "properties": {
                    "limit": {"type": "integer", "description": "返回条数，默认10"}
                },
                "required": []
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "assistant_insight",
            "description": "获取经营分析数据，如销售趋势、热销商品、会员分布",
            "parameters": {
                "type": "object",
                "properties": {
                    "metric": {"type": "string", "description": "指标类型：sales_trend、top_products、member_segments"},
                    "days": {"type": "integer", "description": "统计天数，默认7"}
                },
                "required": ["metric"]
            }
        }
    }
]


def execute_tool(tool_name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
    if tool_name == "member_get_profile":
        member_id = arguments.get("member_id", "")
        profile = member_repo.get_member(member_id)
        if not profile:
            return {"status": "not_found", "message": "会员不存在", "member_id": member_id}
        return {"status": "success", "data": profile}

    elif tool_name == "member_search":
        keyword = arguments.get("keyword", "")
        limit = arguments.get("limit", 10)
        members = member_repo.search_members(keyword, limit)
        return {"status": "success", "data": members}

    elif tool_name == "order_get_detail":
        order_no = arguments.get("order_no", "")
        bundle = order_repo.get_order_with_items(order_no)
        if not bundle:
            return {"status": "not_found", "message": "订单不存在", "order_no": order_no}
        summary = order_repo.summarize_order(bundle)
        return {"status": "success", "data": summary}

    elif tool_name == "order_recent":
        limit = arguments.get("limit", 10)
        orders = order_repo.list_recent_orders(limit)
        return {"status": "success", "data": orders}

    elif tool_name == "assistant_insight":
        metric = arguments.get("metric", "sales_trend")
        days = arguments.get("days", 7)
        if metric == "sales_trend":
            data = analytics_repo.get_sales_trend(days)
        elif metric == "top_products":
            data = analytics_repo.get_top_products(days)
        elif metric == "member_segments":
            data = analytics_repo.get_member_segments()
        else:
            return {"status": "error", "message": f"未知指标: {metric}"}
        return {"status": "success", "metric": metric, "data": data}

    else:
        return {"status": "error", "message": f"未知工具: {tool_name}"}


async def call_deepseek(messages: List[Dict[str, Any]]) -> Dict[str, Any]:
    if not DEEPSEEK_API_KEY:
        return {"error": "未配置 DEEPSEEK_API_KEY"}

    url = f"{DEEPSEEK_BASE_URL}/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": DEEPSEEK_MODEL,
        "messages": messages,
        "tools": TOOLS,
        "tool_choice": "auto"
    }

    print(f"[DEBUG] API Key: {DEEPSEEK_API_KEY[:10]}...")
    print(f"[DEBUG] URL: {url}")
    
    async with httpx.AsyncClient(timeout=60.0) as client:
        resp = await client.post(url, headers=headers, json=payload)
        print(f"[DEBUG] Status: {resp.status_code}")
        print(f"[DEBUG] Response: {resp.text[:500]}")
        if resp.status_code != 200:
            return {"error": f"DeepSeek API错误: {resp.status_code}", "detail": resp.text}
        return resp.json()


async def agent_loop(user_query: str) -> str:
    messages = [
        {
            "role": "system",
            "content": "你是智慧超市的智能助手，可以帮助用户查询会员信息、订单详情、经营分析等。请用中文简洁回答，遇到查询类请求请调用相应的工具函数。回答时不要使用markdown格式，不要加星号、井号等特殊符号，直接用纯文本回答即可。"
        },
        {"role": "user", "content": user_query}
    ]

    max_iterations = 5
    for _ in range(max_iterations):
        result = await call_deepseek(messages)
        if "error" in result:
            return f"抱歉，AI服务暂时不可用: {result['error']}"

        choice = result.get("choices", [{}])[0]
        message = choice.get("message", {})
        finish_reason = choice.get("finish_reason", "")

        if finish_reason == "stop" and message.get("content"):
            return message["content"]

        if finish_reason == "tool_calls" and message.get("tool_calls"):
            messages.append(message)
            for tool_call in message["tool_calls"]:
                tool_name = tool_call["function"]["name"]
                arguments = json.loads(tool_call["function"]["arguments"])
                tool_result = execute_tool(tool_name, arguments)
                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call["id"],
                    "content": json.dumps(tool_result, ensure_ascii=False)
                })
            continue

        if message.get("content"):
            return message["content"]

        return "抱歉，我无法处理这个请求。"

    return "抱歉，处理超时，请简化您的请求。"


@app.post("/agent_query")
async def agent_query(req: AgentQuery, user: Dict[str, Any] = Depends(verify_token)) -> Dict[str, Any]:
    try:
        summary = await agent_loop(req.query)
        return {
            "status": "success",
            "data": {
                "summary": summary,
                "raw_text": summary
            }
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}


@app.get("/health")
async def health() -> Dict[str, str]:
    return {"status": "ok", "model": DEEPSEEK_MODEL}


if __name__ == "__main__":
    import uvicorn

    host = "0.0.0.0"
    port = int(os.getenv("AGENT_PROXY_PORT", "9000"))
    print(f"DeepSeek Agent Proxy 启动在 http://{host}:{port}")
    print(f"模型: {DEEPSEEK_MODEL}")
    uvicorn.run(app, host=host, port=port)