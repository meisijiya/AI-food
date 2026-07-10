<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listRecommendations } from '@/api/recommendation'
import SearchForm from '@/components/SearchForm.vue'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
// ponytail: RecommendationResult 只有 sessionId / mode / foodName 字段,无 userId / accepted
const query = ref({
  page: 1,
  size: 20,
  sessionId: '' as string,
  foodName: '' as string
})

async function load() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.sessionId) params.sessionId = query.value.sessionId
    if (query.value.foodName) params.foodName = query.value.foodName
    const res: any = await listRecommendations(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.value.page = 1
  load()
}

function onReset() {
  query.value = { page: 1, size: 20, sessionId: '', foodName: '' }
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="会话 ID">
        <el-input v-model="query.sessionId" placeholder="精确匹配" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="食物名">
        <el-input v-model="query.foodName" placeholder="模糊匹配" clearable style="width: 180px" />
      </el-form-item>
    </SearchForm>

    <el-card class="page-card">
      <div class="total-line">
        共 <b class="total-num">{{ total }}</b> 条推荐记录
      </div>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="sessionId" label="会话 ID" width="220" show-overflow-tooltip />
        <el-table-column prop="foodName" label="推荐食物" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reason" label="推荐理由" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="load"
        @size-change="load"
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

.total-line {
  margin-bottom: var(--space-md);
  color: var(--color-text-secondary);
  font-size: var(--font-base);
}
.total-num { color: var(--color-primary); font-weight: 600; }
.placeholder { color: var(--color-text-tertiary); }

.pagination-bar {
  margin-top: var(--space-md);
  justify-content: flex-end;
  display: flex;
}
</style>
