<template>
  <div class="report-alert">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存预警列表</span>
        </div>
      </template>
      <el-table :data="warningList" v-loading="loading">
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="productCode" label="条码" width="160" />
        <el-table-column prop="stockQuantity" label="当前库存" width="120" />
        <el-table-column prop="warningQuantity" label="预警阈值" width="120" />
        <el-table-column label="差值" width="120">
          <template #default="{ row }">
            {{ row.diff }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getInventoryWarnings } from '@/api/modules/report'

const warningList = ref([])
const loading = ref(false)

const loadWarnings = async () => {
  try {
    loading.value = true
    const res = await getInventoryWarnings()
    warningList.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadWarnings()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
