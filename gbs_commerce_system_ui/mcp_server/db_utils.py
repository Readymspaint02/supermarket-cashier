import os
import re
from contextlib import contextmanager
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path
from typing import Any, Dict, Generator

import pymysql
from pymysql.cursors import DictCursor


IDENTIFIER_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
_DOTENV_LOADED = False


def _load_env_from_file() -> None:
    """Load basic KEY=VALUE pairs from a .env file so the MCP 服务可以复用 SpringBoot 的账号."""

    global _DOTENV_LOADED
    if _DOTENV_LOADED:
        return

    candidates = []
    explicit = os.getenv("MCP_ENV_FILE")
    if explicit:
        candidates.append(Path(explicit))

    module_dir = Path(__file__).resolve().parent
    candidates.append(module_dir / ".env")
    candidates.append(module_dir.parent / ".env")

    for path in candidates:
        if not path.exists() or not path.is_file():
            continue
        for raw_line in path.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = value
        _DOTENV_LOADED = True
        break


_load_env_from_file()


def clean_identifier(value: str, fallback: str) -> str:
    if not value:
        return fallback
    value = value.strip()
    if IDENTIFIER_RE.match(value):
        return value
    return fallback


def get_db_config() -> Dict[str, Any]:
    return {
        "host": os.getenv("MYSQL_HOST", "127.0.0.1"),
        "port": int(os.getenv("MYSQL_PORT", "3306")),
        "user": os.getenv("MYSQL_USER", "root"),
        "password": os.getenv("MYSQL_PASSWORD", "123456"),
        "database": os.getenv("MYSQL_DATABASE", "gbs_commerce_system_02"),
        "charset": os.getenv("MYSQL_CHARSET", "utf8mb4"),
        "autocommit": True,
        "cursorclass": DictCursor,
    }


@contextmanager
def get_connection() -> Generator[pymysql.connections.Connection, None, None]:
    conn = pymysql.connect(**get_db_config())
    try:
        yield conn
    finally:
        conn.close()


def normalize_record(record: Dict[str, Any]) -> Dict[str, Any]:
    normalized: Dict[str, Any] = {}
    for key, value in record.items():
        normalized[key] = normalize_value(value)
    return normalized


def normalize_value(value: Any) -> Any:
    if isinstance(value, Decimal):
        return float(value)
    if isinstance(value, (datetime, date)):
        # 统一返回 ISO 字符串，保留时间
        return value.isoformat(sep=" ")
    return value


def ping_database() -> bool:
    with get_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT 1")
            cursor.fetchone()
    return True


__all__ = [
    "get_connection",
    "normalize_record",
    "normalize_value",
    "ping_database",
    "clean_identifier",
]
