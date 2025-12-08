import axios from 'axios';

const isClient = typeof window !== 'undefined';

const getRuntimeInjection = (key) => {
  if (!isClient || !key) {
    return undefined;
  }
  const value = window[key];
  return typeof value === 'string' ? value : undefined;
};

const getPageOrigin = () => {
  if (!isClient || !window.location?.origin) {
    return '';
  }
  return window.location.origin;
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

const resolveBaseUrl = ({ envValue, runtimeKey, fallbackValue }) => {
  const runtimeValue = getRuntimeInjection(runtimeKey);
  const origin = getPageOrigin();
  const candidate =
    (typeof envValue === 'string' && envValue.trim()) ||
    runtimeValue ||
    origin ||
    fallbackValue;

  const normalized = normalizeToHttps(candidate);
  return normalized || fallbackValue;
};

const defaultAssistantBase = resolveBaseUrl({
  envValue: import.meta.env.VITE_ASSISTANT_SERVICE_URL,
  runtimeKey: '__ASSISTANT_SERVICE_URL__',
  fallbackValue: 'https://42.194.158.40:8202',
});

const defaultMemberServiceBase = resolveBaseUrl({
  envValue: import.meta.env.VITE_MEMBER_SERVICE_URL,
  runtimeKey: '__MEMBER_SERVICE_URL__',
  fallbackValue: 'https://42.194.158.40:8201',
});

const assistantHttp = axios.create({
  baseURL: defaultAssistantBase,
  timeout: 15000,
});

const memberHttp = axios.create({
  baseURL: defaultMemberServiceBase,
  timeout: 15000,
});

export const fetchAssistantInsight = (metric, params = {}) => {
  return assistantHttp.get('/assistant/insight', {
    params: {
      metric,
      ...params,
    },
  });
};

export const fetchRecentOrders = (limit = 10) => {
  return assistantHttp.get('/order/recent', { params: { limit } });
};

export const fetchMemberProfile = (memberId) => {
  return memberHttp.post('/member/get_profile', { member_id: memberId });
};
