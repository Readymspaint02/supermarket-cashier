<template>
  <div class="report-sales">
    <el-row :gutter="16" class="summary-row">
      <el-col :span="6" v-for="card in summaryConfigs" :key="card.key">
        <el-card :class="['summary-card', card.class]">
          <p class="summary-title">{{ card.title }}</p>
          <p class="summary-value">{{ card.formatter(overview[card.key]) }}</p>
          <p class="summary-subtitle">{{ card.subtitle }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="时间范围">
          <el-select v-model="selectedDays" style="width: 160px">
            <el-option v-for="option in daysOptions" :key="option" :label="`近 ${option} 天`" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTrend">刷新趋势</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="chart-section">
      <el-col :span="14">
        <el-card v-loading="trendLoading">
          <template #header>
            <div class="card-header">
              <span>最近 {{ selectedDays }} 天销售趋势</span>
              <el-tag size="small" type="info">实时</el-tag>
            </div>
          </template>
          <div class="chart-panel" ref="trendChartRef"></div>
          <el-table :data="trendData" size="small" class="trend-table">
            <el-table-column prop="day" label="日期" width="140" />
            <el-table-column prop="totalSales" label="销售额">
              <template #default="{ row }">
                ￥{{ formatCurrency(row.totalSales) }}
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="120" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>热销商品 Top5</span>
            </div>
          </template>
          <div class="chart-panel" ref="topChartRef"></div>
          <el-table :data="overview.topProducts || []" size="small">
            <el-table-column prop="productName" label="商品名" />
            <el-table-column prop="totalQuantity" label="销量" width="120" />
            <el-table-column prop="totalAmount" label="销售额" width="140">
              <template #default="{ row }">
                ￥{{ formatCurrency(row.totalAmount) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getSalesOverview, getSalesTrend } from '@/api/modules/report'

const overview = ref({
  totalSales: 0,
  totalOrders: 0,
  avgOrderValue: 0,
  totalMembers: 0,
  topProducts: []
})
const trendData = ref([])
const selectedDays = ref(7)
const daysOptions = [7, 14, 30]
const trendLoading = ref(false)

const summaryConfigs = [
  {
    key: 'totalSales',
    title: '总销售额',
    subtitle: '累计销售额（含所有门店）',
    class: 'summary-card--gold',
    formatter: (val) => `￥${formatCurrency(val)}`
  },
  {
    key: 'totalOrders',
    title: '订单数',
    subtitle: '已完成的订单数量',
    class: 'summary-card--blue',
    formatter: (val) => Number(val || 0)
  },
  {
    key: 'avgOrderValue',
    title: '平均客单价',
    subtitle: '销售额 / 订单数',
    class: 'summary-card--green',
    formatter: (val) => `￥${formatCurrency(val)}`
  },
  {
    key: 'totalMembers',
    title: '会员总数',
    subtitle: '注册会员数量',
    class: 'summary-card--purple',
    formatter: (val) => Number(val || 0)
  }
]

const trendChartRef = ref(null)
const topChartRef = ref(null)
let trendChartInstance = null
let topChartInstance = null

const initCharts = () => {
  if (trendChartRef.value && !trendChartInstance) {
    trendChartInstance = echarts.init(trendChartRef.value)
  }
  if (topChartRef.value && !topChartInstance) {
    topChartInstance = echarts.init(topChartRef.value)
  }
  updateTrendChart()
  updateTopChart()
}

const updateTrendChart = () => {
  if (!trendChartInstance) return
  const categories = trendData.value.map((item) => item.day)
  const sales = trendData.value.map((item) => Number(item.totalSales || 0))
  const orders = trendData.value.map((item) => Number(item.orderCount || 0))
  trendChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '订单数'] },
    grid: { left: '3%', right: '4%', top: 50, bottom: 40, containLabel: true },
    xAxis: { type: 'category', data: categories },
    yAxis: [
      { type: 'value', name: '销售额', axisLabel: { formatter: '￥{value}' } },
      { type: 'value', name: '订单数' }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        data: sales,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(80,141,255,0.45)' },
            { offset: 1, color: 'rgba(80,141,255,0.05)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 18,
        itemStyle: { color: '#f7ba1e' },
        data: orders
      }
    ]
  })
}

const updateTopChart = () => {
  if (!topChartInstance) return
  const list = (overview.value.topProducts || []).map((item) => ({
    name: item.productName,
    value: Number(item.totalAmount || 0),
    quantity: item.totalQuantity || 0
  }))
  topChartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => `${params.name}<br/>销售额：￥${formatCurrency(params.value)}<br/>销量：${list[params.dataIndex]?.quantity || 0}`
    },
    series: [
      {
        name: '热销占比',
        type: 'pie',
        radius: ['45%', '70%'],
        data: list,
        label: { formatter: '{b}\n￥{c}' }
      }
    ]
  })
}

const loadOverview = async () => {
  const res = await getSalesOverview()
  overview.value = res.data || overview.value
  nextTick(updateTopChart)
}

const loadTrend = async () => {
  try {
    trendLoading.value = true
    const res = await getSalesTrend({ days: selectedDays.value })
    trendData.value = res.data || []
    nextTick(updateTrendChart)
  } finally {
    trendLoading.value = false
  }
}

const formatCurrency = (val) => {
  const num = Number(val || 0)
  return num.toFixed(2)
}

const resizeCharts = () => {
  trendChartInstance?.resize()
  topChartInstance?.resize()
}

watch(trendData, () => nextTick(updateTrendChart), { deep: true })
watch(
  () => overview.value.topProducts,
  () => nextTick(updateTopChart),
  { deep: true }
)
watch(selectedDays, () => {
  loadTrend()
})

onMounted(() => {
  loadOverview()
  loadTrend()
  nextTick(initCharts)
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChartInstance?.dispose()
  topChartInstance?.dispose()
})
</script>

<style scoped>
.report-sales {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-row {
  margin-bottom: 4px;
}

.summary-card {
  padding: 16px;
  border: none;
  color: #fff;
}

.summary-card--gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
}

.summary-card--blue {
  background: linear-gradient(135deg, #5c96ff, #6dd5fa);
}

.summary-card--green {
  background: linear-gradient(135deg, #42e695, #3bb2b8);
}

.summary-card--purple {
  background: linear-gradient(135deg, #a18cd1, #fbc2eb);
}

.summary-title {
  margin: 0;
  font-size: 14px;
  opacity: 0.8;
}

.summary-value {
  margin: 8px 0 4px;
  font-size: 26px;
  font-weight: 700;
}

.summary-subtitle {
  margin: 0;
  font-size: 12px;
  opacity: 0.8;
}

.filter-card {
  border-left: 3px solid #5c96ff;
}

.filter-form {
  display: flex;
  align-items: center;
}

.chart-section {
  margin-top: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-panel {
  height: 260px;
  margin-bottom: 12px;
}

.trend-table {
  border-top: 1px solid #f0f2f5;
}
</style>
