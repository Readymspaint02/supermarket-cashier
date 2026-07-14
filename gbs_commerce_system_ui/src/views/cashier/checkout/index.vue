<template>
  <div class="checkout-container">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="box-card cart-card">
          <template #header>
            <div class="card-header">
              <span class="title">收银购物车</span>
              <el-button type="danger" size="small" @click="clearCart" :disabled="cart.length === 0">
                清空购物车
              </el-button>
            </div>
          </template>

          <div class="manual-panel">
            <p class="manual-tip">输入商品编码或条形码并按回车，或点击扫码按钮使用摄像头扫描。</p>
            <div class="input-row">
              <el-input
                v-model="barcodeInput"
                placeholder="请输入商品编码 / 条形码"
                clearable
                size="large"
                @keyup.enter="handleManualScan"
                ref="barcodeInputRef"
              >
                <template #prepend>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button size="large" @click="openScannerDialog">
                <el-icon><Camera /></el-icon>
                扫码
              </el-button>
              <el-button type="primary" size="large" :loading="manualLoading" @click="handleManualScan">
                添加商品
              </el-button>
            </div>
          </div>

          <el-table
            :data="cart"
            border
            stripe
            :height="420"
            v-loading="cartLoading"
            empty-text="暂无商品，请先输入编码"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="productName" label="商品名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="price" label="单价" width="90" align="right">
              <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="160" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :max="row.stock"
                  :precision="0"
                  :step="1"
                  controls-position="right"
                  size="small"
                  style="width: 130px"
                  @change="handleQuantityChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="removeFromCart($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="box-card settle-card">
          <template #header>
            <div class="card-header">
              <span class="title">结算信息</span>
            </div>
          </template>

          <div class="settle-info">
            <div class="info-row">
              <span class="label">商品种类</span>
              <span class="value">{{ cart.length }} 种</span>
            </div>
            <div class="info-row">
              <span class="label">商品数量</span>
              <span class="value">{{ totalQuantity }} 件</span>
            </div>
            <div class="info-row total">
              <span class="label">商品总额</span>
              <span class="value">¥{{ totalAmount.toFixed(2) }}</span>
            </div>

            <el-divider />

            <el-form label-width="100px" class="discount-form">
              <el-form-item label="优惠金额">
                <el-input-number
                  v-model="discountAmount"
                  :min="0"
                  :max="totalAmount"
                  :precision="2"
                  :step="0.1"
                  controls-position="right"
                  style="width: 220px"
                />
              </el-form-item>
            </el-form>

            <div class="info-row final-amount">
              <span class="label">应付金额</span>
              <span class="value">¥{{ paidAmount.toFixed(2) }}</span>
            </div>

            <el-divider />

            <div class="payment-method">
              <p class="label">支付方式</p>
              <el-radio-group v-model="paymentMethod">
                <el-radio :value="1" border>现金</el-radio>
                <el-radio :value="2" border>微信</el-radio>
                <el-radio :value="3" border>支付宝</el-radio>
                <el-radio :value="4" border>银行卡</el-radio>
                <el-radio :value="5" border>刷脸支付</el-radio>
                <el-radio :value="6" border :disabled="!memberInfo">余额支付</el-radio>
              </el-radio-group>
              <div v-if="memberInfo" class="member-info">
                <span>会员：{{ memberInfo.name }}</span>
                <span style="margin-left: 20px">余额：¥{{ memberInfo.balance?.toFixed(2) || '0.00' }}</span>
                <span style="margin-left: 20px">积分：{{ memberInfo.points || 0 }}</span>
              </div>
              <div class="member-input-row">
                <el-input
                  v-model="memberIdInput"
                  placeholder="输入会员编号后按回车查询"
                  style="width: 200px"
                  @keyup.enter="queryMember"
                />
                <el-button size="small" @click="queryMember">查询会员</el-button>
                <el-button size="small" type="danger" v-if="memberInfo" @click="clearMember">清除会员</el-button>
              </div>
            </div>

            <el-input
              v-model="remark"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="备注信息（选填）"
            />

            <el-button
              type="primary"
              size="large"
              class="full-btn"
              :disabled="cart.length === 0"
              :loading="checkoutLoading"
              @click="handleCheckout"
            >
              <el-icon><CreditCard /></el-icon>
              立即结算
            </el-button>

            <el-button class="full-btn" size="large" :disabled="cart.length === 0" @click="clearCart">
              取消订单
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      width="520px"
      title="结算成功"
      v-model="successDialogVisible"
      :close-on-click-modal="false"
    >
      <el-result icon="success" title="订单创建成功">
        <template #extra>
          <div class="order-info">
            <p><strong>订单号：</strong>{{ currentOrder?.orderNo }}</p>
            <p><strong>实付金额：</strong>¥{{ currentOrder?.paidAmount }}</p>
            <p><strong>支付方式：</strong>{{ getPaymentMethodName(currentOrder?.paymentMethod) }}</p>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="openReceiptDialog">电子小票</el-button>
        <el-button @click="handleContinue">继续收银</el-button>
      </template>
    </el-dialog>

    <el-dialog
      title="电子小票"
      v-model="receiptDialogVisible"
      width="360px"
      :close-on-click-modal="true"
    >
      <Receipt ref="receiptComponentRef" :order="currentOrder" :showQRCode="true" />
      <template #footer>
        <el-button @click="downloadReceipt" :loading="receiptDownloading" type="primary">
          <el-icon><Download /></el-icon>
          下载图片
        </el-button>
        <el-button @click="receiptDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      title="扫码识别"
      v-model="scannerDialogVisible"
      width="480px"
      :close-on-click-modal="false"
      @close="closeScanner"
    >
      <BarcodeScanner
        v-if="scannerDialogVisible"
        @detected="handleBarcodeDetected"
        @close="closeScanner"
      />
    </el-dialog>

    <el-dialog
      title="刷脸支付"
      v-model="facePayDialogVisible"
      width="480px"
      :close-on-click-modal="false"
      @open="openFacePayDialog"
      @close="closeFacePay"
    >
      <div class="face-pay-content">
        <div class="face-tip">
          <el-icon :size="24" color="#409eff"><Camera /></el-icon>
          <span>请正对摄像头，系统将自动识别已注册会员</span>
        </div>
        
        <div class="face-capture">
          <video ref="faceVideoRef" autoplay playsinline class="face-video"></video>
          <canvas ref="faceCanvasRef" style="display: none;"></canvas>
          <div class="face-overlay" v-if="facePayDetecting">
            <div class="detecting-animation">
              <el-icon class="is-loading" :size="32"><Loading /></el-icon>
              <span>正在识别中...</span>
            </div>
          </div>
        </div>
        
        <div class="face-status" v-if="facePayStatus">
          <el-alert :type="facePayStatus.type" :closable="false" show-icon>
            {{ facePayStatus.message }}
          </el-alert>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeFacePay">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, CreditCard, Camera, Loading, Download } from '@element-plus/icons-vue'
import html2canvas from 'html2canvas'
import { getProductByBarcode, getProductByCode } from '@/api/modules/product'
import { getInventoryByProductId } from '@/api/modules/inventory'
import { checkout } from '@/api/modules/order'
import BarcodeScanner from '@/components/BarcodeScanner.vue'
import Receipt from '@/components/Receipt.vue'
import http from '@/api/request'

const barcodeInput = ref('')
const barcodeInputRef = ref(null)
const manualLoading = ref(false)
const cartLoading = ref(false)

const cart = ref([])
const discountAmount = ref(0)
const paymentMethod = ref(1)
const remark = ref('')
const checkoutLoading = ref(false)
const successDialogVisible = ref(false)
const currentOrder = ref(null)
const scannerDialogVisible = ref(false)

const facePayDialogVisible = ref(false)
const faceVideoRef = ref(null)
const faceCanvasRef = ref(null)
const facePayStream = ref(null)
const facePayDetecting = ref(false)
const facePayStatus = ref(null)
let facePayTimer = null
let isFacePayProcessing = false

const receiptDialogVisible = ref(false)
const receiptComponentRef = ref(null)
const receiptDownloading = ref(false)

const memberInfo = ref(null)
const memberIdInput = ref('')

const totalQuantity = computed(() =>
  cart.value.reduce((sum, item) => sum + item.quantity, 0)
)
const totalAmount = computed(() =>
  cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)
const paidAmount = computed(() => Math.max(0, totalAmount.value - discountAmount.value))

onMounted(() => {
  focusInput()
})

const focusInput = () => {
  nextTick(() => barcodeInputRef.value?.focus())
}

const handleManualScan = async () => {
  if (!barcodeInput.value || !barcodeInput.value.trim()) {
    ElMessage.warning('请输入商品编码或条形码')
    return
  }
  await addProductByManual(barcodeInput.value.trim())
}

const addProductByManual = async (code) => {
  manualLoading.value = true
  cartLoading.value = true
  try {
    const product = await fetchProduct(code)
    if (!product) {
      return
    }

    if (product.status !== 0) {
      ElMessage.error('该商品已下架')
      return
    }

    const inventory = await fetchInventory(product.id)
    if (!inventory || inventory.stockQuantity <= 0) {
      ElMessage.warning('库存不足，无法添加')
      return
    }

    const exist = cart.value.find((item) => item.productId === product.id)
    if (exist) {
      if (exist.quantity >= inventory.stockQuantity) {
        ElMessage.warning('库存不足，无法继续增加数量')
      } else {
        exist.quantity += 1
        ElMessage.success('数量 +1')
      }
    } else {
      cart.value.push({
        productId: product.id,
        productName: product.productName,
        productCode: product.productCode,
        barcode: product.barcode || '--',
        price: Number(product.price) || 0,
        quantity: 1,
        stock: inventory.stockQuantity,
      })
      ElMessage.success('商品已加入购物车')
    }
  } catch (error) {
    console.error('手动添加失败', error)
    if (error?.msg) {
      ElMessage.error(error.msg)
    } else {
      ElMessage.error('添加失败，请稍后重试')
    }
  } finally {
    manualLoading.value = false
    cartLoading.value = false
    barcodeInput.value = ''
    focusInput()
  }
}

const fetchProduct = async (code) => {
  let lastError = null
  const tryRequest = async (requestFn) => {
    try {
      const res = await requestFn()
      if (res && res.data) {
        return res.data
      }
    } catch (err) {
      lastError = err
    }
    return null
  }

  const byBarcode = await tryRequest(() => getProductByBarcode(code))
  if (byBarcode) {
    return byBarcode
  }

  const byCode = await tryRequest(() => getProductByCode(code))
  if (byCode) {
    return byCode
  }

  if (lastError?.msg) {
    ElMessage.error(lastError.msg)
  } else {
    ElMessage.error('未找到对应商品')
  }
  return null
}

const fetchInventory = async (productId) => {
  try {
    const res = await getInventoryByProductId(productId)
    if (res && res.data) {
      return res.data
    }
  } catch (error) {
    if (error?.msg) {
      ElMessage.error(error.msg)
    } else {
      ElMessage.error('查询库存失败')
    }
  }
  return null
}

const handleQuantityChange = (row) => {
  if (row.quantity < 1) {
    row.quantity = 1
  }
  if (row.quantity > row.stock) {
    row.quantity = row.stock
    ElMessage.warning('超过库存上限')
  }
}

const removeFromCart = (index) => {
  cart.value.splice(index, 1)
}

const clearCart = () => {
  if (cart.value.length === 0) {
    return
  }
  ElMessageBox.confirm('确认清空购物车吗？', '提示', { type: 'warning' })
    .then(() => {
      cart.value = []
      discountAmount.value = 0
      remark.value = ''
    })
    .catch(() => {})
}

const openScannerDialog = () => {
  scannerDialogVisible.value = true
}

const closeScanner = () => {
  scannerDialogVisible.value = false
}

const handleBarcodeDetected = async (code) => {
  if (!code || !code.trim()) {
    return
  }
  ElMessage.success(`识别到条码: ${code}`)
  closeScanner()
  await addProductByManual(code.trim())
}

const handleCheckout = () => {
  if (cart.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }

  if (paymentMethod.value === 5) {
    facePayDialogVisible.value = true
    return
  }

  ElMessageBox.confirm(`确认结算 ¥${paidAmount.value.toFixed(2)} ?`, '结算确认', {
    type: 'info',
  })
    .then(async () => {
      try {
        checkoutLoading.value = true
        
        if (paymentMethod.value === 6 && memberInfo.value) {
          if ((memberInfo.value.balance || 0) < paidAmount.value) {
            ElMessage.error('会员余额不足')
            checkoutLoading.value = false
            return
          }
        }
        
        const orderData = {
          cartItems: cart.value.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
          })),
          paidAmount: paidAmount.value,
          discountAmount: discountAmount.value,
          paymentMethod: paymentMethod.value,
          remark: remark.value,
        }
        
        if (memberInfo.value) {
          orderData.memberId = memberInfo.value.memberId
        }
        
        const res = await checkout(orderData)
        if (res.code === 200) {
          currentOrder.value = res.data
          successDialogVisible.value = true
          cart.value = []
          discountAmount.value = 0
          remark.value = ''
          memberInfo.value = null
          memberIdInput.value = ''
        }
      } finally {
        checkoutLoading.value = false
      }
    })
    .catch(() => {})
}

const handlePrint = () => {
  ElMessage.info('打印功能待接入')
}

const handleContinue = () => {
  successDialogVisible.value = false
  focusInput()
}

const getPaymentMethodName = (val) => {
  const map = { 1: '现金', 2: '微信', 3: '支付宝', 4: '银行卡', 5: '刷脸支付', 6: '余额支付' }
  return map[val] || '-'
}

const queryMember = async () => {
  if (!memberIdInput.value || !memberIdInput.value.trim()) {
    ElMessage.warning('请输入会员编号')
    return
  }
  try {
    const res = await http.get(`/system/member/byMemberId/${memberIdInput.value.trim()}`)
    if (res.code === 200 && res.data) {
      memberInfo.value = res.data
      ElMessage.success(`会员：${res.data.name}，余额：¥${res.data.balance?.toFixed(2) || '0.00'}`)
    } else {
      ElMessage.error('未找到该会员')
    }
  } catch (error) {
    ElMessage.error('查询会员失败')
  }
}

const clearMember = () => {
  memberInfo.value = null
  memberIdInput.value = ''
  if (paymentMethod.value === 6) {
    paymentMethod.value = 1
  }
}

const openReceiptDialog = () => {
  receiptDialogVisible.value = true
}

const downloadReceipt = async () => {
  if (!receiptComponentRef.value) return
  
  receiptDownloading.value = true
  try {
    const receiptEl = receiptComponentRef.value.$el?.querySelector('.receipt-container') || 
                      receiptComponentRef.value.$el
    if (!receiptEl) {
      ElMessage.error('小票内容获取失败')
      return
    }
    
    const canvas = await html2canvas(receiptEl, {
      backgroundColor: '#ffffff',
      scale: 2,
      useCORS: true
    })
    
    const link = document.createElement('a')
    link.download = `小票_${currentOrder.value?.orderNo || Date.now()}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    
    ElMessage.success('小票下载成功')
  } catch (error) {
    console.error('下载小票失败', error)
    ElMessage.error('下载小票失败')
  } finally {
    receiptDownloading.value = false
  }
}

const openFacePayDialog = async () => {
  facePayStatus.value = null
  facePayDetecting.value = false
  isFacePayProcessing = false
  
  try {
    facePayStream.value = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user', width: 640, height: 480 }
    })
    if (faceVideoRef.value) {
      faceVideoRef.value.srcObject = facePayStream.value
    }
    startFaceDetection()
  } catch (error) {
    ElMessage.error('无法访问摄像头')
    console.error(error)
    facePayStatus.value = { type: 'error', message: '无法访问摄像头，请检查权限设置' }
  }
}

const startFaceDetection = () => {
  if (facePayTimer) {
    clearInterval(facePayTimer)
  }
  
  facePayTimer = setInterval(() => {
    if (!isFacePayProcessing && facePayDialogVisible.value) {
      captureAndVerify()
    }
  }, 1500)
}

const stopFaceDetection = () => {
  if (facePayTimer) {
    clearInterval(facePayTimer)
    facePayTimer = null
  }
}

const captureAndVerify = async () => {
  if (!faceVideoRef.value || !faceCanvasRef.value || isFacePayProcessing) {
    return
  }
  
  isFacePayProcessing = true
  facePayDetecting.value = true
  
  try {
    const video = faceVideoRef.value
    const canvas = faceCanvasRef.value
    
    canvas.width = video.videoWidth || 640
    canvas.height = video.videoHeight || 480
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    const base64Data = canvas.toDataURL('image/jpeg', 0.8).split(',')[1]
    
    const orderData = {
      cartItems: cart.value.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
      })),
      paidAmount: paidAmount.value,
      discountAmount: discountAmount.value,
      remark: remark.value,
      faceImage: base64Data
    }
    
    const res = await http.post('/payment/facepay/search', orderData)
    
    if (res.code === 200) {
      stopFaceDetection()
      stopFaceCamera()
      facePayDialogVisible.value = false
      
      currentOrder.value = res.data
      successDialogVisible.value = true
      cart.value = []
      discountAmount.value = 0
      remark.value = ''
      
      ElMessage.success('刷脸支付成功')
    } else {
      facePayStatus.value = { type: 'warning', message: res.msg || '识别失败，请重试' }
    }
  } catch (error) {
    const errMsg = error?.msg || error?.message || '识别失败'
    facePayStatus.value = { type: 'error', message: errMsg }
  } finally {
    facePayDetecting.value = false
    isFacePayProcessing = false
  }
}

const stopFaceCamera = () => {
  if (facePayStream.value) {
    facePayStream.value.getTracks().forEach(track => track.stop())
    facePayStream.value = null
  }
  if (faceVideoRef.value) {
    faceVideoRef.value.srcObject = null
  }
}

const closeFacePay = () => {
  stopFaceDetection()
  stopFaceCamera()
  facePayDialogVisible.value = false
  facePayStatus.value = null
}

onBeforeUnmount(() => {
  stopFaceDetection()
  stopFaceCamera()
})
</script>

<style scoped>
.checkout-container {
  padding: 20px;
}
.box-card {
  min-height: 640px;
}
.cart-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.manual-panel {
  margin-bottom: 20px;
  padding: 16px;
  border-radius: 10px;
  background: #f8f9fb;
}
.manual-tip {
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
}
.input-row {
  display: flex;
  gap: 12px;
}
.input-row .el-input {
  flex: 1;
}
.settle-card {
  position: sticky;
  top: 20px;
}
.settle-info {
  padding: 10px 0;
}
.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 15px;
  color: #606266;
}
.info-row .value {
  font-weight: 600;
}
.info-row.total {
  font-size: 18px;
  color: #409eff;
}
.info-row.final-amount {
  font-size: 22px;
  color: #f56c6c;
  font-weight: 700;
}
.payment-method {
  margin-bottom: 16px;
}
.payment-method .label {
  font-weight: 500;
  margin-bottom: 8px;
}
.payment-method :deep(.el-radio) {
  width: 48%;
  margin-bottom: 8px;
}
.full-btn {
  width: 100%;
  margin-top: 14px;
}
.order-info {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  line-height: 1.8;
}

.face-pay-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.face-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
  color: #409eff;
  font-size: 14px;
}

.face-capture {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
  position: relative;
}

.face-video {
  width: 100%;
  height: 320px;
  object-fit: cover;
  display: block;
}

.face-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detecting-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #fff;
}

.face-status {
  margin-top: 8px;
}

.member-info {
  margin-top: 10px;
  padding: 8px 12px;
  background: #e6f7ff;
  border-radius: 6px;
  font-size: 13px;
  color: #1890ff;
}

.member-input-row {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>