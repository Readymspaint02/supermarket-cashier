from __future__ import annotations

import os
from typing import Any, Dict, List, Optional

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from repositories import AnalyticsRepository, OrderRepository


class OrderQuery(BaseModel):
    order_no: Optional[str] = None
    order_id: Optional[str] = None

    def value(self) -> str:
        if self.order_no:
            return self.order_no
        if self.order_id:
            return self.order_id
        raise ValueError("order_no/order_id 至少填写一个")


repository = OrderRepository()
analytics_repository = AnalyticsRepository()
app = FastAPI(title="Order Service HTTP")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/order/get_detail")
async def get_order_detail(req: OrderQuery) -> Dict[str, Any]:
    try:
        order_no = req.value()
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    try:
        bundle = repository.get_order_with_items(order_no)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"查询失败: {exc}") from exc

    if not bundle:
        return {"status": "not_found", "message": "订单不存在", "order_no": order_no}

    summary = repository.summarize_order(bundle)
    return {"status": "success", "data": summary}


@app.get("/order/recent")
async def recent_orders(limit: int = Query(10, ge=1, le=100)) -> Dict[str, Any]:
    try:
        orders: List[Dict[str, Any]] = repository.list_recent_orders(limit)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"查询失败: {exc}") from exc
    return {"status": "success", "data": orders}


@app.get("/assistant/insight")
async def assistant_insight(
    metric: str = Query("sales_trend"),
    days: int = Query(7, ge=1, le=90),
    limit: int = Query(5, ge=1, le=20),
) -> Dict[str, Any]:
    metric_key = metric.lower()
    try:
        if metric_key == "sales_trend":
            data = analytics_repository.get_sales_trend(days)
            chart = {
                "type": "line",
                "xField": "date",
                "series": [
                    {"field": "paid_amount", "name": "实收金额"},
                    {"field": "order_count", "name": "订单数"},
                ],
            }
        elif metric_key == "top_products":
            data = analytics_repository.get_top_products(days, limit)
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
            raise HTTPException(status_code=400, detail=f"未知的 metric: {metric}")
    except HTTPException:
        raise
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"生成图谱数据失败: {exc}") from exc

    return {"status": "success", "metric": metric_key, "chart": chart, "data": data}


@app.get("/health")
async def health() -> Dict[str, str]:
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    host = "0.0.0.0"
    port = int(os.getenv("ORDER_HTTP_PORT", "8202"))
    print(f"Order Service HTTP 服务启动 -> http://{host}:{port}")
    uvicorn.run(app, host=host, port=port)
