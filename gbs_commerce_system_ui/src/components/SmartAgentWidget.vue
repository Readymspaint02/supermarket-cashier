<script setup>
import { ref, reactive, computed, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import {
  sendAgentQuery,
  synthesizeSpeech,
  checkAgentHealth,
} from '@/api/modules/agent';
import { recognizeSpeech } from '@/api/modules/asr';

const isClient = typeof window !== 'undefined';

const supportsSpeechSynthesis = isClient && 'speechSynthesis' in window;

const wait = (ms = 1000) =>
  new Promise((resolve) => {
    setTimeout(resolve, ms);
  });

const isOpen = ref(false);
const inputText = ref('');
const sending = ref(false);
const recording = ref(false);
const recognitionStatus = ref('');
const voiceDraft = ref('');
const isSpeaking = ref(false);
const networkRecoveryInProgress = ref(false);
const networkRecoveryMessage = ref('');
const networkRecoveryFailed = ref(false);
const networkRecoveryAttempt = ref(0);
const maxNetworkRecoveryAttempts = 3;

const messages = ref([
  {
    id: 'hello',
    role: 'assistant',
    text: '您好，我是智慧助手，可以帮您查询会员、订单等信息，也支持语音对话。',
    time: new Date().toLocaleTimeString(),
  },
]);

let mediaStream = null;
let audioContext = null;
let processorNode = null;
let sourceNode = null;
let latestAudio = null;
let audioChunks = [];
const targetSampleRate = 16000;

const togglePanel = () => {
  isOpen.value = !isOpen.value;
};

const appendMessage = (role, text) => {
  messages.value.push({
    id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    text,
    time: new Date().toLocaleTimeString(),
  });
};

const startVoiceInput = async () => {
  if (recording.value) {
    stopVoiceCapture(true);
    return;
  }
  if (
    typeof navigator === 'undefined' ||
    !navigator.mediaDevices?.getUserMedia
  ) {
    ElMessage.error('当前浏览器不支持录音功能');
    return;
  }
  try {
    recognitionStatus.value = '正在准备录音...';
    audioChunks = [];
    await setupAudioGraph();
    recording.value = true;
    recognitionStatus.value = '正在聆听，请开始说话';
    voiceDraft.value = '';
  } catch (error) {
    cleanupAudio();
    recognitionStatus.value = '';
    recording.value = false;
    ElMessage.error(error?.message || '语音识别服务不可用');
  }
};

const setupAudioGraph = async () => {
  mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
  const AudioContextClass =
    (isClient && (window.AudioContext || window.webkitAudioContext)) || null;
  if (!AudioContextClass) {
    throw new Error('当前环境无法创建音频上下文');
  }
  audioContext = new AudioContextClass({
    sampleRate: targetSampleRate,
  });
  if (audioContext.state === 'suspended') {
    await audioContext.resume();
  }
  sourceNode = audioContext.createMediaStreamSource(mediaStream);
  processorNode = audioContext.createScriptProcessor(4096, 1, 1);
  sourceNode.connect(processorNode);
  processorNode.connect(audioContext.destination);
  processorNode.onaudioprocess = (event) => {
    if (!recording.value) {
      return;
    }
    const inputData = event.inputBuffer.getChannelData(0);
    const downsampled = downSampleBuffer(
      inputData,
      audioContext.sampleRate,
      targetSampleRate,
    );
    if (!downsampled) return;
    const int16Data = convertFloat32ToInt16(downsampled);
    audioChunks.push(int16Data);
  };
};

const finalizeVoiceText = async () => {
  stopVoiceCapture(false);
  recognitionStatus.value = '正在识别语音...';
  
  try {
    const totalLength = audioChunks.reduce((sum, chunk) => sum + chunk.length, 0);
    const combinedData = new Int16Array(totalLength);
    let offset = 0;
    for (const chunk of audioChunks) {
      combinedData.set(chunk, offset);
      offset += chunk.length;
    }
    
    const base64Audio = arrayBufferToBase64(combinedData.buffer);
    
    const res = await recognizeSpeech({
      audioBase64: base64Audio,
      audioFormat: 'wav',
      property: 'chinese_16k_common'
    });
    
    const text = res.data || '';
    recognitionStatus.value = '';
    voiceDraft.value = '';
    if (text) {
      inputText.value = inputText.value
        ? `${inputText.value} ${text}`
        : text;
    }
    audioChunks = [];
  } catch (error) {
    recognitionStatus.value = '';
    voiceDraft.value = '';
    audioChunks = [];
    ElMessage.error(error?.message || '语音识别失败');
  }
};

const arrayBufferToBase64 = (buffer) => {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
};

const downSampleBuffer = (buffer, sampleRate, outRate) => {
  if (outRate === sampleRate) {
    return buffer;
  }
  if (outRate > sampleRate) {
    return null;
  }
  const sampleRateRatio = sampleRate / outRate;
  const newLength = Math.round(buffer.length / sampleRateRatio);
  const result = new Float32Array(newLength);
  let offsetResult = 0;
  let offsetBuffer = 0;
  while (offsetResult < result.length) {
    const nextOffsetBuffer = Math.round((offsetResult + 1) * sampleRateRatio);
    let accum = 0;
    let count = 0;
    for (let i = offsetBuffer; i < nextOffsetBuffer && i < buffer.length; i++) {
      accum += buffer[i];
      count++;
    }
    result[offsetResult] = accum / (count || 1);
    offsetResult++;
    offsetBuffer = nextOffsetBuffer;
  }
  return result;
};

const convertFloat32ToInt16 = (buffer) => {
  const l = buffer.length;
  const result = new Int16Array(l);
  for (let i = 0; i < l; i++) {
    const s = Math.max(-1, Math.min(1, buffer[i]));
    result[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
  }
  return result;
};

const stopVoiceCapture = async (sendForRecognition = true) => {
  if (!recording.value && !mediaStream) return;
  recording.value = false;
  if (processorNode) {
    processorNode.disconnect();
    processorNode.onaudioprocess = null;
    processorNode = null;
  }
  if (sourceNode) {
    sourceNode.disconnect();
    sourceNode = null;
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach((track) => track.stop());
    mediaStream = null;
  }
  if (audioContext) {
    audioContext.close();
    audioContext = null;
  }
  if (sendForRecognition && audioChunks.length > 0) {
    await finalizeVoiceText();
  } else {
    audioChunks = [];
    recognitionStatus.value = '';
    voiceDraft.value = '';
  }
};

const cleanupAudio = () => {
  if (processorNode) {
    processorNode.disconnect();
    processorNode.onaudioprocess = null;
  }
  if (sourceNode) {
    sourceNode.disconnect();
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach((track) => track.stop());
  }
  if (audioContext) {
    audioContext.close();
  }
  processorNode = null;
  sourceNode = null;
  mediaStream = null;
  audioContext = null;
  audioChunks = [];
};

const toggleVoice = async () => {
  if (recording.value) {
    await stopVoiceCapture(true);
  } else {
    await startVoiceInput();
  }
};

const decodeEscapedText = (content) => {
  if (!content) return '';
  return content.replace(/\\n/g, '\n').replace(/\\"/g, '"');
};

const formatToolCallContent = (content) => {
  if (!content) return '';
  const match = content.match(/<tool_call>\s*(\{[\s\S]*?\})\s*<\/tool_call>/);
  if (!match) {
    return decodeEscapedText(content);
  }
  try {
    const payload = JSON.parse(match[1]);
    const func = payload.function || {};
    const params = func.parameters ? JSON.stringify(func.parameters) : '';
    return `助手正在调用 ${func.name || '工具'}${params ? `，参数：${params}` : ''}`;
  } catch (error) {
    console.warn('解析 tool_call 失败', error);
    return '助手正在调用工具';
  }
};

const extractPrettySummaryFromRaw = (raw) => {
  if (typeof raw !== 'string') return '';
  const match = raw.match(
    /"event":"summary_response"[\s\S]*?"content":"([\s\S]*?)","role":"assistant"/
  );
  if (!match) return '';
  let text = match[1];
  try {
    text = JSON.parse(`"${text.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`);
  } catch {
    text = text.replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\"/g, '"');
  }
  text = text.replace(/\r\n/g, '\n');
  text = text.replace(/\n{3,}/g, '\n\n');
  text = text.replace(/\*\*/g, '');
  text = text
    .split('\n')
    .map((line) => line.replace(/^[\s\-√·•]+/, ''))
    .join('\n')
    .trim();
  return text;
};

const cleanupAgentText = (text) => {
  if (!text) return '';
  let formatted = String(text);
  formatted = formatted.replace(/\\r\\n/g, '\n').replace(/\\n/g, '\n').replace(/\r\n/g, '\n');
  formatted = formatted.replace(/\n{3,}/g, '\n\n');
  formatted = formatted
    .split('\n')
    .map((line) => line.trimStart())
    .join('\n')
    .trim();
  return formatted;
};

const buildSuccessReply = (summary, events = []) => ({
  status: 'success',
  data: {
    summary: summary || '助手已完成指令。',
    events,
  },
});

const extractAgentReply = (raw) => {
  if (raw && typeof raw === 'object' && typeof raw.status !== 'undefined') {
    if (raw.status !== 'success') {
      return raw;
    }
    const pretty = extractPrettySummaryFromRaw(raw.data?.raw_text || '');
    const summary = cleanupAgentText(pretty || raw.data?.summary || '');
    return buildSuccessReply(summary, raw.data?.events || []);
  }

  if (typeof raw === 'string') {
    const pretty = extractPrettySummaryFromRaw(raw);
    const summary = cleanupAgentText(pretty || raw);
    return buildSuccessReply(summary);
  }

  return { status: 'error', message: '智能体响应格式异常' };
};

const resetNetworkRecoveryState = () => {
  networkRecoveryInProgress.value = false;
  networkRecoveryFailed.value = false;
  networkRecoveryAttempt.value = 0;
  networkRecoveryMessage.value = '';
};

const showNetworkRecoveryAttempt = (attempt) => {
  networkRecoveryInProgress.value = true;
  networkRecoveryFailed.value = false;
  networkRecoveryAttempt.value = attempt;
  const labels = {
    1: '网络异常，正在重连 (1/3)',
    2: '正在重连服务 (2/3)',
  };
  networkRecoveryMessage.value =
    labels[attempt] ||
    `网络重连 (${attempt}/${maxNetworkRecoveryAttempts})`;
};

const markNetworkRecoverySuccess = () => {
  networkRecoveryInProgress.value = false;
  networkRecoveryFailed.value = false;
  networkRecoveryAttempt.value = 0;
  networkRecoveryMessage.value = '连接已恢复，可以继续询问 AI';
  setTimeout(() => {
    networkRecoveryMessage.value = '';
  }, 2500);
};

const markNetworkRecoveryFailure = () => {
  networkRecoveryInProgress.value = false;
  networkRecoveryFailed.value = true;
  networkRecoveryAttempt.value = maxNetworkRecoveryAttempts;
  networkRecoveryMessage.value = '网络异常，请等待网络维修中 (3/3)';
};

const isLikelyNetworkIssue = (error) => {
  if (!error) return false;
  const message = String(error?.message || '').toLowerCase();
  return (
    message.includes('network') ||
    message.includes('timeout') ||
    error?.code === 'ERR_NETWORK' ||
    error?.code === 'ECONNABORTED'
  );
};

const safeCheckAgentHealth = async () => {
  try {
    await checkAgentHealth();
  } catch (error) {
    console.warn('Agent health check failed', error);
  }
};

const reloadApplication = () => {
  if (isClient) {
    window.location.reload();
  }
};

const sendAgentQueryWithRecovery = async (content) => {
  let lastError = null;
  for (
    let attemptIndex = 0;
    attemptIndex < maxNetworkRecoveryAttempts;
    attemptIndex++
  ) {
    try {
      if (attemptIndex > 0) {
        showNetworkRecoveryAttempt(attemptIndex);
        await safeCheckAgentHealth();
        await wait(1000);
      }
      const response = await sendAgentQuery(content);
      if (attemptIndex > 0) {
        markNetworkRecoverySuccess();
      } else {
        resetNetworkRecoveryState();
      }
      return response;
    } catch (error) {
      lastError = error;
      const shouldRetry =
        isLikelyNetworkIssue(error) &&
        attemptIndex < maxNetworkRecoveryAttempts - 1;
      if (!shouldRetry) {
        if (isLikelyNetworkIssue(error)) {
          markNetworkRecoveryFailure();
        } else {
          resetNetworkRecoveryState();
        }
        break;
      }
    }
  }
  throw lastError;
};

const sendMessage = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入要咨询的内容');
    return;
  }
  const content = inputText.value.trim();
  appendMessage('user', content);
  inputText.value = '';
  sending.value = true;
  try {
    const { data } = await sendAgentQueryWithRecovery(content);
    const parsed = extractAgentReply(data);
    if (parsed.status !== 'success') {
      throw new Error(parsed.message || '智能体服务异常');
    }
    const reply = parsed.data?.summary || '我已经收到您的问题，稍后处理。';
    appendMessage('assistant', reply);
    await playAssistantSpeech(reply);
  } catch (error) {
    if (error?.isAuthError) {
      appendMessage('assistant', error.authMessage || '请登录后使用');
      ElMessage.warning('请先登录后再使用智慧助手');
    } else {
      appendMessage('assistant', '抱歉，我暂时无法处理这个请求。');
      ElMessage.error(error?.message || '智能体接口调用失败');
    }
  } finally {
    sending.value = false;
  }
};

const playAssistantSpeech = async (text) => {
  if (!text) return;
  if (latestAudio) {
    latestAudio.pause();
    latestAudio = null;
  }
  isSpeaking.value = true;
  try {
    const { data } = await synthesizeSpeech({ text });
    if (data.status !== 'success') {
      throw new Error(data.message || '语音合成失败');
    }
    const url = `data:audio/${data.format || 'mp3'};base64,${data.audio_base64}`;
    const audio = new Audio(url);
    latestAudio = audio;
    audio.onended = () => {
      isSpeaking.value = false;
      latestAudio = null;
    };
    await audio.play();
  } catch (error) {
    isSpeaking.value = false;
    latestAudio = null;
    if (supportsSpeechSynthesis) {
      const utter = new SpeechSynthesisUtterance(text);
      window.speechSynthesis.cancel();
      window.speechSynthesis.speak(utter);
    } else {
      console.warn('语音播报失败', error);
    }
  }
};

const handleKeySend = (event) => {
  if (event.ctrlKey && event.key === 'Enter') {
    sendMessage();
  }
};

onBeforeUnmount(() => {
  stopVoiceCapture(false);
  cleanupAudio();
  if (latestAudio) {
    latestAudio.pause();
    latestAudio = null;
  }
});
</script>

<template>
  <div class="smart-agent">
    <transition name="agent-fade">
      <div v-if="isOpen" class="agent-panel">
        <header class="agent-panel__header">
          <div>
            <div class="agent-panel__title">智慧语音助手</div>
            <div class="agent-panel__subtitle">
              语音识别、会员/订单咨询，实时语音播报
            </div>
          </div>
          <div class="agent-panel__header-actions">
            <span class="agent-panel__status" v-if="recording">聆听中...</span>
            <button class="agent-icon-button" @click="isOpen = false" aria-label="关闭">
              ×
            </button>
          </div>
        </header>

        <div class="agent-messages" ref="messageContainer">
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['agent-bubble', msg.role]"
          >
            <div class="agent-bubble__text">{{ msg.text }}</div>
            <div class="agent-bubble__time">{{ msg.time }}</div>
          </div>
        </div>

        <div v-if="recognitionStatus" class="agent-voice-status">
          <span>{{ recognitionStatus }}</span>
          <span v-if="voiceDraft" class="agent-voice-draft">识别：{{ voiceDraft }}</span>
        </div>

        <div
          v-if="networkRecoveryMessage"
          class="agent-network-status"
          :class="{ 'agent-network-status--error': networkRecoveryFailed }"
        >
          <span>{{ networkRecoveryMessage }}</span>
          <el-button
            v-if="networkRecoveryFailed"
            size="small"
            type="warning"
            @click="reloadApplication"
          >
            刷新页面
          </el-button>
        </div>

        <div class="agent-input">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="请输入咨询内容，或点击话筒开始说话..."
            @keydown="handleKeySend"
          />
          <div class="agent-input__actions">
            <el-button
              size="small"
              :type="recording ? 'danger' : 'info'"
              @click="toggleVoice"
            >
              {{ recording ? '结束录音' : '语音输入' }}
            </el-button>
            <el-button
              size="small"
              type="primary"
              :loading="sending"
              :disabled="sending || networkRecoveryInProgress"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </transition>

    <button class="agent-fab" @click="togglePanel" aria-label="智能助手">
      <span v-if="!isOpen">🤖</span>
      <span v-else>×</span>
      <span class="agent-fab__text">助手</span>
    </button>
  </div>
</template>

<style scoped>
.smart-agent {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 2000;
}

.agent-panel {
  width: 340px;
  height: 460px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
  overflow: hidden;
}

.agent-panel__header {
  padding: 16px 20px;
  border-bottom: 1px solid #f2f2f2;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.agent-panel__title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}

.agent-panel__subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.agent-panel__header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-panel__status {
  font-size: 12px;
  color: #67c23a;
}

.agent-messages {
  flex: 1;
  padding: 12px 16px;
  overflow-y: auto;
  background: #f8f9fb;
}

.agent-bubble {
  max-width: 85%;
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  position: relative;
  color: #1f2329;
}

.agent-bubble.user {
  margin-left: auto;
  background: #3370ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.agent-bubble.assistant {
  margin-right: auto;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

.agent-bubble__time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 6px;
}

.agent-voice-status {
  padding: 8px 16px;
  font-size: 12px;
  color: #409eff;
  border-top: 1px dashed #e0e0e0;
  background: #fdfcff;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.agent-voice-draft {
  color: #606266;
}

.agent-network-status {
  padding: 8px 16px;
  font-size: 12px;
  color: #e6a23c;
  border-top: 1px dashed #f3d19e;
  background: #fff8ee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.agent-network-status--error {
  color: #d9534f;
  border-color: #f5c2c0;
  background: #fff1f0;
}

.agent-input {
  padding: 12px 16px 16px;
  border-top: 1px solid #f2f2f2;
  background: #fff;
}

.agent-input__actions {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
}

.agent-fab {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #6a7bff, #4581ff);
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16px 32px rgba(53, 96, 255, 0.35);
  transition: transform 0.2s ease;
}

.agent-fab__text {
  font-size: 12px;
  margin-top: 2px;
}

.agent-fab:hover {
  transform: translateY(-2px);
}

.agent-icon-button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #f5f5f5;
  cursor: pointer;
  font-size: 16px;
  line-height: 28px;
  text-align: center;
}

.agent-fade-enter-active,
.agent-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.agent-fade-enter-from,
.agent-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
