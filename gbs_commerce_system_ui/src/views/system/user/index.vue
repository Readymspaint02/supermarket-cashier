<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div v-if="isSuperAdmin">
            <el-button type="primary" @click="handleAdd">新增用户</el-button>
          </div>
        </div>
      </template>

      <!-- 用户搜索 -->
      <el-form :inline="true" :model="searchForm" class="demo-form-inline" v-if="isSuperAdmin">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="用户名"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户表格 -->
      <el-table :data="userList" style="width: 100%" v-loading="loading">
        <el-table-column prop="username" label="用户名"></el-table-column>
        <el-table-column prop="avatar" label="头像">
          <template #default="scope">
            <img :src="scope.row.avatar?'/api'+scope.row.avatar:defaultAvatar" style="width: 50px;height: 50px;"></img>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱"></el-table-column>
        <el-table-column prop="role" label="角色">
          <template #default="scope">
            <el-tag v-for="role in scope.row.roles" :key="role.id" :type="role.roleKey === 'admin' ? '' : 'info'" style="margin-right: 5px;">
              {{ role.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"></el-table-column>
        <el-table-column label="操作" width="200" v-if="isSuperAdmin">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button 
              size="small" 
              type="danger" 
              :disabled="scope.row.id === currentUser.id"
              @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="isSuperAdmin"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        style="margin-top: 20px; text-align: right;">
      </el-pagination>
    </el-card>

    <!-- 用户对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="userForm" :rules="userFormRules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!userForm.id"></el-input>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
            <el-upload
              class="avatar-uploader"
              action="/api/system/file/upload"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              :headers="uploadHeaders"
            >
              <img v-if="userForm.avatar" :src="'/api'+userForm.avatar" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="roles" v-if="isSuperAdmin">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色">
            <el-option
              v-for="item in roleList"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!userForm.id">
          <el-input v-model="userForm.password" type="password"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUserForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, getCurrentUserInfo, createUser, updateUser, deleteUser } from '@/api/modules/user'
import { getRoleAll } from '@/api/modules/role'
import defaultAvatar from '@/assets/anon.jpeg'
import { Plus } from '@element-plus/icons-vue'
// 用户数据
const userList = ref([])
const loading = ref(false)
const currentUser = ref({})
const roleList=ref([])

// 分页参数
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 搜索表单
const searchForm = reactive({
  username: ''
})

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const userForm = reactive({
  id: '',
  username: '',
  avatar:'',
  email: '',
  roleIds: [],
  password: ''
})

// 表单引用
const userFormRef = ref(null)

// 表单验证规则
const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  roleIds: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}
const uploadHeaders = {
  Authorization:'Bearer ' + localStorage.getItem('token')
}
const handleAvatarSuccess= (response,uploadFile
) => {
  if (response.code !== 200) {
    ElMessage.error(response.msg||'头像上传失败')
    return false
  }
  userForm.avatar = response.data
  ElMessage.success('头像上传成功')  
}

const beforeAvatarUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg'&&rawFile.type !== 'image/png') {
    ElMessage.error('头像必须是 JPG 或 PNG 格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}
// 判断是否为超级管理员
const isSuperAdmin = computed(() => {
  // 根据用户数据结构判断是否为超级管理员
  if (currentUser.value.roles && currentUser.value.roles.length > 0) {
    return currentUser.value.roles.some(role => role.roleKey === 'admin');
  }
  return false;
})
const loadRoleList = async () => { 
  if (isSuperAdmin.value) {
    try{
      loading.value = true
      const res=await getRoleAll()
      roleList.value = res.data
    }
    catch(err){
      console.error(err)
    }
    finally{
      loading.value = false
    }
  }
}
// 获取用户列表
const loadUserList = async () => {
  if (!isSuperAdmin.value) {
    // 普通用户只获取自己的信息
    try {
      loading.value = true
      const res = await getCurrentUserInfo()
      if (res.code === 200) {
        userList.value = [res.data]
      }
    } catch (err) {
      console.error(err)
    } finally {
      loading.value = false
    }
  } else {
    // 超级管理员获取所有用户
    try {
      loading.value = true
      const params = {
        pageNum: pagination.currentPage,
        pageSize: pagination.pageSize,
        queryParams:searchForm
      }
      const res = await getUserPage(params)
      if (res.code === 200) {
        // console.log(res)
        userList.value = res.data.records
        pagination.total = res.data.total
      }
    } catch (err) {
      console.error(err)
    } finally {
      loading.value = false
    }
  }
}

// 获取当前用户信息
const getCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
}

// 处理搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadUserList()
}

// 处理重置
const handleReset = () => {
  searchForm.username = ''
  pagination.currentPage = 1
  loadUserList()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.currentPage = 1
  loadUserList()
}

// 处理当前页变化
const handleCurrentChange = (val) => {
  pagination.currentPage = val
  loadUserList()
}

// 新增用户
const handleAdd = () => {
  dialogTitle.value = '新增用户'
  Object.assign(userForm, {
    id: '',
    username: '',
    avatar: '',
    email: '',
    roleIds: [],
    password: ''
  })
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    ...row,
    roleIds: row.roles ? row.roles.map(role => role.id) : []
  })
  // 清空密码字段，编辑时不强制修改密码
  userForm.password = ''
  dialogVisible.value = true
}

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确认要删除用户"${row.username}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteUser(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadUserList()
      }
    } catch (err) {
      console.error(err)
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 提交表单
const submitUserForm = () => {
  userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 处理角色数据，将角色key转换为角色对象
        const formData = {
          ...userForm,
          roles: userForm.roleIds.map(key => ({id:key}))
        };

        let res
        if (userForm.id) {
          // 更新用户
          const { id, ...updateData } = formData
          // 如果密码为空，则不更新密码
          if (!updateData.password) {
            delete updateData.password
          }
          res = await updateUser(id,updateData)
        } else {
          // 创建用户
          res = await createUser(formData)
        }

        if (res.code === 200) {
          ElMessage.success(userForm.id ? '更新成功' : '创建成功')
          dialogVisible.value = false
          loadUserList()
        }
      } catch (err) {
        console.error(err)
      }
    }
  })
}

// 组件挂载时执行
onMounted(() => {
  getCurrentUser()
  loadUserList()
  loadRoleList();
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.demo-form-inline {
  margin-bottom: 20px;
}
.avatar-uploader .avatar {
  width: 80px;
  height: 80px;
  display: block;
}
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 80px;
  height: 80px;
  text-align: center;
}
</style>