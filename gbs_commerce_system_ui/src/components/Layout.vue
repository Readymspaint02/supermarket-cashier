<template>
  <div class="common-layout">
    <el-container class="layout-container">
      <el-header class="layout-header">
        <div class="header-content">
          <span class="header-title">智慧收银系统</span>
          <div class="header-actions">
            <div class="user-info">
              <span class="welcome-text">您好，{{ getCurrentUserRole() }}：</span>
              <el-upload
              class="avatar-uploader"
              action="/api/system/user/updateAvatar"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              :headers="uploadHeaders"
>
              <el-avatar
                v-if="currentUser.avatar"
                :src="'/api' + currentUser.avatar"
                size="small"
              />
              <el-avatar
                v-else
                :src="defaultAvatar"
                size="small"
              />
              </el-upload>
              <span class="username">{{ currentUser.username }}</span>
            </div>
            <el-dropdown @command="handleUserCommand">
              <el-button size="small" type="primary">
                个人中心
                <el-icon class="el-icon--right">
                  <arrow-down />
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" class="layout-aside">
          <el-menu
            :default-active="route.path"
            router
            class="layout-menu"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"
            :default-openeds="['1']"
          >
            <template v-for="item in authMenuList" :key="item.id">
              <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
                <template #title>
                  <span>{{ item.menuName }}</span>
                </template>
                <el-menu-item
                  v-for="child in item.children"
                  :key="child.id"
                  :index="child.path"
                >
                  <span>{{ child.menuName }}</span>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="item.path">
                <span>{{ item.menuName }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-aside>
        <el-main class="layout-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
  <!-- 修改密码对话框 -->
    <el-dialog
      title="修改密码"
      v-model="passwordDialogVisible"
      width="500px"
      @close="handlePasswordDialogClose"
    >
      <el-form
        :model="passwordForm"
        :rules="passwordFormRules"
        ref="passwordFormRef"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPasswordForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
</template>

<script setup>
import { ref, reactive,onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { changePassword } from '@/api/modules/user'
import defaultAvatar from '@/assets/anon.jpeg'
import { he } from 'element-plus/es/locales.mjs'
const router = useRouter()
const route = useRoute()
const passwordFormRef = ref(null)
const passwordDialogVisible = ref(false)
const currentUser = ref({})

let authMenuList = localStorage.getItem('authMenuList')
if (authMenuList) {
  authMenuList = JSON.parse(authMenuList)
}
const uploadHeaders = {
  Authorization:'Bearer ' + localStorage.getItem('token')
}
const getCurrentUserRole = () => {
  if (currentUser.value && currentUser.value.roles && currentUser.value.roles.length > 0) {
    // 如果用户有多个角色，显示第一个角色的名称
    return currentUser.value.roles[0].roleName || '未知角色'
  }
  return '未知角色'
}
const handleAvatarSuccess= (response,uploadFile
) => {
  if (response.code !== 200) {
    ElMessage.error(response.msg||'头像更新失败')
    return false
  }
  currentUser.value.avatar = response.data
  ElMessage.success('头像更新成功')
  // 更新本地存储的用户信息
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    user.avatar = response.data
    localStorage.setItem('user', JSON.stringify(user))
  }
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
// 修改密码表单数据
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 修改密码表单验证规则
const passwordFormRules = reactive({
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const handleUserCommand = (command) => {
  if (command === 'changePassword') {
    // 打开修改密码对话框
    passwordDialogVisible.value = true
  } else if (command === 'logout') {
    // 退出登录
    handleLogout()
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('authMenuList')
  router.push('/login')
}

// 提交修改密码表单
const submitPasswordForm = async () => {
  passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      // 调用修改密码的API
      const res = await changePassword(currentUser.value.id,passwordForm)
      if (res.code === 200) {
        ElMessage.success('密码修改成功')
        passwordDialogVisible.value = false
        handlePasswordDialogClose()
      }
    } catch (error) {
      console.error(error)
      ElMessage.error('密码修改失败')
    }
  })
}

// 关闭修改密码对话框时重置表单
const handlePasswordDialogClose = () => {
  passwordFormRef.value?.resetFields()
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
}
// 获取当前用户信息
const getCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
}
// 页面加载时初始化
onMounted(() => {
  getCurrentUser()
})
</script>

<style scoped>
.common-layout {
  height: 100vh;
}

.layout-container {
  height: 100%;
}

.layout-header {
  background-color: #409eff;
  color: #fff;
  height: 60px;/*修改*/
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;/*新增*/
  padding: 0 20px;
}
/* 新增*/
.header-actions {
  display: flex;
  align-items: center;
  height: 100%;
}
.user-info {
  display: flex;
  align-items: center;
  margin-right: 20px;
}
.welcome-text {
  color: #fff;
  font-weight: 500;
  margin-right: 10px;
}
.avatar-uploader :deep(.el-upload) {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.username {
  margin-left: 8px;
  color: #fff;
  font-weight: 500;
}
/* 新增*/
.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
.header-title {
  font-size: 24px;
  font-weight: bold;
}

.layout-aside {
  background-color: #545c64;
  color: #fff;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  z-index: 99;
}

.layout-menu {
  border-right: none;
  height: calc(100vh - 60px);
}

.layout-main {
  background-color: #f5f5f5;
  padding: 20px;
  overflow: auto;
}

/* 菜单项悬停效果 */
:deep(.el-menu-item:hover) {
  background-color: #434b55 !important;
}

:deep(.el-sub-menu__title:hover) {
  background-color: #434b55 !important;
}

/* 滚动条样式 */
.layout-main::-webkit-scrollbar {
  width: 6px;
}

.layout-main::-webkit-scrollbar-thumb {
  background-color: #c1c1c1;
  border-radius: 3px;
}

.layout-main::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}
</style>
