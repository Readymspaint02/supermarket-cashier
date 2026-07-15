from __future__ import annotations

from typing import Any, Dict, Optional

from mcp.server.fastmcp import FastMCP

from db_utils import ping_database
from repositories import AnalyticsRepository, OrderRepository


def _format_error(message: str, order_no: Optional[str] = None) -> Dict[str, Any]:
    payload: Dict[str, Any] = {"status": "error", "message": message}
    if order_no is not None:
        payload["order_no"] = order_no
    return payload


mcp = FastMCP("Order-Service")
repository = OrderRepository()
analytics_repository = AnalyticsRepository()


@mcp.tool()
def order_get_detail(order_no: str) -> Dict[str, Any]:
    """根据订单号查询数据库中的订单主数据 + 明细。"""
    try:
        bundle = repository.get_order_with_items(order_no)
    except Exception as exc:  # noqa: BLE001
        return _format_error(f"查询订单失败: {exc}", order_no)

    if not bundle:
        return {"status": "not_found", "message": "订单不存在", "order_no": order_no}

    summary = repository.summarize_order(bundle)
    return {"status": "success", "data": summary}


@mcp.tool()
def order_recent_list(limit: int = 10) -> Dict[str, Any]:
    """获取最近的订单列表，方便大模型追问或二次汇总。"""
    try:
        orders = repository.list_recent_orders(limit)
    except Exception as exc:  # noqa: BLE001
        return _format_error(f"查询订单列表失败: {exc}")
    return {"status": "success", "data": orders}


@mcp.tool()
def assistant_generate_insight(
    metric: str = "sales_trend",
    days: int = 7,
    limit: int = 5,
) -> Dict[str, Any]:
    """为助手图谱生成所需的数据，支持销售趋势/热销商品/会员等级画像等。"""

    try:
        metric_key = metric.lower()
        if metric_key == "sales_trend":
            data = analytics_repository.get_sales_trend(max(days, 1))
            chart = {
                "type": "line",
                "xField": "date",
                "series": [
                    {"field": "paid_amount", "name": "实收金额"},
                    {"field": "order_count", "name": "订单数"},
                ],
            }
        elif metric_key == "top_products":
            data = analytics_repository.get_top_products(max(days, 1), max(limit, 1))
            chart = {
                "type": "bar",
                "xField": "product_name",
                "series": [
                    {"field": "quantity", "name": "销量（件）"},
                    {"field": "amount", "name": "销售额"},
                ],
            }
        elif metric_key == "member_segments":
            data = analytics_repository.get_member_segments()
            chart = {
                "type": "pie",
                "angleField": "member_count",
                "colorField": "level",
            }
        else:
            return {
                "status": "error",
                "message": f"未知的 metric: {metric}",
                "metric": metric,
            }
    except Exception as exc:  # noqa: BLE001
        return {
            "status": "error",
            "message": f"生成图谱数据失败: {exc}",
            "metric": metric,
        }

    return {"status": "success", "metric": metric_key, "chart": chart, "data": data}


@mcp.tool()
def order_health_check() -> str:
    """检测订单库连通性。"""
    ping_database()
    return "Order Service MCP 已连接数据库。"


if __name__ == "__main__":
    import sys
    transport = sys.argv[1] if len(sys.argv) > 1 else "stdio"
    if transport == "sse":
        mcp.settings.host = "0.0.0.0"
        mcp.settings.port = 8302
        print("Order Service MCP 监听端口 8302 (SSE)")
        mcp.run(transport="sse")
    else:
        mcp.run(transport="stdio")
