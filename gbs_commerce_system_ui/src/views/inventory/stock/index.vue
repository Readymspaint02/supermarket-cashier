<template>
  <div class="inventory-container">
    <!-- 页面标题 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">库存查询</span>
          <el-button type="warning" @click="loadWarningInventories">
            <el-icon><Warning /></el-icon>
            查看库存预警
          </el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商品名称">
          <el-input
            v-model="searchForm.productName"
            placeholder="请输入商品名称"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="是否预警">
          <el-select
            v-model="searchForm.warning"
            placeholder="请选择"
            clearable
            @clear="handleSearch"
          >
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        style="width: 100%"
        border
        v-loading="loading"
      >
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="productName" label="商品名称" min-width="200">
          <template #default="{ row }">
            {{ row.product?.productName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="商品编码" width="150">
          <template #default="{ row }">
            {{ row.product?.productCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="库存数量" width="120" align="right">
          <template #default="{ row }">
            <span :class="{ 'warning-text': row.stockQuantity <= row.warningQuantity }">
              {{ row.stockQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="warningQuantity" label="预警数量" width="120" align="right" />
        <el-table-column prop="status" label="库存状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.stockQuantity > row.warningQuantity ? 'success' : 'danger'"
            >
              {{ row.stockQuantity > row.warningQuantity ? '充足' : '预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 库存预警对话框 -->
    <el-dialog title="库存预警列表" v-model="warningDialogVisible" width="800px">
      <el-table :data="warningList" border>
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="productName" label="商品名称" min-width="200">
          <template #default="{ row }">
            {{ row.product?.productName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="库存数量" width="120" align="right">
          <template #default="{ row }">
            <span class="warning-text">{{ row.stockQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warningQuantity" label="预警数量" width="120" align="right" />
      </el-table>
      <div v-if="warningList.length === 0" class="empty-warning">
        暂无库存预警商品
      </div>
    </el-dialog>

    <!-- 库存详情对话框 -->
    <el-dialog title="库存详情" v-model="detailDialogVisible" width="600px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="商品ID">
          {{ currentRow.productId }}
        </el-descriptions-item>
        <el-descriptions-item label="商品名称">
          {{ currentRow.product?.productName }}
        </el-descriptions-item>
        <el-descriptions-item label="商品编码">
          {{ currentRow.product?.productCode }}
        </el-descriptions-item>
        <el-descriptions-item label="库存数量">
          <span :class="{ 'warning-text': currentRow.stockQuantity <= currentRow.warningQuantity }">
            {{ currentRow.stockQuantity }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="预警数量">
          {{ currentRow.warningQuantity }}
        </el-descriptions-item>
        <el-descriptions-item label="库存状态">
          <el-tag
            :type="currentRow.stockQuantity > currentRow.warningQuantity ? 'success' : 'danger'"
          >
            {{ currentRow.stockQuantity > currentRow.warningQuantity ? '充足' : '预警' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">
          {{ currentRow.updateTime }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Warning } from '@element-plus/icons-vue';
import {
  getInventoryPage,
  getWarningInventories
} from '@/api/modules/inventory';

// ==================== 数据定义 ====================
const loading = ref(false);
const tableData = ref([]);
const warningList = ref([]);
const warningDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const currentRow = ref(null);

// 搜索表单
const searchForm = reactive({
  productName: '',
  warning: null
});

// 分页数据
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

// ==================== 生命周期 ====================
onMounted(() => {
  loadTableData();
});

// ==================== 方法定义 ====================

/**
 * 加载表格数据
 */
const loadTableData = async () => {
  try {
    loading.value = true;
    const res = await getInventoryPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      queryParams: searchForm
    });
    if (res.code === 200) {
      tableData.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    }
  } catch (error) {
    console.error('加载数据失败', error);
  } finally {
    loading.value = false;
  }
};

/**
 * 加载库存预警列表
 */
const loadWarningInventories = async () => {
  try {
    const res = await getWarningInventories();
    if (res.code === 200) {
      warningList.value = res.data || [];
      warningDialogVisible.value = true;
      if (warningList.value.length > 0) {
        ElMessage.warning(`当前有 ${res.count} 个商品库存预警`);
      } else {
        ElMessage.success('暂无库存预警商品');
      }
    }
  } catch (error) {
    console.error('加载预警列表失败', error);
  }
};

/**
 * 查看详情
 */
const viewDetail = (row) => {
  currentRow.value = row;
  detailDialogVisible.value = true;
};

/**
 * 搜索
 */
const handleSearch = () => {
  pagination.pageNum = 1;
  loadTableData();
};

/**
 * 重置搜索
 */
const handleReset = () => {
  Object.assign(searchForm, {
    productName: '',
    warning: null
  });
  handleSearch();
};

/**
 * 分页大小改变
 */
const handleSizeChange = (size) => {
  pagination.pageSize = size;
  loadTableData();
};

/**
 * 当前页改变
 */
const handleCurrentChange = (page) => {
  pagination.pageNum = page;
  loadTableData();
};
</script>

<style scoped>
.inventory-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.search-form {
  margin-bottom: 20px;
}

.box-card {
  min-height: calc(100vh - 120px);
}

.warning-text {
  color: #f56c6c;
  font-weight: bold;
}

.empty-warning {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 14px;
}
</style>

