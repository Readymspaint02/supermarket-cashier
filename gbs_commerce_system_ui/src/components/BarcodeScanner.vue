<template>
  <div class="barcode-scanner">
    <video ref="videoRef" class="scanner-video" autoplay playsinline muted></video>
    <p class="scanner-hint">将条码置于取景框内，保持手稳</p>
    
    <div class="device-select" v-if="videoDevices.length > 1">
      <span>摄像头：</span>
      <el-select v-model="selectedDeviceId" size="small" @change="switchDevice" style="width: 200px">
        <el-option
          v-for="(device, index) in videoDevices"
          :key="device.deviceId"
          :label="device.label || `摄像头 ${index + 1}`"
          :value="device.deviceId"
        />
      </el-select>
    </div>
    
    <div class="scanner-actions">
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
import { ref, onBeforeUnmount, onMounted } from 'vue'

const emit = defineEmits(['detected', 'close'])

const videoRef = ref(null)
const reader = ref(null)
const scanning = ref(false)
const errorMsg = ref('')
const isDetected = ref(false)
const videoDevices = ref([])
const selectedDeviceId = ref('')

const requestCamera = async () => {
  errorMsg.value = ''
  scanning.value = true
  
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    stream.getTracks().forEach(track => track.stop())
    
    const devices = await navigator.mediaDevices.enumerateDevices()
    videoDevices.value = devices.filter(d => d.kind === 'videoinput')
    
    if (videoDevices.value.length === 0) {
      errorMsg.value = '未检测到摄像头设备'
      scanning.value = false
      return
    }
    
    selectedDeviceId.value = videoDevices.value[0].deviceId
    await startScan()
  } catch (error) {
    console.error('requestCamera error', error)
    scanning.value = false
    handleCameraError(error)
  }
}

const handleCameraError = (error) => {
  if (error.name === 'NotAllowedError') {
    errorMsg.value = '摄像头权限被拒绝，请点击地址栏左侧图标允许访问'
  } else if (error.name === 'NotFoundError') {
    errorMsg.value = '未找到摄像头设备'
  } else if (error.name === 'NotReadableError') {
    errorMsg.value = '摄像头被其他应用占用，请关闭其他使用摄像头的程序'
  } else if (location.protocol !== 'https:' && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
    errorMsg.value = `摄像头需要 HTTPS 环境，当前协议: ${location.protocol}`
  } else {
    errorMsg.value = `摄像头访问失败: ${error.message || error.name}`
  }
}

const startScan = async () => {
  if (!selectedDeviceId.value) {
    errorMsg.value = '请先选择摄像头设备'
    scanning.value = false
    return
  }
  
  errorMsg.value = ''
  isDetected.value = false
  
  try {
    if (reader.value) {
      reader.value.reset()
      reader.value = null
    }
    
    reader.value = new BrowserMultiFormatReader()
    await reader.value.decodeFromVideoDevice(selectedDeviceId.value, videoRef.value, (result, err) => {
      if (result && result.getText() && !isDetected.value) {
        isDetected.value = true
        emit('detected', result.getText())
        stopScan()
      }
      if (err && err.name !== 'NotFoundException') {
        console.warn('scan err:', err.name)
      }
    })
  } catch (error) {
    console.error('startScan error', error)
    handleCameraError(error)
    scanning.value = false
  }
}

const switchDevice = async () => {
  stopScan()
  scanning.value = true
  await startScan()
}

const stopScan = () => {
  if (reader.value) {
    try {
      reader.value.reset()
    } catch (e) {
      console.warn(e)
    }
    reader.value = null
  }
  if (videoRef.value && videoRef.value.srcObject) {
    const tracks = videoRef.value.srcObject.getTracks()
    tracks.forEach(track => track.stop())
    videoRef.value.srcObject = null
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

onMounted(() => {
  requestCamera()
})
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

.device-select {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
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