<template>
  <div class="login-container">
    <div class="login-form">
      <h2>智慧收银系统</h2>
      <el-form :model="loginForm" label-width="80px" :rules="loginRules" ref="loginFormRef">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit" :loading="loading" >登录</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <div style="display:flex;gap:12px;align-items:center;justify-content:center;margin-top:6px;">
        <el-button type="success" @click="toggleFacePanel">人脸登录</el-button>
        <el-button v-if="showFacePanel && !streaming" @click="startCamera">打开摄像头</el-button>
        <el-button v-if="showFacePanel && streaming" @click="stopCamera">关闭摄像头</el-button>
        <el-button type="primary" :loading="faceLoading" v-if="showFacePanel" @click="captureAndLogin">识别并登录</el-button>
      </div>
      <div v-if="showFacePanel" style="margin-top:12px;display:flex;gap:12px;justify-content:center;">
        <video ref="videoRef" autoplay playsinline style="width:280px;height:210px;background:#000;border-radius:6px;"></video>
        <canvas ref="canvasRef" width="280" height="210" style="display:none;"></canvas>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive ,useTemplateRef,ref, onBeforeUnmount} from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/modules/login'
import { getUserPermissions } from '@/api/modules/user'
import {initDynamicRouter} from '@/router/modules/dynamicRouter'
import { ElMessage, ElNotification } from 'element-plus';
import http from '@/api/request'

const router = useRouter()
const loginFormRef = useTemplateRef('loginFormRef');
// 定义加载状态变量
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})
// 定义表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const onSubmit = async() => {
  try{
  await loginFormRef.value.validate()
  loading.value = true  // 开始加载
  // 模拟登录，实际项目中应发送请求到后端验证
  // if (loginForm.username && loginForm.password) {
  //   // 保存token到localStorage
  //   localStorage.setItem('token', 'fake-token')
  //   router.push('/')
  // } else {
  //   alert('请输入用户名和密码')
  // }
  const {code,data:{token,user}} = await login({...loginForm})
   if (code !== 200) {
        ElMessage.error('用户名或密码错误');
        return;
    }
  
    // 登录成功，保存token到localStorage
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    
    // 获取用户权限列表
    try {
      const permsRes = await getUserPermissions()
      if (permsRes.code === 200) {
        // 构建用户信息对象（包含权限）
        const userInfo = {
          ...user,
          permissions: permsRes.data || []
        }
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        console.log('用户权限列表:', permsRes.data)
      }
    } catch (error) {
      console.error('获取权限失败:', error)
    }
    
    // 获取用户权限菜单
    await initDynamicRouter()
    router.push('/')
    ElNotification({
        message: '欢迎登录智慧收银系统',
        type: 'success',
        duration: 3000,
      });
  }catch(error){}
  finally{
    loading.value = false  // 结束加载
  }
}

const onReset = () => {
  loginForm.username = ''
  loginForm.password = ''
}

const showFacePanel = ref(false)
const streaming = ref(false)
const faceLoading = ref(false)
const videoRef = ref(null)
const canvasRef = ref(null)
let mediaStream = null

const toggleFacePanel = () => {
  showFacePanel.value = !showFacePanel.value
}

const startCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: 640, height: 480 } })
    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
      streaming.value = true
    }
  } catch (e) {
    ElMessage.error('无法打开摄像头')
  }
}

const stopCamera = () => {
  try {
    if (mediaStream) {
      mediaStream.getTracks().forEach(t => t.stop())
      mediaStream = null
    }
    streaming.value = false
  } catch {}
}

onBeforeUnmount(() => {
  stopCamera()
})

const captureAndLogin = async () => {
  if (!videoRef.value) return
  try {
    faceLoading.value = true
    const video = videoRef.value
    const canvas = canvasRef.value
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    const dataUrl = canvas.toDataURL('image/jpeg', 0.9)
    const base64 = dataUrl.split(',')[1]
    const res = await http.post('/auth/faceLogin', { image: base64 })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '人脸识别失败')
      return
    }
    const { token, user } = res.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    try {
      const permsRes = await getUserPermissions()
      if (permsRes.code === 200) {
        const userInfo = { ...user, permissions: permsRes.data || [] }
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
      }
    } catch {}
    await initDynamicRouter()
    router.push('/')
    ElNotification({ message: '人脸登录成功', type: 'success', duration: 3000 })
  } catch (e) {
    ElMessage.error('人脸登录异常')
  } finally {
    faceLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.login-form {
  width: 400px;
  padding: 30px;
  background-color: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-form h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}
</style>