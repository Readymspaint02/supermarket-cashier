from __future__ import annotations

import os
from typing import Any, Dict, List

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from repositories import MemberRepository


class MemberQuery(BaseModel):
    member_id: str


class MemberUpdatePoints(BaseModel):
    member_id: str
    delta_points: int


repository = MemberRepository()
app = FastAPI(title="Member Service HTTP")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/member/get_profile")
async def get_member_profile(req: MemberQuery) -> Dict[str, Any]:
    try:
        profile = repository.get_member(req.member_id)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"查询失败: {exc}") from exc
    if not profile:
        return {"status": "not_found", "message": "会员不存在", "member_id": req.member_id}
    return {"status": "success", "data": profile}


@app.post("/member/update_points")
async def update_member_points(req: MemberUpdatePoints) -> Dict[str, Any]:
    try:
        updated = repository.update_points(req.member_id, req.delta_points)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"更新失败: {exc}") from exc
    if not updated:
        return {"status": "not_found", "message": "会员不存在", "member_id": req.member_id}
    return {"status": "success", "data": updated}


@app.get("/member/search")
async def member_search(
    keyword: str = Query("", description="会员编号 / 姓名 / 手机号"),
    limit: int = Query(10, ge=1, le=100, description="返回条数"),
) -> Dict[str, Any]:
    try:
        members: List[Dict[str, Any]] = repository.search_members(keyword, limit)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"查询失败: {exc}") from exc
    return {"status": "success", "data": members}


@app.get("/health")
async def health() -> Dict[str, str]:
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    host = "0.0.0.0"
    port = int(os.getenv("MEMBER_HTTP_PORT", "8201"))
    print(f"Member Service HTTP 服务启动 -> http://{host}:{port}")
    uvicorn.run(app, host=host, port=port)
