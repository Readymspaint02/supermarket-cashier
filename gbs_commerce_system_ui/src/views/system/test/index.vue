<template>
  <div>
    <!-- 操作按钮 -->
    <div style="margin-bottom: 20px;" v-if="hasPermission('system:test:add')">
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table :data="tableData" style="width: 100%" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="姓名" width="150" />
      <el-table-column prop="age" label="年龄" width="100" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="address" label="地址" />

      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)" v-if="hasPermission('system:test:edit')">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)" v-if="hasPermission('system:test:delete')">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑/新增对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="formData.age" :min="0" :max="150" style="width: 100%" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="formData.address" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" v-if="dialogType === 'add' ? hasPermission('system:test:add') : hasPermission('system:test:edit')">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed,onMounted} from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPermissions } from '@/api/modules/user'
import { el } from 'element-plus/es/locales.mjs'

// 表格数据
const tableData = ref([
  { id: 1, name: '张三', age: 25, email: 'zhangsan@example.com', address: '北京市朝阳区' },
  { id: 2, name: '李四', age: 30, email: 'lisi@example.com', address: '上海市浦东新区' },
  { id: 3, name: '王五', age: 28, email: 'wangwu@example.com', address: '广州市天河区' }
])

// 对话框相关
const dialogVisible = ref(false)
const dialogType = ref('add') // add 或 edit
const formRef = ref(null)
const isAdd = ref(true) // true为新增，false为编辑
const editingIndex = ref(-1) // 编辑项的索引
const userPermissions = ref([])
// 表单数据
const formData = reactive({
  id: '',
  name: '',
  age: null,
  email: '',
  address: ''
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

// 对话框标题
const dialogTitle = computed(() => {
  return isAdd.value ? '新增' : '编辑'
})
// 权限检查方法
const hasPermission = (permission) => {
  // 如果权限列表为空，则默认有权限（向后兼容）
  if (!userPermissions.value || userPermissions.value.length === 0) {
    return true
  }
  return userPermissions.value.includes(permission)
}
//加载用户权限
const loadUserPermissions = async () => {
  const res = await getUserPermissions()
  if (res.code === 200) {
    userPermissions.value = res.data||[]
    console.log('用户权限列表:', userPermissions.value)
  }else {
    ElMessage.error("获取用户权限失败")
  }
}
// 新增
const handleAdd = () => {
  isAdd.value = true
  // 重置表单数据
  Object.assign(formData, {
    id: '',
    name: '',
    age: null,
    email: '',
    address: ''
  })
  dialogType.value = 'add'
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isAdd.value = false
  dialogType.value = 'edit'
  // 查找编辑项的索引
  editingIndex.value = tableData.value.findIndex(item => item.id === row.id)
  // 填充表单数据
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
    if (!hasPermission('system:test:delete')) {
    ElMessage.warning('您没有删除权限')
    return
  }
  ElMessageBox.confirm(
    `确定要删除"${row.name}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 执行删除操作
    const index = tableData.value.findIndex(item => item.id === row.id)
    if (index !== -1) {
      tableData.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 提交表单
const submitForm = () => {
     // 再次检查权限
  if (dialogType.value === 'add' && !hasPermission('system:test:add')) {
    ElMessage.warning('您没有新增权限')
    return
  }
  
  if (dialogType.value === 'edit' && !hasPermission('system:test:edit')) {
    ElMessage.warning('您没有编辑权限')
    return
  }
  formRef.value.validate((valid) => {
    if (valid) {
      if (isAdd.value) {
        // 新增操作
        const newId = tableData.value.length > 0 
          ? Math.max(...tableData.value.map(item => item.id)) + 1 
          : 1
        
        tableData.value.push({
          id: newId,
          ...formData
        })
        ElMessage.success('新增成功')
      } else {
        // 编辑操作
        if (editingIndex.value !== -1) {
          tableData.value[editingIndex.value] = { ...formData }
          ElMessage.success('编辑成功')
        }
      }
      dialogVisible.value = false
    }
  })
}
onMounted(()=> {
  loadUserPermissions();
})
</script>

<style scoped>
/* 可以添加一些自定义样式 */
</style>