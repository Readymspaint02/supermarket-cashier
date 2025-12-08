import os
import base64
from typing import Dict, Any

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from aip import AipSpeech


# -------- 百度 TTS 客户端封装 --------
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

        options = {
            "spd": max(0, min(9, int(spd))),
            "pit": max(0, min(9, int(pit))),
            "vol": max(0, min(15, int(vol))),
            "per": int(per),
            "aue": int(aue),  # 3: mp3
        }

        result = self.client.synthesis(text, lang, 1, options)

        # 成功时返回 bytes，失败时返回 dict
        if isinstance(result, dict):
            err_no = result.get("err_no")
            err_msg = result.get("err_msg")
            raise RuntimeError(f"百度 TTS 调用失败: err_no={err_no}, err_msg={err_msg}")

        audio_base64 = base64.b64encode(result).decode("utf-8")

        return {
            "audio_base64": audio_base64,
            "format": "mp3",
            "spd": options["spd"],
            "pit": options["pit"],
            "vol": options["vol"],
            "per": options["per"],
        }


_tts_client: BaiduTTSClient | None = None


def get_tts_client() -> BaiduTTSClient:
    global _tts_client
    if _tts_client is None:
        _tts_client = BaiduTTSClient()
    return _tts_client


# -------- FastAPI HTTP 服务 --------
class TTSRequest(BaseModel):
    text: str
    spd: int = 5
    pit: int = 5
    vol: int = 5
    per: int = 0


app = FastAPI(title="Baidu TTS HTTP Test Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 调试用，正式可按需收紧
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/tts_synthesize")
async def tts_synthesize_http(req: TTSRequest) -> Dict[str, Any]:
    """HTTP 版 TTS 接口，供浏览器 Demo 使用。"""
    try:
        client = get_tts_client()
        result = client.synthesize(
            text=req.text,
            spd=req.spd,
            pit=req.pit,
            vol=req.vol,
            per=req.per,
        )
        return {"status": "success", **result}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@app.get("/health")
async def health() -> Dict[str, str]:
    try:
        _ = get_tts_client()
        return {"status": "ok"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


if __name__ == "__main__":
    import uvicorn

    host = "0.0.0.0"
    port = int(os.getenv("BAIDU_TTS_HTTP_PORT", "8101"))
    print(f"Baidu TTS HTTP 测试服务启动在 http://{host}:{port}")
    uvicorn.run(app, host=host, port=port)

