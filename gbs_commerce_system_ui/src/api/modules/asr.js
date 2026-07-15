import http from "../request";

export function recognizeSpeech(data) {
  return http('/asr/recognize', { method: 'POST', data });
}

export function getAsrToken() {
  return http('/asr/token', { method: 'POST' });
}