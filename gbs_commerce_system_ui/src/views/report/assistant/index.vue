<template>
  <div class="assistant-visual">
    <el-card class="entry-card">
      <template #header>
        <div class="card-header">
          <span>助手智能图谱沙盘</span>
          <el-tag type="success">ECharts</el-tag>
        </div>
      </template>
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="会员号">
          <el-input
            v-model="queryForm.memberId"
            placeholder="例如：MB20250001"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="generateInsight">
            生成图谱
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        title="助手会实时调用本地 MCP 服务，生成热销商品、销售趋势等图谱供大模型参考。"
        type="info"
        show-icon
      />
    </el-card>

    <el-row :gutter="16" class="chart-grid">
      <el-col :span="10">
        <el-card shadow="hover" class="profile-card">
          <template #header>
            <div class="card-header">
              <span>会员画像</span>
            </div>
          </template>
          <div v-if="currentMember" class="profile-box">
            <p><strong>姓名：</strong>{{ currentMember.name }}</p>
            <p><strong>等级：</strong>{{ currentMember.level }}</p>
            <p><strong>积分：</strong>{{ currentMember.points }}</p>
            <p><strong>余额：</strong>￥{{ formatCurrency(currentMember.balance) }}</p>
          </div>
          <el-empty v-else description="请输入会员号后生成画像" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card v-loading="chartLoading">
          <template #header>
            <div class="card-header">
              <span>热销商品（最近 30 天）</span>
            </div>
          </template>
          <div class="chart-panel" ref="barChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card v-loading="chartLoading">
          <template #header>
            <div class="card-header">
              <span>近 7 天销售雷达</span>
            </div>
          </template>
          <div class="chart-panel" ref="radarChartRef"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>标签建议</span>
            </div>
          </template>
          <el-empty v-if="!personaTags.length" description="暂无标签，请先生成画像" />
          <template v-else>
            <el-tag
              v-for="tag in personaTags"
              :key="tag"
              type="success"
              effect="plain"
              class="persona-tag"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchAssistantInsight, fetchMemberProfile } from '@/api/modules/assistant'

const queryForm = ref({
  memberId: ''
})

const loading = ref(false)
const chartLoading = ref(false)
const currentMember = ref(null)
const personaTags = ref([])

const barChartRef = ref(null)
const radarChartRef = ref(null)
let barChartInstance = null
let radarChartInstance = null

const formatCurrency = (val) => Number(val || 0).toFixed(2)

const generateInsight = async () => {
  if (!queryForm.value.memberId) {
    ElMessage.warning('请输入会员号')
    return
  }
  loading.value = true
  try {
    await loadMemberProfile(queryForm.value.memberId.trim())
    await loadAssistantCharts()
    ElMessage.success('助手图谱已更新')
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '生成图谱失败')
  } finally {
    loading.value = false
  }
}

const loadMemberProfile = async (memberId) => {
  const { data } = await fetchMemberProfile(memberId)
  if (data?.status !== 'success' || !data.data) {
    throw new Error(data?.message || '未查询到会员')
  }
  currentMember.value = data.data
  personaTags.value = buildPersonaTags(data.data)
}

const loadAssistantCharts = async () => {
  chartLoading.value = true
  try {
    const [topRes, trendRes] = await Promise.all([
      fetchAssistantInsight('top_products', { days: 30, limit: 5 }),
      fetchAssistantInsight('sales_trend', { days: 7 })
    ])

    const topData = topRes.data
    if (topData?.status === 'success') {
      const list = topData.data || []
      const categories = list.map(
        (item, idx) => item.product_name || `商品 ${idx + 1}`
      )
      const values = list.map((item) => Number(item.quantity || 0))
      updateBarChart(categories, values)
    } else {
      throw new Error(topData?.message || '获取热销商品失败')
    }

    const trendData = trendRes.data
    if (trendData?.status === 'success') {
      const radar = buildRadarFromTrend(trendData.data || [])
      updateRadarChart(radar)
    } else {
      throw new Error(trendData?.message || '获取销售趋势失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '助手图谱接口异常')
  } finally {
    chartLoading.value = false
  }
}

const buildPersonaTags = (member) => {
  const tags = []
  if (member.level?.includes('钻石') || member.level?.includes('铂金')) {
    tags.push('高价值会员')
  }
  if ((member.points || 0) > 3000) {
    tags.push('高积分')
  }
  if ((member.balance || 0) > 1000) {
    tags.push('高余额')
  }
  if (member.gender === 1) {
    tags.push('偏好数码/饮料')
  } else if (member.gender === 2) {
    tags.push('关注生鲜/家清')
  }
  return tags.length ? tags : ['潜力会员']
}

const buildRadarFromTrend = (trend) => {
  if (!trend.length) {
    return null
  }
  const values = trend.map((item) => Number(item.order_count || 0))
  const maxValue = Math.max(...values, 1)
  return {
    indicators: trend.map((item) => ({
      name: item.date,
      max: Math.ceil(maxValue * 1.2)
    })),
    values
  }
}

const initCharts = () => {
  if (barChartRef.value && !barChartInstance) {
    barChartInstance = echarts.init(barChartRef.value)
  }
  if (radarChartRef.value && !radarChartInstance) {
    radarChartInstance = echarts.init(radarChartRef.value)
  }
}

const updateBarChart = (categories, values) => {
  if (!barChartInstance) return
  barChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: categories },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        barWidth: 24,
        itemStyle: { color: '#5c96ff' },
        data: values
      }
    ]
  })
}

const updateRadarChart = (radar) => {
  if (!radarChartInstance || !radar) {
    radarChartInstance?.clear()
    return
  }
  radarChartInstance.setOption({
    tooltip: {},
    radar: {
      indicator: radar.indicators,
      splitNumber: 4
    },
    series: [
      {
        type: 'radar',
        areaStyle: { opacity: 0.2 },
        lineStyle: { color: '#f56c6c' },
        data: [{ value: radar.values, name: '近 7 天订单数' }]
      }
    ]
  })
}

const resetForm = () => {
  queryForm.value.memberId = ''
  currentMember.value = null
  personaTags.value = []
}

const resizeCharts = () => {
  barChartInstance?.resize()
  radarChartInstance?.resize()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    loadAssistantCharts()
    window.addEventListener('resize', resizeCharts)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  barChartInstance?.dispose()
  radarChartInstance?.dispose()
})
</script>

<style scoped>
.assistant-visual {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.entry-card {
  border-left: 3px solid #67c23a;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-grid {
  align-items: stretch;
}

.chart-panel {
  height: 260px;
}

.profile-box p {
  margin: 6px 0;
}

.persona-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}
</style>
