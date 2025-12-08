import axios from 'axios';

const isClient = typeof window !== 'undefined';

const getRuntimeInjection = (key) => {
  if (!isClient || !key) {
    return undefined;
  }
  const value = window[key];
  return typeof value === 'string' ? value : undefined;
};

const normalizeToHttps = (value) => {
  if (typeof value !== 'string') {
    return value;
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return '';
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed.replace(/^http:\/\//i, 'https://');
  }
  if (/^\/\//.test(trimmed)) {
    return `https:${trimmed}`;
  }
  return trimmed;
};

const resolveBaseUrl = (envValue, runtimeKey, fallbackValue) => {
  const runtimeValue = getRuntimeInjection(runtimeKey);
  const candidate =
    (typeof envValue === 'string' && envValue.trim()) ||
    runtimeValue ||
    fallbackValue;

  const normalized = normalizeToHttps(candidate);
  return normalized || fallbackValue;
};

const defaultAgentBase = resolveBaseUrl(
  import.meta.env.VITE_AGENT_PROXY_URL,
  '__AGENT_PROXY_URL__',
  'https://42.194.158.40:9000'
);

const defaultTtsBase = resolveBaseUrl(
  import.meta.env.VITE_TTS_HTTP_URL,
  '__TTS_HTTP_URL__',
  'https://42.194.158.40:8101'
);

const agentHttp = axios.create({
  baseURL: defaultAgentBase,
  timeout: 20000,
});

const ttsHttp = axios.create({
  baseURL: defaultTtsBase,
  timeout: 20000,
});

export const sendAgentQuery = (message) => {
  return agentHttp.post('/agent_query', { query: message });
};

export const synthesizeSpeech = (payload) => {
  return ttsHttp.post('/tts_synthesize', payload);
};

export const checkAgentHealth = () => {
  return agentHttp.get('/health');
};
