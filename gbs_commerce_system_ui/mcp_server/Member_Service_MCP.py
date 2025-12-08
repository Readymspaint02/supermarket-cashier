from __future__ import annotations

from typing import Any, Dict, Optional

from mcp.server.fastmcp import FastMCP

from db_utils import ping_database
from repositories import MemberRepository


def _format_error(message: str, member_id: Optional[str] = None) -> Dict[str, Any]:
    payload: Dict[str, Any] = {"status": "error", "message": message}
    if member_id is not None:
        payload["member_id"] = member_id
    return payload


mcp = FastMCP("Member-Service")
repository = MemberRepository()


@mcp.tool()
def member_get_profile(member_id: str) -> Dict[str, Any]:
    """根据会员 ID 查询数据库中的真实资料（实时查询）。"""
    try:
        profile = repository.get_member(member_id)
    except Exception as exc:  # noqa: BLE001
        return _format_error(f"查询会员信息失败: {exc}", member_id)

    if not profile:
        return {"status": "not_found", "message": "会员不存在", "member_id": member_id}
    return {"status": "success", "data": profile}


@mcp.tool()
def member_update_points(member_id: str, delta_points: int) -> Dict[str, Any]:
    """为会员增减积分，直接更新数据库。"""
    try:
        updated = repository.update_points(member_id, delta_points)
    except Exception as exc:  # noqa: BLE001
        return _format_error(f"更新积分失败: {exc}", member_id)

    if not updated:
        return {"status": "not_found", "message": "会员不存在", "member_id": member_id}
    return {"status": "success", "data": updated}


@mcp.tool()
def member_search(keyword: str = "", limit: int = 10) -> Dict[str, Any]:
    """模糊搜索会员，关键字可以是会员编号 / 姓名 / 手机号。"""
    try:
        members = repository.search_members(keyword, limit)
    except Exception as exc:  # noqa: BLE001
        return _format_error(f"查询会员列表失败: {exc}")
    return {"status": "success", "data": members}


@mcp.tool()
def member_health_check() -> str:
    """检测数据库连通性。"""
    ping_database()
    return "Member Service MCP 已连接数据库，状态正常。"


if __name__ == "__main__":
    mcp.settings.host = "0.0.0.0"
    mcp.settings.port = 8301
    print("Member Service MCP 监听端口 8301 (SSE)")
    mcp.run(transport="sse")
