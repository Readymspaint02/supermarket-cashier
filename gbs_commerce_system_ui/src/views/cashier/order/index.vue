<template>
  <div class="order-container">
    <!-- 页面标题 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">订单管理</span>
        </div>
      </template>

      <!-- 搜索区域 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input
            v-model="searchForm.orderNo"
            placeholder="请输入订单号"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select
            v-model="searchForm.orderStatus"
            placeholder="请选择订单状态"
            clearable
            @clear="handleSearch"
          >
            <el-option label="已完成" :value="1" />
            <el-option label="已退款" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select
            v-model="searchForm.paymentMethod"
            placeholder="请选择支付方式"
            clearable
            @clear="handleSearch"
          >
            <el-option label="现金" :value="1" />
            <el-option label="微信" :value="2" />
            <el-option label="支付宝" :value="3" />
            <el-option label="银行卡" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
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
        <el-table-column prop="id" label="订单ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="totalAmount" label="订单总额" width="120" align="right">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="discountAmount" label="优惠金额" width="120" align="right">
          <template #default="{ row }">
            ¥{{ row.discountAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="实付金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold">¥{{ row.paidAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100" align="center">
          <template #default="{ row }">
            {{ getPaymentMethodName(row.paymentMethod) }}
          </template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)">
              {{ getStatusName(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cashierName" label="收银员" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(row)"
              v-if="hasPermission('cashier:order:detail')"
            >
              查看
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleRefund(row)"
              v-if="hasPermission('cashier:order:refund') && row.orderStatus === 1"
            >
              退款
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

    <!-- 订单详情对话框 -->
    <el-dialog
      title="订单详情"
      v-model="detailDialogVisible"
      width="800px"
    >
      <div v-if="currentOrder">
        <!-- 订单基本信息 -->
        <el-descriptions title="订单信息" :column="2" border>
          <el-descriptions-item label="订单号">
            {{ currentOrder.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(currentOrder.orderStatus)">
              {{ getStatusName(currentOrder.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单总额">
            ¥{{ currentOrder.totalAmount }}
          </el-descriptions-item>
          <el-descriptions-item label="优惠金额">
            ¥{{ currentOrder.discountAmount }}
          </el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span style="color: #f56c6c; font-weight: bold">
              ¥{{ currentOrder.paidAmount }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ getPaymentMethodName(currentOrder.paymentMethod) }}
          </el-descriptions-item>
          <el-descriptions-item label="收银员">
            {{ currentOrder.cashierName }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ currentOrder.createTime }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ currentOrder.remark || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 订单明细 -->
        <el-divider content-position="left">订单明细</el-divider>
        <el-table
          :data="orderItems"
          border
          show-summary
          :summary-method="getOrderSummary"
        >
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="productName" label="商品名称" min-width="200" />
          <el-table-column prop="productCode" label="商品编码" width="150" />
          <el-table-column prop="price" label="单价" width="100" align="right">
            <template #default="{ row }">
              ¥{{ row.price }}
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="subtotal" label="小计" width="120" align="right">
            <template #default="{ row }">
              ¥{{ row.subtotal }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getOrderPage,
  getOrderById,
  getOrderItems,
  refundOrder
} from '@/api/modules/order';

// ==================== 数据定义 ====================
const loading = ref(false);
const tableData = ref([]);
const detailDialogVisible = ref(false);
const currentOrder = ref(null);
const orderItems = ref([]);
const dateRange = ref([]);

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  orderStatus: null,
  paymentMethod: null,
  startTime: '',
  endTime: ''
});

// 分页数据
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

// ==================== 权限控制 ====================
const hasPermission = (permission) => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const permissions = userInfo.permissions || [];
  return permissions.includes(permission) || permissions.includes('*:*:*');
};

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
    const res = await getOrderPage({
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
 * 查看详情
 */
const viewDetail = async (row) => {
  try {
    // 获取订单详情
    const orderRes = await getOrderById(row.id);
    if (orderRes.code === 200) {
      currentOrder.value = orderRes.data;
    }

    // 获取订单明细
    const itemsRes = await getOrderItems(row.id);
    if (itemsRes.code === 200) {
      orderItems.value = itemsRes.data || [];
    }

    detailDialogVisible.value = true;
  } catch (error) {
    console.error('加载订单详情失败', error);
  }
};

/**
 * 退款
 */
const handleRefund = (row) => {
  ElMessageBox.confirm(
    `确定要退款订单"${row.orderNo}"吗？退款后将恢复库存。`,
    '退款确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        const res = await refundOrder(row.id);
        if (res.code === 200) {
          ElMessage.success('退款成功');
          loadTableData();
        }
      } catch (error) {
        console.error('退款失败', error);
      }
    })
    .catch(() => {});
};

/**
 * 日期范围改变
 */
const handleDateChange = (value) => {
  if (value && value.length === 2) {
    searchForm.startTime = value[0];
    searchForm.endTime = value[1];
  } else {
    searchForm.startTime = '';
    searchForm.endTime = '';
  }
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
    orderNo: '',
    orderStatus: null,
    paymentMethod: null,
    startTime: '',
    endTime: ''
  });
  dateRange.value = [];
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

/**
 * 获取支付方式名称
 */
const getPaymentMethodName = (method) => {
  const map = {
    1: '现金',
    2: '微信',
    3: '支付宝',
    4: '银行卡'
  };
  return map[method] || '-';
};

/**
 * 获取订单状态名称
 */
const getStatusName = (status) => {
  const map = {
    1: '已完成',
    2: '已退款',
    3: '部分退款'
  };
  return map[status] || '-';
};

/**
 * 获取状态标签类型
 */
const getStatusType = (status) => {
  const map = {
    1: 'success',
    2: 'danger',
    3: 'warning'
  };
  return map[status] || 'info';
};

/**
 * 订单明细合计行
 */
const getOrderSummary = (param) => {
  const { columns, data } = param;
  const sums = [];
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计';
      return;
    }
    if (index === 4) {
      // 数量合计
      sums[index] = data.reduce((sum, item) => sum + item.quantity, 0);
      return;
    }
    if (index === 5) {
      // 小计合计
      const total = data.reduce((sum, item) => sum + parseFloat(item.subtotal), 0);
      sums[index] = `¥${total.toFixed(2)}`;
      return;
    }
    sums[index] = '';
  });
  return sums;
};
</script>

<style scoped>
.order-container {
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
</style>

