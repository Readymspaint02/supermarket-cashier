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
            <p class="manual-tip">输入商品编码或条形码并按回车，即可将商品加入购物车。</p>
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
              <template #append>
                <el-button type="primary" :loading="manualLoading" @click="handleManualScan">
                  添加商品
                </el-button>
              </template>
            </el-input>
          </div>

          <el-table
            :data="cart"
            border
            stripe
            :height="420"
            v-loading="cartLoading"
            show-summary
            :summary-method="getSummaries"
            empty-text="暂无商品，请先输入编码"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="productName" label="商品名称" min-width="150" />
            <el-table-column prop="productCode" label="商品编码" width="140" show-overflow-tooltip />
            <el-table-column prop="barcode" label="条形码" width="140" show-overflow-tooltip />
            <el-table-column prop="price" label="单价" width="100" align="right">
              <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="140" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :max="row.stock"
                  @change="handleQuantityChange(row)"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="120" align="right">
              <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
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
              <span class="value">{{ cart.length }} 件</span>
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
              </el-radio-group>
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
        <el-button type="primary" @click="handlePrint">打印小票</el-button>
        <el-button @click="handleContinue">继续收银</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, CreditCard } from '@element-plus/icons-vue'
import { getProductByBarcode, getProductByCode } from '@/api/modules/product'
import { getInventoryByProductId } from '@/api/modules/inventory'
import { checkout } from '@/api/modules/order'

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

const handleCheckout = () => {
  if (cart.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  ElMessageBox.confirm(`确认结算 ¥${paidAmount.value.toFixed(2)} ?`, '结算确认', {
    type: 'info',
  })
    .then(async () => {
      try {
        checkoutLoading.value = true
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
        const res = await checkout(orderData)
        if (res.code === 200) {
          currentOrder.value = res.data
          successDialogVisible.value = true
          cart.value = []
          discountAmount.value = 0
          remark.value = ''
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
  const map = { 1: '现金', 2: '微信', 3: '支付宝', 4: '银行卡' }
  return map[val] || '-'
}

const getSummaries = () => {
  return ['合计', '', '', '', '', totalQuantity.value, `¥${totalAmount.value.toFixed(2)}`, '']
}
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
</style>
