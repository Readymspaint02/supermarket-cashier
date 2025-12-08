<template>
  <div class="stock-out-container">
    <!-- 页面标题 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">商品出库</span>
        </div>
      </template>

      <!-- 出库表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        style="max-width: 600px"
      >
        <el-form-item label="选择商品" prop="productId">
          <el-select
            v-model="formData.productId"
            placeholder="请选择商品"
            filterable
            @change="handleProductChange"
            style="width: 100%"
          >
            <el-option
              v-for="item in productList"
              :key="item.id"
              :label="`${item.productName} (${item.productCode})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <!-- 当前库存信息 -->
        <el-form-item label="当前库存" v-if="currentInventory">
          <el-tag type="info" size="large">
            {{ currentInventory.stockQuantity }} {{ selectedProduct?.unit || '件' }}
          </el-tag>
          <el-tag
            :type="currentInventory.stockQuantity > currentInventory.warningQuantity ? 'success' : 'warning'"
            size="large"
            style="margin-left: 10px"
          >
            {{ currentInventory.stockQuantity > currentInventory.warningQuantity ? '库存充足' : '库存预警' }}
          </el-tag>
        </el-form-item>

        <el-form-item label="出库数量" prop="quantity">
          <el-input-number
            v-model="formData.quantity"
            :min="1"
            :max="currentInventory?.stockQuantity || 99999"
            controls-position="right"
            style="width: 100%"
          />
          <span style="margin-left: 10px; color: #909399">
            {{ selectedProduct?.unit || '件' }}
          </span>
          <div v-if="currentInventory && formData.quantity > currentInventory.stockQuantity" class="error-tip">
            ⚠️ 出库数量不能大于当前库存
          </div>
        </el-form-item>

        <el-form-item label="出库后数量" v-if="currentInventory">
          <el-tag
            :type="(currentInventory.stockQuantity - (formData.quantity || 0)) > 0 ? 'success' : 'danger'"
            size="large"
          >
            {{ currentInventory.stockQuantity - (formData.quantity || 0) }} {{ selectedProduct?.unit || '件' }}
          </el-tag>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注（如：损耗、领用、退货等）"
            maxlength="500"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="danger" @click="handleSubmit" :loading="submitLoading">
            <el-icon><Check /></el-icon>
            确认出库
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 出库记录表格 -->
      <el-divider>最近出库记录</el-divider>
      <el-table
        :data="logList"
        style="width: 100%"
        border
        v-loading="loading"
      >
        <el-table-column prop="productName" label="商品名称" min-width="150">
          <template #default="{ row }">
            {{ row.product?.productName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="changeQuantity" label="出库数量" width="120" align="right">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.changeQuantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="beforeQuantity" label="出库前" width="100" align="right" />
        <el-table-column prop="afterQuantity" label="出库后" width="100" align="right" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="createTime" label="出库时间" width="180" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>

      <!-- 分页组件 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Check, Refresh } from '@element-plus/icons-vue';
import { getProductList } from '@/api/modules/product';
import {
  getInventoryByProductId,
  stockOut,
  getInventoryLogPage
} from '@/api/modules/inventory';

// ==================== 数据定义 ====================
const loading = ref(false);
const submitLoading = ref(false);
const formRef = ref(null);
const productList = ref([]);
const currentInventory = ref(null);
const selectedProduct = ref(null);
const logList = ref([]);

// 表单数据
const formData = reactive({
  productId: null,
  quantity: 1,
  remark: ''
});

// 表单验证规则
const rules = {
  productId: [
    { required: true, message: '请选择商品', trigger: 'change' }
  ],
  quantity: [
    { required: true, message: '请输入出库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (currentInventory.value && value > currentInventory.value.stockQuantity) {
          callback(new Error('出库数量不能大于当前库存'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 分页数据
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

// ==================== 生命周期 ====================
onMounted(() => {
  loadProductList();
  loadLogList();
});

// ==================== 方法定义 ====================

/**
 * 加载商品列表
 */
const loadProductList = async () => {
  try {
    const res = await getProductList();
    if (res.code === 200) {
      productList.value = (res.data || []).filter(item => item.status === 0);
    }
  } catch (error) {
    console.error('加载商品列表失败', error);
  }
};

/**
 * 商品选择改变
 */
const handleProductChange = async (productId) => {
  if (!productId) {
    currentInventory.value = null;
    selectedProduct.value = null;
    return;
  }

  // 获取选中的商品信息
  selectedProduct.value = productList.value.find(item => item.id === productId);

  // 查询当前库存
  try {
    const res = await getInventoryByProductId(productId);
    if (res.code === 200) {
      currentInventory.value = res.data;
    }
  } catch (error) {
    console.error('查询库存失败', error);
  }
};

/**
 * 加载出库记录
 */
const loadLogList = async () => {
  try {
    loading.value = true;
    const res = await getInventoryLogPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      queryParams: {
        changeType: 2 // 2-出库
      }
    });
    if (res.code === 200) {
      logList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    }
  } catch (error) {
    console.error('加载记录失败', error);
  } finally {
    loading.value = false;
  }
};

/**
 * 提交出库
 */
const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  // 再次检查库存
  if (formData.quantity > currentInventory.value.stockQuantity) {
    ElMessage.error('出库数量不能大于当前库存');
    return;
  }

  ElMessageBox.confirm(
    `确认出库 ${formData.quantity} ${selectedProduct.value?.unit || '件'} 吗？`,
    '出库确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        submitLoading.value = true;
        const res = await stockOut(formData);
        if (res.code === 200) {
          ElMessage.success('出库成功');
          handleReset();
          loadLogList();
        }
      } catch (error) {
        console.error('出库失败', error);
      } finally {
        submitLoading.value = false;
      }
    })
    .catch(() => {});
};

/**
 * 重置表单
 */
const handleReset = () => {
  Object.assign(formData, {
    productId: null,
    quantity: 1,
    remark: ''
  });
  currentInventory.value = null;
  selectedProduct.value = null;
  formRef.value?.clearValidate();
};

/**
 * 分页大小改变
 */
const handleSizeChange = (size) => {
  pagination.pageSize = size;
  loadLogList();
};

/**
 * 当前页改变
 */
const handleCurrentChange = (page) => {
  pagination.pageNum = page;
  loadLogList();
};
</script>

<style scoped>
.stock-out-container {
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

.box-card {
  min-height: calc(100vh - 120px);
}

.error-tip {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}
</style>

