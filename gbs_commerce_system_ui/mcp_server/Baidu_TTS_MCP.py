from mcp.server.fastmcp import FastMCP
from typing import Optional, Dict, Any
import os
import base64
from aip import AipSpeech


# 创建 MCP 服务器实例
mcp = FastMCP("Baidu-TTS-Service")


class BaiduTTSClient:
    """简单封装百度语音合成客户端，读取环境变量中的配置。"""

    def __init__(self) -> None:
        app_id = os.getenv("BAIDU_TTS_APP_ID", "").strip()
        api_key = os.getenv("BAIDU_TTS_API_KEY", "").strip()
        secret_key = os.getenv("BAIDU_TTS_SECRET_KEY", "").strip()

        if not app_id or not api_key or not secret_key:
            raise RuntimeError(
                "BAIDU_TTS_APP_ID / BAIDU_TTS_API_KEY / BAIDU_TTS_SECRET_KEY 未配置，请设置环境变量后再启动服务"
            )

        self.client = AipSpeech(app_id, api_key, secret_key)

    def synthesize(
        self,
        text: str,
        spd: int = 5,
        pit: int = 5,
        vol: int = 5,
        per: int = 0,
        lang: str = "zh",
        aue: int = 3,
    ) -> Dict[str, Any]:
        """调用百度 TTS 进行语音合成，返回 base64 编码的音频。"""
        if not text:
            raise ValueError("text 不能为空")

        # 调用百度 SDK 合成语音
        options = {
            "spd": max(0, min(9, int(spd))),
            "pit": max(0, min(9, int(pit))),
            "vol": max(0, min(15, int(vol))),
            "per": int(per),
            "aue": int(aue),  # 3: mp3
        }

        result = self.client.synthesis(text, lang, 1, options)  # 1: 普通话

        # 成功时返回 bytes，失败时返回 dict
        if isinstance(result, dict):
            err_no = result.get("err_no")
            err_msg = result.get("err_msg")
            raise RuntimeError(f"百度 TTS 调用失败: err_no={err_no}, err_msg={err_msg}")

        # 将音频二进制转为 base64，方便通过 JSON 传输
        audio_base64 = base64.b64encode(result).decode("utf-8")

        return {
            "audio_base64": audio_base64,
            "format": "mp3",
            "spd": options["spd"],
            "pit": options["pit"],
            "vol": options["vol"],
            "per": options["per"],
        }


# 创建全局 TTS 客户端实例
_tts_client: Optional[BaiduTTSClient] = None


def get_tts_client() -> BaiduTTSClient:
    global _tts_client
    if _tts_client is None:
        _tts_client = BaiduTTSClient()
    return _tts_client


@mcp.tool()
def tts_synthesize(
    text: str,
    spd: int = 5,
    pit: int = 5,
    vol: int = 5,
    per: int = 0,
    lang: str = "zh",
) -> Dict[str, Any]:
    """使用百度语音合成将文本转换为语音。

    参数:
    - text: 待合成文本，建议不超过 60 个汉字或字母数字。
    - spd: 语速，0-9，默认 5。
    - pit: 音调，0-9，默认 5。
    - vol: 音量，0-15，默认 5。
    - per: 发音人选择，0=度小美(默认)，1=度小宇，3=度逍遥，4=度丫丫，及其他精品音色 ID。
    - lang: 语言，默认 "zh"。

    返回:
    - 包含 base64 编码音频和格式信息的字典，可直接在前端解码播放。
    """
    client = get_tts_client()
    return client.synthesize(text=text, spd=spd, pit=pit, vol=vol, per=per, lang=lang)


@mcp.tool()
def tts_health_check() -> str:
    """检查 TTS 服务是否可用。"""
    # 这里只做最简单的检查：确认客户端初始化成功
    _ = get_tts_client()
    return "Baidu TTS MCP 服务已就绪"


if __name__ == "__main__":
    # 启动 MCP 服务器，默认 SSE 方式，端口可按需调整
    mcp.settings.host = "0.0.0.0"
    mcp.settings.port = 8001
    print("Baidu TTS MCP 服务器启动在端口 8001")
    mcp.run(transport="sse")

