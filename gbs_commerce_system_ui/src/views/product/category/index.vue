<template>
  <div class="category-container">
    <!-- 页面标题和操作区 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">商品分类管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
            v-if="hasPermission('product:category:add')"
          >
            <el-icon><Plus /></el-icon>
            新增分类
          </el-button>
        </div>
      </template>

      <!-- 树形表格 -->
      <el-table
        :data="categoryTree"
        style="width: 100%"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        v-loading="loading"
      >
        <el-table-column prop="id" label="分类ID" width="100" />
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
              v-if="hasPermission('product:category:edit')"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
              v-if="hasPermission('product:category:delete')"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="分类名称" prop="categoryName">
          <el-input
            v-model="formData.categoryName"
            placeholder="请输入分类名称"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="父级分类" prop="parentId">
          <el-tree-select
            v-model="formData.parentId"
            :data="categoryTreeSelect"
            :render-after-expand="false"
            placeholder="请选择父级分类（不选则为顶级分类）"
            check-strictly
            :props="{ label: 'categoryName', value: 'id' }"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="formData.sortOrder"
            :min="0"
            :max="999"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
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
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import {
  getCategoryTree,
  addCategory,
  updateCategory,
  deleteCategory
} from '@/api/modules/productCategory';

// ==================== 数据定义 ====================
const loading = ref(false);
const categoryTree = ref([]);
const categoryTreeSelect = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('');
const submitLoading = ref(false);
const formRef = ref(null);

// 表单数据
const formData = reactive({
  id: null,
  categoryName: '',
  parentId: 0,
  sortOrder: 0,
  status: 0,
  remark: ''
});

// 表单验证规则
const rules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  sortOrder: [
    { required: true, message: '请输入排序', trigger: 'blur' }
  ]
};

// ==================== 权限控制 ====================
const hasPermission = (permission) => {
  // 从localStorage获取用户权限列表
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const permissions = userInfo.permissions || [];
  return permissions.includes(permission) || permissions.includes('*:*:*');
};

// ==================== 生命周期 ====================
onMounted(() => {
  loadCategoryTree();
});

// ==================== 方法定义 ====================

/**
 * 加载分类树
 */
const loadCategoryTree = async () => {
  try {
    loading.value = true;
    const res = await getCategoryTree();
    if (res.code === 200) {
      categoryTree.value = res.data || [];
      // 构建树形选择器数据（添加顶级选项）
      categoryTreeSelect.value = [
        { id: 0, categoryName: '顶级分类', children: res.data || [] }
      ];
    }
  } catch (error) {
    console.error('加载分类树失败', error);
  } finally {
    loading.value = false;
  }
};

/**
 * 打开新增对话框
 */
const handleAdd = () => {
  dialogTitle.value = '新增分类';
  resetForm();
  dialogVisible.value = true;
};

/**
 * 打开编辑对话框
 */
const handleEdit = (row) => {
  dialogTitle.value = '编辑分类';
  Object.assign(formData, row);
  dialogVisible.value = true;
};

/**
 * 删除分类
 */
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除分类"${row.categoryName}"吗？如果有子分类或关联商品将无法删除。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        const res = await deleteCategory(row.id);
        if (res.code === 200) {
          ElMessage.success('删除成功');
          loadCategoryTree();
        }
      } catch (error) {
        console.error('删除失败', error);
      }
    })
    .catch(() => {
      // 取消删除
    });
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
  // 验证表单
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  try {
    submitLoading.value = true;
    let res;
    if (formData.id) {
      // 编辑
      res = await updateCategory(formData.id, formData);
    } else {
      // 新增
      res = await addCategory(formData);
    }

    if (res.code === 200) {
      ElMessage.success(formData.id ? '编辑成功' : '新增成功');
      dialogVisible.value = false;
      loadCategoryTree();
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
    categoryName: '',
    parentId: 0,
    sortOrder: 0,
    status: 0,
    remark: ''
  });
  formRef.value?.clearValidate();
};
</script>

<style scoped>
.category-container {
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
</style>

