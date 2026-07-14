import os
import base64
from typing import Dict, Any
from pathlib import Path

from dotenv import load_dotenv
env_path = Path(__file__).parent / ".env.secrets"
if env_path.exists():
    load_dotenv(env_path)
else:
    load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from aip import AipSpeech

app = FastAPI(title="Baidu TTS HTTP Proxy")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

BAIDU_TTS_APP_ID = os.getenv("BAIDU_TTS_APP_ID", "").strip()
BAIDU_TTS_API_KEY = os.getenv("BAIDU_TTS_API_KEY", "").strip()
BAIDU_TTS_SECRET_KEY = os.getenv("BAIDU_TTS_SECRET_KEY", "").strip()

_tts_client = None

def get_tts_client():
    global _tts_client
    if _tts_client is None:
        if not all([BAIDU_TTS_APP_ID, BAIDU_TTS_API_KEY, BAIDU_TTS_SECRET_KEY]):
            raise RuntimeError("请配置 BAIDU_TTS_APP_ID / BAIDU_TTS_API_KEY / BAIDU_TTS_SECRET_KEY")
        _tts_client = AipSpeech(BAIDU_TTS_APP_ID, BAIDU_TTS_API_KEY, BAIDU_TTS_SECRET_KEY)
    return _tts_client


class TTSRequest(BaseModel):
    text: str
    spd: int = 5
    pit: int = 5
    vol: int = 5
    per: int = 0


@app.post("/tts_synthesize")
async def tts_synthesize(req: TTSRequest) -> Dict[str, Any]:
    try:
        client = get_tts_client()
        options = {
            "spd": max(0, min(9, req.spd)),
            "pit": max(0, min(9, req.pit)),
            "vol": max(0, min(15, req.vol)),
            "per": req.per,
            "aue": 3
        }
        result = client.synthesis(req.text, "zh", 1, options)
        
        if isinstance(result, dict):
            return {"status": "error", "message": f"TTS失败: {result.get('err_msg', '未知错误')}"}
        
        audio_base64 = base64.b64encode(result).decode("utf-8")
        return {
            "status": "success",
            "audio_base64": audio_base64,
            "format": "mp3",
            "spd": options["spd"],
            "pit": options["pit"],
            "vol": options["vol"],
            "per": options["per"]
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}


@app.get("/health")
async def health():
    return {"status": "ok", "service": "Baidu TTS HTTP Proxy"}


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("TTS_PROXY_PORT", "8101"))
    print(f"Baidu TTS HTTP Proxy 启动在 http://0.0.0.0:{port}")
    uvicorn.run(app, host="0.0.0.0", port=port)
