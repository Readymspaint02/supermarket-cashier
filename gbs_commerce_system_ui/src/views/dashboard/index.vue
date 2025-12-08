<template>
  <div class="dashboard-modern">
    <el-card class="hero-card" shadow="hover">
      <div class="hero-left">
        <p class="hero-greeting">尊敬的 {{ displayName }}，{{ greeting }}</p>
        
        <p class="hero-subtitle">
          当前角色：<strong>{{ roleLabel }}</strong>。您可以随时进入智能收银、会员档案、库存看板等常用模块。
        </p>
        <div class="hero-buttons">
          <el-button type="primary" @click="openRemote('checkout')">开始收银</el-button>
          <el-button plain @click="openRemote('member')">会员管理</el-button>
          <el-button plain @click="openRemote('stock')">库存查询</el-button>
        </div>
      </div>
      <div class="hero-right">
        <el-statistic title="今日待办" :value="mockStats.todo" />
        <el-statistic title="会员咨询" :value="mockStats.member" />
        <el-statistic title="库存预警" :value="mockStats.stock" />
      </div>
    </el-card>

    <el-row :gutter="16" class="quick-grid">
      <el-col v-for="item in quickEntries" :key="item.title" :xs="24" :md="8">
        <el-card shadow="hover" class="quick-card" @click="item.action && openRemote(item.action)">
          <div class="quick-icon" :style="{ background: item.bg }">
            <el-icon :size="20"><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <p class="quick-title">{{ item.title }}</p>
            <p class="quick-desc">{{ item.desc }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>常用能力</span>
              <el-tag type="success" effect="plain">智能体驱动</el-tag>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, idx) in capabilityList"
              :key="idx"
              :timestamp="item.scene"
              placement="top"
            >
              <h4>{{ item.title }}</h4>
              <p>{{ item.desc }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>操作指南</span>
              <el-tag effect="plain">快捷提示</el-tag>
            </div>
          </template>
          <ul class="guide-list">
            <li v-for="(item, idx) in guideList" :key="idx">
              <el-icon><CircleCheckFilled /></el-icon>
              <div>
                <p class="guide-title">{{ item.title }}</p>
                <p class="guide-desc">{{ item.desc }}</p>
              </div>
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  ShoppingCartFull,
  CollectionTag,
  DataAnalysis,
  CircleCheckFilled,
} from '@element-plus/icons-vue'

const ORIGINAL = window?.ORIGINAL_URL || window?.originalUrl || null
const baseOrigin = ORIGINAL || window?.location?.origin || ''
const REMOTE_URLS = {
  checkout: '/checkout',
  member: '/member',
  stock: '/stock',
}

const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

const displayName = computed(() => userInfo.nickname || userInfo.username || '管理员')
const roleLabel = computed(() => {
  const { roles, roleName, role } = userInfo
  if (Array.isArray(roles) && roles.length) {
    return roles
      .map((item) => (typeof item === 'string' ? item : item?.roleName || item?.name || ''))
      .filter(Boolean)
      .join(' / ')
  }
  return roleName || role || '超级管理员'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const mockStats = {
  todo: 3,
  member: 12,
  stock: 2,
}

const quickEntries = [
  {
    title: '智能收银',
    desc: '快速启动副屏收银流程',
    action: 'checkout',
    icon: ShoppingCartFull,
    bg: 'rgba(64, 158, 255, 0.15)',
  },
  {
    title: '会员档案',
    desc: '查询积分、余额与活跃度',
    action: 'member',
    icon: CollectionTag,
    bg: 'rgba(103, 194, 58, 0.15)',
  },
  {
    title: '库存看板',
    desc: '显示库存查询、库存预警',
    action: 'stock',
    icon: DataAnalysis,
    bg: 'rgba(230, 162, 60, 0.15)',
  },
]

const capabilityList = [
  { scene: '会员洞察', title: '精准积分 & 储值', desc: '实时查询会员积分、余额，并联动营销工作流快速响应。' },
  { scene: '语音驱动', title: '语音播报 & 语音指令', desc: '摄像头与语音识别双模态，支持免动手的收银体验。' },
]

const guideList = [
  { title: '如何快速接待会员？', desc: '在副屏上唤醒“会员助手”，即可按手机号或会员号快速查询。' },
  { title: '想查看今日经营指标？', desc: '进入“报表中心-销售概览”即可获取实时订单、支付与热销榜。' },
]

const openRemote = (key) => {
  const path = REMOTE_URLS[key]
  if (!path || !baseOrigin) return
  window.location.href = `${baseOrigin}${path}`
}
</script>

<style scoped>
.dashboard-modern {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.hero-left {
  flex: 1;
}

.hero-greeting {
  margin: 0;
  color: #409EFF;
  font-size: 20px;
}

.hero-subtitle {
  margin: 8px 0 16px;
  color: #606266;
}

.hero-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 200px;
  justify-content: center;
}

.quick-grid {
  margin-bottom: 8px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
}

.quick-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}

.quick-title {
  margin: 0;
  font-weight: 600;
  color: #303133;
}

.quick-desc {
  margin: 0;
  color: #909399;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.guide-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.guide-title {
  margin: 0;
  font-weight: 600;
  color: #303133;
}

.guide-desc {
  margin: 4px 0 0;
  color: #909399;
}
</style>
