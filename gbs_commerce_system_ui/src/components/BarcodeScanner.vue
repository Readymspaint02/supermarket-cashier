<template>
  <div class="barcode-scanner">
    <video ref="videoRef" class="scanner-video" autoplay playsinline></video>
    <p class="scanner-hint">将条码置于取景框内，保持手稳</p>
    <div class="scanner-actions">
      <el-button type="primary" @click="startScan" :loading="scanning">开始识别</el-button>
      <el-button @click="stopAndClose">关闭</el-button>
    </div>
    <el-alert
      v-if="errorMsg"
      :title="errorMsg"
      type="error"
      :closable="false"
      class="scanner-alert"
    />
  </div>
</template>

<script setup>
import { BrowserMultiFormatReader } from '@zxing/library'
import { ref, onBeforeUnmount } from 'vue'

const emit = defineEmits(['detected', 'close'])

const videoRef = ref(null)
const reader = ref(null)
const scanning = ref(false)
const errorMsg = ref('')

const startScan = async () => {
  if (scanning.value) return
  errorMsg.value = ''
  scanning.value = true
  try {
    reader.value = new BrowserMultiFormatReader()
    await reader.value.decodeFromVideoDevice(null, videoRef.value, (result) => {
      if (result && result.text) {
        emit('detected', result.text)
        stopScan()
      }
    })
  } catch (error) {
    console.error('barcode scan error', error)
    errorMsg.value = '无法访问摄像头，请检查权限或使用 HTTPS'
    scanning.value = false
  }
}

const stopScan = () => {
  if (reader.value) {
    try {
      reader.value.reset()
    } catch (error) {
      console.warn(error)
    }
    reader.value = null
  }
  scanning.value = false
}

const stopAndClose = () => {
  stopScan()
  emit('close')
}

onBeforeUnmount(() => {
  stopScan()
})

startScan()
</script>

<style scoped>
.barcode-scanner {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scanner-video {
  width: 100%;
  border-radius: 8px;
  background: #000;
  min-height: 240px;
  object-fit: cover;
}

.scanner-hint {
  margin: 0;
  font-size: 14px;
  color: #909399;
  text-align: center;
}

.scanner-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.scanner-alert {
  margin-bottom: 0;
}
</style>
