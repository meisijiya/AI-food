<script setup lang="ts">
/**
 * Token 用量统计 — 按 day/model/user(session) 聚合展示。
 *
 * ponytail: 后端 stats 接口不返回 total,这里用前端 client-side 分页切页
 * (Group 维度本身数量有限,日分组 < 100,模型 < 20,session < 1000 —
 * 切 50/页够用,真要万级再换 server-side)。
 */
import { ref, computed, onMounted } from 'vue'
import { getStats } from '@/api/tokenUsage'

const groupBy = ref('day')
const allStats = ref<any[]>([])
const page = ref(1)
const size = ref(20)

async function load() {
  page.value = 1
  const res: any = await getStats({ groupBy: groupBy.value })
  allStats.value = res.data || []
}

/** 当前页可见的统计数据(按 totalTokens 已经在后端排序过) */
const stats = computed(() => allStats.value.slice((page.value - 1) * size.value, page.value * size.value))
const total = computed(() => allStats.value.length)

onMounted(load)
</script>

<template>
  <div class="page-container">
    <el-card class="page-card">
      <div class="toolbar-row">
        <el-radio-group v-model="groupBy" @change="load">
          <el-radio-button label="day">按日</el-radio-button>
          <el-radio-button label="model">按模型</el-radio-button>
          <el-radio-button label="user">按 session</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="stats" stripe class="stats-table">
        <el-table-column prop="key" label="分组" />
        <el-table-column prop="promptTokens" label="Prompt Tokens" />
        <el-table-column prop="completionTokens" label="Completion Tokens" />
        <el-table-column prop="totalTokens" label="Total Tokens" />
        <el-table-column prop="count" label="记录数" />
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="stats"
        @size-change="stats"
        class="pagination-bar"
      />
    </el-card>
  </div>
</template>

<style scoped>
.page-card {
  border-radius: var(--radius-md);
}
.page-card :deep(.el-card__body) {
  box-shadow: var(--shadow-sm);
}

.toolbar-row {
  margin-bottom: var(--space-md);
}
.stats-table :deep(.el-table__cell) {
  padding: var(--space-sm) 0;
}

.pagination-bar {
  margin-top: var(--space-md);
  justify-content: flex-end;
  display: flex;
}
</style>