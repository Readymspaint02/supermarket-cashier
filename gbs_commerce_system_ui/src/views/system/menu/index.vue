<template>
  <div>
    <!-- 操作按钮 -->
    <div style="margin-bottom: 20px;">
      <el-button type="primary" @click="handleAdd">新增菜单</el-button>
    </div>

    <!-- 菜单列表表格 -->
    <el-table 
      :data="menuList" 
      style="width: 100%" 
      v-loading="loading"
      row-key="id"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      border
      default-expand-all
    >
      <el-table-column prop="menuName" label="菜单名称" width="150" />
      <el-table-column prop="icon" label="图标" width="80">
        <template #default="scope">
          <el-icon v-if="scope.row.icon!='#'">
            <component :is="scope.row.icon" />
          </el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" width="150" />
      <el-table-column prop="component" label="组件路径" width="200" />
      <el-table-column prop="menuType" label="类型" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.menuType === 'M'">目录</el-tag>
          <el-tag v-else-if="scope.row.menuType === 'C'" type="success">菜单</el-tag>
          <el-tag v-else-if="scope.row.menuType === 'F'" type="warning">按钮</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="perms" label="权限标识" width="150" />
      <el-table-column prop="orderNum" label="排序" width="80" />
      <el-table-column prop="visible" label="状态" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.visible === 0" type="success">显示</el-tag>
          <el-tag v-else type="danger">隐藏</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleAddChild(scope.row)" v-if="scope.row.menuType != 'F'">新增</el-button>
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 菜单编辑/新增对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="menuOptions"
                node-key="id"
                :props="{ label: 'menuName', children: 'children' }"
                value-key="id"
                check-strictly
                default-expand-all
                render-after-expand
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="form.menuType" @change="handleMenuTypeChange">
                <el-radio label="M">目录</el-radio>
                <el-radio label="C">菜单</el-radio>
                <el-radio label="F">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="菜单状态" prop="visible">
              <el-radio-group v-model="form.visible">
                <el-radio :label="0">显示</el-radio>
                <el-radio :label="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" />
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          
          <el-col :span="12" v-if="form.menuType !== 'F'">
            <el-form-item label="路由路径" prop="path">
              <el-input v-model="form.path" placeholder="请输入路由路径" />
            </el-form-item>
          </el-col>
          
          <el-col :span="12" v-if="form.menuType === 'C'">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="请输入组件路径" />
            </el-form-item>
          </el-col>
          
          <el-col :span="12" v-if="form.menuType !== 'F'">
            <el-form-item label="菜单图标" prop="icon">
              <el-input v-model="form.icon" placeholder="请输入图标名称" />
            </el-form-item>
          </el-col>
          
          <el-col :span="12" v-if="form.menuType === 'F'">
            <el-form-item label="权限标识" prop="perms">
              <el-input v-model="form.perms" placeholder="请输入权限标识" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getAllMenuList,
  getMenuInfo,
  createMenu,
  updateMenu,
  deleteMenu
} from '@/api/modules/menu'

const menuList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const isAdd = ref(true)
const editingMenuId = ref(null)

// 表单数据
const form = ref({
  parentId: 0,
  menuName: '',
  icon: '',
  path: '',
  component: '',
  menuType: 'M',
  perms: '',
  orderNum: 0,
  visible: 0
})

// 表单验证规则
const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  component: [{ required: true, message: '请输入组件路径', trigger: 'blur' }],
  perms: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入显示排序', trigger: 'blur' }]
}

// 下拉框菜单选项
const menuOptions = computed(() => {
  const options = [{
    id: 0,
    menuName: '主类目',
    children: []
  }];
  
  const getMenuTree = (menus) => {
    return menus.map(menu => ({
      id: menu.id,
      menuName: menu.menuName,
      children: menu.children ? getMenuTree(menu.children) : []
    }));
  };
  
  options[0].children = getMenuTree(menuList.value);
  return options;
});

// 对话框标题
const dialogTitle = computed(() => {
  return isAdd.value ? '新增菜单' : '编辑菜单'
})

// 获取菜单列表
const fetchMenuList = async () => {
  loading.value = true
  try {
    const response = await getAllMenuList()
    if (response.code !== 200) {
      ElMessage.error('获取菜单列表失败')
      return
    }
    menuList.value = response.data
  } catch (error) {
    ElMessage.error('获取菜单列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 处理菜单类型切换
const handleMenuTypeChange = () => {
  // 清空不需要的字段
  if (form.value.menuType === 'M') {
    form.value.perms = ''
  } else if (form.value.menuType === 'C') {
    form.value.perms = ''
  } else if (form.value.menuType === 'F') {
    form.value.path = ''
    form.value.component = ''
    form.value.icon = ''
  }
}

// 新增菜单
const handleAdd = () => {
  isAdd.value = true
  form.value = {
    parentId: 0,
    menuName: '',
    icon: '',
    path: '',
    component: '',
    menuType: 'M',
    perms: '',
    orderNum: 0,
    visible: 0
  }
  dialogVisible.value = true
}

// 新增子菜单
const handleAddChild = (row) => {
  isAdd.value = true
  form.value = {
    parentId: row.id,
    menuName: '',
    icon: '',
    path: '',
    component: '',
    menuType: 'M',
    perms: '',
    orderNum: 0,
    visible: 0
  }
  dialogVisible.value = true
}

// 编辑菜单
const handleEdit = async (row) => {
  isAdd.value = false
  editingMenuId.value = row.id
  
  try {
    const response = await getMenuInfo(row.id)
    if (response.code !== 200) {
      ElMessage.error('获取菜单信息失败')
      return
    }
    
    form.value = {
      ...response.data
    }
  } catch (error) {
    ElMessage.error('获取菜单信息失败')
    console.error(error)
    return
  }
  
  dialogVisible.value = true
}

// 删除菜单
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除"${row.menuName}"菜单吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await deleteMenu(row.id)
      if (response.code === 200) {
        ElMessage.success('删除成功')
        fetchMenuList() // 重新加载数据
      } else {
        ElMessage.error('删除失败: ' + response.msg)
      }
    } catch (error) {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let response
        if (isAdd.value) {
          // 新增菜单
          response = await createMenu(form.value)
          if (response.code === 200) {
            ElMessage.success('新增成功')
          } else {
            ElMessage.error('新增失败: ' + response.msg)
            return
          }
        } else {
          // 编辑菜单
          response = await updateMenu(editingMenuId.value, form.value)
          if (response.code === 200) {
            ElMessage.success('更新成功')
          } else {
            ElMessage.error('更新失败: ' + response.msg)
            return
          }
        }
        dialogVisible.value = false
        fetchMenuList() // 重新加载数据
      } catch (error) {
        ElMessage.error(isAdd.value ? '新增失败' : '更新失败')
        console.error(error)
      }
    }
  })
}

onMounted(() => {
  fetchMenuList()
})
</script>

<style scoped>
/* 可以添加一些自定义样式 */
</style>