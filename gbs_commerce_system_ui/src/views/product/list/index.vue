<template>
  <div class="product-container">
    <!-- 页面标题和操作区 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">商品信息管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
            v-if="hasPermission('product:info:add')"
          >
            <el-icon><Plus /></el-icon>
            新增商品
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
        <el-form-item label="商品编码">
          <el-input
            v-model="searchForm.productCode"
            placeholder="请输入商品编码"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="商品分类">
          <el-tree-select
            v-model="searchForm.categoryId"
            :data="categoryTree"
            placeholder="请选择商品分类"
            clearable
            :props="{ label: 'categoryName', value: 'id' }"
            style="width: 200px"
            @clear="handleSearch"
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
        <el-table-column prop="id" label="商品ID" width="80" />
        <el-table-column label="商品图片" width="100" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.productImage"
              :src="row.productImage"
              :preview-src-list="[row.productImage]"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px"
            />
            <span v-else>暂无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="productCode" label="商品编码" width="150" />
        <el-table-column prop="price" label="售价" width="100" align="right">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
              v-if="hasPermission('product:info:edit')"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
              v-if="hasPermission('product:info:delete')"
            >
              删除
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="productName">
              <el-input
                v-model="formData.productName"
                placeholder="请输入商品名称"
                maxlength="100"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品编码" prop="productCode">
              <el-input
                v-model="formData.productCode"
                placeholder="请输入商品编码/条形码"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-tree-select
                v-model="formData.categoryId"
                :data="categoryTree"
                placeholder="请选择商品分类"
                :props="{ label: 'categoryName', value: 'id' }"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input
                v-model="formData.unit"
                placeholder="请输入单位（如：件、瓶、包）"
                maxlength="20"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="售价" prop="price">
              <el-input-number
                v-model="formData.price"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成本价" prop="costPrice">
              <el-input-number
                v-model="formData.costPrice"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品图片" prop="productImage">
          <el-upload
            class="avatar-uploader"
            action="/api/product/uploadImage"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
          >
            <img
              v-if="formData.productImage"
              :src="formData.productImage"
              class="avatar"
            />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">支持jpg/png格式，大小不超过2MB</div>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">下架</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import {
  getProductPage,
  addProduct,
  updateProduct,
  deleteProduct
} from '@/api/modules/product';
import { getCategoryTree } from '@/api/modules/productCategory';

// ==================== 数据定义 ====================
const loading = ref(false);
const tableData = ref([]);
const categoryTree = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('');
const submitLoading = ref(false);
const formRef = ref(null);
const token = ref(localStorage.getItem('token') || '');

// 上传headers（计算属性，自动响应token变化）
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + token.value
}));

// 搜索表单
const searchForm = reactive({
  productName: '',
  productCode: '',
  categoryId: null
});

// 分页数据
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

// 表单数据
const formData = reactive({
  id: null,
  productName: '',
  productCode: '',
  categoryId: null,
  price: 0,
  costPrice: 0,
  unit: '件',
  productImage: '',
  description: '',
  status: 0,
  remark: ''
});

// 表单验证规则
const rules = {
  productName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' }
  ],
  productCode: [
    { required: true, message: '请输入商品编码', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' }
  ],
  unit: [
    { required: true, message: '请输入单位', trigger: 'blur' }
  ]
};

// ==================== 权限控制 ====================
const hasPermission = (permission) => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const permissions = userInfo.permissions || [];
  return permissions.includes(permission) || permissions.includes('*:*:*');
};

// ==================== 生命周期 ====================
onMounted(() => {
  loadCategoryTree();
  loadTableData();
});

// ==================== 方法定义 ====================

/**
 * 加载分类树
 */
const loadCategoryTree = async () => {
  try {
    const res = await getCategoryTree();
    if (res.code === 200) {
      categoryTree.value = res.data || [];
    }
  } catch (error) {
    console.error('加载分类树失败', error);
  }
};

/**
 * 加载表格数据
 */
const loadTableData = async () => {
  try {
    loading.value = true;
    const res = await getProductPage({
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
    productCode: '',
    categoryId: null
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

/**
 * 打开新增对话框
 */
const handleAdd = () => {
  dialogTitle.value = '新增商品';
  resetForm();
  dialogVisible.value = true;
};

/**
 * 打开编辑对话框
 */
const handleEdit = (row) => {
  dialogTitle.value = '编辑商品';
  Object.assign(formData, row);
  dialogVisible.value = true;
};

/**
 * 删除商品
 */
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除商品"${row.productName}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        const res = await deleteProduct(row.id);
        if (res.code === 200) {
          ElMessage.success('删除成功');
          loadTableData();
        }
      } catch (error) {
        console.error('删除失败', error);
      }
    })
    .catch(() => {});
};

/**
 * 图片上传成功
 */
const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    formData.productImage = response.data;
    ElMessage.success('图片上传成功');
  }
};

/**
 * 上传前校验
 */
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isImage) {
    ElMessage.error('只能上传图片文件！');
    return false;
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB！');
    return false;
  }
  return true;
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  try {
    submitLoading.value = true;
    let res;
    if (formData.id) {
      res = await updateProduct(formData.id, formData);
    } else {
      res = await addProduct(formData);
    }

    if (res.code === 200) {
      ElMessage.success(formData.id ? '编辑成功' : '新增成功');
      dialogVisible.value = false;
      loadTableData();
    }
  } catch (error) {
    console.error('提交失败', error);
  } finally {
    submitLoading.value = false;
  }
};

/**
 * 重置表单
 */
const resetForm = () => {
  Object.assign(formData, {
    id: null,
    productName: '',
    productCode: '',
    categoryId: null,
    price: 0,
    costPrice: 0,
    unit: '件',
    productImage: '',
    description: '',
    status: 0,
    remark: ''
  });
  formRef.value?.clearValidate();
};
</script>

<style scoped>
.product-container {
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

.avatar-uploader {
  width: 150px;
  height: 150px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 150px;
  height: 150px;
  text-align: center;
  line-height: 150px;
}

.avatar {
  width: 150px;
  height: 150px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>

