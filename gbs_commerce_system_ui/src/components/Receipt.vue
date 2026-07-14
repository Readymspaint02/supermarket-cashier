<template>
  <div class="receipt-wrapper">
    <div ref="receiptRef" class="receipt-container">
      <div class="receipt-header">
        <div class="store-name">智慧收银系统</div>
        <div class="store-subtitle">购物小票</div>
      </div>
      
      <div class="receipt-divider">********************************</div>
      
      <div class="receipt-info">
        <div class="info-row">
          <span>订单号：</span>
          <span>{{ order?.orderNo || '-' }}</span>
        </div>
        <div class="info-row">
          <span>交易时间：</span>
          <span>{{ formatTime(order?.createTime) }}</span>
        </div>
        <div class="info-row">
          <span>收银员：</span>
          <span>{{ order?.cashierName || '系统' }}</span>
        </div>
        <div class="info-row" v-if="order?.memberId">
          <span>会员：</span>
          <span>{{ order.memberId }}</span>
        </div>
      </div>
      
      <div class="receipt-divider">--------------------------------</div>
      
      <div class="receipt-items">
        <div class="items-header">
          <span class="col-name">商品名称</span>
          <span class="col-qty">数量</span>
          <span class="col-price">单价</span>
          <span class="col-total">小计</span>
        </div>
        <div class="items-body">
          <div class="item-row" v-for="(item, index) in order?.items" :key="index">
            <span class="col-name">{{ item.productName }}</span>
            <span class="col-qty">{{ item.quantity }}</span>
            <span class="col-price">¥{{ item.price?.toFixed(2) }}</span>
            <span class="col-total">¥{{ (item.price * item.quantity)?.toFixed(2) }}</span>
          </div>
        </div>
      </div>
      
      <div class="receipt-divider">--------------------------------</div>
      
      <div class="receipt-summary">
        <div class="summary-row">
          <span>商品数量：</span>
          <span>{{ totalQuantity }} 件</span>
        </div>
        <div class="summary-row">
          <span>商品总额：</span>
          <span>¥{{ order?.totalAmount?.toFixed(2) || '0.00' }}</span>
        </div>
        <div class="summary-row" v-if="order?.discountAmount > 0">
          <span>优惠金额：</span>
          <span class="discount">-¥{{ order.discountAmount?.toFixed(2) }}</span>
        </div>
        <div class="summary-row total">
          <span>实付金额：</span>
          <span class="amount">¥{{ order?.paidAmount?.toFixed(2) || '0.00' }}</span>
        </div>
        <div class="summary-row">
          <span>支付方式：</span>
          <span>{{ getPaymentMethodName(order?.paymentMethod) }}</span>
        </div>
      </div>
      
      <div class="receipt-divider">********************************</div>
      
      <div class="receipt-footer">
        <div class="thank-you">感谢您的光临，欢迎再次惠顾！</div>
        <div class="qrcode-hint">扫码查看电子小票</div>
        <div class="qrcode-placeholder" v-if="showQRCode">
          <div class="qrcode-box">
            <svg viewBox="0 0 100 100" class="qrcode-svg">
              <rect x="0" y="0" width="100" height="100" fill="#fff"/>
              <text x="50" y="55" text-anchor="middle" font-size="12" fill="#333">小票二维码</text>
            </svg>
          </div>
        </div>
      </div>
      
      <div class="receipt-time">
        <span>打印时间：{{ formatTime(new Date()) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  order: {
    type: Object,
    default: null
  },
  showQRCode: {
    type: Boolean,
    default: true
  }
})

const totalQuantity = computed(() => {
  if (!props.order?.items) return 0
  return props.order.items.reduce((sum, item) => sum + item.quantity, 0)
})

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

const getPaymentMethodName = (method) => {
  const map = { 1: '现金', 2: '微信', 3: '支付宝', 4: '银行卡', 5: '刷脸支付' }
  return map[method] || '-'
}

defineExpose({})
</script>

<style scoped>
.receipt-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px;
  background: #f5f5f5;
}

.receipt-container {
  width: 280px;
  background: #fff;
  padding: 20px 16px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  color: #333;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.receipt-header {
  text-align: center;
  padding-bottom: 12px;
}

.store-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}

.store-subtitle {
  font-size: 14px;
  color: #666;
}

.receipt-divider {
  text-align: center;
  color: #999;
  margin: 8px 0;
  font-size: 10px;
  letter-spacing: 1px;
}

.receipt-info {
  padding: 8px 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 11px;
}

.receipt-items {
  padding: 8px 0;
}

.items-header {
  display: flex;
  font-weight: bold;
  font-size: 11px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ddd;
}

.items-body {
  padding-top: 6px;
}

.item-row {
  display: flex;
  font-size: 10px;
  margin-bottom: 4px;
  line-height: 1.4;
}

.col-name {
  width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-qty {
  width: 15%;
  text-align: center;
}

.col-price {
  width: 20%;
  text-align: right;
}

.col-total {
  width: 20%;
  text-align: right;
  font-weight: 500;
}

.receipt-summary {
  padding: 8px 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 11px;
}

.summary-row.total {
  font-size: 14px;
  font-weight: bold;
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px dashed #ddd;
}

.summary-row .discount {
  color: #52c41a;
}

.summary-row .amount {
  color: #f5222d;
  font-size: 16px;
}

.receipt-footer {
  text-align: center;
  padding: 12px 0;
}

.thank-you {
  font-size: 12px;
  margin-bottom: 8px;
}

.qrcode-hint {
  font-size: 10px;
  color: #999;
  margin-bottom: 8px;
}

.qrcode-placeholder {
  display: flex;
  justify-content: center;
}

.qrcode-box {
  width: 80px;
  height: 80px;
  border: 1px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qrcode-svg {
  width: 100%;
  height: 100%;
}

.receipt-time {
  text-align: center;
  font-size: 10px;
  color: #999;
  margin-top: 8px;
}
</style>