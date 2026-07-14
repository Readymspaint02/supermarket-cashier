<template>
  <div class="receipt-page">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>正在加载小票...</p>
    </div>
    
    <div v-else-if="error" class="error-container">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="fetchOrder">重新加载</el-button>
        </template>
      </el-result>
    </div>
    
    <Receipt v-else :order="order" :showQRCode="true" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import Receipt from '@/components/Receipt.vue'
import http from '@/api/request'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const error = ref('')

const fetchOrder = async () => {
  const orderNo = route.params.orderNo
  if (!orderNo) {
    error.value = '订单号无效'
    loading.value = false
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const res = await http.get(`/public/order/${orderNo}`)
    if (res.code === 200) {
      order.value = res.data
    } else {
      error.value = res.msg || '订单不存在'
    }
  } catch (err) {
    error.value = err?.msg || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.receipt-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.loading-container p {
  margin-top: 16px;
  color: #666;
}
</style>
