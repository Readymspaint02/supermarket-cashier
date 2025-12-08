from __future__ import annotations

import os
from typing import Any, Dict, List, Optional

from db_utils import clean_identifier, get_connection, normalize_record, normalize_value


class MemberRepository:
    """封装会员相关的数据库访问逻辑，供 MCP 与 HTTP 服务复用。"""

    def __init__(self) -> None:
        self.table = clean_identifier(os.getenv("MEMBER_TABLE", "members"), "members")
        self.key_column = clean_identifier(
            os.getenv("MEMBER_KEY_COLUMN", "member_id"), "member_id"
        )
        self.points_column = clean_identifier(
            os.getenv("MEMBER_POINTS_COLUMN", "points"), "points"
        )
        self.name_column = clean_identifier(
            os.getenv("MEMBER_NAME_COLUMN", "name"), "name"
        )
        self.phone_column = clean_identifier(
            os.getenv("MEMBER_PHONE_COLUMN", "phone"), "phone"
        )

    def get_member(self, member_id: str) -> Optional[Dict[str, Any]]:
        query = f"SELECT * FROM `{self.table}` WHERE `{self.key_column}`=%s LIMIT 1"
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(query, (member_id,))
            row = cursor.fetchone()
        if not row:
            return None
        return normalize_record(row)

    def search_members(self, keyword: Optional[str], limit: int = 20) -> List[Dict[str, Any]]:
        base_sql = f"SELECT * FROM `{self.table}`"
        params: List[Any] = []
        if keyword:
            like_value = f"%{keyword.strip()}%"
            base_sql += (
                f" WHERE `{self.key_column}` LIKE %s"
                f" OR `{self.name_column}` LIKE %s"
                f" OR `{self.phone_column}` LIKE %s"
            )
            params.extend([like_value, like_value, like_value])
        base_sql += f" ORDER BY `update_time` DESC LIMIT %s"
        params.append(limit)
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(base_sql, params)
            rows = cursor.fetchall() or []
        return [normalize_record(row) for row in rows]

    def update_points(self, member_id: str, delta_points: int) -> Optional[Dict[str, Any]]:
        update_sql = (
            f"UPDATE `{self.table}` "
            f"SET `{self.points_column}` = `{self.points_column}` + %s "
            f"WHERE `{self.key_column}` = %s"
        )
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(update_sql, (delta_points, member_id))
            affected = cursor.rowcount
        if affected == 0:
            return None
        return self.get_member(member_id)


class OrderRepository:
    """封装订单与订单明细的查询逻辑。"""

    def __init__(self) -> None:
        self.order_table = clean_identifier(os.getenv("ORDER_TABLE", "orders"), "orders")
        self.order_pk_column = clean_identifier(os.getenv("ORDER_PK_COLUMN", "id"), "id")
        self.order_no_column = clean_identifier(
            os.getenv("ORDER_NO_COLUMN", "order_no"), "order_no"
        )
        self.order_item_table = clean_identifier(
            os.getenv("ORDER_ITEM_TABLE", "order_item"), "order_item"
        )
        self.order_item_fk_column = clean_identifier(
            os.getenv("ORDER_ITEM_FK_COLUMN", "order_id"), "order_id"
        )

    def get_order(self, order_no: str) -> Optional[Dict[str, Any]]:
        query = (
            f"SELECT * FROM `{self.order_table}` WHERE `{self.order_no_column}`=%s LIMIT 1"
        )
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(query, (order_no,))
            row = cursor.fetchone()
        if not row:
            return None
        return normalize_record(row)

    def get_order_items(self, order_pk: Any) -> List[Dict[str, Any]]:
        query = (
            f"SELECT * FROM `{self.order_item_table}` "
            f"WHERE `{self.order_item_fk_column}`=%s ORDER BY id"
        )
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(query, (order_pk,))
            rows = cursor.fetchall() or []
        return [normalize_record(row) for row in rows]

    def get_order_with_items(self, order_no: str) -> Optional[Dict[str, Any]]:
        order = self.get_order(order_no)
        if not order:
            return None
        pk_value = order.get(self.order_pk_column)
        items = self.get_order_items(pk_value)
        return {"order": order, "items": items}

    def list_recent_orders(self, limit: int = 10) -> List[Dict[str, Any]]:
        sql = (
            f"SELECT * FROM `{self.order_table}` ORDER BY `create_time` DESC LIMIT %s"
        )
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(sql, (limit,))
            rows = cursor.fetchall() or []
        return [normalize_record(row) for row in rows]

    def summarize_order(self, order_bundle: Dict[str, Any]) -> Dict[str, Any]:
        order = order_bundle["order"]
        items = order_bundle["items"]

        subtotal = 0.0
        total_qty = 0
        normalized_items: List[Dict[str, Any]] = []

        for item in items:
            normalized_items.append(item)
            item_subtotal = item.get("subtotal")
            if item_subtotal is None:
                price = normalize_value(item.get("price") or 0)
                qty = normalize_value(item.get("quantity") or 0)
                item_subtotal = float(price) * float(qty)
            subtotal += float(item_subtotal)
            total_qty += int(item.get("quantity") or 0)

        discount = float(order.get("discount_amount") or 0.0)
        paid = float(order.get("paid_amount") or 0.0)
        total_amount = float(order.get("total_amount") or subtotal)

        payment_method = int(order.get("payment_method") or 0)
        status = int(order.get("order_status") or 0)

        payment_map = {
            1: "现金",
            2: "微信",
            3: "支付宝",
            4: "银行卡",
        }
        status_map = {
            1: "已完成",
            2: "已退款",
            3: "部分退款",
        }

        order_pk = order.get(self.order_pk_column)
        payment_label = payment_map.get(payment_method, f"支付方式{payment_method}")
        status_label = status_map.get(status, f"状态{status}")

        summary = {
            "order_id": order_pk,
            "order_no": order.get("order_no"),
            "member_id": order.get("member_id"),
            "subtotal": round(subtotal, 2),
            "discount": round(discount, 2),
            "paid_amount": round(paid, 2),
            "total_amount": round(total_amount, 2),
            "item_count": total_qty,
            "payment_method": payment_label,
            "order_status": status_label,
            "cashier": order.get("cashier_name"),
            "created_at": order.get("create_time"),
            "remark": order.get("remark"),
            "items": normalized_items,
        }
        summary["raw_order"] = order
        return summary


class AnalyticsRepository:
    """封装助手图谱所需的统计查询。"""

    def __init__(self) -> None:
        self.orders_table = clean_identifier(os.getenv("ORDER_TABLE", "orders"), "orders")
        self.order_item_table = clean_identifier(
            os.getenv("ORDER_ITEM_TABLE", "order_item"), "order_item"
        )
        self.order_pk_column = clean_identifier(os.getenv("ORDER_PK_COLUMN", "id"), "id")
        self.order_item_fk_column = clean_identifier(
            os.getenv("ORDER_ITEM_FK_COLUMN", "order_id"), "order_id"
        )
        self.members_table = clean_identifier(os.getenv("MEMBER_TABLE", "members"), "members")

    def get_sales_trend(self, days: int = 7) -> List[Dict[str, Any]]:
        query = (
            "SELECT DATE(create_time) AS order_date, "
            "SUM(paid_amount) AS paid_amount, "
            "SUM(total_amount) AS total_amount, "
            "COUNT(*) AS order_count "
            f"FROM `{self.orders_table}` "
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL %s DAY) "
            "GROUP BY order_date ORDER BY order_date"
        )
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(query, (days,))
            rows = cursor.fetchall() or []
        return [
            {
                "date": normalize_value(row.get("order_date")),
                "paid_amount": round(float(row.get("paid_amount") or 0), 2),
                "total_amount": round(float(row.get("total_amount") or 0), 2),
                "order_count": int(row.get("order_count") or 0),
            }
            for row in rows
        ]

    def get_top_products(self, days: int = 30, limit: int = 5) -> List[Dict[str, Any]]:
        query = (
            "SELECT oi.product_id, oi.product_name, "
            "SUM(oi.quantity) AS total_qty, SUM(oi.subtotal) AS total_amount "
            f"FROM `{self.order_item_table}` oi "
            f"JOIN `{self.orders_table}` o ON oi.`{self.order_item_fk_column}` = o.`{self.order_pk_column}` "
            "WHERE o.create_time >= DATE_SUB(CURDATE(), INTERVAL %s DAY) "
            "GROUP BY oi.product_id, oi.product_name "
            "ORDER BY total_qty DESC LIMIT %s"
        )
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(query, (days, limit))
            rows = cursor.fetchall() or []
        return [
            {
                "product_id": row.get("product_id"),
                "product_name": row.get("product_name"),
                "quantity": int(row.get("total_qty") or 0),
                "amount": round(float(row.get("total_amount") or 0), 2),
            }
            for row in rows
        ]

    def get_member_segments(self) -> List[Dict[str, Any]]:
        query = (
            "SELECT `level`, COUNT(*) AS member_count, "
            "COALESCE(AVG(points), 0) AS avg_points, "
            "COALESCE(SUM(points), 0) AS total_points, "
            "COALESCE(AVG(balance), 0) AS avg_balance "
            f"FROM `{self.members_table}` "
            "GROUP BY `level` ORDER BY member_count DESC"
        )
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute(query)
            rows = cursor.fetchall() or []
        return [
            {
                "level": row.get("level") or "未分级",
                "member_count": int(row.get("member_count") or 0),
                "avg_points": round(float(row.get("avg_points") or 0), 2),
                "total_points": int(row.get("total_points") or 0),
                "avg_balance": round(float(row.get("avg_balance") or 0), 2),
            }
            for row in rows
        ]
