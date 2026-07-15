"""
============================================================
【MCP-01】Unified_MCP_Service.py - 统一MCP服务（AI工具）
============================================================

文件作用：
基于FastMCP框架开发MCP Server，为AI助手提供业务工具。
核心功能：会员查询、订单查询、数据统计、语音合成。

技术原理：
- MCP（Model Context Protocol）：模型上下文协议，AI工具标准
- FastMCP：快速开发MCP Server的Python框架
- @mcp.tool()：装饰器，将函数注册为AI可调用的工具
- SSE（Server-Sent Events）：服务器推送事件，实现流式传输

MCP架构：
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  AI助手     │ ──SSE──▶│  MCP Server │ ──SQL──▶│   MySQL     │
│ (DeepSeek)  │         │  (Python)   │         │             │
└─────────────┘         └─────────────┘         └─────────────┘
       │                       │
       │                       ├─ member_repo（会员仓储）
       │                       ├─ order_repo（订单仓储）
       │                       └─ analytics_repo（统计仓储）

业务流程（AI查询会员）：
1. 用户语音："查询会员M001的信息"
2. AI助手识别意图，调用member_get_profile工具
3. MCP Server执行工具函数，查询MySQL
4. 返回会员信息给AI助手
5. AI助手语音播报结果

面试考点：
- Q1：什么是MCP？为什么要用MCP？
  A1：MCP（Model Context Protocol）是AI工具协议标准。
      让AI助手可以调用外部工具（查询数据库、调用API等）。
      我们用MCP让AI助手能查询会员、订单等业务数据。

- Q2：FastMCP的作用？
  A2：FastMCP是快速开发MCP Server的框架。
      提供@mcp.tool()装饰器，自动生成工具描述和API。
      无需手动处理协议细节，专注业务逻辑。

- Q3：@mcp.tool()装饰器的作用？
  A3：将Python函数注册为MCP工具。
      AI助手可以通过工具名称调用函数。
      函数文档字符串会作为工具描述，帮助AI理解如何使用。

- Q4：为什么要用SSE？
  A4：SSE（Server-Sent Events）实现服务器推送。
      AI助手和MCP Server建立长连接，实时传输数据。
      适合AI对话场景，支持流式响应。

- Q5：MCP Server和后端API的区别？
  A5：- 后端API：给人用的，返回JSON
      - MCP Server：给AI用的，提供工具描述和调用接口
      两者可以共享数据库和业务逻辑。

- Q6：如何处理AI调用工具失败？
  A6：返回错误信息给AI，AI会尝试其他方式或告知用户。
      我们封装了统一错误格式：{"status": "error", "message": "..."}

提供的工具：
1. member_get_profile：查询会员信息
2. member_update_points：更新会员积分
3. order_get_recent：查询最近订单
4. analytics_get_sales：查询销售统计
5. text_to_speech：文本转语音（百度TTS）

关联文件：
- repositories.py（数据仓储，封装数据库操作）
- db_utils.py（数据库连接工具）
- Baidu_TTS_MCP.py（百度TTS服务）

参考文档：
- 梳理项目.md 一、项目概述
============================================================
"""

from __future__ import annotations

import os
import sys
import base64
from typing import Any, Dict, List, Optional

from mcp.server.fastmcp import FastMCP
from aip import AipSpeech

from db_utils import ping_database
from repositories import MemberRepository, OrderRepository, AnalyticsRepository


# 【MCP-01-初始化】创建FastMCP实例
# 参数：服务名称，显示在AI助手的工具列表中
mcp = FastMCP("SmartMarket-Service")

# 【MCP-01-数据仓储】初始化数据库仓储对象
# 面试考点：为什么要用Repository模式？
# 答：封装数据库操作，业务层不直接操作数据库，
#     便于测试、维护、切换数据库
member_repo = MemberRepository()      # 会员仓储
order_repo = OrderRepository()        # 订单仓储
analytics_repo = AnalyticsRepository() # 统计仓储


# ===== 【MCP-01-会员工具】 =====

# @mcp.tool()装饰器：将函数注册为MCP工具
# 面试考点：文档字符串的作用？
# 答：作为工具描述，帮助AI理解如何使用工具
@mcp.tool()
def member_get_profile(member_id: str) -> Dict[str, Any]:
    """根据会员ID查询会员资料，包括积分、等级、余额等信息。"""
    try:
        profile = member_repo.get_member(member_id)
    except Exception as exc:
        return {"status": "error", "message": f"查询会员信息失败: {exc}", "member_id": member_id}

    if not profile:
        return {"status": "not_found", "message": "会员不存在", "member_id": member_id}
    return {"status": "success", "data": profile}


@mcp.tool()
def member_update_points(member_id: str, delta_points: int) -> Dict[str, Any]:
    """为会员增减积分，直接更新数据库。"""
    try:
        updated = member_repo.update_points(member_id, delta_points)
    except Exception as exc:
        return {"status": "error", "message": f"更新积分失败: {exc}", "member_id": member_id}

    if not updated:
        return {"status": "not_found", "message": "会员不存在", "member_id": member_id}
    return {"status": "success", "data": updated}


@mcp.tool()
def member_search(keyword: str = "", limit: int = 10) -> Dict[str, Any]:
    """模糊搜索会员，关键字可以是会员编号、姓名或手机号。"""
    try:
        members = member_repo.search_members(keyword, limit)
    except Exception as exc:
        return {"status": "error", "message": f"查询会员列表失败: {exc}"}
    return {"status": "success", "data": members}


# ===== 订单工具 =====

@mcp.tool()
def order_get_detail(order_no: str) -> Dict[str, Any]:
    """根据订单号查询订单详情，包括商品明细、金额、支付方式等。"""
    try:
        bundle = order_repo.get_order_with_items(order_no)
    except Exception as exc:
        return {"status": "error", "message": f"查询订单失败: {exc}", "order_no": order_no}

    if not bundle:
        return {"status": "not_found", "message": "订单不存在", "order_no": order_no}

    summary = order_repo.summarize_order(bundle)
    return {"status": "success", "data": summary}


@mcp.tool()
def order_recent_list(limit: int = 10) -> Dict[str, Any]:
    """获取最近的订单列表。"""
    try:
        orders = order_repo.list_recent_orders(limit)
    except Exception as exc:
        return {"status": "error", "message": f"查询订单列表失败: {exc}"}
    return {"status": "success", "data": orders}


# ===== 经营分析工具 =====

@mcp.tool()
def assistant_sales_trend(days: int = 7) -> Dict[str, Any]:
    """获取销售趋势数据，用于生成折线图。"""
    try:
        data = analytics_repo.get_sales_trend(max(days, 1))
    except Exception as exc:
        return {"status": "error", "message": f"查询销售趋势失败: {exc}"}
    return {
        "status": "success",
        "chart": {"type": "line", "xField": "date", "series": [
            {"field": "paid_amount", "name": "实收金额"},
            {"field": "order_count", "name": "订单数"}
        ]},
        "data": data
    }


@mcp.tool()
def assistant_top_products(days: int = 30, limit: int = 5) -> Dict[str, Any]:
    """获取热销商品排行。"""
    try:
        data = analytics_repo.get_top_products(max(days, 1), max(limit, 1))
    except Exception as exc:
        return {"status": "error", "message": f"查询热销商品失败: {exc}"}
    return {
        "status": "success",
        "chart": {"type": "bar", "xField": "product_name", "series": [
            {"field": "quantity", "name": "销量"},
            {"field": "amount", "name": "销售额"}
        ]},
        "data": data
    }


@mcp.tool()
def assistant_member_segments() -> Dict[str, Any]:
    """获取会员等级分布。"""
    try:
        data = analytics_repo.get_member_segments()
    except Exception as exc:
        return {"status": "error", "message": f"查询会员分布失败: {exc}"}
    return {
        "status": "success",
        "chart": {"type": "pie", "angleField": "member_count", "colorField": "level"},
        "data": data
    }


# ===== TTS 工具 =====

_tts_client: Optional[AipSpeech] = None


def get_tts_client() -> AipSpeech:
    global _tts_client
    if _tts_client is None:
        app_id = os.getenv("BAIDU_TTS_APP_ID", "").strip()
        api_key = os.getenv("BAIDU_TTS_API_KEY", "").strip()
        secret_key = os.getenv("BAIDU_TTS_SECRET_KEY", "").strip()
        if not all([app_id, api_key, secret_key]):
            raise RuntimeError("请配置 BAIDU_TTS_APP_ID / BAIDU_TTS_API_KEY / BAIDU_TTS_SECRET_KEY")
        _tts_client = AipSpeech(app_id, api_key, secret_key)
    return _tts_client


@mcp.tool()
def tts_synthesize(text: str, spd: int = 5, pit: int = 5, vol: int = 5, per: int = 0) -> Dict[str, Any]:
    """将文本转换为语音（百度TTS）。

    参数:
    - text: 待合成文本
    - spd: 语速 0-9，默认5
    - pit: 音调 0-9，默认5
    - vol: 音量 0-15，默认5
    - per: 发音人，0=度小美，1=度小宇，3=度逍遥，4=度丫丫
    """
    try:
        client = get_tts_client()
        options = {"spd": spd, "pit": pit, "vol": vol, "per": per, "aue": 3}
        result = client.synthesis(text, "zh", 1, options)
        if isinstance(result, dict):
            return {"status": "error", "message": f"TTS失败: {result.get('err_msg')}"}
        audio_base64 = base64.b64encode(result).decode("utf-8")
        return {"status": "success", "audio_base64": audio_base64, "format": "mp3"}
    except Exception as exc:
        return {"status": "error", "message": str(exc)}


# ===== 健康检查 =====

@mcp.tool()
def health_check() -> str:
    """检查数据库连接状态。"""
    ping_database()
    return "SmartMarket MCP 服务正常，数据库连接正常"


if __name__ == "__main__":
    transport = sys.argv[1] if len(sys.argv) > 1 else "stdio"
    if transport == "sse":
        mcp.settings.host = "0.0.0.0"
        mcp.settings.port = 8300
        print("Unified MCP Service 监听端口 8300 (SSE)")
        mcp.run(transport="sse")
    else:
        mcp.run(transport="stdio")
