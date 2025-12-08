<template>
  <div class="member-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>会员管理</span>
          <el-button type="primary" @click="openDialog()">新增会员</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="会员号">
          <el-input v-model="searchForm.memberId" placeholder="会员号"></el-input>
        </el-form-item>
        <el-form-item label="会员姓名">
          <el-input v-model="searchForm.name" placeholder="姓名"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="手机号"></el-input>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="searchForm.level" placeholder="全部" clearable>
            <el-option
              v-for="item in levelOptions"
              :key="item.value"
              :label="`${item.badge} ${item.value}`"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="memberList" v-loading="loading">
        <el-table-column prop="memberId" label="会员号" width="140" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            {{ genderMap[row.gender] || '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="level" label="等级" width="180">
          <template #default="{ row }">
            <span class="level-badge" :style="getLevelBadgeStyle(row.level)">
              {{ getLevelConfig(row.level).badge }}
            </span>
            <span class="level-text">{{ row.level || '未设置' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" />
        <el-table-column prop="balance" label="余额" width="120">
          <template #default="{ row }">
            ￥{{ Number(row.balance || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="member-pagination"
      />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="520px">
      <el-form :model="memberForm" :rules="memberRules" ref="memberFormRef" label-width="90px">
        <el-form-item label="会员号" prop="memberId">
          <el-input v-model="memberForm.memberId" :disabled="!!memberForm.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="memberForm.name" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="memberForm.gender">
            <el-option label="未知" :value="0" />
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="memberForm.age" :min="0" :max="120" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="memberForm.phone" />
        </el-form-item>
        <el-form-item label="会员等级" prop="level">
          <el-select v-model="memberForm.level" placeholder="请选择">
            <el-option
              v-for="item in levelOptions"
              :key="item.value"
              :label="`${item.badge} ${item.value}`"
              :value="item.value"
            >
              <span class="level-badge" :style="badgeInlineStyle(item)">
                {{ item.badge }}
              </span>
              <span class="level-text">{{ item.value }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="积分" prop="points">
          <el-input-number v-model="memberForm.points" :min="0" />
        </el-form-item>
        <el-form-item label="余额" prop="balance">
          <el-input-number
            v-model="memberForm.balance"
            :min="0"
            :precision="2"
            :step="10"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="memberForm.email" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="memberForm.address" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="memberForm.status"
            :active-value="0"
            :inactive-value="1"
            active-text="正常"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="memberForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMember">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMemberPage,
  createMember,
  updateMember,
  deleteMember
} from '@/api/modules/member'

const memberList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增会员')

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const searchForm = reactive({
  memberId: '',
  name: '',
  phone: '',
  level: ''
})

const levelOptions = [
  { value: '普通会员', badge: 'S1', color: '#606266', background: 'rgba(96,98,102,0.18)' },
  { value: '白银会员', badge: 'S2', color: '#5c8def', background: 'rgba(92,141,239,0.2)' },
  { value: '黄金会员', badge: 'S3', color: '#e6a23c', background: 'rgba(230,162,60,0.2)' },
  { value: '铂金会员', badge: 'S4', color: '#7b88ff', background: 'rgba(123,136,255,0.22)' },
  { value: '钻石会员', badge: 'S5', color: '#d864ff', background: 'rgba(216,100,255,0.2)' }
]
const genderMap = {
  0: '未知',
  1: '男',
  2: '女'
}

const getLevelConfig = (level) => {
  return levelOptions.find(item => item.value === level) || levelOptions[0]
}

const getLevelBadgeStyle = (level) => {
  const cfg = getLevelConfig(level)
  return {
    backgroundColor: cfg.background,
    color: cfg.color,
    borderColor: cfg.color
  }
}

const badgeInlineStyle = (config) => ({
  backgroundColor: config.background,
  color: config.color,
  borderColor: config.color,
  marginRight: '8px'
})

const memberFormRef = ref(null)
const memberForm = reactive({
  id: '',
  memberId: '',
  name: '',
  gender: 0,
  age: null,
  phone: '',
  email: '',
  level: levelOptions[0].value,
  points: 0,
  balance: 0,
  address: '',
  status: 0,
  remark: ''
})

const memberRules = {
  memberId: [{ required: true, message: '请输入会员号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const loadMembers = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      queryParams: { ...searchForm }
    }
    const res = await getMemberPage(params)
    if (res.code === 200) {
      memberList.value = res.data.records || []
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadMembers()
}

const handleReset = () => {
  Object.assign(searchForm, {
    memberId: '',
    name: '',
    phone: '',
    level: ''
  })
  pagination.currentPage = 1
  loadMembers()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadMembers()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadMembers()
}

const openDialog = (row) => {
  resetMemberForm()
  if (row) {
    dialogTitle.value = '编辑会员'
    Object.assign(memberForm, row)
  } else {
    dialogTitle.value = '新增会员'
  }
  dialogVisible.value = true
}

const resetMemberForm = () => {
  Object.assign(memberForm, {
    id: '',
    memberId: '',
    name: '',
    gender: 0,
    age: null,
    phone: '',
    email: '',
    level: levelOptions[0].value,
    points: 0,
    balance: 0,
    address: '',
    status: 0,
    remark: ''
  })
}

const submitMember = () => {
  memberFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      let res
      if (memberForm.id) {
        const { id, ...payload } = memberForm
        res = await updateMember(id, payload)
      } else {
        res = await createMember(memberForm)
      }
      if (res.code === 200) {
        ElMessage.success(memberForm.id ? '更新成功' : '新增成功')
        dialogVisible.value = false
        loadMembers()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } catch (error) {
      console.error(error)
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除会员「${row.name}」吗？`, '删除提醒', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteMember(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadMembers()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error(error)
    }
  })
}

onMounted(() => {
  loadMembers()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.member-pagination {
  margin-top: 20px;
  text-align: right;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  padding: 2px 6px;
  border-radius: 12px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  margin-right: 6px;
}

.level-text {
  font-weight: 500;
  color: #303133;
}
</style>
