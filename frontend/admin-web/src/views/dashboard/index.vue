<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getSummary, getTrends } from '@/api/dashboard'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const summary = ref<any>({})
const trendRef = ref<HTMLElement>()

onMounted(async () => {
  summary.value = (await getSummary()).data || {}
  const trends = (await getTrends(7)).data || { userTrend: [], conversationTrend: [] }
  if (trendRef.value && trends.userTrend) {
    const chart = echarts.init(trendRef.value)
    chart.setOption({
      title: { text: '近 7 天趋势' },
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增用户', '对话数'] },
      xAxis: { type: 'category', data: trends.userTrend.map((t: any) => t.date) },
      yAxis: { type: 'value' },
      series: [
        { name: '新增用户', type: 'line', data: trends.userTrend.map((t: any) => t.count) },
        { name: '对话数', type: 'line', data: trends.conversationTrend.map((t: any) => t.count) }
      ]
    })
  }
})
</script>

<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric">{{ summary.userCount || 0 }}</div>
          <div class="label">总用户数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric">{{ summary.todayNew || 0 }}</div>
          <div class="label">今日新增</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric">{{ summary.conversationCount || 0 }}</div>
          <div class="label">对话总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric">{{ summary.tokenToday || 0 }}</div>
          <div class="label">今日 Token</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top: 20px">
      <div ref="trendRef" style="height: 400px"></div>
    </el-card>
  </div>
</template>