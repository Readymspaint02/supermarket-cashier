<template>
  <div class="member-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>会员管理</span>
          <el-button type="primary" @click="openDialog()">新增会员</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="会员号">
          <el-input v-model="searchForm.memberId" placeholder="会员号" @keyup.enter.native="handleSearch"></el-input>
        </el-form-item>
        <el-form-item label="会员姓名">
          <el-input v-model="searchForm.name" placeholder="姓名" @keyup.enter.native="handleSearch"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="手机号" @keyup.enter.native="handleSearch"></el-input>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="searchForm.level" placeholder="全部" clearable style="width: 160px">
            <el-option
              v-for="item in levelOptions"
              :key="item.value"
              :label="`${item.badge} ${item.value}`"
              :value="item.value"
            >
              <span>{{ item.badge }} {{ item.value }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="memberList" v-loading="loading">
        <el-table-column prop="memberId" label="会员号" width="140" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            {{ genderMap[row.gender] || '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="level" label="等级" width="180">
          <template #default="{ row }">
            <span class="level-badge" :style="getLevelBadgeStyle(row.level)">
              {{ getLevelConfig(row.level).badge }}
            </span>
            <span class="level-text">{{ row.level || '未设置' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" />
        <el-table-column prop="balance" label="余额" width="120">
          <template #default="{ row }">
            ￥{{ Number(row.balance || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- <el-table-column prop="createTime" label="创建时间" /> -->
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="openRechargeDialog(row)">充值</el-button>
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button 
              size="small" 
              :type="row.faceRegistered === 1 ? 'warning' : 'success'"
              :style="row.faceRegistered === 1 ? 'background-color: #d0b019; border-color: #d0b019;' : ''"
              @click="openFaceRegister(row)"
            >
              {{ row.faceRegistered === 1 ? '人脸更新' : '人脸注册' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="member-pagination"
      />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="520px">
      <el-form :model="memberForm" :rules="memberRules" ref="memberFormRef" label-width="90px">
        <el-form-item label="会员号" prop="memberId">
          <el-input v-model="memberForm.memberId" :disabled="!!memberForm.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="memberForm.name" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="memberForm.gender">
            <el-option label="未知" :value="0" />
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="memberForm.age" :min="0" :max="120" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="memberForm.phone" />
        </el-form-item>
        <el-form-item label="会员等级" prop="level">
          <el-select v-model="memberForm.level" placeholder="请选择">
            <el-option
              v-for="item in levelOptions"
              :key="item.value"
              :label="`${item.badge} ${item.value}`"
              :value="item.value"
            >
              <span class="level-badge" :style="badgeInlineStyle(item)">
                {{ item.badge }}
              </span>
              <span class="level-text">{{ item.value }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="积分" prop="points">
          <el-input-number v-model="memberForm.points" :min="0" />
        </el-form-item>
        <el-form-item label="余额" prop="balance">
          <el-input-number
            v-model="memberForm.balance"
            :min="0"
            :precision="2"
            :step="10"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="memberForm.email" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="memberForm.address" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="memberForm.status"
            :active-value="0"
            :inactive-value="1"
            active-text="正常"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="memberForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMember">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog title="会员充值" v-model="rechargeDialogVisible" width="450px">
      <div class="recharge-info">
        <p><strong>会员号：</strong>{{ rechargeMember.memberId }}</p>
        <p><strong>姓名：</strong>{{ rechargeMember.name }}</p>
        <p><strong>当前余额：</strong>￥{{ Number(rechargeMember.balance || 0).toFixed(2) }}</p>
        <p><strong>当前积分：</strong>{{ rechargeMember.points || 0 }}</p>
      </div>
      <el-divider />
      <div class="recharge-rules">
        <el-alert type="info" :closable="false">
          <template #title>充值规则</template>
          <div class="rule-content">
            <p>1. 充值100元以下：无赠送</p>
            <p>2. 充值100-499元：赠送5%金额 + 充值金额/10 积分</p>
            <p>3. 充值500-999元：赠送10%金额 + 充值金额/10 积分</p>
            <p>4. 充值1000元以上：赠送15%金额 + 充值金额/10 积分</p>
          </div>
        </el-alert>
      </div>
      <el-form :model="rechargeForm" :rules="rechargeRules" ref="rechargeFormRef" label-width="100px" style="margin-top: 16px">
        <el-form-item label="充值金额" prop="rechargeAmount">
          <el-input-number v-model="rechargeForm.rechargeAmount" :min="1" :precision="2" :step="100" style="width: 200px" @change="calculateGift" />
        </el-form-item>
        <el-form-item label="赠送金额">
          <span class="auto-value gift-amount">￥{{ rechargeForm.giftAmount.toFixed(2) }}</span>
          <span class="auto-tip">（自动计算）</span>
        </el-form-item>
        <el-form-item label="赠送积分">
          <span class="auto-value points-value">{{ rechargeForm.pointsToAdd }} 积分</span>
          <span class="auto-tip">（自动计算）</span>
        </el-form-item>
        <el-form-item label="到账金额">
          <span class="total-amount">￥{{ ((rechargeForm.rechargeAmount || 0) + rechargeForm.giftAmount).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="2" placeholder="充值备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecharge" :loading="rechargeLoading">确认充值</el-button>
      </template>
    </el-dialog>

    <el-dialog :title="faceDialogTitle" v-model="faceDialogVisible" width="480px">
      <div class="face-register-content">
        <div class="face-info">
          <p><strong>会员号：</strong>{{ faceRegisterMember.memberId }}</p>
          <p><strong>姓名：</strong>{{ faceRegisterMember.name }}</p>
        </div>
        <div class="face-capture">
          <video ref="videoRef" autoplay playsinline class="face-video"></video>
          <canvas ref="canvasRef" style="display: none;"></canvas>
        </div>
        <div class="face-preview" v-if="capturedImage">
          <img :src="capturedImage" alt="人脸预览" class="preview-image" />
        </div>
      </div>
      <template #footer>
        <el-button @click="stopCamera">取消</el-button>
        <el-button type="primary" @click="captureAndRegister" :loading="faceRegistering">
          {{ capturedImage ? '确认' : '拍照' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMemberPage,
  createMember,
  updateMember,
  deleteMember
} from '@/api/modules/member'
import http from '@/api/request'

const memberList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增会员')

const rechargeDialogVisible = ref(false)
const rechargeMember = ref({})
const rechargeLoading = ref(false)
const rechargeFormRef = ref(null)
const rechargeForm = reactive({
  memberId: '',
  rechargeAmount: 100,
  giftAmount: 0,
  pointsToAdd: 0,
  remark: ''
})
const rechargeRules = {
  rechargeAmount: [{ required: true, message: '请输入充值金额', trigger: 'blur' }]
}

const calculateGift = () => {
  const amount = rechargeForm.rechargeAmount || 0
  let giftRate = 0
  if (amount >= 1000) {
    giftRate = 0.15
  } else if (amount >= 500) {
    giftRate = 0.10
  } else if (amount >= 100) {
    giftRate = 0.05
  }
  rechargeForm.giftAmount = Number((amount * giftRate).toFixed(2))
  rechargeForm.pointsToAdd = Math.floor(amount / 10)
}

const faceDialogVisible = ref(false)
const faceDialogTitle = ref('人脸注册')
const faceRegisterMember = ref({})
const faceRegistering = ref(false)
const capturedImage = ref('')
const videoRef = ref(null)
const canvasRef = ref(null)
let mediaStream = null

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const searchForm = reactive({
  memberId: '',
  name: '',
  phone: '',
  level: ''
})

const levelOptions = [
  { value: '普通会员', badge: 'S1', color: '#606266', background: 'rgba(96,98,102,0.18)' },
  { value: '白银会员', badge: 'S2', color: '#5c8def', background: 'rgba(92,141,239,0.2)' },
  { value: '黄金会员', badge: 'S3', color: '#e6a23c', background: 'rgba(230,162,60,0.2)' },
  { value: '铂金会员', badge: 'S4', color: '#7b88ff', background: 'rgba(123,136,255,0.22)' },
  { value: '钻石会员', badge: 'S5', color: '#d864ff', background: 'rgba(216,100,255,0.2)' }
]
const genderMap = {
  0: '未知',
  1: '男',
  2: '女'
}

const getLevelConfig = (level) => {
  return levelOptions.find(item => item.value === level) || levelOptions[0]
}

const getLevelBadgeStyle = (level) => {
  const cfg = getLevelConfig(level)
  return {
    backgroundColor: cfg.background,
    color: cfg.color,
    borderColor: cfg.color
  }
}

const badgeInlineStyle = (config) => ({
  backgroundColor: config.background,
  color: config.color,
  borderColor: config.color,
  marginRight: '8px'
})

const memberFormRef = ref(null)
const memberForm = reactive({
  id: '',
  memberId: '',
  name: '',
  gender: 0,
  age: null,
  phone: '',
  email: '',
  level: levelOptions[0].value,
  points: 0,
  balance: 0,
  address: '',
  status: 0,
  remark: ''
})

const memberRules = {
  memberId: [{ required: true, message: '请输入会员号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const loadMembers = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      queryParams: { ...searchForm }
    }
    const res = await getMemberPage(params)
    if (res.code === 200) {
      memberList.value = res.data.records || []
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadMembers()
}

const handleReset = () => {
  Object.assign(searchForm, {
    memberId: '',
    name: '',
    phone: '',
    level: ''
  })
  pagination.currentPage = 1
  loadMembers()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadMembers()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadMembers()
}

const openDialog = (row) => {
  resetMemberForm()
  if (row) {
    dialogTitle.value = '编辑会员'
    Object.assign(memberForm, row)
  } else {
    dialogTitle.value = '新增会员'
  }
  dialogVisible.value = true
}

const resetMemberForm = () => {
  Object.assign(memberForm, {
    id: '',
    memberId: '',
    name: '',
    gender: 0,
    age: null,
    phone: '',
    email: '',
    level: levelOptions[0].value,
    points: 0,
    balance: 0,
    address: '',
    status: 0,
    remark: ''
  })
}

const submitMember = () => {
  memberFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      let res
      if (memberForm.id) {
        const { id, ...payload } = memberForm
        res = await updateMember(id, payload)
      } else {
        res = await createMember(memberForm)
      }
      if (res.code === 200) {
        ElMessage.success(memberForm.id ? '更新成功' : '新增成功')
        dialogVisible.value = false
        loadMembers()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } catch (error) {
      console.error(error)
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除会员「${row.name}」吗？`, '删除提醒', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteMember(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadMembers()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error(error)
    }
  })
}

onMounted(() => {
  loadMembers()
})

const openRechargeDialog = (row) => {
  rechargeMember.value = row
  rechargeForm.memberId = row.memberId
  rechargeForm.rechargeAmount = 100
  rechargeForm.giftAmount = 0
  rechargeForm.pointsToAdd = 0
  rechargeForm.remark = ''
  rechargeDialogVisible.value = true
  calculateGift()
}

const submitRecharge = () => {
  rechargeFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      rechargeLoading.value = true
      const res = await http.post('/system/member/recharge', rechargeForm)
      if (res.code === 200) {
        ElMessage.success(`充值成功！到账金额：￥${res.data.totalAmount}`)
        rechargeDialogVisible.value = false
        loadMembers()
      } else {
        ElMessage.error(res.msg || '充值失败')
      }
    } catch (error) {
      ElMessage.error('充值失败')
      console.error(error)
    } finally {
      rechargeLoading.value = false
    }
  })
}

const openFaceRegister = async (row) => {
  faceRegisterMember.value = row
  capturedImage.value = ''
  faceDialogVisible.value = true
  
  try {
    const res = await http.get('/face/check', { params: { userId: row.memberId } })
    if (res.code === 200 && res.data?.registered) {
      faceDialogTitle.value = '人脸更新'
      row.faceRegistered = 1
    } else {
      faceDialogTitle.value = '人脸注册'
      row.faceRegistered = 0
    }
  } catch (error) {
    faceDialogTitle.value = row.faceRegistered === 1 ? '人脸更新' : '人脸注册'
    console.error('检查人脸状态失败:', error)
  }
  
  await startCamera()
}

const startCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ 
      video: { facingMode: 'user', width: 640, height: 480 } 
    })
    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
    }
  } catch (error) {
    ElMessage.error('无法访问摄像头，请检查权限设置')
    console.error(error)
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
  capturedImage.value = ''
  faceDialogVisible.value = false
}

const captureAndRegister = async () => {
  if (!capturedImage.value) {
    const video = videoRef.value
    const canvas = canvasRef.value
    if (!video || !canvas) return
    
    canvas.width = video.videoWidth || 640
    canvas.height = video.videoHeight || 480
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    capturedImage.value = canvas.toDataURL('image/jpeg', 0.8)
    return
  }
  
  faceRegistering.value = true
  try {
    const base64Data = capturedImage.value.split(',')[1]
    const res = await http.post('/face/register', {
      userId: faceRegisterMember.value.memberId,
      image: base64Data,
      userInfo: faceRegisterMember.value.name
    })
    
    if (res.code === 200) {
      const msg = faceDialogTitle.value === '人脸更新' ? '人脸更新成功' : '人脸注册成功'
      ElMessage.success(msg)
      faceRegisterMember.value.faceRegistered = 1
      stopCamera()
    } else {
      ElMessage.error(res.msg || '人脸注册失败')
    }
  } catch (error) {
    const errMsg = error?.response?.data?.msg || error?.message || '人脸注册失败，请重试'
    ElMessage.error(errMsg)
    console.error('人脸注册错误:', error)
  } finally {
    faceRegistering.value = false
  }
}

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.member-pagination {
  margin-top: 20px;
  text-align: right;
}

.recharge-info {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  line-height: 1.8;
}

.recharge-info p {
  margin: 4px 0;
}

.recharge-rules {
  margin-bottom: 8px;
}

.rule-content {
  font-size: 12px;
  line-height: 1.6;
}

.rule-content p {
  margin: 2px 0;
}

.auto-value {
  font-size: 16px;
  font-weight: 600;
}

.gift-amount {
  color: #67c23a;
}

.points-value {
  color: #409eff;
}

.auto-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.total-amount {
  font-size: 18px;
  font-weight: bold;
  color: #e6a23c;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  padding: 2px 6px;
  border-radius: 12px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  margin-right: 6px;
}

.level-text {
  font-weight: 500;
  color: #303133;
}

.face-register-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.face-info {
  width: 100%;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.face-info p {
  margin: 4px 0;
}

.face-capture {
  width: 100%;
  max-width: 400px;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.face-video {
  width: 100%;
  height: 300px;
  object-fit: cover;
  display: block;
}

.face-preview {
  width: 100%;
  max-width: 400px;
}

.preview-image {
  width: 100%;
  border-radius: 8px;
}
</style>
