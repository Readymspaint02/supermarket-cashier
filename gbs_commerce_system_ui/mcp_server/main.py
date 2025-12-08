from mcp.server.fastmcp import FastMCP
import os
import base64
import asyncio
import websockets  # 继续用于浏览器 <-> 本地 WebSocket 服务器
import json
import requests
import aiohttp  # 新增，用于连接华为云 WebSocket
from huaweicloudsdksis.v1.region.sis_region import SisRegion
from huaweicloudsdksis.v1 import *
from huaweicloudsdkcore.auth.credentials import BasicCredentials

# 创建MCP服务器
mcp = FastMCP("Huawei-SIS-Streaming-Recognition")


class StreamingSpeechRecognizer:
    def __init__(self):
        # 区域和项目配置
        self.region = os.getenv("HUAWEI_REGION", "cn-north-4")
        # project_id 建议通过环境变量传入
        self.project_id = os.getenv("HUAWEI_PROJECT_ID", "")
        # SIS WebSocket endpoint，例如 sis-ext.cn-north-4.myhuaweicloud.com
        self.sis_endpoint = os.getenv(
            "HUAWEI_SIS_ENDPOINT",
            f"sis-ext.{self.region}.myhuaweicloud.com",
        )

        # IAM 用户名/密码/域名从环境变量读取，避免写死在代码里
        self.username = os.getenv("HUAWEI_USERNAME", "")
        self.password = os.getenv("HUAWEI_PASSWORD", "")
        self.domain_name = os.getenv("HUAWEI_DOMAIN_NAME", "")

    def get_iam_token(self) -> str:
        """通过用户名密码向 IAM 获取 X-Auth-Token。"""
        if not all([self.username, self.password, self.domain_name, self.project_id]):
            raise RuntimeError(
                "IAM 配置信息不完整，请设置环境变量: "
                "HUAWEI_USERNAME, HUAWEI_PASSWORD, HUAWEI_DOMAIN_NAME, HUAWEI_PROJECT_ID"
            )

        url = f"https://iam.{self.region}.myhuaweicloud.com/v3/auth/tokens"
        payload = {
            "auth": {
                "identity": {
                    "methods": ["password"],
                    "password": {
                        "user": {
                            "name": self.username,
                            "password": self.password,
                            "domain": {"name": self.domain_name},
                        }
                    },
                },
                "scope": {"project": {"name": self.region}},
            }
        }

        headers = {"Content-Type": "application/json"}
        resp = requests.post(url, headers=headers, data=json.dumps(payload), timeout=10)
        if resp.status_code not in (200, 201):
            raise RuntimeError(
                f"获取 IAM Token 失败: status={resp.status_code}, body={resp.text}"
            )

        token = resp.headers.get("X-Subject-Token")
        if not token:
            raise RuntimeError("IAM 响应中缺少 X-Subject-Token")
        return token

    async def process_audio_stream(self, websocket):
        """作为 WebSocket 代理: 浏览器 <-> 本地 <-> 华为云 SIS WebSocket"""
        print("客户端连接建立")

        # 1. 获取 IAM Token
        try:
            token = self.get_iam_token()
            print("IAM Token 获取成功")
        except Exception as e:
            msg = f"获取 IAM Token 失败: {e}"
            print(msg)
            await websocket.send(json.dumps({"type": "error", "message": msg}))
            return

        # 2. 连接华为云 WebSocket 短音频接口
        if not self.project_id:
            msg = "HUAWEI_PROJECT_ID 未配置，无法连接华为云 WebSocket 接口"
            print(msg)
            await websocket.send(json.dumps({"type": "error", "message": msg}))
            return

        # 标准 WebSocket URL，使用 aiohttp 作为客户端，Header 携带 Token
        huawei_ws_url = (
            f"wss://{self.sis_endpoint}/v1/{self.project_id}/asr/short-audio"
        )

        try:
            # 使用 aiohttp 连接华为云 WebSocket，避免 websockets.extra_headers 的兼容性问题
            async with aiohttp.ClientSession(
                headers={"X-Auth-Token": token}
            ) as session:
                async with session.ws_connect(huawei_ws_url) as huawei_ws:
                    print(f"已连接华为云 SIS WebSocket: {huawei_ws_url}")

                    # 2.1 发送 START 指令（文本消息）
                    start_msg = {
                        "command": "START",
                        "config": {
                            "audio_format": "pcm16k16bit",
                            "property": "chinese_16k_general",
                            "add_punc": "yes",
                            "interim_results": "yes",
                        },
                    }
                    await huawei_ws.send_str(json.dumps(start_msg))
                    print("已向华为云发送 START 指令")

                    async def forward_client_to_huawei():
                        """浏览器 -> 本地 -> 华为云: 转发音频数据和 END 指令"""
                        try:
                            async for message in websocket:
                                print(
                                    f"来自浏览器的消息，类型: {type(message)}, 长度: "
                                    f"{len(message) if isinstance(message, (bytes, bytearray)) else 'N/A'}"
                                )
                                if isinstance(message, bytes):
                                    # PCM 二进制直接转发给华为云（binary message）
                                    await huawei_ws.send_bytes(message)
                                else:
                                    # 文本消息，可能是 {"command": "END"}
                                    try:
                                        data = json.loads(message)
                                        if (
                                            isinstance(data, dict)
                                            and data.get("command") == "END"
                                        ):
                                            await huawei_ws.send_str(
                                                json.dumps({"command": "END"})
                                            )
                                            print("已向华为云发送 END 指令")
                                    except Exception:
                                        # 非 JSON 文本，忽略或根据需要处理
                                        pass
                        except websockets.exceptions.ConnectionClosed:
                            print("浏览器 WebSocket 连接关闭（发送方向）")

                    async def forward_huawei_to_client():
                        """华为云 -> 本地 -> 浏览器: 转发布局结果/错误/结束"""
                        try:
                            async for msg in huawei_ws:
                                if msg.type == aiohttp.WSMsgType.TEXT:
                                    raw = msg.data
                                    print(f"来自华为云的消息: {raw}")
                                    try:
                                        data = json.loads(raw)
                                    except Exception:
                                        continue

                                    resp_type = data.get("resp_type")
                                    if resp_type == "RESULT":
                                        segments = data.get("segments", [])
                                        for seg in segments:
                                            result = seg.get("result") or {}
                                            text = result.get("text", "")
                                            is_final = seg.get("is_final", False)
                                            await websocket.send(
                                                json.dumps(
                                                    {
                                                        "type": "recognition_result",
                                                        "text": text,
                                                        "is_final": is_final,
                                                    }
                                                )
                                            )
                                    elif resp_type == "ERROR":
                                        err_msg = data.get("error_msg", "未知错误")
                                        await websocket.send(
                                            json.dumps(
                                                {
                                                    "type": "error",
                                                    "message": f"华为云识别错误: {err_msg}",
                                                }
                                            )
                                        )
                                    elif resp_type == "END":
                                        reason = data.get("reason", "")
                                        await websocket.send(
                                            json.dumps(
                                                {
                                                    "type": "end",
                                                    "message": f"识别结束: {reason}",
                                                }
                                            )
                                        )
                                elif msg.type == aiohttp.WSMsgType.ERROR:
                                    print(
                                        f"华为云 WebSocket 错误: {huawei_ws.exception()}"
                                    )
                                    break
                        except websockets.exceptions.ConnectionClosed:
                            print("与华为云 WebSocket 的连接关闭（接收方向）")

                    # 并行执行两个方向的转发
                    await asyncio.gather(
                        forward_client_to_huawei(), forward_huawei_to_client()
                    )

        except Exception as e:
            msg = f"连接或转发华为云 WebSocket 时发生错误: {e}"
            print(msg)
            try:
                await websocket.send(json.dumps({"type": "error", "message": msg}))
            except Exception:
                pass

# 创建识别器实例
recognizer = StreamingSpeechRecognizer()

# WebSocket服务器
async def websocket_server():
    server = await websockets.serve(
        recognizer.process_audio_stream, 
        "0.0.0.0",  # 允许所有IP连接
        8765         # WebSocket端口
    )
    print("WebSocket语音识别服务器启动在端口 8765")
    await server.wait_closed()

# 保留原有的MCP工具（兼容性）
@mcp.tool()
def streaming_speech_recognition_status() -> str:
    """检查流式语音识别服务状态"""
    return "流式语音识别服务运行中，WebSocket端口: 8765"

if __name__ == "__main__":
    import threading
    
    # 在单独线程中启动WebSocket服务器
    def start_websocket_server():
        asyncio.new_event_loop().run_until_complete(websocket_server())
    
    ws_thread = threading.Thread(target=start_websocket_server, daemon=True)
    ws_thread.start()
    
    # 同时启动MCP服务器（原有功能）
    mcp.settings.host = "0.0.0.0"
    mcp.settings.port = 8000
    print("MCP服务器启动在端口 8000")
    mcp.run(transport="sse")

