<script setup lang="ts">
/**
 * Dashboard — 业务总览
 *  4 指标卡:icon + 主数字 + 趋势 %(从 7-day series 末两日差计算,非 mock)
 *  1 趋势图:ECharts dual-line(新增用户 / 对话数)+ 7d/30d 切换
 *  ponytail: chart dispose on unmount 顺手修 P1-F6(M1)leak
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { User, ChatDotRound, ChatLineRound, Coin, CaretTop, CaretBottom } from '@element-plus/icons-vue'
import { getSummary, getTrends } from '@/api/dashboard'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const summary = ref<any>({})
const trends = ref<any>({ userTrend: [], conversationTrend: [] })
const trendRef = ref<HTMLElement>()
const period = ref<7 | 30>(7)
let chart: echarts.ECharts | null = null

/** 末两日差百分比(无基线返回 null) */
function pctChange(arr: any[], key: string): number | null {
  if (!arr || arr.length < 2) return null
  const last = arr[arr.length - 1]?.[key] ?? 0
  const prev = arr[arr.length - 2]?.[key] ?? 0
  if (prev === 0) return null
  return Math.round(((last - prev) / prev) * 1000) / 10
}

const userPct = computed(() => pctChange(trends.value.userTrend, 'count'))
const convPct = computed(() => pctChange(trends.value.conversationTrend, 'count'))

/** 渲染 ECharts 到容器(防重入) */
function renderChart(): void {
  if (!trendRef.value) return
  if (!chart) chart = echarts.init(trendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '对话数'], right: 16, top: 8 },
    grid: { left: 48, right: 24, top: 48, bottom: 32 },
    xAxis: { type: 'category', boundaryGap: false,
             data: trends.value.userTrend.map((t: any) => t.date) },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef0f3' } } },
    series: [
      { name: '新增用户', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        itemStyle: { color: '#3b6cf0' }, lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(59, 108, 240, 0.10)' },
        data: trends.value.userTrend.map((t: any) => t.count) },
      { name: '对话数', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        itemStyle: { color: '#10b981' }, lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(16, 185, 129, 0.08)' },
        data: trends.value.conversationTrend.map((t: any) => t.count) }
    ]
  })
}

/** 切换 7d / 30d 后重新拉数据并重渲图表 */
async function switchPeriod(v: 7 | 30): Promise<void> {
  period.value = v
  trends.value = (await getTrends(v)).data || { userTrend: [], conversationTrend: [] }
  await new Promise(r => requestAnimationFrame(r))
  renderChart()
}

onMounted(async () => {
  summary.value = (await getSummary()).data || {}
  await switchPeriod(7)
})

/** 路由切走时释放 chart 实例,避免 SPA 内存泄漏 */
onBeforeUnmount(() => {
  if (chart) { chart.dispose(); chart = null }
})
</script>

<template>
  <div class="page-container dashboard">
    <header class="page-header">
      <div>
        <h2>Dashboard</h2>
        <p class="subtitle">业务关键指标总览</p>
      </div>
    </header>

    <el-row :gutter="20" class="metrics">
      <el-col :xs="12" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon metric-icon--blue"><el-icon><User /></el-icon></div>
          <div class="metric-body">
            <div class="metric-num">{{ summary.userCount || 0 }}</div>
            <div class="metric-label">总用户数</div>
            <div v-if="userPct != null" class="metric-trend" :class="userPct >= 0 ? 'up' : 'down'">
              <el-icon><component :is="userPct >= 0 ? CaretTop : CaretBottom" /></el-icon>
              <span>{{ userPct > 0 ? '+' : '' }}{{ userPct }}%</span>
              <span class="vs">较昨日</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon metric-icon--green"><el-icon><ChatDotRound /></el-icon></div>
          <div class="metric-body">
            <div class="metric-num">{{ summary.todayNew || 0 }}</div>
            <div class="metric-label">今日新增</div>
            <div class="metric-trend placeholder">— 实时</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon metric-icon--indigo"><el-icon><ChatLineRound /></el-icon></div>
          <div class="metric-body">
            <div class="metric-num">{{ summary.conversationCount || 0 }}</div>
            <div class="metric-label">对话总数</div>
            <div v-if="convPct != null" class="metric-trend" :class="convPct >= 0 ? 'up' : 'down'">
              <el-icon><component :is="convPct >= 0 ? CaretTop : CaretBottom" /></el-icon>
              <span>{{ convPct > 0 ? '+' : '' }}{{ convPct }}%</span>
              <span class="vs">较昨日</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon metric-icon--amber"><el-icon><Coin /></el-icon></div>
          <div class="metric-body">
            <div class="metric-num">{{ summary.tokenToday || 0 }}</div>
            <div class="metric-label">今日 Token</div>
            <div class="metric-trend placeholder">— 实时</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card class="trend-card" shadow="never">
      <template #header>
        <div class="trend-head">
          <div>
            <div class="trend-title">业务趋势</div>
            <div class="trend-sub">用户增长与对话活跃度</div>
          </div>
          <el-radio-group :model-value="period" size="small" @change="switchPeriod">
            <el-radio-button :value="7">7 天</el-radio-button>
            <el-radio-button :value="30">30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendRef" class="trend-chart"></div>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard { /* 走 page padding(--space-lg)*/ }

.page-header {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: var(--space-lg);
}
.page-header h2 {
  margin: 0; font-size: var(--font-xl); font-weight: 600;
  color: var(--color-text-primary);
}
.subtitle { margin: 4px 0 0; color: var(--color-text-secondary); font-size: var(--font-sm); }

.metrics { margin-bottom: var(--space-lg); }
.metrics .el-col { margin-bottom: var(--space-md); }

/* === metric-card(类名不动 — e2e 数节点)==*/
.metric-card {
  display: flex; gap: var(--space-md); padding: var(--space-lg);
  background: var(--color-bg-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}
.metric-card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }
.metric-icon {
  width: 48px; height: 48px; border-radius: var(--radius-lg);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; flex-shrink: 0;
}
.metric-icon--blue   { background: #eaf0fe; color: var(--color-primary); }
.metric-icon--green  { background: #d1fae5; color: var(--color-success); }
.metric-icon--indigo { background: #e0e7ff; color: var(--color-info); }
.metric-icon--amber  { background: #fef3c7; color: var(--color-warning); }
.metric-body { flex: 1; min-width: 0; }
.metric-num  { font-size: var(--font-2xl); font-weight: 700; color: var(--color-text-primary); line-height: 1.2; }
.metric-label { font-size: var(--font-sm); color: var(--color-text-secondary); margin-top: 2px; }
.metric-trend {
  margin-top: var(--space-sm); font-size: var(--font-xs);
  display: inline-flex; align-items: center; gap: 2px;
}
.metric-trend.up   { color: var(--color-success); }
.metric-trend.down { color: var(--color-danger);  }
.metric-trend.placeholder { color: var(--color-text-tertiary); }
.metric-trend .vs   { color: var(--color-text-tertiary); margin-left: 4px; font-weight: normal; }

/* === trend card === */
.trend-card { border-radius: var(--radius-md); }
.trend-card :deep(.el-card__header) { padding: var(--space-md) var(--space-lg); }
.trend-head { display: flex; justify-content: space-between; align-items: center; }
.trend-title { font-size: var(--font-lg); font-weight: 600; color: var(--color-text-primary); }
.trend-sub   { font-size: var(--font-xs); color: var(--color-text-secondary); margin-top: 2px; }
.trend-chart { height: 360px; }
</style>